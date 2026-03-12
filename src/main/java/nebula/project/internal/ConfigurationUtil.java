package nebula.project.internal;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.jspecify.annotations.NullMarked;

/**
 * reusable utility functions for dealing with Configurations
 */
@NullMarked
public class ConfigurationUtil {
    private ConfigurationUtil() {}

    /**
     * copies dependencies and attributes from one source set to another
     */
    public static void extendSourceSet(Project project, SourceSet from, SourceSet to){
        project.getConfigurations().named(to.getImplementationConfigurationName(), c -> {
            c.extendsFrom(project.getConfigurations().getByName(from.getImplementationConfigurationName()));
        });
        project.getConfigurations().named(to.getRuntimeOnlyConfigurationName(), c -> {
            c.extendsFrom(project.getConfigurations().getByName(from.getRuntimeOnlyConfigurationName()));
        });
        project.getConfigurations().named(to.getCompileOnlyConfigurationName(), c -> {
            c.extendsFrom(project.getConfigurations().getByName(from.getCompileOnlyConfigurationName()));
        });
        project.getConfigurations().named(to.getAnnotationProcessorConfigurationName(), c -> {
            c.extendsFrom(project.getConfigurations().getByName(from.getAnnotationProcessorConfigurationName()));
        });
        project.getConfigurations().named(to.getRuntimeClasspathConfigurationName(), c -> {
            c.getAttributes().addAllLater(
                    project.getConfigurations().getByName(from.getRuntimeClasspathConfigurationName()).getAttributes()
            );
        });
        project.getConfigurations().named(to.getCompileClasspathConfigurationName(), c -> {
            c.getAttributes().addAllLater(
                    project.getConfigurations().getByName(from.getCompileClasspathConfigurationName()).getAttributes()
            );
        });
    }
}
