Nebula Project Plugin
=====================
![Support Status](https://img.shields.io/badge/nebula-active-green.svg)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.netflix.nebula/nebula-project-plugin/maven-metadata.xml.svg?label=gradlePluginPortal)](https://plugins.gradle.org/plugin/nebula.project)
[![Maven Central](https://img.shields.io/maven-central/v/com.netflix.nebula/nebula-project-plugin)](https://maven-badges.herokuapp.com/maven-central/com.netflix.nebula/nebula-project-plugin)
![Build](https://github.com/nebula-plugins/nebula-project-plugin/actions/workflows/nebula.yml/badge.svg)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-project-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)
Provides healthy defaults for a Gradle project. Currently adds:

* Builds Javadoc and Sources jars
* Record information about the build and stores it in the .jar, via gradle-info-plugin
* Easy specification of people involved in a project via gradle-contacts-plugin
* Doesn't fail javadoc if there are none found

`nebula-project` plugin introduces [Nebula Dependency Lock Plugin](https://github.com/nebula-plugins/gradle-dependency-lock-plugin) out of the box.

If you prefer to use [Gradle's Locking dependency versions mechanism](https://docs.gradle.org/current/userguide/dependency_locking.html), you can use `nebula.dependencyLockPluginEnable` project property to disable Nebula's plugin. 

Compatibilty notes
======================
This plugin uses APIs that are not available on Gradle < 5.0.

From v7.0.0, nebula-project-plugin supports only Gradle 5.0+

Nebula Facet Plugin
=======================
A routine pattern is wanting a new [SourceSet](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html) with an accompanying [Configuration](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html) for dependencies. We consider this another facet of your project and can be modeled via the Nebula Facet plugin. This plugin will create a SourceSet with the name provided, which extends the main SourceSet, and consequently it'll create configurations for compile and runtime, which extends from the parent SourceSet. Their "classes" task will be wired up to the build task. 

```groovy
apply plugin: 'nebula.facet'
facets {
    examples
    performance
}
```

The previous definition would make examples and performance SourceSets, so that code can go in `src/examples/java` and `src/performance/java`. It'll get four configurations: `examplesCompile`, `examplesRuntime`, `performanceCompile`, `performanceRuntime`. Those configurations will extend compile and runtime respectively. Each one can be configured to inherit from another SourceSet, e.g.

```groovy
facets {
    functional {
        parentSourceSet = 'test'
    }
}
```

That will cause the `functionalCompile` to extend from `testCompile`, and `functionalRuntime` to extend from `testRuntime`, since those are the configurations from the "test" SourceSet.  

Test Facets
--------------

If "Test" is in the facet name then a Test task will be created (though it will still inherit from the "main" SourceSet--use the above configuration to make the test facet extends from the test SourceSet). For example:

```groovy
facets {
    integTest
}
```

This will create a test task called `integTest` in addition to the `integTest` SourceSet. The parent SourceSet can still be overriden like above, and the task name can be set:

```groovy
facets {
    integTest {
        parentSourceSet = 'main'
        testTaskName = 'integrationTest'
    }
}
```

Test facets may opt out of a dependency on the 'check' task by using `includeInCheckLifecycle`:

```groovy
facets {
    integTest {
        parentSourceSet = 'main'
        testTaskName = 'integrationTest'
        includeInCheckLifecycle = false
    }
}
```

Nebula IntegTest Plugin
=======================
A corrolary from the Facet Plugin is a concrete Facet, this plugin provides one specifically for Integration Tests. By applying this plugin, you'll get an `integrationTest` Test task, where sources go in `src/integTest/java` and dependencies can go into `integTestImplementation` and `integTestRuntimeOnly` (which extend from the test SourceSet), with the 'check' task depending on the task. To apply the plugin:

```groovy
apply plugin: 'nebula.integtest'
```

Alternatively, the task can be a standalone task that isn't depended on by `check` by applying:

```groovy
apply plugin: 'nebula.integtest-standalone'
```
