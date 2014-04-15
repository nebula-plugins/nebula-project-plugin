package nebula.plugin.responsible

import nebula.test.ProjectSpec

/**
 * Unit tests for the NebulaIntegTestPlugin class.
 */
class NebulaIntegTestPluginSpec extends ProjectSpec {

    def 'apply plugin does not throw exception'() {
        when:
        project.plugins.apply(NebulaIntegTestPlugin)

        then:
        noExceptionThrown()
    }
}
