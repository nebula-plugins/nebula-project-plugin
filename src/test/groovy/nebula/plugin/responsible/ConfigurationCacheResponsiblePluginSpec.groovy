package nebula.plugin.responsible

import spock.lang.Issue

/**
 * Configuration cache tests for NebulaResponsiblePlugin (the main plugin).
 * Tests the full responsible plugin suite with configuration cache.
 */
class ConfigurationCacheResponsiblePluginSpec extends BaseIntegrationTestKitSpec {

    @Issue("Test NebulaResponsiblePlugin with configuration cache")
    def 'NebulaResponsiblePlugin works with configuration cache'() {
        given:
        writeHelloWorld('nebula.test')

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.project'
            }

            version = '1.0.0'
            group = 'nebula.test'

            contacts {
                'test@example.com' {
                    moniker 'Test User'
                }
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('build')

        then:
        result1.task(':build').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('build')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

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

    @Issue("Test NebulaResponsiblePlugin with integTest plugin")
    def 'NebulaResponsiblePlugin combined with integTest works with configuration cache'() {
        given:
        writeHelloWorld('nebula.test')
        createFile('src/integTest/java/nebula/test/IntegTest.java') << """
package nebula.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegTest {
    @Test
    public void test() {
        assertTrue(true);
    }
}
"""

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.project'
                id 'com.netflix.nebula.integtest'
            }

            version = '1.0.0'
            group = 'nebula.test'

            ${junit5Configuration()}

            contacts {
                'test@example.com' {
                    moniker 'Test User'
                }
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('build')

        then:
        result1.task(':integrationTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('build')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    @Issue("Test NebulaResponsiblePlugin with custom facets")
    def 'NebulaResponsiblePlugin with custom facets works with configuration cache'() {
        given:
        writeHelloWorld('nebula.test')
        createFile('src/functional/java/Functional.java') << 'public class Functional {}'

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.project'
                id 'com.netflix.nebula.facet'
            }

            version = '1.0.0'
            group = 'nebula.test'

            facets {
                functional
            }

            contacts {
                'test@example.com' {
                    moniker 'Test User'
                }
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('build')

        then:
        result1.task(':functionalClasses').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('build')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    @Issue("Test NebulaResponsiblePlugin with dependency locking disabled")
    def 'NebulaResponsiblePlugin with dependency lock disabled works with configuration cache'() {
        given:
        writeHelloWorld('nebula.test')

        createFile('gradle.properties') << '''
            org.gradle.configuration-cache=true
            nebula.dependencyLockPluginEnabled=false
        '''.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.project'
            }

            version = '1.0.0'
            group = 'nebula.test'

            contacts {
                'test@example.com' {
                    moniker 'Test User'
                }
            }
        """

        when: 'first build to populate cache'
        def result1 = runTasks('build')

        then:
        result1.task(':build').outcome
        result1.output.contains('Configuration cache entry stored')
        !result1.output.contains('dependencyLock')

        when: 'second build to use cache'
        def result2 = runTasks('build')

        then:
        result2.output.contains('Configuration cache entry reused')
    }
}
