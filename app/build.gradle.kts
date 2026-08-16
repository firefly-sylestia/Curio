plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ── Release signing wiring ───────────────────────────────────────────────────
//
// Curio consumes the repository KEYSTORE_* secrets from the Android CI
// pipeline (KEYSTORE_BASE64 +
// KEYSTORE_PASSWORD + KEY_ALIAS + KEY_PASSWORD). The CI workflow decodes the
// base64-encoded keystore to ./release.keystore and exports KEYSTORE_PATH etc.
// as env vars at build time, which we read here.
//
// ⚠️  Naming: the local vals below are PREFIXED (envKeyStorePath, envKeyAlias, …)
// on purpose. Inside `create("release") { ... }` the SigningConfig is the implicit
// receiver and its members `keyAlias` / `keyPassword` SHADOW any outer top-level
// vals with the same names. Writing `keyAlias = keyAlias` there is a silent
// self-assignment of null and fails at package time with "SigningConfig 'release'
// is missing required property keyPassword". The env* prefix sidesteps that.
//
// Local dev (no env vars set): falls back to the default debug signing config,
// so `gradlew assembleRelease` still produces an installable-but-debug-keyed
// APK. CI: produces a properly-signed release APK.
val envKeyStorePath: String? = System.getenv("KEYSTORE_PATH")?.trim()?.takeIf { it.isNotEmpty() }
val envKeyStorePassword: String? = System.getenv("KEYSTORE_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
val envKeyAlias: String? = System.getenv("KEY_ALIAS")?.trim()?.takeIf { it.isNotEmpty() }
val envKeyPassword: String? = System.getenv("KEY_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }

// Release tags drive the shipped version name: the release workflow passes
// the git tag (e.g. "v1.2.3") as RELEASE_VERSION and we strip the leading
// "v" so the build's versionName matches the tag ("1.2.3"). Local dev and
// PR CI don't set the env var, so the default "1.0.0" stays. versionCode
// remains the date-based value (store changelogs are keyed to it).
val envReleaseVersion: String? = System.getenv("RELEASE_VERSION")
    ?.trim()
    ?.removePrefix("v")
    ?.takeIf { it.isNotEmpty() }

// Only create release signing if ALL four secrets are present and non-empty.
// GitHub Actions exports missing secrets as empty strings, so .takeIf { it.isNotEmpty() }
// converts them back to null. Without this guard, AGP would create a signing config
// with null/empty values and fail at package time. Falling back to debug signing
// lets builds succeed locally; to get a signed release APK, populate all 4 KEYSTORE_*
// secrets in repo Settings > Secrets and variables > Actions.
val hasReleaseSigningMaterial: Boolean =
    envKeyStorePath != null &&
    envKeyStorePassword != null &&
    envKeyAlias != null &&
    envKeyPassword != null

android {
    namespace = "com.curio.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.curio.app"
        minSdk = 26
        targetSdk = 37
        // v113 — 1.1.0: new cosmic launcher icon + the accumulated feature
        // releases (Updates page, auto backup, dark mode, …). versionCode is
        // date-based; 20260920 is the +1 bump over the previous 20260919.
        versionCode = 20260920
        versionName = envReleaseVersion ?: "1.1.0"

        // Only include English locale — saves ~5-8 MB of APK size.
        // Curio ships as a single-language app. Add others as needed.
        androidResources.localeFilters.clear()
        androidResources.localeFilters.add("en")
    }

    signingConfigs {
        // Only create the release signing config when ALL four env vars are
        // present and non-empty. When any are missing (e.g. local dev), we skip — the
        // release buildType falls back to the default debug signing below so
        // local `gradlew assembleRelease` still works for testing.
        if (hasReleaseSigningMaterial && envKeyStorePath != null && envKeyStorePassword != null && envKeyAlias != null && envKeyPassword != null) {
            create("release") {
                storeFile = file(envKeyStorePath)
                storePassword = envKeyStorePassword
                this.keyAlias = envKeyAlias
                this.keyPassword = envKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Production hardening: shrink and obfuscate release code. The
            // data-layer keep rules in proguard-rules.pro preserve Gson/Room
            // field names and generated database contracts.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigningMaterial) {
                logger.lifecycle("✓ Release APK signed with custom keystore (${envKeyStorePath})")
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "Curio release signing material not configured " +
                    "(KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD). " +
                    "Release APK signed with debug keystore — installable but not for " +
                    "distribution. For a properly-signed release APK, populate the " +
                    "4 secrets in repo Settings > Secrets and variables > Actions."
                )
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // ── Per-ABI release APK splits ──────────────────────────────────────────
    //
    // GitHub Releases are the sideload distribution path, so instead of one
    // fat universal APK we emit a universal APK plus one small APK per CPU
    // architecture. Every device can install the matching ABI; the universal
    // APK is the safe fallback. The release workflow renames each output to a
    // device-friendly name (e.g. Curio-1.0.0-20260906-arm64-v8a-Android8.0+.apk)
    // and publishes an install guide, so there is no per-device guesswork.
    //
    // Note: AGP 9 removed DENSITY splits (use app bundles there), but ABI
    // splits via this DSL are still supported.
    splits {
        abi {
            // AGP 9 renamed the Split toggle from isEnabled to isEnable
            // (verified against gradle-api 9.2.1 sources: `Split.isEnable`).
            // The per-ABI splits are gated on `-PcurioAbiSplits=true` (the
            // default). PR CI passes `-PcurioAbiSplits=false` so it builds
            // ONLY the single universal APK — no per-ABI split packaging
            // (faster PR checks); the tag release workflow keeps the full
            // universal + per-ABI set for sideloading.
            isEnable = project.providers.gradleProperty("curioAbiSplits")
                .orNull?.toBoolean() ?: true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Window-size-class breakpoints for the adaptive tablet/landscape layouts
    // (compact < 600dp, medium 600-839, expanded >= 840).
    implementation(libs.androidx.material3.window.size)
    // Icons are rendered via Material Symbols font ligatures (CurioIcon), NOT
    // the bundled M2 vector set, so androidx.compose.material.icons.core is
    // intentionally absent. Re-add only if a screen needs an M2 vector icon.
    implementation(libs.androidx.compose.animation)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ExoPlayer for audio playback
    implementation(libs.androidx.media3.exoplayer)

    // Gson for JSON serialization (CaptureData -> Room blob)
    implementation(libs.com.google.code.gson.gson)


    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ── CI release APK naming helper ──────────────────────────────────────────
//
// Prints "versionName:versionCode" (single line) so the release workflow can
// name the split APKs without duplicating version numbers. The source of truth
// stays `defaultConfig` above — on a release tag the workflow's RELEASE_VERSION
// env var overrides versionName (tag minus the leading "v"), so the printed
// name:code always matches the APK metadata.
//
// Consumed by .github/workflows/release.yml, which greps the line matching
// ^[0-9][0-9.]*:[0-9]+$ (Gradle may also print warnings to stdout).
// The string is captured at configuration time (cleaner for the configuration
// cache than reading the extension inside doLast).
val ciReleaseVersion = "${android.defaultConfig.versionName}:${android.defaultConfig.versionCode}"
tasks.register("printReleaseVersion") {
    group = "help"
    description = "Prints the app version as NAME:CODE for CI release APK naming."
    doLast {
        println(ciReleaseVersion)
    }
}

// ── Kotlin stdlib alignment ───────────────────────────────────────────────
// Maven Central has begun returning 403 for the LEGACY kotlin-stdlib-jdk8
// redirect artifacts that transitive deps pin on the androidTest/lint
// classpath (kotlin-stdlib-jdk8:1.8.21). Since Kotlin 1.8.20 those artifacts
// are EMPTY POM redirects — every class lives in kotlin-stdlib — so forcing
// them to the project's Kotlin version resolves identically while skipping
// the now-blocked legacy files.
configurations.configureEach {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}"
        )
    }
}

// ── Topic data validation (CURIO_DATA_PLAN.md §5.2 step 3) ─────────────────
//
// Validates every JSON file under app/src/main/assets/topics/*.json against
// the §2 schema. The root is a BARE JSON ARRAY of topic objects (see
// SCHEMA.md in this directory — there is no wrapper). Asserts:
//   - root IS a JSON array (wrapper format is a hard error)
//   - every topic has id (unique cross-file) + categoryId (matches filename)
//   - every topic has subtype/name/teaser/imageUrl/exploreAction
//   - every exploreAction has verb/targetName/durationMinutes/instruction
//   - every instruction <= 600 chars
//   - tier, if present, is in 1..3
//
// Note: empty arrays are ACCEPTED with a warning (placeholder-empty is OK
// during the build-out phase — categories ship one-per-PR cadence per
// CURIO_DATA_PLAN.md §5.1, so a freshly-created category will sit at [] for
// a PR or two before content lands). Schema errors (malformed field,
// duplicate cross-file id, bad categoryId, instruction > 600 chars, tier
// out of range) are still hard fails — they're real bugs, not placeholders.
//
// When assets/topics/ contains any JSON files, this task is wired into
// preBuild so a malformed entry fails the assemble. When the directory is
// empty (placeholder UI ships), the task is a no-op and preBuild is not
// affected.
val topicsDir = file("src/main/assets/topics")
val hasTopicFiles: Boolean = topicsDir.exists() &&
    topicsDir.listFiles { f -> f.extension == "json" }?.isNotEmpty() == true

tasks.register("validateTopics") {
    group = "verification"
    description = "Validates assets/topics/*.json against the CurioTopic schema (CURIO_DATA_PLAN.md §2)."
    doLast {
        if (!topicsDir.exists()) {
            logger.warn("topics/ directory missing — nothing to validate (OK for placeholder UI ships).")
            return@doLast
        }
        val jsonFiles = topicsDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        if (jsonFiles.isEmpty()) {
            logger.warn("topics/ has no JSON files — nothing to validate.")
            return@doLast
        }
        val parser = groovy.json.JsonSlurper()
        // Collect every id across all files first so we can assert global
        // uniqueness (cross-file collisions would break the Room FK on `id`).
        val seenIds = mutableMapOf<String, String>()  // id -> first filename
        var populatedFileCount = 0
        jsonFiles.forEach { json ->
            val expectedCategoryId = json.nameWithoutExtension.uppercase()
            @Suppress("UNCHECKED_CAST")
            val topics = parser.parse(json) as? List<Map<String, Any?>>
                ?: throw GradleException(
                    "${json.name}: root must be a bare JSON array of topic objects " +
                    "(see SCHEMA.md — the wrapper `{categoryId, version, curatedDate, topics}` format was retired)"
                )
            if (topics.isEmpty()) {
                logger.warn("⚠️  ${json.name}: 0 topics (placeholder — content not yet shipped for $expectedCategoryId)")
                return@forEach
            }
            populatedFileCount++
            topics.forEachIndexed { idx, t ->
                val id = t["id"] as? String
                    ?: throw GradleException("${json.name}: topic #$idx missing or non-string `id`")
                val previousFile = seenIds[id]
                if (previousFile != null) {
                    throw GradleException(
                        "duplicate topic id '$id' across files: first seen in $previousFile, also in ${json.name}"
                    )
                }
                seenIds[id] = json.name
                val categoryId = t["categoryId"] as? String
                    ?: throw GradleException("${json.name}: topic '$id' missing or non-string `categoryId`")
                require(categoryId == expectedCategoryId) {
                    "${json.name}: topic '$id' categoryId '$categoryId' " +
                    "does not match filename '$expectedCategoryId'"
                }
                listOf("subtype", "name", "teaser", "imageUrl", "exploreAction").forEach { f ->
                    require(t.containsKey(f)) {
                        throw GradleException("${json.name}: topic '$id' missing required field `$f`")
                    }
                }
                @Suppress("UNCHECKED_CAST")
                val action = t["exploreAction"] as Map<String, Any?>
                listOf("verb", "targetName", "durationMinutes", "instruction").forEach { f ->
                    require(action.containsKey(f)) {
                        throw GradleException("${json.name}: topic '$id' exploreAction missing required field `$f`")
                    }
                }
                val instruction = action["instruction"] as? String
                    ?: throw GradleException("${json.name}: topic '$id' exploreAction.instruction missing or non-string")
                require(instruction.length <= 600) {
                    throw GradleException("${json.name}: topic '$id' instruction is ${instruction.length} chars (max 600)")
                }
                if (t.containsKey("tier")) {
                    val tier = t["tier"]
                    require(tier is Number && tier.toInt() in 1..3) {
                        throw GradleException("${json.name}: topic '$id' tier must be 1, 2, or 3 (got $tier)")
                    }
                }
            }
            logger.lifecycle("✓ ${json.name}: $expectedCategoryId, ${topics.size} topics validated")
        }
        logger.lifecycle(
            "── validateTopics: $populatedFileCount of ${jsonFiles.size} files have content " +
            "(${jsonFiles.size - populatedFileCount} placeholder). " +
            "Schema errors (if any) are listed above.)"
        )
    }
}

// Only hook validateTopics into preBuild when there's actually JSON to check.
// Keeps placeholder-UI builds (no topics yet) friction-free.
if (hasTopicFiles) {
    tasks.named("preBuild") {
        dependsOn("validateTopics")
    }
}
