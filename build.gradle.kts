import org.gradle.plugin.compatibility.compatibility

/*
 * Copyright 2014-2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    id("com.netflix.nebula.plugin-plugin")
    `kotlin-dsl`
}

description = "Gradle plugin to setup a responsible Gradle project"

contacts {
    addPerson("nebula-plugins-oss@netflix.com") {
        moniker = "Nebula Plugins Maintainers"
        github = "nebula-plugins"
    }
}

dependencies {
    implementation("com.netflix.nebula:nebula-gradle-interop:latest.release")
    implementation("com.netflix.nebula:nebula-publishing-plugin:latest.release")
    implementation("com.netflix.nebula:gradle-contacts-plugin:latest.release")
    implementation("com.netflix.nebula:gradle-dependency-lock-plugin:latest.release")
    implementation("com.netflix.nebula:gradle-info-plugin:latest.release")
    testImplementation("org.spockframework:spock-junit4:2.4-groovy-4.0")
}

gradlePlugin {
    plugins {
        create("nebulaProject") {
            id = "com.netflix.nebula.project"
            displayName = "Nebula Project"
            description = project.description
            implementationClass = "nebula.plugin.responsible.NebulaResponsiblePlugin"
            tags.addAll("nebula", "project")
        }
        create("nebulaIntegTest") {
            id = "com.netflix.nebula.integtest"
            displayName = "Nebula Integration Test"
            description = "Adds source set and task for running integration tests separately from unit tests"
            implementationClass = "nebula.plugin.responsible.NebulaIntegTestPlugin"
            tags.addAll("nebula", "project")
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("nebulaFacet") {
            id = "com.netflix.nebula.facet"
            displayName = "Nebula Facet"
            description = "Reduce boilerplate for adding additional source sets"
            implementationClass = "nebula.plugin.responsible.NebulaFacetPlugin"
            tags.addAll("nebula", "project")
        }
        create("nebulaIntegTestStandalone") {
            id = "com.netflix.nebula.integtest-standalone"
            displayName = "Nebula Integration Test Standalone"
            description =
                "Adds source set and task for running integration tests separately from unit tests (standalone)"
            implementationClass = "nebula.plugin.responsible.NebulaIntegTestStandalonePlugin"
            tags.addAll("nebula", "project")
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
            targets.all {
                testTask.configure {
                    maxParallelForks = 4
                }
            }
        }
    }
}
tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL // ALL helps when debugging gradle plugins
    gradleVersion = "9.2.1"
    distributionSha256Sum = "72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f"
}