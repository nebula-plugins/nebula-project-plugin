package nebula.plugin.responsible

import nebula.core.NamedContainerProperOrder
import nebula.plugin.responsible.ide.EclipsePluginConfigurer
import nebula.plugin.responsible.ide.IdePluginConfigurer
import nebula.plugin.responsible.ide.IdeaPluginConfigurer
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.project.AbstractProject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.reflect.Instantiator

class NebulaFacetPlugin implements Plugin<Project> {

    Project project
    NamedDomainObjectContainer<FacetDefinition> extension

    @Override
    void apply(Project project) {
        this.project = project

        extension = container(FacetDefinition, new NamedDomainObjectFactory<FacetDefinition>() {
            @Override
            FacetDefinition create(String name) {
                if ( name.contains('Test') ) {
                    return new TestFacetDefinition(name)
                } else {
                    return new FacetDefinition(name)
                }
            }
        })

        project.extensions.add('facets', extension)

        // TODO Add remove call, to protect against removals
        extension.all { FacetDefinition facet ->
            // Might have to perform this in afterEvaluate
            project.plugins.withType(JavaBasePlugin) {

                JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
                SourceSetContainer sourceSets = javaConvention.sourceSets
                sourceSets.matching { it.name == facet.parentSourceSet } .all { SourceSet parentSourceSet ->

                    // Since we're using NamedContainerProperOrder, we're configured already.
                    SourceSet sourceSet = createSourceSet(parentSourceSet, facet)

                    Configuration parentCompile = project.configurations.getByName(parentSourceSet.compileConfigurationName)
                    project.configurations.getByName(sourceSet.compileConfigurationName).extendsFrom(parentCompile)

                    Configuration parentRuntime = project.configurations.getByName(parentSourceSet.runtimeConfigurationName)
                    project.configurations.getByName(sourceSet.runtimeConfigurationName).extendsFrom(parentRuntime)

                    // Make sure at the classes get built as part of build
                    project.tasks.getByName('build').dependsOn(sourceSet.classesTaskName)

                    if (facet instanceof TestFacetDefinition) {

                        Test testTask = createTestTask(facet.testTaskName, sourceSet)

                        testTask.mustRunAfter(project.tasks.getByName('test'))
                        project.tasks.getByName('check').dependsOn(testTask)
                    }

                    // Idea module.scopes is initialized by the JavaPlugin, without waiting for the Java plugin, we'll
                    // get an NPE when we access the plus method.
                    project.plugins.withType(JavaPlugin) {
                        IdePluginConfigurer idePluginConfigurer = new IdeaPluginConfigurer(project)
                        idePluginConfigurer.configure(sourceSet, facet)

                        IdePluginConfigurer eclipsePluginConfigurer = new EclipsePluginConfigurer(project)
                        eclipsePluginConfigurer.configure(sourceSet, facet)
                    }
                }
            }
        }
    }

    /**
     * Creates the integration test Gradle task and defines the output directories.
     *
     * @param sourceSet to be used for the integration test task.
     * @return the integration test task, as a Gradle Test object.
     */
    Test createTestTask( String testName, SourceSet sourceSet ) {
        Test task = project.tasks.create( testName, Test )
        task.setGroup( JavaBasePlugin.VERIFICATION_GROUP )
        task.description( "Runs the ${sourceSet.name} tests" )
        task.reports.html.destination = new File( "${project.buildDir}/reports/${sourceSet.name}" )
        task.reports.junitXml.destination = new File( "${project.buildDir}/${sourceSet.name}-results" )
        task.testClassesDir = sourceSet.output.classesDir
        task.classpath = sourceSet.runtimeClasspath
        task
    }
    /**
     * Based on the JavaPluginConvention, creates a SourceSet for the appropriate to UsableSourceSet.
     *
     * @return the new SourceSet
     */
    SourceSet createSourceSet(SourceSet parentSourceSet, FacetDefinition set) {
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        SourceSetContainer sourceSets = javaConvention.sourceSets
        sourceSets.create(set.name) {
            compileClasspath += parentSourceSet.output
            compileClasspath += parentSourceSet.compileClasspath
            runtimeClasspath += it.output + it.compileClasspath
        }
    }

    public <C> NamedContainerProperOrder<C> container(Class<C> type, NamedDomainObjectFactory<C> factory) {
        Instantiator instantiator = ((AbstractProject) project).getServices().get(Instantiator.class);
        return instantiator.newInstance(NamedContainerProperOrder.class, type, instantiator, factory);
    }

    // TODO React to changes on a FacetDefinition, and re-create source set
}

