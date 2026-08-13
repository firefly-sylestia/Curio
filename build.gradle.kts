// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Declared here (apply false) so the Kotlin Gradle Plugin lands on the shared
    // buildscript classpath with a KNOWN version. `org.jetbrains.kotlin.plugin.compose`
    // pulls KGP transitively, which otherwise leaves `org.jetbrains.kotlin.jvm` on the
    // classpath with an *unknown* version — the :desktop module then fails to resolve
    // it with "already on the classpath with an unknown version".
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
}