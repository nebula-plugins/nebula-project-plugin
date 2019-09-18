package nebula.plugin.responsible

import groovy.transform.CompileStatic

/**
 * Extends the {@link NebulaIntegTestPlugin}, without a dependency on the test task in the 'check' lifecycle.
 */
@CompileStatic
class NebulaIntegTestStandalonePlugin extends NebulaIntegTestPlugin {
    protected boolean shouldIncludeInCheckLifecycle() {
        return false
    }
}
