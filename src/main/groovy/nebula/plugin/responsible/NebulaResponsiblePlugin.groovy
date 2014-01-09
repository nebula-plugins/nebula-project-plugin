package nebula.plugin.responsible

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.javadoc.Javadoc

/**
 * Provide a responsible environment for a Gradle plugin
 */
class NebulaResponsiblePlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(NebulaResponsiblePlugin);

    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        // TODO sources jar
        // TODO javadoc jar
        // TODO Publish javadoc somehow
        project.tasks.withType(Javadoc) {
            failOnError = false
        }

        // TODO test jar
    }
}