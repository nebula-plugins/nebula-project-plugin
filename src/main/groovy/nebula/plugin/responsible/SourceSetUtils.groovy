package nebula.plugin.responsible

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer

class SourceSetUtils {

    static SourceSetContainer getSourceSets(Project project) {
        return project.extensions.getByType(JavaPluginExtension).sourceSets
    }
}
