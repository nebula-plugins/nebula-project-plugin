package nebula.project

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import javax.xml.parsers.DocumentBuilderFactory

/** Bounded coverage for locally applied subplugins, independent of the project facade. */
internal class StandalonePluginsIsolatedProjectsTest {
    @TempDir
    lateinit var projectDir: File

    data class Mode(val gradle: SupportedGradleVersion, val isolated: Boolean) {
        override fun toString() = "${gradle.version ?: "wrapper"}-${if (isolated) "isolated" else "configuration-cache"}"
    }

    data class IntegrationCase(val mode: Mode, val standalone: Boolean) {
        val pluginId = "com.netflix.nebula.integtest" + if (standalone) "-standalone" else ""
        override fun toString() = "$pluginId-$mode"
    }

    companion object {
        @JvmStatic
        fun modes(): Stream<Mode> = SupportedGradleVersion.entries.flatMap { version ->
            listOf(Mode(version, false), Mode(version, true))
        }.stream()

        @JvmStatic
        fun integrationCases(): Stream<IntegrationCase> = modes().flatMap { mode ->
            Stream.of(IntegrationCase(mode, false), IntegrationCase(mode, true))
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modes")
    fun `root facets preserve parent outputs renamed tests and lifecycle opt outs`(mode: Mode) {
        settings()
        write("gradle.properties", "includeFunctional=true\n")
        write("build.gradle", """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }
            $junit
            facets {
                examples
                functionalTest {
                    parentSourceSet = 'test'
                    testTaskName = 'verifyFunctional'
                    includeInCheckLifecycle = providers.gradleProperty('includeFunctional').get().toBoolean()
                }
                smokeTest {
                    parentSourceSet = 'functionalTest'
                    includeInCheckLifecycle = false
                }
            }
            tasks.register('unusedTest', Test) { throw new GradleException('unused task realized') }
        """)
        write("src/main/java/Value.java", "public class Value { public static int get() { return 42; } }")
        write("src/test/java/Support.java", "public class Support { public static int value() { return Value.get(); } }")
        writeTest("", "test", "UnitTest", "org.junit.Assert.assertEquals(42, Support.value());")
        write("src/examples/java/Example.java", "public class Example { int value = Value.get(); }")
        write("src/examples/resources/example.txt", "example-resource")
        write("src/test/resources/parent.txt", "parent-resource")
        writeTest("", "functionalTest", "FunctionalTest", """
            org.junit.Assert.assertEquals(42, Support.value());
            assertResource("parent.txt", "parent-resource");
        """)
        // Calling a parent test proves the whole main -> test -> functionalTest output chain.
        writeTest("", "smokeTest", "SmokeTest", "new FunctionalTest().verifiesBehavior();")

        val first = run(mode, "build")
        stored(first)
        executed(first, ":test", ":verifyFunctional", ":compileExamplesJava", ":processExamplesResources", ":compileSmokeTestJava")
        absent(first, ":functionalTest", ":smokeTest", ":unusedTest")
        facetOutputs()
        report("", "test", "UnitTest")
        report("", "functionalTest", "FunctionalTest")
        val warm = run(mode, "build")
        reused(warm)
        upToDate(warm, ":verifyFunctional", ":compileExamplesJava", ":processExamplesResources")

        // Regenerate outputs using the saved task graph, with the build cache disabled.
        deleteBuildOutputs("")
        val regenerated = run(mode, "build")
        reused(regenerated)
        executed(regenerated, ":test", ":verifyFunctional", ":compileExamplesJava", ":processExamplesResources")
        facetOutputs()
        report("", "functionalTest", "FunctionalTest")

        write("gradle.properties", "includeFunctional=false\n")
        val optedOut = run(mode, "build")
        stored(optedOut)
        absent(optedOut, ":verifyFunctional", ":functionalTest", ":smokeTest")
        reused(run(mode, "build"))

        write("src/examples/java/Example.java", "public class Example { int value = Value.get() + 1; }")
        write("src/examples/resources/example.txt", "changed-example-resource")
        val sourceChanged = run(mode, "build")
        reused(sourceChanged)
        executed(sourceChanged, ":compileExamplesJava", ":processExamplesResources")
        assertEquals("changed-example-resource", file("build/resources/examples/example.txt").readText())
        absent(sourceChanged, ":verifyFunctional", ":smokeTest")

        val explicit = run(mode, "smokeTest")
        stored(explicit)
        executed(explicit, ":smokeTest")
        absent(explicit, ":verifyFunctional") // Parent classes are needed; its Test task is not.
        report("", "smokeTest", "SmokeTest")
        val explicitWarm = run(mode, "smokeTest")
        reused(explicitWarm)
        upToDate(explicitWarm, ":smokeTest")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("integrationCases")
    fun `nested local integration plugins preserve dependency outputs and cache inputs`(case: IntegrationCase) {
        val mode = case.mode
        settings("include 'libraries:producer', 'services:app', 'unrelated'")
        for (path in listOf("", "libraries/", "services/", "unrelated/")) write("${path}build.gradle", "")
        write("libraries/producer/build.gradle", "plugins { id 'java-library' }")
        producer(42)
        write("libraries/producer/src/main/resources/producer.txt", "producer-resource")
        write("services/app/build.gradle", """
            plugins {
                id '${case.pluginId}'
                id 'java'
            }
            $junit
            dependencies { implementation project(':libraries:producer') }
            tasks.register('unusedTest', Test) { throw new GradleException('unused task realized') }
        """)
        write("services/app/src/main/java/Application.java", "public class Application { public static int value() { return Library.value(); } }")
        write("services/app/src/test/java/Support.java", "public class Support { public static int value() { return Application.value(); } }")
        writeTest("services/app/", "test", "UnitTest", "org.junit.Assert.assertTrue(Support.value() > 0);")
        write("services/app/src/main/resources/application.txt", "application-resource")
        write("services/app/src/test/resources/parent.txt", "parent-resource")
        write("services/app/src/integTest/resources/integration.txt", "integration-resource")
        integrationTest(42, "integration-resource")

        val check = run(mode, ":services:app:check")
        stored(check)
        executed(check, ":services:app:test")
        report("services/app/", "test", "UnitTest")
        absent(check, ":services:app:unusedTest")
        if (case.standalone) {
            absent(check, ":services:app:integrationTest", ":services:app:compileIntegTestJava", ":services:app:processIntegTestResources")
            assertFalse(file("services/app/build/integTest-results/TEST-IntegrationTest.xml").exists())
        } else {
            executed(check, ":services:app:integrationTest")
            integrationOutputs()
        }
        val warmCheck = run(mode, ":services:app:check")
        reused(warmCheck)
        upToDate(warmCheck, ":services:app:test")
        if (case.standalone) absent(warmCheck, ":services:app:integrationTest")

        // Keep check selected so the standalone opt-out remains observable in later runs.
        val tasks = if (case.standalone) arrayOf(":services:app:check", ":services:app:integrationTest")
            else arrayOf(":services:app:check")
        if (case.standalone) {
            val explicit = run(mode, *tasks)
            stored(explicit)
            executed(explicit, ":services:app:integrationTest")
            integrationOutputs()
            val warm = run(mode, *tasks)
            reused(warm)
            upToDate(warm, ":services:app:integrationTest")
        }

        deleteBuildOutputs("libraries/producer/", "services/app/")
        val regenerated = run(mode, *tasks)
        reused(regenerated)
        executed(regenerated, ":libraries:producer:compileJava", ":libraries:producer:jar",
            ":services:app:compileIntegTestJava", ":services:app:processIntegTestResources", ":services:app:integrationTest")
        integrationOutputs()

        // Explicit container creation is deliberate: this does not exercise bare child DSL.
        file("services/app/build.gradle").appendText("\n" + """
            facets.create('smokeTest') {
                parentSourceSet = 'integTest'
                testTaskName = 'verifySmoke'
            }
        """.trimIndent())
        smokeTest(42)
        val configured = run(mode, *tasks)
        stored(configured)
        executed(configured, ":services:app:verifySmoke")
        absent(configured, ":services:app:smokeTest")
        report("services/app/", "smokeTest", "SmokeTest")
        val configuredWarm = run(mode, *tasks)
        reused(configuredWarm)
        upToDate(configuredWarm, ":services:app:verifySmoke")
        if (case.standalone) {
            // The newly added facet joins check, but must not pull in its parent's Test task.
            val checkAfterCreation = run(mode, ":services:app:check")
            stored(checkAfterCreation)
            absent(checkAfterCreation, ":services:app:integrationTest")
            assertNotNull(checkAfterCreation.task(":services:app:verifySmoke"))
            reused(run(mode, ":services:app:check"))
        }

        producer(43)
        integrationTest(43, "changed-integration-resource")
        smokeTest(43)
        write("services/app/src/integTest/resources/integration.txt", "changed-integration-resource")
        val sourceChanged = run(mode, *tasks)
        reused(sourceChanged)
        executed(sourceChanged, ":libraries:producer:compileJava", ":libraries:producer:jar",
            ":services:app:compileIntegTestJava", ":services:app:processIntegTestResources",
            ":services:app:integrationTest", ":services:app:verifySmoke")
        integrationOutputs("changed-integration-resource")
        report("services/app/", "smokeTest", "SmokeTest")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SupportedGradleVersion::class)
    fun `bare child facet syntax retains its isolation boundary`(gradle: SupportedGradleVersion) {
        settings("include 'child'")
        write("build.gradle", "")
        write("child/build.gradle", """
            plugins { id 'java'; id 'com.netflix.nebula.facet' }
            facets { examples }
        """)
        write("child/src/examples/java/Example.java", "public class Example {}")
        val ordinary = Mode(gradle, false)
        val first = run(ordinary, ":child:build")
        stored(first)
        executed(first, ":child:compileExamplesJava")
        reused(run(ordinary, ":child:build"))

        // Expected failure is a boundary characterization, never compatibility evidence.
        val rejected = run(Mode(gradle, true), ":child:build", expectFailure = true)
        assertTrue(rejected.output.contains("cannot dynamically look up a property in the parent project"), rejected.output)
        assertTrue(rejected.output.contains("Build file 'child/build.gradle'"), rejected.output)
        assertTrue(rejected.output.contains("Configuration cache entry discarded"), rejected.output)
        assertFalse(rejected.output.contains("Configuration cache entry stored"), rejected.output)
        assertFalse(rejected.output.contains("Configuration cache entry reused"), rejected.output)
    }

    private fun producer(value: Int) = write("libraries/producer/src/main/java/Library.java",
        "public class Library { public static int value() { return $value; } }")

    private fun integrationTest(value: Int, resource: String) = writeTest("services/app/", "integTest", "IntegrationTest", """
        org.junit.Assert.assertEquals($value, Support.value());
        org.junit.Assert.assertEquals($value, Library.value());
        assertResource("producer.txt", "producer-resource");
        assertResource("application.txt", "application-resource");
        assertResource("parent.txt", "parent-resource");
        assertResource("integration.txt", "$resource");
    """)

    private fun smokeTest(value: Int) = writeTest("services/app/", "smokeTest", "SmokeTest", """
        new IntegrationTest().verifiesBehavior();
        org.junit.Assert.assertEquals($value, Support.value());
    """)

    private fun integrationOutputs(resource: String = "integration-resource") {
        assertTrue(file("services/app/build/classes/java/integTest/IntegrationTest.class").isFile)
        assertEquals(resource, file("services/app/build/resources/integTest/integration.txt").readText())
        report("services/app/", "integTest", "IntegrationTest")
    }

    private fun facetOutputs() {
        assertTrue(file("build/classes/java/examples/Example.class").isFile)
        assertTrue(file("build/classes/java/functionalTest/FunctionalTest.class").isFile)
        assertTrue(file("build/classes/java/smokeTest/SmokeTest.class").isFile)
        assertEquals("example-resource", file("build/resources/examples/example.txt").readText())
    }

    private fun settings(includes: String = "") = write("settings.gradle", "rootProject.name = 'fixture'\n$includes\n")
    private fun file(path: String) = projectDir.resolve(path)

    private fun write(path: String, content: String) {
        file(path).apply { parentFile.mkdirs(); writeText(content.trimIndent()) }
    }

    private fun writeTest(prefix: String, sourceSet: String, name: String, body: String) =
        write("${prefix}src/$sourceSet/java/$name.java", """
            public class $name {
                @org.junit.Test public void verifiesBehavior() throws Exception { $body }
                private static void assertResource(String path, String expected) throws Exception {
                    try (java.io.InputStream input = $name.class.getResourceAsStream("/" + path)) {
                        org.junit.Assert.assertNotNull(path, input);
                        org.junit.Assert.assertEquals(expected, new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
            }
        """)

    private fun deleteBuildOutputs(vararg prefixes: String) = prefixes.forEach { prefix ->
        assertTrue(file("${prefix}build").deleteRecursively(), "Could not delete fixture outputs for $prefix")
    }

    private fun run(mode: Mode, vararg tasks: String, expectFailure: Boolean = false): BuildResult {
        // These versions predate the stable --isolated-projects flag (Gradle 9.7).
        val runner = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath()
            .withArguments(*tasks, "--configuration-cache", "--configuration-cache-problems=fail",
                "-Dorg.gradle.unsafe.isolated-projects=${mode.isolated}", "--no-build-cache",
                "--stacktrace", "--console=plain", "--max-workers=2", "--no-watch-fs")
        mode.gradle.version?.let { runner.withGradleVersion(it) }
        return if (expectFailure) runner.buildAndFail() else runner.build()
    }

    private fun stored(result: BuildResult) {
        assertTrue(result.output.contains("Configuration cache entry stored"), result.output)
        assertFalse(result.output.contains("Configuration cache entry reused"), result.output)
        assertFalse(result.output.contains("Configuration cache entry discarded"), result.output)
    }

    private fun reused(result: BuildResult) {
        assertTrue(result.output.contains("Configuration cache entry reused"), result.output)
        assertFalse(result.output.contains("Configuration cache entry stored"), result.output)
        assertFalse(result.output.contains("Configuration cache entry discarded"), result.output)
    }

    private fun executed(result: BuildResult, vararg paths: String) = outcomes(result, TaskOutcome.SUCCESS, *paths)
    private fun upToDate(result: BuildResult, vararg paths: String) = outcomes(result, TaskOutcome.UP_TO_DATE, *paths)
    private fun outcomes(result: BuildResult, outcome: TaskOutcome, vararg paths: String) = paths.forEach {
        assertEquals(outcome, result.task(it)?.outcome, result.output)
    }

    private fun absent(result: BuildResult, vararg paths: String) = paths.forEach {
        assertNull(result.task(it), "Unexpected task $it\n${result.output}")
    }

    private fun report(prefix: String, sourceSet: String, testClass: String) {
        val directory = if (sourceSet == "test") "test-results/test" else "$sourceSet-results"
        val report = file("${prefix}build/$directory/TEST-$testClass.xml")
        val suite = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report).documentElement
        assertEquals("1", suite.getAttribute("tests"), report.path)
        for (attribute in listOf("failures", "errors", "skipped")) assertEquals("0", suite.getAttribute(attribute), report.path)
        assertEquals("verifiesBehavior", suite.getElementsByTagName("testcase").item(0).attributes.getNamedItem("name").nodeValue)
        val htmlDirectory = if (sourceSet == "test") "tests/test" else sourceSet
        val html = file("${prefix}build/reports/$htmlDirectory/index.html")
        assertTrue(html.isFile, html.path)
    }

    private val junit = """
        repositories { mavenCentral() }
        dependencies { testImplementation 'junit:junit:4.13.2' }
    """.trimIndent()
}
