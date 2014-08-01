package nebula.plugin.responsible

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * Applies the Nebula integration test convention to the project.  This
 * convention adds a build task called 'integrationTest' that will execute
 * longer running tests.  The plugin expects that all integration tests are
 * located in 'src/integTest/java'.
 */
class NebulaIntegTestPlugin implements Plugin<Project> {
    static final String FACET_NAME = 'integTest'

    @Override
    void apply( Project project ) {
        def facetPlugin = project.plugins.apply(NebulaFacetPlugin)

        project.plugins.withType(JavaPlugin) {
            facetPlugin.extension.create(FACET_NAME) {
                testTaskName = 'integrationTest'
                parentSourceSet = 'test'
            }
        }
    }
}
