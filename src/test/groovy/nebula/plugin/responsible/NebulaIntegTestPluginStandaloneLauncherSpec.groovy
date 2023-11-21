package nebula.plugin.responsible
/**
 * Runs Gradle Launcher style integration Spock tests on the NebulaIntegTestPlugin class
 */
class NebulaIntegTestPluginStandaloneLauncherSpec extends AbstractNebulaIntegTestPluginLauncherSpec {
    @Override
    String getPluginId() {
        return 'com.netflix.nebula.integtest-standalone'
    }

    def "check does not depend on integration test task"() {
        when:
        def result = runTasks('check')

        then:
        result.task(':test').outcome
        !result.task(':integrationTest')?.outcome
    }
}
