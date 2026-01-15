pluginManagement {
    plugins {
        id("com.netflix.nebula.plugin-plugin") version ("25.+")
    }
}

plugins {
    id("com.gradle.develocity") version ("4.2")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}

rootProject.name = "nebula-project-plugin"
