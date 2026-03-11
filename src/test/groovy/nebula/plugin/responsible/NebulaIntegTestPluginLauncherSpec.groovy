package nebula.plugin.responsible

/**
 * Runs Gradle Launcher style integration Spock tests on the NebulaIntegTestPlugin class
 */
class NebulaIntegTestPluginLauncherSpec extends AbstractNebulaIntegTestPluginLauncherSpec {
    @Override
    String getPluginId() {
        return 'com.netflix.nebula.integtest'
    }
}
