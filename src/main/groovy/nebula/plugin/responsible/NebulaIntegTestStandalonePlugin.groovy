package nebula.plugin.responsible

/**
 * Extends the {@link NebulaIntegTestPlugin}, without a dependency on the test task in the 'check' lifecycle.
 */
class NebulaIntegTestStandalonePlugin extends NebulaIntegTestPlugin {
    protected boolean shouldIncludeInCheckLifecycle() {
        return false
    }
}
