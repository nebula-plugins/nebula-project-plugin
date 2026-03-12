package nebula.project.internal

import org.assertj.core.api.AbstractAssert
import org.assertj.core.error.ShouldBeTrue.shouldBeTrue
import org.assertj.core.error.ShouldContain
import org.assertj.core.internal.Failures
import org.assertj.core.internal.Iterables
import org.assertj.core.presentation.PredicateDescription
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.jetbrains.annotations.Contract

class ConfigurationAssert(configuration: Configuration?) : AbstractAssert<ConfigurationAssert, Configuration?>(
    configuration, ConfigurationAssert::class.java
) {
    companion object {
        /**
         * fluent entry point
         *
         * @param actual the configuration task to assert on
         * @return a new instance of the configuration assertion
         */
        @Contract("_ -> new")
        @JvmStatic
        fun assertThat(actual: Configuration): ConfigurationAssert {
            return ConfigurationAssert(actual)
        }

        private val iterables : Iterables =  Iterables.instance()
    }

    fun <T: Any> hasAttribute(attribute: Attribute<T>, value: T): ConfigurationAssert{
        this.objects.assertNotNull(info, actual)
        iterables.assertContains(info, actual!!.attributes.keySet(), arrayOf(attribute))
        objects.assertEqual(info, actual!!.attributes.getAttribute(attribute), value)
        return this
    }

    fun hasDependency(group: String? = null, name: String? = null, version: String? = null): ConfigurationAssert {
        this.objects.assertNotNull(this.info, actual)
        val dependency = buildString {
            if (group != null) {
                append(group)
            }
            if (name != null) {
                if (this.isNotEmpty()) {
                    append(":")
                }
                append(name)
            }
            if (version != null) {
                if (this.isNotEmpty()) {
                    append(":")
                }
                append(version)
            }
        }
        iterables.assertAnyMatch(
            this.info,
            actual!!.allDependencies.map {
                buildString {
                    if (group != null) {
                        append(group)
                    }
                    if (name != null) {
                        if (this.isNotEmpty()) {
                            append(":")
                        }
                        append(name)
                    }
                    if (version != null) {
                        if (this.isNotEmpty()) {
                            append(":")
                        }
                        append(version)
                    }
                }
            }, { it == dependency },
            PredicateDescription("Configuration $name inherits dependency $dependency")
        )
        return this
    }
}