package nebula.plugin.responsible

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies the Nebula integration test convention to the project.  This
 * convention adds a build task called 'integrationTest' that will execute
 * longer running tests.  The plugin expects that all integration tests are
 * located in 'src/integTest/java'.
 */
class NebulaIntegTestPlugin implements Plugin<Project> {

    @Override
    void apply( Project project ) {

        def sourceSetPlugin = project.plugins.apply(NebulaFacetPlugin)
        sourceSetPlugin.extension.create('integTest') {
            testTaskName = 'integrationTest'
            parentSourceSet = 'test'
        }
    }

}
