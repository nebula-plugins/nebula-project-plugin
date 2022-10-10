package nebula.plugin.responsible

import nebula.test.IntegrationTestKitSpec
import org.gradle.testkit.runner.TaskOutcome

class NebulaResponsiblePluginLauncherSpec extends IntegrationTestKitSpec {

    def setup() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.project'
            }
        """
    }

    def 'should apply nebula dependency lock plugin by default'() {
        when:
        def result = runTasks('generateLock')

        then:
        result.task(':generateLock').outcome == TaskOutcome.SUCCESS
    }

    def 'should allow to disable nebula dependency lock via project property'() {
        when:
        def result = runTasksAndFail('generateLock', '-Pnebula.dependencyLockPluginEnabled=false')

        then:
        result.output.contains('Task \'generateLock\' not found in root project')
    }
}
