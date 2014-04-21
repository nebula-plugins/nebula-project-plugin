package nebula.plugin.responsible

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

/**
 * Applies the Nebula integration test convention to the project.  This
 * convention adds a build task called 'integrationTest' that will execute
 * longer running tests.  The plugin expects that all integration tests are
 * located in 'src/integTest/java'.
 */
class NebulaIntegTestPlugin implements Plugin<Project> {

    static final String SOURCE_SET = 'integTest'
    static final String TASK_NAME = 'integrationTest'

    protected Project project

    @Override
    void apply( Project project ) {
        this.project = project

        project.plugins.withType( JavaPlugin ) {
            SourceSet integTestSourceSet = createSourceSet()

            Configuration testCompile = project.configurations.getByName('testCompile')
            project.configurations.getByName(integTestSourceSet.compileConfigurationName).extendsFrom(testCompile)

            Configuration testRuntime = project.configurations.getByName('testRuntime')
            project.configurations.getByName(integTestSourceSet.runtimeConfigurationName).extendsFrom(testRuntime)

            Test integTestTask = createIntegTestTask( integTestSourceSet )

            integTestTask.mustRunAfter( project.tasks.getByName('test') )
            project.tasks.getByName('check').dependsOn( integTestTask )
        }
    }

    /**
     * Based on the JavaPluginConvention, creates a SourceSet for the integTest plugin.
     *
     * @return the integTest SourceSet
     */
    SourceSet createSourceSet() {
        JavaPluginConvention javaConvention = project.convention.getPlugin( JavaPluginConvention )
        SourceSetContainer sourceSets = javaConvention.sourceSets
        sourceSets.create( SOURCE_SET ) {
            compileClasspath += sourceSets.getByName('main').output
            compileClasspath += project.configurations.getByName('testRuntime')
            runtimeClasspath = it.output + it.compileClasspath
        }
    }

    /**
     * Creates the integration test Gradle task and defines the output directories.
     *
     * @param sourceSet to be used for the integration test task.
     * @return the integration test task, as a Gradle Test object.
     */
    Test createIntegTestTask( SourceSet sourceSet ) {
        Test task = project.tasks.create( TASK_NAME, Test )
        task.setGroup( JavaBasePlugin.VERIFICATION_GROUP )
        task.description( 'Runs the integration tests' )
        task.reports.html.destination = new File( "${project.buildDir}/reports/$SOURCE_SET" )
        task.reports.junitXml.destination = new File( "${project.buildDir}/$SOURCE_SET-results" )
        task.testClassesDir = sourceSet.output.classesDir
        task.classpath = sourceSet.runtimeClasspath
        task
    }
}
