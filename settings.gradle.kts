pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            // The :desktop module applies org.jetbrains.compose, whose Gradle
            // plugin pulls the Kotlin Gradle plugin onto the project classpath
            // transitively. Explicit org.jetbrains.kotlin.* requests then fail
            // with "already on the classpath with an unknown version, so
            // compatibility cannot be checked". Pin every Kotlin plugin to the
            // catalog Kotlin version so the requests can be verified.
            if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
                useVersion(libs.versions.kotlin.get())
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Mapbox Maven repository (requires access token in gradle.properties)
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("mapbox_downloads_token").orElse("").get()
            }
        }
    }
}

rootProject.name = "Curio"
include(":app")
include(":desktop")
