package nebula.plugin.responsible

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Definition of a source facet, which via the NebulaFacetPlugin will create
 * a source set and runtime/compile configurations. Name is used for the source
 * directory and the prefix for configurations.
 */
@Canonical
@CompileStatic
class TestFacetDefinition extends FacetDefinition {

    public TestFacetDefinition(String name) {
        super(name)
    }

    /**
     * Name of the test task that will get created
     */
    String testTaskName

    String getTestTaskName() {
        testTaskName ?: getName()
    }

    /**
     * Whether the task created for the test facet should be a dependency of 'check'.
     */
    boolean includeInCheckLifecycle = true
}
