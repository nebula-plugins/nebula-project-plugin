package nebula.plugin.responsible

import groovy.transform.CompileStatic
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * Definition of a source facet, which via the NebulaFacetPlugin will create
 * a source set and runtime/compile configurations. Name is used for the source
 * directory and the prefix for configurations.
 */
@CompileStatic
class FacetDefinition implements Named {
    private final String name
    final Property<String> parentSourceSet

    @Inject
    FacetDefinition(String name, ObjectFactory objects) {
        this.name = name
        this.parentSourceSet = objects.property(String).convention('main')
    }

    @Override
    String getName() {
        return name
    }

    /**
     * downstream projects are using this setter, so we should not remove it
     */
    void setParentSourceSet(String name) {
        parentSourceSet.set(name)
    }
}
