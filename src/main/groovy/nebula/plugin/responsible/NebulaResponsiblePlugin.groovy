package nebula.plugin.responsible

import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.dependencylock.DependencyLockPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.NebulaJavadocJarPlugin
import nebula.plugin.publishing.NebulaPublishingPlugin
import nebula.plugin.publishing.NebulaSourceJarPlugin
import nebula.plugin.publishing.NebulaTestJarPlugin
import nebula.plugin.publishing.sign.NebulaSignPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test

/**
 * Provide a responsible environment for a Gradle plugin.
 */
class NebulaResponsiblePlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(NebulaResponsiblePlugin);

    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply(FixJavaPlugin)

        // Publishing
        project.plugins.apply(NebulaPublishingPlugin)
        project.plugins.apply(NebulaSignPlugin)
        project.plugins.apply(NebulaJavadocJarPlugin)
        project.plugins.apply(NebulaSourceJarPlugin)

        // Info
        project.plugins.apply(InfoPlugin)

        // Contacts
        project.plugins.apply(ContactsPlugin)

        // Dependency Locking
        project.plugins.apply(DependencyLockPlugin)

        // TODO Publish javadoc somehow
        project.tasks.withType(Javadoc) {
            failOnError = false
        }
        project.tasks.withType(Test) { Test testTask ->
            testTask.testLogging.exceptionFormat = 'full'
        }
    }
}