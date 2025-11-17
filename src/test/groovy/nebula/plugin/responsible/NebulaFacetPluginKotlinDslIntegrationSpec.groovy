package nebula.plugin.responsible

/**
 * Integration test using REAL Kotlin DSL (build.gradle.kts) to verify
 * the plugin works correctly with Kotlin build scripts and the programmatic API.
 *
 * This demonstrates that users can create facets programmatically from Kotlin DSL
 * build scripts using idiomatic Kotlin lambda syntax.
 */
class NebulaFacetPluginKotlinDslIntegrationSpec extends BaseIntegrationTestKitSpec {

    def 'create test facet from Kotlin DSL with lambda syntax'() {
        given:
        // Use actual build.gradle.kts (Kotlin DSL) with Java source
        // This is a common real-world scenario: Kotlin build scripts with Java code
        def buildFileKts = new File(projectDir, 'build.gradle.kts')
        buildFileKts.text = '''
            plugins {
                java
                id("com.netflix.nebula.facet")
            }

            import nebula.plugin.responsible.NebulaFacetPlugin

            val facetPlugin = project.plugins.getPlugin(NebulaFacetPlugin::class.java)

            // Idiomatic Kotlin lambda syntax - no Groovy closures required!
            facetPlugin.createTestFacet("integTest") {
                testTaskName = "integrationTest"
                parentSourceSet = "test"
                includeInCheckLifecycle = true
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation("junit:junit:4.13.2")
            }
        '''.stripIndent()

        // Java test source (common scenario: Kotlin DSL + Java source)
        writeTest('src/integTest/java/', 'com.example', false)

        when:
        def result = runTasks('integrationTest')

        then:
        result.task(':integrationTest')
        fileExists('build/classes/java/integTest/com/example/HelloWorldTest.class')
    }

    boolean fileExists(String path) {
        new File(projectDir, path).exists()
    }
}
