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

The suite uses `SupportedGradleVersion`: Gradle **9.1.0** and the repository wrapper
(**9.5.0** at validation time). Each positive scenario runs in two modes:

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

JUnit XML assertions require one executed test, zero failures/errors/skips and the
expected test method. File assertions also verify compiled classes, exact resource
contents and HTML report locations.

## DSL boundary

The nested positive fixture deliberately uses explicit container creation:

```groovy
facets.create('smokeTest') {
    parentSourceSet = 'integTest'
    testTaskName = 'verifySmoke'
}
```

This does **not** establish support for bare child-project syntax such as
`facets { examples }` under Isolated Projects. A separate boundary test verifies
that bare child syntax stores/reuses ordinary configuration cache, but fails strict
isolation with a parent-project dynamic property lookup and discards the cache on
both tested Gradle versions. This expected failure records an unresolved limitation;
it is not positive compatibility evidence. Bare root syntax is covered positively.

## Reproduction and limits

```shell
./gradlew test --tests nebula.project.StandalonePluginsIsolatedProjectsTest \
  -Pcom.netflix.gradle-predictive-test-selection.enabled=false
```

The matrix contains 12 positive invocations and two boundary invocations. The
boundary invocations each include an intentionally rejected isolated build.

Local validation on **2026-11-05**, with tests running on **JDK 21**, passed all
14 invocations with zero skipped tests. A separate existing regression selection
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
