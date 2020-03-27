package nebula.plugin.responsible.ide

import nebula.plugin.responsible.FacetDefinition
import nebula.plugin.responsible.TestFacetDefinition
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.idea.model.IdeaModule

class EclipsePluginConfigurer implements IdePluginConfigurer {
    private final Project project

    EclipsePluginConfigurer(Project project) {
        this.project = project
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void configure(SourceSet sourceSet, FacetDefinition facet) {
        if(facet instanceof TestFacetDefinition) {
            configurePluginForTestSourceSet(sourceSet)
        }
        else {
            configurePluginForSourceSet(sourceSet)
        }
    }

    private void withEclipseModel(Closure c) {
        project.plugins.withType(EclipsePlugin) {
            project.eclipse { EclipseModel model ->
                c(model)
            }
        }
    }

    /**
     * Configures Eclipse plugin to add given SourceSet the plus configurations.
     *
     * @param sourceSet SourceSet
     */
    private void configurePluginForSourceSet(SourceSet sourceSet) {
        withEclipseModel { EclipseModel model ->
            model.classpath.plusConfigurations += [ getConfiguration(sourceSet.compileClasspathConfigurationName) ]
            model.classpath.plusConfigurations += [ getConfiguration(sourceSet.runtimeClasspathConfigurationName) ]
        }
    }

    /**
     * Configures Eclipse plugin to add given SourceSet the plus configurations and excludes from exporting.
     *
     * @param sourceSet SourceSet
     */
    private void configurePluginForTestSourceSet(SourceSet sourceSet) {
        withEclipseModel { EclipseModel model ->
            model.classpath.plusConfigurations += [ getConfiguration(sourceSet.compileClasspathConfigurationName) ]
            model.classpath.plusConfigurations += [ getConfiguration(sourceSet.runtimeClasspathConfigurationName) ]
        }
    }

    private Configuration getConfiguration(String sourceSetName) {
        project.configurations.getByName(sourceSetName)
    }
}
