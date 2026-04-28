package nebula.project

import nebula.test.dsl.*
import nebula.test.dsl.TestKitAssertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File

internal class NebulaIntegTestPluginTest {
    @TempDir
    lateinit var projectDir: File

    @ParameterizedTest
    @EnumSource(SupportedGradleVersion::class)
    fun test(gradle: SupportedGradleVersion) {
        val runner = testProject(projectDir) {
            properties {
                buildCache(true)
                configurationCache(true)
            }
            rootProject {
                plugins {
                    id("java-library")
                    id("com.netflix.nebula.integtest")
                }
                repositories {
                    mavenCentral()
                }
                dependencies("""testImplementation("junit:junit:latest.release")""")
                src {
                    sourceSet("integTest") {
                        junit4Test()
                        language("resources", "integTest.properties", "")
                    }
                }
            }
        }

        val result = runner.run("check") {
            if(gradle.version != null) {
                withGradleVersion(gradle.version)
            }
        }
        assertThat(result)
            .hasNoMutableStateWarnings()
            .hasNoDeprecationWarnings()
        assertThat(result.task(":test"))
            .hasOutcome(TaskOutcome.NO_SOURCE)
        assertThat(result.task(":integrationTest"))
            .`as`("check depends on integrationTest")
            .hasOutcome(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE)
        assertThat(projectDir.resolve("build/classes/java/integTest/nebula/HelloWorldTest.class"))
            .`as`("compiles integration test classes")
            .exists()
        assertThat(projectDir.resolve("build/resources/integTest/integTest.properties"))
            .`as`("copies integTest resources")
            .exists()
        assertThat(projectDir.resolve("build/integTest-results/TEST-nebula.HelloWorldTest.xml"))
            .`as`("produces integTest xml test report")
            .exists()
        assertThat(projectDir.resolve("build/reports/integTest/index.html"))
            .`as`("produces integTest html test report")
            .exists()
    }
}