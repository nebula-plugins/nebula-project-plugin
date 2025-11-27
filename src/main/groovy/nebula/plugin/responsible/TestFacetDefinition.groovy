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
    private final Property<String> testTaskNameProperty
    private final Property<Boolean> includeInCheckLifecycleProperty

    @Inject
    TestFacetDefinition(String name, ObjectFactory objects) {
        super(name, objects)
        this.testTaskNameProperty = objects.property(String).convention(name)
        this.includeInCheckLifecycleProperty = objects.property(Boolean).convention(true)
    }

    /**
     * Name of the test task that will get created
     */
    Property<String> getTestTaskNameProperty() {
        return testTaskNameProperty
    }

    String getTestTaskName() {
        return testTaskNameProperty.get()
    }

    void setTestTaskName(String testTaskName) {
        this.testTaskNameProperty.set(testTaskName)
    }

    /**
     * Whether the task created for the test facet should be a dependency of 'check'.
     */
    Property<Boolean> getIncludeInCheckLifecycleProperty() {
        return includeInCheckLifecycleProperty
    }

    boolean getIncludeInCheckLifecycle() {
        return includeInCheckLifecycleProperty.get()
    }

    void setIncludeInCheckLifecycle(boolean includeInCheckLifecycle) {
        this.includeInCheckLifecycleProperty.set(includeInCheckLifecycle)
    }
}
