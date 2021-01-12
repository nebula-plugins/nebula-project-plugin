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

        if (GradleVersion.current().baseVersion < GradleVersion.version("7.0").baseVersion) {
            assert project.configurations.size() == 28
        } else {
            // Gradle 7.+ removes the following configurations: compile, runtime, testCompile, testRuntime, integTestCompile, integTestRuntime
            assert project.configurations.size() == 22
        }

        def compileConf = project.configurations.getByName('integTestCompileClasspath')
        compileConf
        compileConf.extendsFrom.any { it.name == 'compileClasspath'}
        def runtimeConf = project.configurations.getByName('integTestRuntimeClasspath')
        runtimeConf
        runtimeConf.extendsFrom.any { it.name == 'runtimeClasspath'}
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

        def compileConf = project.configurations.getByName('examplesCompileClasspath')
        compileConf
        compileConf.extendsFrom.any { it.name == 'testCompileClasspath'}

    }
}
