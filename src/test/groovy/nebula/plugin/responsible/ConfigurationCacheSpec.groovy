package nebula.plugin.responsible

import spock.lang.Issue

/**
 * Tests to verify configuration cache compatibility for all plugins in this project.
 * Configuration cache is enabled by default in BaseIntegrationTestKitSpec.
 */
class ConfigurationCacheSpec extends BaseIntegrationTestKitSpec {

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

    def 'NebulaFacetPlugin works with configuration cache on build task'() {
        given:
        createFile('src/examples/java/Hello.java') << 'public class Hello {}'

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }
            facets {
                examples
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('build')

        then:
        result1.task(':examplesClasses').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('build')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    def 'NebulaFacetPlugin works with configuration cache on custom test facet'() {
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

        when: 'first build to populate cache'
        def result1 = runTasks('functionalTest')

        then:
        result1.task(':functionalTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('functionalTest')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    def 'NebulaIntegTestPlugin works with configuration cache'() {
        given:
        writeHelloWorld('nebula.hello')
        writeJUnit5Test('src/integTest/java', 'nebula.hello')

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.integtest'
            }

            ${junit5Configuration()}
        """

        when: 'first build to populate cache'
        def result1 = runTasks('integrationTest')

        then:
        result1.task(':integrationTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('integrationTest')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    def 'NebulaIntegTestStandalonePlugin works with configuration cache'() {
        given:
        writeHelloWorld('nebula.hello')
        writeJUnit5Test('src/integTest/java', 'nebula.hello')

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.integtest-standalone'
            }

            ${junit5Configuration()}
        """

        when: 'first build to populate cache'
        def result1 = runTasks('integrationTest')

        then:
        result1.task(':integrationTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('integrationTest')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    def 'NebulaFacetPlugin with multiple facets works with configuration cache'() {
        given:
        createFile('src/functional/java/Functional.java') << 'public class Functional {}'
        createFile('src/smoke/java/Smoke.java') << 'public class Smoke {}'
        createFile('src/examples/java/Example.java') << 'public class Example {}'

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }

            facets {
                functional
                smoke
                examples
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('build')

        then:
        result1.task(':functionalClasses').outcome
        result1.task(':smokeClasses').outcome
        result1.task(':examplesClasses').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('build')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    def 'NebulaFacetPlugin with test facet included in check works with configuration cache'() {
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
                    includeInCheckLifecycle = true
                }
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('check')

        then:
        result1.task(':functionalTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('check')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    def 'NebulaFacetPlugin with test facet excluded from check works with configuration cache'() {
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
                    includeInCheckLifecycle = false
                }
            }
        """

        when: 'first run to populate cache'
        def result1 = runTasks('check')

        then:
        result1.output.contains('Configuration cache entry stored')

        when: 'second run to use cache'
        def result2 = runTasks('check')

        then:
        result2.output.contains('Configuration cache entry reused')

        when: 'run the test task explicitly'
        def result3 = runTasks('functionalTest')

        then:
        result3.task(':functionalTest').outcome
    }

}
