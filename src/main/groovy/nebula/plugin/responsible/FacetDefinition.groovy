package nebula.plugin.responsible

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.gradle.api.Named

/**
 * Definition of a source facet, which via the NebulaFacetPlugin will create
 * a source set and runtime/compile configurations. Name is used for the source
 * directory and the prefix for configurations.
 */
@Canonical
@CompileStatic
class FacetDefinition implements Named {
    // TODO Use convention mapping to provide good defaults
    FacetDefinition(String name) {
        this.name = name
    }

    String name
    String parentSourceSet

    String getParentSourceSet() {
        return parentSourceSet ?: 'main'
    }
}
