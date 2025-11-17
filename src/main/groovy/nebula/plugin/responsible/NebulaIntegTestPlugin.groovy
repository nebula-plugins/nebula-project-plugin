package nebula.plugin.responsible

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * Applies the Nebula integration test convention to the project.  This
 * convention adds a build task called 'integrationTest' that will execute
 * longer running tests.  The plugin expects that all integration tests are
 * located in 'src/integTest/java'.
 */
@CompileStatic
class NebulaIntegTestPlugin implements Plugin<Project> {
    static final String FACET_NAME = 'integTest'
    static final String TASK_NAME = 'integrationTest'
    static final String PARENT_SOURCE_SET = 'test'

    @Override
    void apply(Project project) {
        NebulaFacetPlugin facetPlugin = project.plugins.apply(NebulaFacetPlugin) as NebulaFacetPlugin

        project.plugins.withType(JavaPlugin) {
            // Use the type-safe helper method - works with Java, Kotlin, and Groovy
            facetPlugin.createTestFacet(FACET_NAME, new Action<TestFacetDefinition>() {
                @Override
                void execute(TestFacetDefinition facet) {
                    facet.setTestTaskName(TASK_NAME)
                    facet.setParentSourceSet(PARENT_SOURCE_SET)
                    facet.setIncludeInCheckLifecycle(shouldIncludeInCheckLifecycle())
                }
            })
        }
    }

    protected boolean shouldIncludeInCheckLifecycle() {
        return true
    }
}
