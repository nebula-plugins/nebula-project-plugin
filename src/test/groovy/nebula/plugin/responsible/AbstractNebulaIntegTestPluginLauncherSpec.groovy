package nebula.plugin.responsible

import nebula.test.IntegrationTestKitSpec

/**
 * Runs Gradle Launcher style integration Spock tests on the NebulaIntegTestPlugin class
 */
abstract class AbstractNebulaIntegTestPluginLauncherSpec extends IntegrationTestKitSpec {

    String fakePackage = "nebula"

    abstract String getPluginId()

    def setup() {
        writeTest('src/integTest/java/', fakePackage, false)
        writeResource('src/integTest/resources', 'integTest')
        buildFile << """
            plugins {
                id 'java'
                id '${getPluginId()}'
            }
            apply plugin: 'java'

            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation 'junit:junit:latest.release'
            }
        """
        // Enable configuration cache :)
        new File(projectDir, 'gradle.properties') << '''org.gradle.configuration-cache=true'''.stripIndent()

    }

    def "compiles integration test classes"() {
        when:
        runTasks('integrationTest')

        then:
        fileExists("build/classes/java/integTest/$fakePackage/HelloWorldTest.class")
    }

    def "copies integTest resources"() {
        when:
        runTasks('integrationTest')

        then:
        fileExists('build/resources/integTest/integTest.properties')
    }

    def "runs the integration tests"() {
        when:
        runTasks('integrationTest')

        then:
        fileExists("build/integTest-results/TEST-${fakePackage}.HelloWorldTest.xml")
    }

    def "builds the integration test report"() {
        when:
        runTasks('integrationTest')

        then:
        fileExists('build/reports/integTest/index.html')
    }

    def "Can configures Idea project"() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])
        //  IDEA plugin does not support configuration cache
        new File(projectDir, 'gradle.properties').text = '''org.gradle.configuration-cache=false'''.stripIndent()

        buildFile << """
            apply plugin: 'idea'

            repositories {
                maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
            }

            dependencies {
                integTestImplementation 'foo:bar:2.4'
                integTestRuntimeOnly 'custom:baz:5.1.27'
            }
        """

        writeHelloWorld('nebula.plugin.plugin')
        writeTest("src/$NebulaIntegTestPlugin.FACET_NAME/java/", 'nebula.plugin.plugin', false)
        runTasks('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def testSourceFolders = moduleXml.component.content.sourceFolder.findAll { it.@isTestSource.text() == 'true' }
        def testSourceFolder = testSourceFolders.find {
            it.@url.text() == "file://\$MODULE_DIR\$/src/$NebulaIntegTestPlugin.FACET_NAME/java"
        }
        testSourceFolder
        def orderEntries = moduleXml.component.orderEntry.findAll {
            it.@type.text() == 'module-library' && it.@scope.text() == 'TEST'
        }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('bar-2.4.jar') }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('baz-5.1.27.jar') }
    }

    boolean fileExists(String path) {
        new File(projectDir, path).exists()
    }
}
