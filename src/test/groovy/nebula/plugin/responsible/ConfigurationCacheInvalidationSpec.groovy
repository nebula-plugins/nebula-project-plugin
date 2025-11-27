package nebula.plugin.responsible

import spock.lang.Issue

/**
 * Configuration cache invalidation tests.
 * Tests that configuration cache is properly invalidated when configurations change.
 */
class ConfigurationCacheInvalidationSpec extends BaseIntegrationTestKitSpec {

    String junit5Configuration() {
        """
            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation platform('org.junit:junit-bom:5.10.0')
                testImplementation 'org.junit.jupiter:junit-jupiter'
                testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
            }

            tasks.withType(Test).configureEach {
                useJUnitPlatform()
            }
        """
    }

    void writeJUnit5Test(String dir, String packageName) {
        def path = "/${packageName.replaceAll(/\./, '/')}"
        createFile("${dir}${path}/HelloWorldTest.java") << """
package ${packageName};

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HelloWorldTest {
    @Test
    public void doesSomething() {
        assertFalse(false);
    }
}
"""
    }

    @Issue("Test configuration cache invalidates when facet properties change")
    def 'Configuration cache invalidates when includeInCheckLifecycle changes'() {
        given:
        writeHelloWorld('nebula.plugin.plugin')
        writeJUnit5Test('src/functionalTest/java', 'nebula.plugin.plugin')

        def buildFileContent = """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }

            ${junit5Configuration()}

            facets {
                functionalTest {
                    parentSourceSet = 'test'
                    testTaskName = 'functionalTest'
                    includeInCheckLifecycle = true
                }
            }
        """
        buildFile << buildFileContent

        when: 'first build'
        def result1 = runTasks('check')

        then:
        result1.task(':functionalTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'change facet configuration'
        buildFile.text = buildFileContent.replace('includeInCheckLifecycle = true', 'includeInCheckLifecycle = false')
        def result2 = runTasks('check')

        then: 'cache is invalidated due to configuration change'
        !result2.output.contains('Configuration cache entry reused')
        result2.task(':functionalTest') == null  // Should not run with check anymore
    }

    @Issue("Test configuration cache invalidates when new facet is added")
    def 'Configuration cache invalidates when new facet is added'() {
        given:
        writeHelloWorld('nebula.plugin.plugin')
        writeJUnit5Test('src/functionalTest/java', 'nebula.plugin.plugin')

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }

            ${junit5Configuration()}

            facets {
                functionalTest {
                    parentSourceSet = 'test'
                    testTaskName = 'functionalTest'
                }
            }
        """

        when: 'first build'
        def result1 = runTasks('build')

        then:
        result1.task(':functionalTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'add new facet'
        writeJUnit5Test('src/smokeTest/java', 'nebula.plugin.plugin')
        buildFile << """
            facets {
                smokeTest {
                    parentSourceSet = 'test'
                    testTaskName = 'smokeTest'
                }
            }
        """
        def result2 = runTasks('build')

        then: 'cache is invalidated'
        result2.task(':smokeTest').outcome
        !result2.output.contains('Configuration cache entry reused')
    }

    @Issue("Test configuration cache invalidates when dependencies change")
    def 'Configuration cache invalidates when dependencies are added'() {
        given:
        writeHelloWorld('nebula.plugin.plugin')
        writeJUnit5Test('src/functionalTest/java', 'nebula.plugin.plugin')

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }

            ${junit5Configuration()}

            facets {
                functionalTest {
                    parentSourceSet = 'test'
                }
            }
        """

        when: 'first build'
        def result1 = runTasks('functionalTest')

        then:
        result1.task(':functionalTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'add new dependency'
        buildFile << """
            dependencies {
                functionalTestImplementation 'org.apache.commons:commons-lang3:3.14.0'
            }
        """
        def result2 = runTasks('functionalTest')

        then: 'cache is invalidated due to dependency change'
        !result2.output.contains('Configuration cache entry reused')
    }

    @Issue("Test configuration cache handles parentSourceSet change")
    def 'Configuration cache invalidates when parentSourceSet changes'() {
        given:
        writeHelloWorld('nebula.plugin.plugin')
        createFile('src/examples/java/Example.java') << 'public class Example {}'
        createFile('src/functional/java/Functional.java') << 'public class Functional {}'

        def buildFileContent = """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }

            facets {
                examples
                functional {
                    parentSourceSet = 'main'
                }
            }
        """
        buildFile << buildFileContent

        when: 'first build'
        def result1 = runTasks('build')

        then:
        result1.task(':functionalClasses').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'change parent source set'
        buildFile.text = buildFileContent.replace("parentSourceSet = 'main'", "parentSourceSet = 'examples'")
        def result2 = runTasks('build')

        then: 'cache is invalidated'
        result2.task(':functionalClasses').outcome
        !result2.output.contains('Configuration cache entry reused')
    }
}
