package nebula.project

import nebula.test.dsl.ProjectBuilder
import nebula.test.dsl.SourceSetBuilder

fun ProjectBuilder.useJUnitJupiter() {
    rawBuildScript("""
        testing {
            suites {
                named<JvmTestSuite>("test") {
                    useJUnitJupiter()
                }
            }
        }
    """)
}

fun SourceSetBuilder.junit4Test(){
    java(
        "nebula/HelloWorldTest.java",
        // language=java
        """
package nebula;

import org.junit.Assert;
import org.junit.Test;

public class HelloWorldTest {
    @Test 
    public void doesSomething() {
        Assert.assertTrue(true); 
    }
}
"""
    )
}
fun SourceSetBuilder.junit5Test(){
    java(
        "nebula/HelloWorldTest.java",
        // language=java
        """
package nebula;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HelloWorldTest {
    @Test 
    public void doesSomething() {
        Assertions.assertTrue(true); 
    }
}
"""
    )
}