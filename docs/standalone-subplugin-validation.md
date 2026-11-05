# Standalone facet and integration-test validation

`StandalonePluginsIsolatedProjectsTest` validates these plugins independently of
`com.netflix.nebula.project`:

- `com.netflix.nebula.facet`
- `com.netflix.nebula.integtest`
- `com.netflix.nebula.integtest-standalone`

The TestKit fixtures apply plugins locally through `withPluginClasspath()`. They do
not use a published release or apply plugins to children from a root script.
Production code and public DSL behavior are unchanged.

## Matrix and assertions

The suite keeps `SupportedGradleVersion`: Gradle **9.1.0** and the repository wrapper
(**9.5.0** at validation time), and adds **9.6.1** to cover Gradle's implicit
parent-lookup fix. This does not change the plugin's minimum Gradle version.
Each positive scenario runs in two modes:

1. Ordinary configuration cache with Isolated Projects explicitly disabled.
2. Configuration cache with Isolated Projects enabled using
   `-Dorg.gradle.unsafe.isolated-projects=true` (these versions predate the stable
   `--isolated-projects` flag).

Both modes require `--configuration-cache-problems=fail`, cold cache storage, warm
reuse and no discarded entries. The fixture build cache is disabled so a successful
task outcome proves execution. Removing fixture build outputs must regenerate
classes, resources and test reports while reusing configuration.

| Scenario | Verified behavior |
| --- | --- |
| Root facet application | Bare `facets { examples }`, configured test facets, `build` compiling non-test facets, renamed `verifyFunctional` task, `main -> test -> functionalTest -> smokeTest` parent outputs, parent resources and JUnit dependencies, opt-outs from `check`, explicit smoke execution, unrelated registered Test task remaining unrealized. |
| Nested integration application | Local plugin application in `:services:app`, declared dependency on `:libraries:producer`, producer classes/resources, consumer main/test outputs on the integration classpath, integration classes/resources and XML/HTML reports, and integration execution through `check`. |
| Nested standalone integration application | The same producer/consumer checks, with `integrationTest` and its compilation/resource tasks absent from `check`; explicit integration execution; a child facet inheriting integration outputs without pulling its parent's Test task into `check`. |
| Configuration inputs | Changing the root `includeFunctional` Gradle property removes the renamed test from `check`; changing the nested consumer build script adds an explicitly created, renamed test facet. Each change requires fresh cache storage followed by reuse. |
| Source inputs | Editing facet Java/resources or producer Java plus integration/smoke Java/resources executes affected compilation, resource, JAR and Test tasks while reusing configuration. Tests assert the changed producer value and resource contents at runtime. |
| Bare child facet syntax | `facets { examples }` stores/reuses ordinary configuration cache on all three versions. On 9.6.1 it also stores/reuses under isolation, regenerates outputs, reruns compilation/resource processing after source edits with reused configuration, and invalidates after a second bare facet is added. The 9.1/9.5 isolation rejection remains separately characterized. |

JUnit XML assertions require one executed test, zero failures/errors/skips and the
expected test method. File assertions also verify compiled classes, exact resource
contents and HTML report locations.

## Existing DSL and Gradle version behavior

Existing DSL syntax and production behavior are unchanged. Ordinary builds on
9.1/9.5 do not need to upgrade Gradle or rewrite facet declarations for this change.
The additional 9.6.1 coverage does not raise the plugin's minimum supported version.

The nested positive fixture deliberately uses explicit container creation:

```groovy
facets.create('smokeTest') {
    parentSourceSet = 'integTest'
    testTaskName = 'verifySmoke'
}
```

That fixture specifically verifies explicit creation. Separate tests exercise the
documented bare child-project syntax, `facets { examples }`, directly:

| Gradle version | Ordinary configuration cache | Isolated Projects |
| --- | --- | --- |
| 9.1.0 | Stores and reuses | Expected parent-lookup rejection; entry discarded |
| 9.5.0 | Stores and reuses | Expected parent-lookup rejection; entry discarded |
| 9.6.1 | Stores and reuses | Stores and reuses with unchanged shorthand |

The older rejection also reproduces with a standard Gradle container without this
plugin. [Gradle 9.6 changed implicit parent lookup to avoid false-positive isolation
violations](https://docs.gradle.org/9.6.0/userguide/isolated_projects.html#sec:no_implicit_parents_lookup).
There is no plugin-side change to DSL lookup precedence or container callback order.
Users enabling isolation on 9.1/9.5 can use explicit creation; unchanged bare child
syntax under isolation is verified on 9.6.1. The older expected failures are
limitation checks, not positive compatibility evidence. Bare root syntax is covered
positively in both modes on all three versions.

## Reproduction and limits

```shell
./gradlew test --tests nebula.project.StandalonePluginsIsolatedProjectsTest \
  -Pcom.netflix.gradle-predictive-test-selection.enabled=false
```

The matrix contains 22 positive invocations and two boundary invocations. Each
boundary invocation intentionally requires an older Gradle isolated build to fail.

Local validation on **2026-11-05**, with tests running on **JDK 21**, passed all
24 invocations with zero skipped tests. A separate existing regression selection
passed **38 tests**, also with zero skips:

```shell
./gradlew test \
  --tests nebula.plugin.responsible.NebulaFacetPluginSpec \
  --tests nebula.plugin.responsible.NebulaFacetPluginKotlinDslIntegrationSpec \
  --tests nebula.plugin.responsible.NebulaIntegTestPluginSpec \
  --tests nebula.project.NebulaIntegTestPluginTest \
  --tests nebula.project.NebulaIntegTestStandalonePluginTest \
  --tests nebula.project.internal.ConfigurationUtilTest \
  -Pcom.netflix.gradle-predictive-test-selection.enabled=false
```

This is bounded validation with Groovy build scripts, Java sources and JUnit 4.
It does not certify all Gradle versions, Kotlin DSL or language-plugin combinations,
IDE integration, arbitrary consumer cross-project configuration, or the whole project
facade. Existing tests for other DSL/API paths remain separate. In particular, the
facade's info, contacts and dependency-locking dependencies and inherited opt-outs
have not been migrated or certified by these tests.
