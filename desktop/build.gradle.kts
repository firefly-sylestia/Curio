// ── Curio Desktop — Compose Multiplatform (JVM) port ────────────────────────
//
// Milestone 1: a native desktop window (Windows .exe via jpackage, plus
// macOS/Linux) that runs the Curio shell with the Spin deck + topic browser
// reading the SAME topic JSON files the Android app ships. The Android
// module stays untouched; this module compiles independently.
//
// The topic assets are referenced in-place (no copy) so content edits in
// app/src/main/assets/topics are picked up by the desktop build automatically.
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)
    sourceSets {
        main {
            // Reuse the Android app's topic JSON files as-is (single source
            // of truth — no duplicate assets in git).
            resources.srcDir("../app/src/main/assets/topics")
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    // `compose.material3` (the String accessor) is deprecated in CMP 1.11+ —
    // declare the artifact directly via the catalog (version tracks the CMP release).
    implementation(libs.compose.material3)
    // Topic JSON parsing (same serializer the Android app uses).
    implementation(libs.com.google.code.gson.gson)
}

// v27t — the desktop release workflow exports RELEASE_VERSION (the git tag
// minus the leading "v", e.g. v1.2.3 -> 1.2.3) so jpackage versions the
// Windows installer from the tag, mirroring the Android app's versionName.
// Local builds (no env var) keep the default.
val envDesktopVersion: String? = System.getenv("RELEASE_VERSION")
    ?.trim()
    ?.removePrefix("v")
    ?.takeIf { it.isNotEmpty() }

compose.desktop {
    application {
        mainClass = "com.curio.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Curio"
            packageVersion = envDesktopVersion ?: "1.0.0"
            description = "Curio — discover the things you love, one curious spin at a time."
            vendor = "Curio"
        }
    }
}
