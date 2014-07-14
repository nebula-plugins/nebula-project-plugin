package nebula.plugin.responsible.ide

import nebula.plugin.responsible.FacetDefinition
import org.gradle.api.tasks.SourceSet

interface IDEPluginConfigurer {
    /**
     * Configures IDE plugin.
     *
     * @param sourceSet SourceSet
     * @param facet Facet definition
     */
    void configure(SourceSet sourceSet, FacetDefinition facet)
}
