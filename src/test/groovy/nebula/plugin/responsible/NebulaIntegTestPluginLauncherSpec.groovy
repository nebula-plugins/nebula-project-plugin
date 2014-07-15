package nebula.plugin.responsible

import nebula.test.IntegrationSpec

/**
 * Runs Gradle Launcher style integration Spock tests on the NebulaIntegTestPlugin class
 */
class NebulaIntegTestPluginLauncherSpec extends IntegrationSpec {

    String fakePackage = "nebula"

    def setup() {
        writeTest( 'src/integTest/java/', fakePackage, false )
        writeResource( 'src/integTest/resources', 'integTest' )
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

    def "Can configures Idea project"() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['log4j:log4j:1.2.17', 'mysql:mysql-connector-java:5.1.27'])

        buildFile << """
apply plugin: 'idea'

repositories {
    maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
}

dependencies {
    integTestCompile 'log4j:log4j:1.2.17'
    integTestRuntime 'mysql:mysql-connector-java:5.1.27'
}
"""

        writeHelloWorld('nebula.plugin.plugin')
        writeTest("src/$NebulaIntegTestPlugin.FACET_NAME/java/", 'nebula.plugin.plugin', false)
        runTasksSuccessfully('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def testSourceFolders = moduleXml.component.content.sourceFolder.findAll { it.@isTestSource.text() == 'true' }
        def testSourceFolder = testSourceFolders.find { it.@url.text() == "file://\$MODULE_DIR\$/src/$NebulaIntegTestPlugin.FACET_NAME/java" }
        testSourceFolder
        def orderEntries = moduleXml.component.orderEntry.findAll { it.@type.text() == 'module-library' && it.@scope.text() == 'TEST' }
        def junitLibrary = orderEntries.find { it.library.CLASSES.root.@url.text().contains('log4j-1.2.17.jar') }
        junitLibrary
        def mysqlLibrary = orderEntries.find { it.library.CLASSES.root.@url.text().contains('mysql-connector-java-5.1.27.jar') }
        mysqlLibrary
    }
}
