package nebula.plugin.responsible

import nebula.plugin.publishing.NebulaJavadocJarPlugin
import nebula.plugin.publishing.NebulaSourceJarPlugin
import nebula.plugin.publishing.NebulaTestJarPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test

/**
 * Provide a responsible environment for a Gradle plugin
 */
class NebulaResponsiblePlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(NebulaResponsiblePlugin);

    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply(FixJavaPlugin)
        project.plugins.apply(NebulaJavadocJarPlugin)
        project.plugins.apply(NebulaSourceJarPlugin)
        project.plugins.apply(NebulaTestJarPlugin)

        // TODO Publish javadoc somehow
        project.tasks.withType(Javadoc) {
            failOnError = false
        }
        project.tasks.withType(Test) { Test testTask ->
            testTask.testLogging.exceptionFormat = 'full'
        }
    }
}