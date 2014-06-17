package nebula.plugin.responsible

import nebula.test.PluginProjectSpec
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet

class NebulaFacetPluginSpec extends PluginProjectSpec {

    String getPluginName() { return 'nebula-facet' }

    def 'do nothing without Java plugin'() {
        when:
        project.apply plugin: NebulaFacetPlugin
        project.facets {
            integTest
        }

        then:
        project.configurations.size() == 0
    }

    def 'create functional source set'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin
        project.facets {
            integTest
        }

        then:
        project.sourceSets.size() == 3

        SourceSet integTest = project.sourceSets.find { it.name == 'integTest'}
        integTest
        integTest.compileConfigurationName == 'integTestCompile'
        integTest.compileClasspath.files.find { 'src/integTest'}
        integTest.runtimeConfigurationName == 'integTestRuntime'

        project.configurations.size() == 8
        def compileConf = project.configurations.getByName('integTestCompile')
        compileConf
        compileConf.extendsFrom.any { it.name == 'compile'}
        def runtimeConf = project.configurations.getByName('integTestRuntime')
        runtimeConf
        runtimeConf.extendsFrom.any { it.name == 'runtime'}
    }

    def 'create multiple source sets'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin
        project.facets {
            examples
            samples
        }

        then:
        project.sourceSets.size() == 4
    }

    def 'can run without sourcesets'() {
        when:
        project.apply plugin: NebulaFacetPlugin
        project.apply plugin: 'java-base'
        project.facets {
            examples
            samples
        }

        then:
        project.sourceSets.size() == 0

        when:
        project.apply plugin: 'java'

        then:
        project.sourceSets.size() == 4
    }

    def 'configure facets'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin
        project.facets {
            examples {
                parentSourceSet = 'test'
            }
        }

        then:
        project.sourceSets.size() == 3

        def compileConf = project.configurations.getByName('examplesCompile')
        compileConf
        compileConf.extendsFrom.any { it.name == 'testCompile'}

    }

    def 'test based facet'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin
        project.facets {
            performanceTest
        }

        then:
        project.tasks.getByName('performanceTest')
        project.tasks.getByName('check').dependsOn.any {
            it instanceof Task && ((Task) it).name == 'performanceTest'
        }

        when:
        project.facets {
            functionalTest {
                testTaskName = 'functional'
            }
        }

        then:
        project.tasks.getByName('functional')
        project.tasks.getByName('check').dependsOn.any {
            it instanceof Task && ((Task) it).name == 'performanceTest'
        }
    }
}
