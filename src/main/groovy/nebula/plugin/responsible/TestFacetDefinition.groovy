package nebula.plugin.responsible

import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * Definition of a source facet, which via the NebulaFacetPlugin will create
 * a source set and runtime/compile configurations. Name is used for the source
 * directory and the prefix for configurations.
 */
@CompileStatic
class TestFacetDefinition extends FacetDefinition {
    /**
     * Name of the test task that will get created
     */
    final Property<String> testTaskName

    /**
     * Whether the task created for the test facet should be a dependency of 'check'.
     */
    final Property<Boolean> includeInCheckLifecycle

    @Inject
    TestFacetDefinition(String name, ObjectFactory objects) {
        super(name, objects)
        this.testTaskName = objects.property(String).convention(name)
        this.includeInCheckLifecycle = objects.property(Boolean).convention(true)
    }

    /**
     * downstream projects are using this setter, so we should not remove it
     */
    void setTestTaskName(String name) {
        testTaskName.set(name)
    }

    /**
     * downstream projects are using this setter, so we should not remove it
     */
    void setIncludeInCheckLifecycle(boolean includeInCheckLifecycle) {
        this.includeInCheckLifecycle.set(includeInCheckLifecycle)
    }
}
