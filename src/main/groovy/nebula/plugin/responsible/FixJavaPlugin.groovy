package nebula.plugin.responsible

import org.apache.commons.lang.reflect.FieldUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

/**
 * Restores status of project after Java plugin runs. The one caveat is that this plugin has to be run before the
 * BasePlugin is applied, else we can't restore the status.
 */
class FixJavaPlugin implements Plugin<Project> {

    def savedStatus
    void apply(Project project) {

        if (project.plugins.hasPlugin(BasePlugin)) {
            throw new GradleException("Unable to intercept BasePlugin before it explicitly set the status to release")
        }

        // Save status in case BasePlugin comes in and destroys it
        savedStatus = FieldUtils.readField(project, 'status', true)

        // Force it's hand by running right away
        project.plugins.apply(BasePlugin)

        project.plugins.withType(BasePlugin) {
            // can be null
            project.status = savedStatus
        }

    }
}
