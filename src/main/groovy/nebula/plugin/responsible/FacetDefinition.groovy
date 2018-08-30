package nebula.plugin.responsible

import groovy.transform.Canonical
import org.gradle.api.Named

/**
 * Definition of a source facet, which via the NebulaFacetPlugin will create
 * a source set and runtime/compile configurations. Name is used for the source
 * directory and the prefix for configurations.
 */
@Canonical
class FacetDefinition implements Named {
    // TODO Use convention mapping to provide good defaults
    public FacetDefinition(String name) {
        this.name = name
    }

    String name
    String parentSourceSet
    String srcDir

    def getParentSourceSet() {
        return parentSourceSet ?: 'main'
    }

    def getSrcDir() {
        return srcDir ?: name
    }
}
