package nebula.plugin.responsible.ide

import nebula.plugin.responsible.FacetDefinition
import nebula.plugin.responsible.TestFacetDefinition
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.IdeaModule

class IdeaPluginConfigurer implements IdePluginConfigurer {
    private final Project project

    IdeaPluginConfigurer(Project project) {
        this.project = project
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void configure(SourceSet sourceSet, FacetDefinition facet) {
        if(facet instanceof TestFacetDefinition) {
            configureIdeaPluginForTestSourceSet(sourceSet)
        }
        else {
            configureIdeaPluginForSourceSet(sourceSet)
        }
    }

    private void withIdeaModule(Closure c) {
        project.plugins.withType(IdeaPlugin) {
            project.idea { IdeaModel model ->
                c(model.module)
            }
        }
    }

    /**
     * Configures IDEA plugin to add given SourceSet to test source directories and implicit configurations to the TEST
     * scope.
     *
     * @param testSourceSet Test SourceSet
     */
    private void configureIdeaPluginForTestSourceSet(SourceSet testSourceSet) {
        withIdeaModule { IdeaModule module ->
            testSourceSet.allSource.srcDirs.each { srcDir ->
                module.testSourceDirs += srcDir
            }

            module.scopes.TEST.plus += getSourceSetCompileConfiguration(testSourceSet.name)
            module.scopes.TEST.plus += getSourceSetRuntimeConfiguration(testSourceSet.name)
        }
    }

    /**
     * Configures IDEA plugin to add given SourceSet to source directories and implicit configurations to the COMPILE
     * scope.
     *
     * @param sourceSet SourceSet
     */
    private void configureIdeaPluginForSourceSet(SourceSet sourceSet) {
        withIdeaModule { IdeaModule module ->
            sourceSet.allSource.srcDirs.each { srcDir ->
                module.sourceDirs += srcDir
            }

            module.scopes.COMPILE.plus += getSourceSetCompileConfiguration(sourceSet.name)
            module.scopes.COMPILE.plus += getSourceSetRuntimeConfiguration(sourceSet.name)
        }
    }

    private Configuration getSourceSetCompileConfiguration(String sourceSetName) {
        project.configurations.getByName("${sourceSetName}Compile")
    }

    private Configuration getSourceSetRuntimeConfiguration(String sourceSetName) {
        project.configurations.getByName("${sourceSetName}Runtime")
    }
}
