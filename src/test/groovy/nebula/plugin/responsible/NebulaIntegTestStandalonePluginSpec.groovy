package nebula.plugin.responsible

import nebula.test.PluginProjectSpec

/**
 * Unit tests for the NebulaIntegTestPlugin class.
 */
class NebulaIntegTestStandalonePluginSpec extends PluginProjectSpec {

    @Override
    String getPluginName() {
        return 'com.netflix.nebula.integtest-standalone'
    }

    def 'after applying java plugin'() {
        when:
        project.plugins.apply 'java'
        project.plugins.apply pluginName

        then:
        noExceptionThrown()
    }

    def 'before applying java plugin'() {
        when:
        
        project.plugins.apply pluginName
        project.plugins.apply 'java'

        then:
        noExceptionThrown()
    }
}
