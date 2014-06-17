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
}
