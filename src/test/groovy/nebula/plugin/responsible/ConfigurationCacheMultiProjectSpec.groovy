package nebula.plugin.responsible

import spock.lang.Issue

/**
 * Configuration cache tests for multi-project builds with facets.
 */
class ConfigurationCacheMultiProjectSpec extends BaseIntegrationTestKitSpec {

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

    @Issue("Test configuration cache with facets in multi-project builds")
    def 'Facets in multi-project builds work with configuration cache'() {
        given: 'multi-project setup'
        createFile('settings.gradle') << """
            rootProject.name = 'multi-project-test'
            include 'sub1', 'sub2'
        """

        buildFile << """
            allprojects {
                repositories {
                    mavenCentral()
                }
            }
        """

        // Sub-project 1 with facets
        createFile('sub1/build.gradle') << """
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
        writeHelloWorld('sub1/src/main/java', 'nebula.sub1')
        writeJUnit5Test('sub1/src/functionalTest/java', 'nebula.sub1')

        // Sub-project 2 with facets
        createFile('sub2/build.gradle') << """
            plugins {
                id 'java'
                id 'com.netflix.nebula.facet'
            }

            ${junit5Configuration()}

            facets {
                integrationTest {
                    parentSourceSet = 'test'
                    testTaskName = 'integrationTest'
                }
            }

            dependencies {
                implementation project(':sub1')
            }
        """
        writeHelloWorld('sub2/src/main/java', 'nebula.sub2')
        writeJUnit5Test('sub2/src/integrationTest/java', 'nebula.sub2')

        when: 'first build to populate cache'
        def result1 = runTasks('build')

        then:
        result1.task(':sub1:functionalTest').outcome
        result1.task(':sub2:integrationTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks('build')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    @Issue("Test cross-project facet dependencies")
    def 'Cross-project facet dependencies work with configuration cache'() {
        given:
        createFile('settings.gradle') << """
            rootProject.name = 'cross-project-test'
            include 'lib', 'app'
        """

        buildFile << """
            allprojects {
                repositories {
                    mavenCentral()
                }
            }
        """

        // Library project
        createFile('lib/build.gradle') << """
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
        writeHelloWorld('lib/src/main/java', 'nebula.lib')
        writeJUnit5Test('lib/src/functionalTest/java', 'nebula.lib')

        // App project depending on lib's functionalTest
        createFile('app/build.gradle') << """
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

            dependencies {
                implementation project(':lib')
                functionalTestImplementation project(':lib')
            }
        """
        writeHelloWorld('app/src/main/java', 'nebula.app')
        writeJUnit5Test('app/src/functionalTest/java', 'nebula.app')

        when: 'first build to populate cache'
        def result1 = runTasks(':app:functionalTest')

        then:
        result1.task(':lib:classes').outcome
        result1.task(':app:functionalTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second build to use cache'
        def result2 = runTasks(':app:functionalTest')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    @Issue("Test parallel multi-project build with configuration cache")
    def 'Parallel multi-project builds with facets work with configuration cache'() {
        given:
        createFile('settings.gradle') << """
            rootProject.name = 'parallel-multi-project'
            include 'module1', 'module2', 'module3'
        """

        buildFile << """
            allprojects {
                repositories {
                    mavenCentral()
                }
            }
        """

        ['module1', 'module2', 'module3'].each { module ->
            createFile("${module}/build.gradle") << """
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
            writeHelloWorld("${module}/src/main/java", "nebula.${module}")
            writeJUnit5Test("${module}/src/functionalTest/java", "nebula.${module}")
        }

        when: 'first parallel build'
        def result1 = runTasks('--parallel', 'functionalTest')

        then:
        result1.task(':module1:functionalTest').outcome
        result1.task(':module2:functionalTest').outcome
        result1.task(':module3:functionalTest').outcome
        result1.output.contains('Configuration cache entry stored')

        when: 'second parallel build'
        def result2 = runTasks('--parallel', 'functionalTest')

        then:
        result2.output.contains('Configuration cache entry reused')
    }

    void writeHelloWorld(String dir, String packageName) {
        def path = "/${packageName.replaceAll(/\./, '/')}"
        createFile("${dir}${path}/HelloWorld.java") << """
package ${packageName};

public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
"""
    }
}
