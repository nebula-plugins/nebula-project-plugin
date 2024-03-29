package nebula.plugin.responsible

import groovy.transform.CompileStatic
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.dependencylock.DependencyLockPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.publishing.publications.JavadocJarPlugin
import nebula.plugin.publishing.publications.SourceJarPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test

/**
 * Provide a responsible environment for a Gradle plugin.
 */
@CompileStatic
class NebulaResponsiblePlugin implements Plugin<Project> {
    private static final String DEPENDENCY_LOCK_PLUGIN_ENABLED = 'nebula.dependencyLockPluginEnabled'
    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        // Publishing
        if (isBuildingSomething(project)) {
            project.plugins.apply(MavenPublishPlugin)
        }
        project.plugins.apply(JavadocJarPlugin)
        project.plugins.apply(SourceJarPlugin)

        // Info
        project.plugins.apply(InfoPlugin)

        // Contacts
        project.plugins.apply(ContactsPlugin)

        // Dependency Locking
        def nebulaDependencyLockPluginEnabled = project.hasProperty(DEPENDENCY_LOCK_PLUGIN_ENABLED) ? Boolean.valueOf(project.property(DEPENDENCY_LOCK_PLUGIN_ENABLED) as String) : true
        if(nebulaDependencyLockPluginEnabled) {
            project.plugins.apply(DependencyLockPlugin)
        }

        // TODO Publish javadoc somehow
        project.tasks.withType(Javadoc).configureEach(new Action<Javadoc>() {
            @Override
            void execute(Javadoc javadoc) {
                javadoc.failOnError = false
            }
        })
        project.tasks.withType(Test).configureEach(new Action<Test>() {
            @Override
            void execute(Test test) {
                test.testLogging.exceptionFormat = 'full'
            }
        })
    }

    private boolean isBuildingSomething(Project project) {
        def isParentProject = project.rootProject.subprojects.any { it.parent == project }
        return !isParentProject
    }
}
