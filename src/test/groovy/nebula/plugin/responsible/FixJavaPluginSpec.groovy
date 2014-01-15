package nebula.plugin.responsible

import nebula.test.ProjectSpec

class FixJavaPluginSpec extends ProjectSpec {
    def 'apply plugin'() {
        when:
        project.plugins.apply(FixJavaPlugin)

        then:
        noExceptionThrown()
    }

}