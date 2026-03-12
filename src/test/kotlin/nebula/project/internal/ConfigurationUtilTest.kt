package nebula.project.internal

import nebula.project.internal.ConfigurationAssert.Companion.assertThat
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

internal class ConfigurationUtilTest {
    @Test
    fun `test implementation`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainSourceSet = javaExt.sourceSets.getByName("main")
        project.dependencies.add(mainSourceSet.implementationConfigurationName, "org.assertj:assertj-core:3.27.7")
        val newSourceSet = javaExt.sourceSets.create("new")
        ConfigurationUtil.extendSourceSet(project, mainSourceSet, newSourceSet)
        assertThat(project.configurations.getByName(newSourceSet.implementationConfigurationName))
            .`as`("implementation inherits dependencies")
            .hasDependency("org.assertj","assertj-core","3.27.7")
    }

    @Test
    fun `test runtimeOnly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainSourceSet = javaExt.sourceSets.getByName("main")
        project.dependencies.add(mainSourceSet.runtimeOnlyConfigurationName, "org.assertj:assertj-core:3.27.7")
        val newSourceSet = javaExt.sourceSets.create("new")
        ConfigurationUtil.extendSourceSet(project, mainSourceSet, newSourceSet)
        assertThat(project.configurations.getByName(newSourceSet.runtimeOnlyConfigurationName))
            .`as`("implementation inherits dependencies")
            .hasDependency("org.assertj","assertj-core","3.27.7")
    }

    @Test
    fun `test compileOnly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainSourceSet = javaExt.sourceSets.getByName("main")
        project.dependencies.add(mainSourceSet.compileOnlyConfigurationName, "org.assertj:assertj-core:3.27.7")
        val newSourceSet = javaExt.sourceSets.create("new")
        ConfigurationUtil.extendSourceSet(project, mainSourceSet, newSourceSet)
        assertThat(project.configurations.getByName(newSourceSet.compileOnlyConfigurationName))
            .`as`("implementation inherits dependencies")
            .hasDependency("org.assertj","assertj-core","3.27.7")
    }

    @Test
    fun `test annotationProcessor`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainSourceSet = javaExt.sourceSets.getByName("main")
        project.dependencies.add(mainSourceSet.annotationProcessorConfigurationName, "org.assertj:assertj-core:3.27.7")
        val newSourceSet = javaExt.sourceSets.create("new")
        ConfigurationUtil.extendSourceSet(project, mainSourceSet, newSourceSet)
        assertThat(project.configurations.getByName(newSourceSet.annotationProcessorConfigurationName))
            .`as`("implementation inherits dependencies")
            .hasDependency("org.assertj","assertj-core","3.27.7")
    }

    @Test
    fun `test runtime`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainSourceSet = javaExt.sourceSets.getByName("main")
        val newSourceSet = javaExt.sourceSets.create("new")
        ConfigurationUtil.extendSourceSet(project, mainSourceSet, newSourceSet)
        val customAttribute = Attribute.of("custom", String::class.java)
        project.configurations.getByName(mainSourceSet.runtimeClasspathConfigurationName).attributes {
            attribute(customAttribute, "foo")
        }
        assertThat(project.configurations.getByName(newSourceSet.runtimeClasspathConfigurationName))
            .`as`("runtime inherits attributes")
            .hasAttribute(customAttribute, "foo")
    }


    @Test
    fun `test compile`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainSourceSet = javaExt.sourceSets.getByName("main")
        val newSourceSet = javaExt.sourceSets.create("new")
        ConfigurationUtil.extendSourceSet(project, mainSourceSet, newSourceSet)
        val customAttribute = Attribute.of("custom", String::class.java)
        project.configurations.getByName(mainSourceSet.compileClasspathConfigurationName).attributes {
            attribute(customAttribute, "foo")
        }
        assertThat(project.configurations.getByName(newSourceSet.compileClasspathConfigurationName))
            .`as`("compile inherits attributes")
            .hasAttribute(customAttribute, "foo")
    }
}