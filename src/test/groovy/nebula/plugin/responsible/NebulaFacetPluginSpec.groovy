package nebula.plugin.responsible

import nebula.test.PluginProjectSpec
import org.gradle.api.Action
import org.gradle.api.tasks.SourceSet
import org.gradle.util.GradleVersion

class NebulaFacetPluginSpec extends PluginProjectSpec {

    String getPluginName() { return 'com.netflix.nebula.facet' }

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

    def 'create facet using Action API'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        plugin.extension.create('examples', new Action<FacetDefinition>() {
            @Override
            void execute(FacetDefinition facet) {
                facet.parentSourceSet = 'test'
            }
        })

        then:
        project.sourceSets.size() == 3
        def examplesConf = project.configurations.getByName('examplesImplementation')
        examplesConf.extendsFrom.any { it.name == 'testImplementation' }
    }

    def 'create test facet using Action API auto-detects TestFacetDefinition'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        def createdFacet = plugin.extension.create('integTest', new Action<FacetDefinition>() {
            @Override
            void execute(FacetDefinition facet) {
                // Should be TestFacetDefinition due to name containing 'Test'
                assert facet instanceof TestFacetDefinition
                ((TestFacetDefinition) facet).testTaskName = 'myTestTask'
            }
        })

        then:
        createdFacet instanceof TestFacetDefinition
        ((TestFacetDefinition) createdFacet).testTaskName.get() == 'myTestTask'
        project.tasks.findByName('myTestTask') != null
    }

    def 'createTestFacet creates TestFacetDefinition with Action'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        TestFacetDefinition facet = plugin.createTestFacet('functional', new Action<TestFacetDefinition>() {
            @Override
            void execute(TestFacetDefinition f) {
                f.testTaskName = 'functionalTest'
                f.parentSourceSet = 'test'
                f.includeInCheckLifecycle = false
            }
        })

        then:
        facet != null
        facet instanceof TestFacetDefinition
        facet.name == 'functional'
        facet.testTaskName.get() == 'functionalTest'
        facet.parentSourceSet.get() == 'test'
        !facet.includeInCheckLifecycle.get()
        project.sourceSets.findByName('functional') != null
        project.tasks.findByName('functionalTest') != null
    }

    def 'createTestFacet without Action uses defaults'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        TestFacetDefinition facet = plugin.createTestFacet('smoke')

        then:
        facet != null
        facet.name == 'smoke'
        facet.testTaskName.get() == 'smoke' // Default is the facet name
        facet.parentSourceSet.get() == 'main' // Default from FacetDefinition
        facet.includeInCheckLifecycle.get() == true // Default for test facets
    }

    def 'createFacet creates regular FacetDefinition with Action'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        FacetDefinition facet = plugin.createFacet('examples', new Action<FacetDefinition>() {
            @Override
            void execute(FacetDefinition f) {
                f.parentSourceSet = 'test'
            }
        })

        then:
        facet != null
        facet instanceof FacetDefinition
        !(facet instanceof TestFacetDefinition)
        facet.name == 'examples'
        facet.parentSourceSet.get() == 'test'
        project.sourceSets.findByName('examples') != null
        project.tasks.findByName('examples') == null // No test task for regular facets
    }

    def 'createFacet without Action uses defaults'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        FacetDefinition facet = plugin.createFacet('docs')

        then:
        facet != null
        facet.name == 'docs'
        facet.parentSourceSet.get() == 'main'
    }

    def 'Action configuration happens before facet is added to container'() {
        given:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        def configurationExecuted = false
        def facetAddedWhenConfigured = false

        when:
        plugin.extension.create('test1', new Action<FacetDefinition>() {
            @Override
            void execute(FacetDefinition facet) {
                configurationExecuted = true
                // Check if facet is already in container during configuration
                facetAddedWhenConfigured = plugin.extension.findByName('test1') != null
                facet.parentSourceSet = 'test'
            }
        })

        then:
        configurationExecuted
        !facetAddedWhenConfigured // Should be configured BEFORE being added
        plugin.extension.findByName('test1') != null // But should be in container after
    }

    def 'both closure and Action APIs can be used together'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        // Old closure-based API
        project.facets {
            examples {
                parentSourceSet = 'test'
            }
        }

        // New Action-based API
        plugin.createTestFacet('integTest') { it.testTaskName = 'integration' }

        then:
        project.sourceSets.size() == 4 // main, test, examples, integTest
        project.tasks.findByName('integration') != null
    }

    def 'can create multiple facets programmatically with Action API'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        plugin.createTestFacet('integration') { it.testTaskName = 'integTest' }
        plugin.createTestFacet('functional') { it.testTaskName = 'funcTest' }
        plugin.createFacet('examples') { it.parentSourceSet = 'test' }

        then:
        project.sourceSets.size() == 5 // main, test, integration, functional, examples
        project.tasks.findByName('integTest') != null
        project.tasks.findByName('funcTest') != null
    }

    def 'NebulaIntegTestPlugin uses new API correctly'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaIntegTestPlugin.class

        then:
        project.sourceSets.findByName('integTest') != null
        project.tasks.findByName('integrationTest') != null
        def checkTask = project.tasks.findByName('check')
        checkTask.taskDependencies.getDependencies(checkTask).any { it.name == 'integrationTest' }
    }

    def 'createTestFacet check lifecycle configuration'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: NebulaFacetPlugin.class
        NebulaFacetPlugin plugin = project.plugins.getPlugin(NebulaFacetPlugin)

        plugin.createTestFacet('smoke') {
            it.testTaskName = 'smokeTest'
            it.includeInCheckLifecycle = true
        }

        plugin.createTestFacet('manual') {
            it.testTaskName = 'manualTest'
            it.includeInCheckLifecycle = false
        }

        then:
        def checkTask = project.tasks.findByName('check')
        def checkDeps = checkTask.taskDependencies.getDependencies(checkTask)
        checkDeps.any { it.name == 'smokeTest' }
        !checkDeps.any { it.name == 'manualTest' }
    }
}
