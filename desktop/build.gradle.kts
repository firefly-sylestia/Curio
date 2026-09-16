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
//
// ⚠️ jpackage's packageVersion is NOT a free-form string — each bundle format
// validates it, and MSI is the strict one: it demands exactly
// MAJOR.MINOR.BUILD (255 / 255 / 65535 ceilings), so a tag carrying fewer
// numeric components fails CONFIGURATION with "Illegal version for 'Msi':
// '2.1' is not a valid version". A configuration failure takes the whole
// build down — release.yml runs :app:assembleRelease with the same
// RELEASE_VERSION exported, and Gradle configures every project first, so a
// desktop version mistake broke the Android release (the v2.1-beta6 tag).
//
// So normalize the tag instead of trusting it: drop prerelease/build
// suffixes (v1.0.2-beta -> 1.0.2), pad the version to the three numeric
// components MSI requires (v2.1-beta6 -> 2.1.0, v2 -> 2.0.0), and fall back
// to the module default when the tag yields nothing jpackage would accept.
// The Android versionName is a plain string and keeps its suffix; the
// release artifacts' names keep the full tag too.

/**
 * The installer version jpackage will accept for [parts]: MAJOR.MINOR.BUILD
 * with missing components padded with 0. Returns null when the components are
 * empty or outside the ranges jpackage allows, so the caller can fall back
 * rather than fail configuration.
 */
fun jpackagePackageVersion(parts: List<Int>): String? {
    val core = parts.take(3)
    if (core.isEmpty()) return null
    val (major, minor, build) = core + List(3 - core.size) { 0 }
    if (major > 255 || minor > 255 || build > 65535) return null
    return "$major.$minor.$build"
}

val rawReleaseVersion: String? = System.getenv("RELEASE_VERSION")
    ?.trim()
    ?.removePrefix("v")
    ?.takeIf { it.isNotEmpty() }

val envDesktopVersion: String? = rawReleaseVersion
    ?.substringBefore('-')
    ?.substringBefore('+')
    ?.split('.')
    ?.mapNotNull { it.toIntOrNull() }
    ?.let { jpackagePackageVersion(it) }

if (rawReleaseVersion != null && envDesktopVersion == null) {
    logger.warn(
        "Curio desktop: RELEASE_VERSION='$rawReleaseVersion' has no usable numeric " +
            "version (jpackage needs MAJOR.MINOR.BUILD); using the default package version."
    )
}

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
