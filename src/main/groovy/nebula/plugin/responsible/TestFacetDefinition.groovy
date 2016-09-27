package nebula.plugin.responsible

import groovy.transform.Canonical

/**
 * Definition of a source facet, which via the NebulaFacetPlugin will create
 * a source set and runtime/compile configurations. Name is used for the source
 * directory and the prefix for configurations.
 */
@Canonical
class TestFacetDefinition extends FacetDefinition {

    public TestFacetDefinition(String name) {
        super(name)
    }

    /**
     * Name of the test task that will get created
     */
    String testTaskName

    def getTestTaskName() {
        testTaskName ?: getName()
    }

    /**
     * Whether the task created for the test facet should be a dependency of 'check'.
     */
    boolean includeInCheckLifecycle = true

    @Override
    protected getDefaultParentSourceSet() {
        return 'test'
    }
}
