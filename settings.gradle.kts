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
            // Pin every org.jetbrains.kotlin.* plugin request to the catalog
            // Kotlin version. When a plugin reaches the classpath with a
            // different (or unknown) version, the request fails with "already on
            // the classpath with an unknown version, so compatibility cannot be
            // checked"; pinning lets Gradle verify it instead. (The :desktop
            // module's Compose plugin was the original reason for this block and
            // is gone — the pin stays because it is what keeps the remaining
            // Kotlin plugin requests checkable.)
            if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
                // Keep in sync with the `kotlin` version in gradle/libs.versions.toml.
                // (The catalog accessor isn't available inside pluginManagement.)
                useVersion("2.3.21")
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
// The Compose Multiplatform desktop module (:desktop) was removed in favour of
// the separate Flutter client in curio_flutter/ — a different app in a different
// language, built by .github/workflows/curio-flutter.yml, not by Gradle.
