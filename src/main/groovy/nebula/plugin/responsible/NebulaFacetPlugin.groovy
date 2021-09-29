package nebula.plugin.responsible

import groovy.transform.CompileStatic
import nebula.plugin.responsible.gradle.NamedContainerProperOrder
import nebula.plugin.responsible.ide.EclipsePluginConfigurer
import nebula.plugin.responsible.ide.IdePluginConfigurer
import nebula.plugin.responsible.ide.IdeaPluginConfigurer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.reflect.Instantiator

@CompileStatic
class NebulaFacetPlugin implements Plugin<Project> {

    private static final String IMPLEMENTATION_CONFIG_NAME = 'implementation'
    private static final String API_CONFIG_NAME = 'api'
    private static final String COMPILE_CONFIG_NAME = 'compile'
    private static final String MAIN_SOURCE_SET_NAME = 'main'

    Project project
    NamedDomainObjectContainer<FacetDefinition> extension

    @Override
    void apply(Project project) {
        this.project = project

        extension = container(FacetDefinition, new NamedDomainObjectFactory<FacetDefinition>() {
            @Override
            FacetDefinition create(String name) {
                if (name.contains('Test')) {
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
                sourceSets.matching { SourceSet sourceSet -> sourceSet.name == facet.parentSourceSet }.all { SourceSet parentSourceSet ->

                    // Since we're using NamedContainerProperOrder, we're configured already.
                    SourceSet sourceSet = createSourceSet(parentSourceSet, facet)

                    Configuration parentImplementation = project.configurations.getByName(parentSourceSet.implementationConfigurationName)
                    project.configurations.getByName(sourceSet.implementationConfigurationName).extendsFrom(parentImplementation)

                    Configuration parentRuntimeOnly = project.configurations.getByName(parentSourceSet.runtimeOnlyConfigurationName)
                    project.configurations.getByName(sourceSet.runtimeOnlyConfigurationName).extendsFrom(parentRuntimeOnly)

                    Configuration annotationProcessor = project.configurations.getByName(parentSourceSet.annotationProcessorConfigurationName)
                    project.configurations.getByName(sourceSet.annotationProcessorConfigurationName).extendsFrom(annotationProcessor)

                    // Make sure at the classes get built as part of build
                    project.tasks.named('build').configure(new Action<Task>() {
                        @Override
                        void execute(Task buildTask) {
                            buildTask.dependsOn(sourceSet.classesTaskName)
                        }
                    })

                    if (facet instanceof TestFacetDefinition) {
                        TaskProvider<Test> testTask = createTestTask(facet.testTaskName.toString(), sourceSet)
                        if (facet.includeInCheckLifecycle) {
                            project.tasks.named('check') configure(new Action<Task>() {
                                @Override
                                void execute(Task checkTask) {
                                    checkTask.dependsOn(testTask)
                                }
                            })
                        }
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
    TaskProvider<Test> createTestTask(String testName, SourceSet sourceSet) {
        TaskProvider<Test> testTask = project.tasks.register(testName, Test)
        testTask.configure(new Action<Test>() {
            @Override
            void execute(Test test) {
                test.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
                test.setDescription("Runs the ${sourceSet.name} tests")
                test.reports.html.setDestination(new File("${project.buildDir}/reports/${sourceSet.name}"))
                test.reports.junitXml.setDestination(new File("${project.buildDir}/${sourceSet.name}-results"))
                test.testClassesDirs = sourceSet.output.classesDirs
                test.classpath = sourceSet.runtimeClasspath
                test.shouldRunAfter(project.tasks.named('test'))
            }
        })

        testTask
    }

    /**
     * Based on the JavaPluginConvention, creates a SourceSet for the appropriate to UsableSourceSet.
     *
     * @return the new SourceSet
     */
    SourceSet createSourceSet(SourceSet parentSourceSet, FacetDefinition set) {
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        SourceSetContainer sourceSets = javaConvention.sourceSets
        sourceSets.create(set.name) { SourceSet sourceSet ->
            //our new source set needs to see compiled classes from its parent
            //the parent can be also inheriting so we need to extract all the output from previous parents
            //e.g smokeTest inherits from test which inherits from main and we need to see classes from main
            Set<Object> compileClasspath = new LinkedHashSet<Object>()
            compileClasspath.add(sourceSet.compileClasspath)
            addParentSourceSetOutputs(compileClasspath, parentSourceSet, sourceSets)

            //we are using from to create ConfigurableFileCollection so if we keep inhering from created facets we can
            //still extract chain of output from all parents
            sourceSet.compileClasspath = project.objects.fileCollection().from(compileClasspath as Object[])
            //runtime classpath of parent already has parent output so we don't need to explicitly add it
            Set<Object> runtimeClasspath = new LinkedHashSet<Object>()
            runtimeClasspath.add(sourceSet.runtimeClasspath)
            addParentSourceSetOutputs(runtimeClasspath, parentSourceSet, sourceSets)

            sourceSet.runtimeClasspath = project.objects.fileCollection().from(runtimeClasspath as Object[])
        }
    }

    private void addParentSourceSetOutputs( Set<Object> classpath, SourceSet parentSourceSet, SourceSetContainer sourceSets) {
        classpath.add(parentSourceSet.output)
        Configuration parentSourceSetImplementationConfiguration = project.configurations.findByName(parentSourceSet.implementationConfigurationName)
        if(!parentSourceSetImplementationConfiguration ||
                !parentSourceSetImplementationConfiguration.extendsFrom ||
                parentSourceSetImplementationConfiguration.extendsFrom.any { it.name in [API_CONFIG_NAME, COMPILE_CONFIG_NAME ] }
        ) {
            return
        }

        Configuration extendsFrom = parentSourceSetImplementationConfiguration.extendsFrom.find()
        if(extendsFrom.name == IMPLEMENTATION_CONFIG_NAME) {
            addParentSourceSetOutputs(classpath, sourceSets.getByName(MAIN_SOURCE_SET_NAME), sourceSets)
        } else if(extendsFrom.name.toLowerCase().contains(IMPLEMENTATION_CONFIG_NAME)) {
            addParentSourceSetOutputs(classpath, sourceSets.getByName(extendsFrom.name.replaceAll(IMPLEMENTATION_CONFIG_NAME.capitalize(), '')), sourceSets)
        }
    }

    public <C> NamedContainerProperOrder<C> container(Class<C> type, NamedDomainObjectFactory<C> factory) {
        Instantiator instantiator = ((ProjectInternal) project).getServices().get(Instantiator.class) as Instantiator
        CollectionCallbackActionDecorator decorator = ((ProjectInternal) project).getServices().get(CollectionCallbackActionDecorator.class) as CollectionCallbackActionDecorator
        return instantiator.newInstance(NamedContainerProperOrder.class, type, instantiator, factory, decorator)
    }

    // TODO React to changes on a FacetDefinition, and re-create source set
}

