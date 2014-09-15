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
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])

        buildFile << """
apply plugin: 'java'
${applyPlugin(NebulaFacetPlugin)}
apply plugin: 'idea'

facets {
    functionalTest
}

repositories {
    maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
}

dependencies {
    functionalTestCompile 'foo:bar:2.4'
    functionalTestRuntime 'custom:baz:5.1.27'
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
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('bar-2.4.jar') }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('baz-5.1.27.jar') }
    }

    def "Configures Idea project files for a custom facet"() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])

        buildFile << """
apply plugin: 'java'
${applyPlugin(NebulaFacetPlugin)}
apply plugin: 'idea'

facets {
    myCustom
}

repositories {
    maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
}

dependencies {
    myCustomCompile 'foo:bar:2.4'
    myCustomRuntime 'custom:baz:5.1.27'
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
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('bar-2.4.jar') }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('baz-5.1.27.jar') }
    }

    def 'Configures Idea project before java plugin'() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])

        buildFile << """
            ${applyPlugin(NebulaFacetPlugin)}
            apply plugin: 'idea'

            facets {
                myCustom
            }

            apply plugin: 'java'

            repositories {
                maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
            }

            dependencies {
                myCustomCompile 'foo:bar:2.4'
                myCustomRuntime 'custom:baz:5.1.27'
            }
            """.stripIndent()

        runTasksSuccessfully('idea')

        then:
        noExceptionThrown()
    }
}
