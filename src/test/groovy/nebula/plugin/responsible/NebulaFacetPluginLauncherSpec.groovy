package nebula.plugin.responsible

import nebula.test.IntegrationSpec

class NebulaFacetPluginLauncherSpec extends IntegrationSpec {
    def 'tasks get run'() {
        createFile('src/examples/java/Hello.java') << 'public class Hello {}'

        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(NebulaFacetPlugin)}
            facets {
                example
            }
        """

        when:
        def result = runTasksSuccessfully( 'build' )

        then:
        result.wasExecuted(':exampleClasses')
    }

    def "configures Idea project files for a custom test facet"() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])

        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(NebulaFacetPlugin)}
            apply plugin: 'idea'

            facets {
                functionalTest
            }

            repositories {
                maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
            }

            dependencies {
                functionalTestImplementation 'foo:bar:2.4'
                functionalTestRuntimeOnly 'custom:baz:5.1.27'
            }
        """

        writeHelloWorld('nebula.plugin.plugin')
        writeTest('src/functionalTest/java/', 'nebula.plugin.plugin', false)
        runTasksSuccessfully('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def testSourceFolders = moduleXml.component.content.sourceFolder.findAll { it.@isTestSource.text() == 'true' }
        def testSourceFolder = testSourceFolders.find { it.@url.text() == "file://\$MODULE_DIR\$/src/functionalTest/java" }
        testSourceFolder
        def orderEntries = moduleXml.component.orderEntry.findAll { it.@type.text() == 'module-library' && it.@scope.text() == 'TEST' }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('bar-2.4.jar') }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('baz-5.1.27.jar') }
    }

    def "configures Idea project files for a custom facet"() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])

        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(NebulaFacetPlugin)}
            apply plugin: 'idea'

            facets {
                myCustom
            }

            repositories {
                maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
            }

            dependencies {
                myCustomImplementation 'foo:bar:2.4'
                myCustomRuntimeOnly 'custom:baz:5.1.27'
            }
        """

        writeHelloWorld('nebula.plugin.plugin')
        writeTest('src/myCustom/java/', 'nebula.plugin.plugin', false)
        runTasksSuccessfully('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def sourceFolders = moduleXml.component.content.sourceFolder.findAll { it.@isTestSource.text() == 'false' }
        def sourceFolder = sourceFolders.find { it.@url.text() == "file://\$MODULE_DIR\$/src/myCustom/java" }
        sourceFolder
        def orderEntries = moduleXml.component.orderEntry.findAll { it.@type.text() == 'module-library' && it.@exported.text() == '' }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('bar-2.4.jar') }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('baz-5.1.27.jar') }
    }

    def 'configures Idea project before java plugin'() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])

        buildFile << """
            ${applyPlugin(NebulaFacetPlugin)}
            apply plugin: 'idea'

            facets {
                myCustom
            }

            apply plugin: 'java'

            repositories {
                maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
            }

            dependencies {
                myCustomImplementation 'foo:bar:2.4'
                myCustomRuntimeOnly 'custom:baz:5.1.27'
            }
        """

        runTasksSuccessfully('idea')

        then:
        noExceptionThrown()
    }

    def 'makes sure we can extend implementation configurations'() {
        createFile('src/test/resources/application.yml') << """
spring:
  application.name: myapp
logging:
  level:
    org.springframework.web: DEBUG 
"""

        createFile('src/test/resources/bootstrap.yml') << """
spring:
  application.name: myapp
logging:
  level:
    org.springframework.web: DEBUG 
"""
        createFile('src/main/java/com/netflix/Application.java') << """
package com.netflix;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @RestController
    public class HelloController {
    
        @RequestMapping("/")
        public String index() {
            return "Greetings from Spring Boot!";
        }
    
    }
}

"""



        createFile('src/smokeTest/java/com/netflix/HelloTest.java') << """
package com.netflix;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HelloTest {
   @LocalServerPort
    private int port;

    private URL base;

    @Autowired
    private TestRestTemplate template;

    @Before
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + port + "/");
    }

    @Test
    public void getHello() throws Exception {
        ResponseEntity<String> response = template.getForEntity(base.toString(),
                String.class);
        assertThat(response.getBody(), equalTo("Greetings from Spring Boot!"));
    } 
}
        """


        buildFile << """
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.5.RELEASE")
    }
}

apply plugin: 'java'
apply plugin: 'org.springframework.boot'
apply plugin: 'io.spring.dependency-management'
${applyPlugin(NebulaFacetPlugin)}

            repositories {
                mavenCentral() 
            }

            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web")
                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testImplementation("junit:junit")
            }
            facets {
                smokeTest {
                    parentSourceSet = 'test'
                }
            }
        """

        when:
        def result = runTasksSuccessfully( 'smokeTest' )

        then:
        result.wasExecuted(':smokeTest')
    }

    def 'makes sure we can extend annotationProcessor configurations'() {
        buildFile << """
buildscript {
    repositories {
        mavenCentral()
    }
}

apply plugin: 'java'
${applyPlugin(NebulaFacetPlugin)}

            repositories {
                mavenCentral() 
            }

            dependencies {
                testAnnotationProcessor("junit:junit:4.12")
            }
            facets {
                smokeTest {
                    parentSourceSet = 'test'
                }
            }
        """

        when:
        def result = runTasksSuccessfully( 'dependencies', '--configuration', 'smokeTestAnnotationProcessor' )

        then:
        result.standardOutput.contains("""smokeTestAnnotationProcessor - Annotation processors and their dependencies for source set 'smoke test'.
\\--- junit:junit:4.12""")
    }

    def 'test based facet'() {
        when:
        MavenRepoFixture mavenRepoFixture = new MavenRepoFixture(new File(projectDir, 'build'))
        mavenRepoFixture.generateMavenRepoDependencies(['foo:bar:2.4', 'custom:baz:5.1.27'])

        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(NebulaFacetPlugin)}
            apply plugin: 'idea'

            facets {
                functionalTest
            }

            repositories {
                maven { url '$mavenRepoFixture.mavenRepoDir.canonicalPath' }
            }

            dependencies {
                functionalTestImplementation 'foo:bar:2.4'
                functionalTestRuntimeOnly 'custom:baz:5.1.27'
            }
        """
        def result = runTasksSuccessfully('check', '--dry-run')

        then:
        result.standardOutput.contains(":functionalTest SKIPPED")
        result.standardOutput.contains(":functionalTestClasses SKIPPED")
    }

    def 'makes sure we can have a parent source set with was created by facet plugin'() {
        createFile('src/test/resources/application.yml') << """
spring:
  application.name: myapp
logging:
  level:
    org.springframework.web: DEBUG 
"""

        createFile('src/test/resources/bootstrap.yml') << """
spring:
  application.name: myapp
logging:
  level:
    org.springframework.web: DEBUG 
"""
        createFile('src/main/java/com/netflix/Application.java') << """
package com.netflix;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @RestController
    public class HelloController {
    
        @RequestMapping("/")
        public String index() {
            return "Greetings from Spring Boot!";
        }
    
    }
}

"""



        createFile('src/specializedSmokeTest/java/com/netflix/HelloTest.java') << """
package com.netflix;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HelloTest {
   @LocalServerPort
    private int port;

    private URL base;

    @Autowired
    private TestRestTemplate template;

    @Before
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + port + "/");
    }

    @Test
    public void getHello() throws Exception {
        ResponseEntity<String> response = template.getForEntity(base.toString(),
                String.class);
        assertThat(response.getBody(), equalTo("Greetings from Spring Boot!"));
    } 
}
        """


        buildFile << """
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.5.RELEASE")
    }
}

apply plugin: 'java'
apply plugin: 'org.springframework.boot'
apply plugin: 'io.spring.dependency-management'
${applyPlugin(NebulaFacetPlugin)}

            repositories {
                mavenCentral() 
            }

            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web")
                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testImplementation("junit:junit")
            }
            facets {
                smokeTest {
                    parentSourceSet = 'test'
                }
                specializedSmokeTest {
                    parentSourceSet = 'smokeTest'
                }
            }
        """

        when:
        def result = runTasksSuccessfully( 'specializedSmokeTest' )

        then:
        result.wasExecuted(':specializedSmokeTest')
    }

    def 'facet plugin properly consumes parents source sets outputs when groovy plugin is used'() {
        given:
        buildFile << """
            apply plugin: 'groovy'
            ${applyPlugin(NebulaFacetPlugin)}

            repositories {
                mavenCentral()
            }

            facets {
                functionalTest
            }

            dependencies {
                implementation('org.codehaus.groovy:groovy-all:2.5.9')
                testImplementation("junit:junit:4.12")
            }
        """

        writeJavaSourceFile("""
            public class Main {
                public String test() {
                    return "Some string";
                }
            }
        """, "src/main/groovy")

        writeJavaSourceFile("""
            import org.junit.Test;

            public class MainTest {
                @Test
                public void test() {
                    new Main().test();
                }
            }
        """, "src/functionalTest/groovy")

        when:
        def result = runTasksSuccessfully("functionalTest")

        then:
        result.wasExecuted("functionalTest")
    }

    def 'current facets resources are read first before parent source sets'() {
        given:
        buildFile << """
            apply plugin: 'groovy'
            ${applyPlugin(NebulaFacetPlugin)}

            repositories {
                mavenCentral()
            }

            facets {
                functionalTest {
                    parentSourceSet = 'test'
                }
            }

            dependencies {
                testImplementation("junit:junit:4.12")
            }
            
            project.tasks.withType(Test) {
                afterTest { descriptor ->
                    logger.lifecycle("Running test: " + descriptor)
                }
            }
            
        """

        writeJavaSourceFile("""
            import java.util.Properties;
            import java.io.IOException;

            public class MyClass {
                public int readProp() throws IOException {
                    Properties properties = new Properties();
                    properties.load(this.getClass().getResourceAsStream("/foo.properties"));
                    return Integer.parseInt(properties.getProperty("myprop"));
                }
            }

        """, "src/main/java")
        addResource("src/main/resources", "foo.properties", "myprop=2")


        writeJavaSourceFile("""
            import org.junit.Test;
            import java.io.IOException;
            import static org.junit.Assert.*;

            public class MyClassTest {
                
                @Test
                public void test() throws IOException {
                    assertEquals(3, new MyClass().readProp());
                }
            }
        """, "src/test/java")
        addResource("src/test/resources", "foo.properties", "myprop=3")

        writeJavaSourceFile("""
            import org.junit.Test;
            import java.io.IOException;
            import static org.junit.Assert.*;

            public class MyClassFunctionalTest {
            
                @Test
                public void test() throws IOException {
                    assertEquals(4, new MyClass().readProp());
                }
            }
        """, "src/functionalTest/java")
        addResource("src/functionalTest/resources", "foo.properties", "myprop=4")

        when:
        def result = runTasksSuccessfully("check")

        then:
        result.standardOutput.contains("Running test: Test test(MyClassTest)")
        result.standardOutput.contains("Running test: Test test(MyClassFunctionalTest)")
        result.wasExecuted("test")
        result.wasExecuted("functionalTest")
    }

    def 'works with java-library-plugin'() {
        given:
        buildFile << """
            apply plugin: 'java-library'
            ${applyPlugin(NebulaFacetPlugin)}

            repositories {
                mavenCentral()
            }

            facets {
                functionalTest {
                    parentSourceSet = 'test'
                }
            }

            dependencies {
                testImplementation("junit:junit:4.12")
            }
            
            project.tasks.withType(Test) {
                afterTest { descriptor ->
                    logger.lifecycle("Running test: " + descriptor)
                }
            }
            
        """

        writeJavaSourceFile("""
            import java.util.Properties;
            import java.io.IOException;

            public class MyClass {
                public int readProp() throws IOException {
                    Properties properties = new Properties();
                    properties.load(this.getClass().getResourceAsStream("/foo.properties"));
                    return Integer.parseInt(properties.getProperty("myprop"));
                }
            }

        """, "src/main/java")
        addResource("src/main/resources", "foo.properties", "myprop=2")


        writeJavaSourceFile("""
            import org.junit.Test;
            import java.io.IOException;
            import static org.junit.Assert.*;

            public class MyClassTest {
                
                @Test
                public void test() throws IOException {
                    assertEquals(3, new MyClass().readProp());
                }
            }
        """, "src/test/java")
        addResource("src/test/resources", "foo.properties", "myprop=3")

        writeJavaSourceFile("""
            import org.junit.Test;
            import java.io.IOException;
            import static org.junit.Assert.*;

            public class MyClassFunctionalTest {
            
                @Test
                public void test() throws IOException {
                    assertEquals(4, new MyClass().readProp());
                }
            }
        """, "src/functionalTest/java")
        addResource("src/functionalTest/resources", "foo.properties", "myprop=4")

        when:
        def result = runTasksSuccessfully("check")

        then:
        result.standardOutput.contains("Running test: Test test(MyClassTest)")
        result.standardOutput.contains("Running test: Test test(MyClassFunctionalTest)")
        result.wasExecuted("test")
        result.wasExecuted("functionalTest")
    }
}
