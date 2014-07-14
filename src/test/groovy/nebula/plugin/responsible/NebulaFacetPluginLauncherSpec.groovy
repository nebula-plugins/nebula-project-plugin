package nebula.plugin.responsible

import nebula.test.IntegrationSpec

class NebulaFacetPluginLauncherSpec extends IntegrationSpec {
    def 'tasks get run'() {
        createFile('src/examples/java/Hello.java') << 'public class Hello {}'

        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(NebulaIntegTestPlugin)}
            facets {
                example
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully( 'build' )

        then:
        result.wasExecuted(':exampleClasses')
    }

    def "Configures Idea project files for a custom test facet"() {
        when:
        buildFile << """
apply plugin: 'java'
${applyPlugin(NebulaFacetPlugin)}
apply plugin: 'idea'

facets {
    functionalTest
}

repositories {
    mavenCentral()
}

dependencies {
    functionalTestCompile 'junit:junit:4.8.2'
    functionalTestRuntime 'mysql:mysql-connector-java:5.1.27'
}
"""

        writeHelloWorld('nebula.plugin.plugin')
        writeTest('src/functionalTest/java/', 'nebula.plugin.plugin', false)
        runTasksSuccessfully('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def testSourceFolders = moduleXml.component.content.sourceFolder.findAll { it.@isTestSource.text() == 'true' }
        def testSourceFolder = testSourceFolders.find { it.@url.text() == "file://\$MODULE_DIR\$/src/functionalTest/java" }
        testSourceFolder
        def orderEntries = moduleXml.component.orderEntry.findAll { it.@type.text() == 'module-library' && it.@scope.text() == 'TEST' }
        def junitLibrary = orderEntries.find { it.library.CLASSES.root.@url.text().contains('junit-4.8.2.jar') }
        junitLibrary
        def mysqlLibrary = orderEntries.find { it.library.CLASSES.root.@url.text().contains('mysql-connector-java-5.1.27.jar') }
        mysqlLibrary
    }

    def "Configures Idea project files for a custom facet"() {
        when:
        buildFile << """
apply plugin: 'java'
${applyPlugin(NebulaFacetPlugin)}
apply plugin: 'idea'

facets {
    myCustom
}

repositories {
    mavenCentral()
}

dependencies {
    myCustomCompile 'commons-io:commons-io:2.4'
    myCustomRuntime 'mysql:mysql-connector-java:5.1.27'
}
"""

        writeHelloWorld('nebula.plugin.plugin')
        writeTest('src/myCustom/java/', 'nebula.plugin.plugin', false)
        runTasksSuccessfully('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def sourceFolders = moduleXml.component.content.sourceFolder.findAll { it.@isTestSource.text() == 'false' }
        def sourceFolder = sourceFolders.find { it.@url.text() == "file://\$MODULE_DIR\$/src/myCustom/java" }
        sourceFolder
        def orderEntries = moduleXml.component.orderEntry.findAll { it.@type.text() == 'module-library' && it.@exported.text() == '' }
        def commonsIoLibrary = orderEntries.find { it.library.CLASSES.root.@url.text().contains('commons-io-2.4.jar') }
        commonsIoLibrary
        def mysqlLibrary = orderEntries.find { it.library.CLASSES.root.@url.text().contains('mysql-connector-java-5.1.27.jar') }
        mysqlLibrary
    }
}
