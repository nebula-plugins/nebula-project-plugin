package nebula.plugin.responsible

import nebula.test.IntegrationSpec
import org.gradle.api.logging.LogLevel

/**
 * Runs Gradle Launcher style integration Spock tests on the NebulaIntegTestPlugin class
 */
class NebulaIntegTestPluginLauncherSpec extends IntegrationSpec {

    String fakePackage = "netflix"

    def setup() {
        writeTest( 'src/integTest/java', fakePackage )
        writeResource( 'src/integTest/resources', 'integTest.properties' )
        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(NebulaIntegTestPlugin)}

            repositories {
                mavenCentral()
            }

            dependencies {
                testCompile 'junit:junit-dep:latest.release'
            }

            """.stripIndent()
    }

    def "compiles integration test classes"() {
        when:
        runTasksSuccessfully( 'integrationTest' )

        then:
        fileExists("build/classes/integTest/$fakePackage/HelloWorldTest.class")
    }

    def "copies integTest resources"() {
        when:
        runTasksSuccessfully( 'integrationTest' )

        then:
        fileExists('build/resources/integTest/integTest.properties')
    }

    def "runs the integration tests"() {
        when:
        runTasksSuccessfully( 'integrationTest' )

        then:
        fileExists("build/integTest-results/TEST-${fakePackage}.HelloWorldTest.xml")
    }

    def "builds the integration test report"() {
        when:
        runTasksSuccessfully( 'integrationTest' )

        then:
        fileExists('build/reports/integTest/index.html')
    }
}
