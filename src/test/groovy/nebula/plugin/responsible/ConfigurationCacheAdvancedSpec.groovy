package nebula.plugin.responsible

import spock.lang.Issue

/**
 * Advanced configuration cache tests for complex facet scenarios.
 * Tests complex hierarchies, multiple facets, dependencies, and parallel execution.
 */
class ConfigurationCacheAdvancedSpec extends BaseIntegrationTestKitSpec {

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

    @Issue("Test complex facet inheritance hierarchy")
    def 'NebulaFacetPlugin with nested facet inheritance works with configuration cache'() {
        given:
        writeHelloWorld('nebula.plugin.plugin')
        writeJUnit5Test('src/functionalTest/java', 'nebula.plugin.plugin')
        writeJUnit5Test('src/smokeTest/java', 'nebula.plugin.plugin')

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
                smokeTest {
                    parentSourceSet = 'functionalTest'
                    testTaskName = 'smokeTest'
                }
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('smokeTest')

        then:
        result1.task(':smokeTest').outcome
        result1.task(':functionalTest') == null  // Should not run functionalTest when running smokeTest
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('smokeTest')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    @Issue("Test multiple test facets with different lifecycle settings")
    def 'Multiple test facets with mixed check lifecycle inclusion work with configuration cache'() {
        given:
        writeHelloWorld('nebula.plugin.plugin')
        writeJUnit5Test('src/functionalTest/java', 'nebula.plugin.plugin')
        writeJUnit5Test('src/smokeTest/java', 'nebula.plugin.plugin')
        writeJUnit5Test('src/integrationTest/java', 'nebula.plugin.plugin')

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
                    includeInCheckLifecycle = true
                }
                smokeTest {
                    parentSourceSet = 'test'
                    testTaskName = 'smokeTest'
                    includeInCheckLifecycle = false
                }
                integrationTest {
                    parentSourceSet = 'test'
                    testTaskName = 'integrationTest'
                    includeInCheckLifecycle = true
                }
            }
        """

        when: 'first build with check'
        def result1 = runTasks('check')

        then: 'included facets run'
        result1.task(':functionalTest').outcome
        result1.task(':integrationTest').outcome
        result1.task(':smokeTest') == null  // Should not run
        result1.output.contains('Configuration cache entry stored')

        when: 'second build with check'
        def result2 = runTasks('check')

        then:
        result2.output.contains('Configuration cache entry reused')

        when: 'explicitly run excluded facet'
        def result3 = runTasks('smokeTest')

        then:
        result3.task(':smokeTest').outcome
    }


}
