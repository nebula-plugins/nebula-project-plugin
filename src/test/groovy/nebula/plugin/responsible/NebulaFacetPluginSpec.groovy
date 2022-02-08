package nebula.plugin.responsible

import nebula.test.PluginProjectSpec
import org.gradle.api.tasks.SourceSet
import org.gradle.util.GradleVersion

class NebulaFacetPluginSpec extends PluginProjectSpec {

    String getPluginName() { return 'nebula.facet' }

    def 'do nothing without Java plugin'() {
        when:
        project.apply plugin: NebulaFacetPlugin.class
        project.facets {
            integTest
        }

        then:
        project.configurations.size() == 0
    }

    def 'create functional source set'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        project.facets {
            integTest
        }

        then:
        project.sourceSets.size() == 3

        SourceSet integTest = project.sourceSets.find { it.name == 'integTest'}
        integTest
        integTest.compileClasspathConfigurationName == 'integTestCompileClasspath'
        integTest.compileClasspath.files.find {
            it.toString().contains('classes/java/main')
        }
        integTest.runtimeClasspathConfigurationName == 'integTestRuntimeClasspath'

        assert project.configurations.size() == 24

        def integTestImplementationConf = project.configurations.getByName('integTestImplementation')
        integTestImplementationConf
        integTestImplementationConf.extendsFrom.any { it.name == 'implementation'}
        def integTestRuntimeOnlyConf = project.configurations.getByName('integTestRuntimeOnly')
        integTestRuntimeOnlyConf
        integTestRuntimeOnlyConf.extendsFrom.any { it.name == 'runtimeOnly'}
    }

    def 'create multiple source sets'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        project.facets {
            examples
            samples
        }

        then:
        project.sourceSets.size() == 4
    }

    def 'can run without sourcesets'() {
        when:
        project.apply plugin: NebulaFacetPlugin.class
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
        project.apply plugin: NebulaFacetPlugin.class
        project.facets {
            examples {
                parentSourceSet = 'test'
            }
        }

        then:
        project.sourceSets.size() == 3

        def examplesImplementationConf = project.configurations.getByName('examplesImplementation')
        examplesImplementationConf
        examplesImplementationConf.extendsFrom.any { it.name == 'testImplementation'}

    }
}
