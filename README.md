Nebula Project Plugin
=====================
[![Build Status](https://travis-ci.org/nebula-plugins/nebula-project-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/nebula-project-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/nebula-project-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/nebula-project-plugin?branch=master)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/nebula-project-plugin?utm_source=badgeutm_medium=badgeutm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-project-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Provides healthy defaults for a Gradle project. Currently adds:

* Builds Javadoc and Sources jars
* Record information about the build and stores it in the .jar, via gradle-info-plugin
* Easy specification of people involved in a project via gradle-contacts-plugin
* Doesn't fail javadoc if there are none found

Nebula Facet Plugin
=======================
A routine pattern is wanting a new SourceSet with an accompanying Configuration for dependencies. We consider this another facet of your project and can be modeled via the Nebula Facet plugin. This plugin will create a SourceSet with the name provided, which extends the main SourceSet, and consequencely it'll create configurations for compile and runtime, which extends from the parent SourceSet. Their "classes" task will be wired up to the build task. 

    apply plugin: 'nebula.facet'
    facets {
        examples
        performance
    }

The previous definition would make a examples and performance SourceSet, so that code can go in src/examples/java and src/performance/java. It'll get four configurations: examplesCompile, examplesRuntime, performanceCompile, performanceRuntime. Those configuration will extends compile and runtime respectively. Each one can be configured to inherit from another SourceSet, e.g.

    facets {
        functional {
            parentSourceSet = 'test'
        }
    }

That will cause the functionCompile to extend from testCompile, and functionalRuntime to extend from testRuntime, since those are the configurations from the "test" SourceSet.  

Test Facets
--------------

If "Test" is in the facet name, then a Test task would be created (though it'll still inherit from the "main" SourceSet", use the above configuration to make the test facet extends from the test SourceSet). E.g.

    facets {
        integTest
    }

That will create a test task called integTest in addition to the integTest SourceSet. The parent SourceSet can still be overriden like above, and the task name can be set:

    facets {
        integTest {
            parentSourceSet = 'main'
            testTaskName = 'integrationTest'
        }
    }

Nebula IntegTest Plugin
=======================
A correlary from the Facet Plugin is a concrete Facet, this plugin provide one specifically for Integration Tests. By applying this plugin, you'll get a integrationTest Test task, where sources go in src/integTest/java and dependencies can go into integTestCompile and integTestRuntime (which extend from the test SourceSet). E.g.

    apply plugin: 'nebula.integtest'


