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
// so `gradlew assembleCoreRelease` / `assembleFullRelease` still produces an
// installable-but-debug-keyed APK. CI: produces a properly-signed release APK.
// (The bare `assembleRelease` is ambiguous once the `edition` flavors exist.)
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

// v354 — OPTIONAL Google Books API key (free tier): when set (e.g. as a
// GitHub Actions secret GOOGLE_BOOKS_API_KEY or a local env var), book-cover
// and rating lookups append &key= for the reliable quota. Empty locally.
// Setup: copy .env.example and set GOOGLE_BOOKS_API_KEY in your environment
// (or repo Settings > Secrets and variables > Actions).
val envGoogleBooksApiKey: String? = System.getenv("GOOGLE_BOOKS_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
// Escaped for the generated BuildConfig string literal (quotes/backslashes).
val gbkEscaped: String = envGoogleBooksApiKey
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// v356 — OPTIONAL LibraryThing developer key (free tier): covers.librarything.com
// is ISBN-based and requires it (see .env.example / repo secrets). Unset = the
// LibraryThing provider row stays hidden and lookups fall through to the
// keyless providers (iTunes → Google Books → Open Library).
val envLibraryThingApiKey: String? = System.getenv("LIBRARY_THING_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
val ltkEscaped: String = envLibraryThingApiKey
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// v358 — OPTIONAL Spotify developer credentials (free): the client-credentials
// flow resolves albums/tracks/artists to real open.spotify.com deep links.
// Unset = Spotify keeps using search links everywhere.
// https://developer.spotify.com/documentation/web-api/concepts/apps
val envSpotifyClientId: String? = System.getenv("SPOTIFY_CLIENT_ID")?.trim()?.takeIf { it.isNotEmpty() }
val spotifyIdEscaped: String = envSpotifyClientId
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()
val envSpotifyClientSecret: String? = System.getenv("SPOTIFY_CLIENT_SECRET")?.trim()?.takeIf { it.isNotEmpty() }
val spotifySecretEscaped: String = envSpotifyClientSecret
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// v389f — OPTIONAL TMDB (The Movie Database) API key (free for personal use):
// the keyed upgrade for FILM and ANIME/TV artwork, the extra facts a film sheet
// can show (year, runtime, rating, genres, director, cast) and the episode list
// of a title that is really a show rather than a film. Unset = the keyless
// providers keep doing the work exactly as before (iTunes → TVMaze for films,
// Jikan → iTunes for anime), so nothing here is required for a lookup to
// succeed.  https://developer.themoviedb.org/docs/getting-started
// Setup: copy .env.example and set TMDB_API_KEY in your environment, or add it
// as a repo secret (Settings > Secrets and variables > Actions) — see
// .github/AGENTS.md for the full GitHub Actions guide.
val envTmdbApiKey: String? = System.getenv("TMDB_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
val tmdbEscaped: String = envTmdbApiKey
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// v428 — AND TMDB'S OTHER KEY: the API READ ACCESS TOKEN.
//
// TMDB hands an account two credentials, and its own documentation says either
// authenticates the same API: the **v3 API key**, sent as the `api_key` query
// parameter, and the **API Read Access Token** (a JWT), sent as
// `Authorization: Bearer <token>` — the token being the one that works across
// v3 AND v4. This is the second one, so a build can carry whichever the account
// actually has (see https://developer.themoviedb.org/docs/authentication-application).
// Unset = nothing changes: the v3 key, or no key at all.
val envTmdbReadToken: String? = System.getenv("TMDB_READ_TOKEN")?.trim()?.takeIf { it.isNotEmpty() }
val tmdbTokenEscaped: String = envTmdbReadToken
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// v426 — OPTIONAL Comic Vine API key (free for non-commercial use): the KEYED
// door for WESTERN COMICS, and the one that actually answers for them — it
// holds Marvel's and DC's volumes both, asked by name, with the cover, the
// publisher, the year and how many issues a volume runs to. It is the door the
// member asked for under "comic vine (needs a free key)", and it is the ONLY
// one: Marvel's own developer API was discontinued (its keys answered nothing
// by 2026) and DC never published one, so a publisher key here would be a door
// onto a closed room. Comic Vine restricts the free key to non-commercial use
// and 200 requests per resource per hour, which is why it is asked only when a
// comics kind is on the shelf and only once per row.
//  https://comicvine.gamespot.com/api/
// Unset = the keyless manga sources keep doing the work exactly as before.
val envComicVineApiKey: String? = System.getenv("COMIC_VINE_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
val cvkEscaped: String = envComicVineApiKey
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// v428b — OPTIONAL OMDb API key (free tier: 1,000 requests/day): the SECOND
// keyed door for a FILM or a SERIES, asked by name when TMDB has nothing for the
// title. It is the one that answers a *description* request where the keyless
// doors state no plot at all (iTunes' film catalogue returned nothing outright
// by 2026, and TVMaze is a television database), and it carries a real poster
// (Amazon's large form), the IMDb rating, the runtime and the genres too.
//  https://www.omdbapi.com/apikey.aspx
// Unset = nothing changes: TMDB, then the keyless cascade, exactly as before.
val envOmdbApiKey: String? = System.getenv("OMDB_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
val omdbEscaped: String = envOmdbApiKey
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// Supabase Android client configuration. The URL and publishable/anon key are
// safe for a public client; the service-role key must never be shipped here.
val envSupabaseUrl: String? = System.getenv("SUPABASE_URL")?.trim()?.takeIf { it.isNotEmpty() }
val supabaseUrlEscaped: String = envSupabaseUrl
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()
val envSupabasePublishableKey: String? = (System.getenv("SUPABASE_PUBLISHABLE_KEY")
    ?: System.getenv("SUPABASE_ANON_KEY"))?.trim()?.takeIf { it.isNotEmpty() }
val supabasePublishableKeyEscaped: String = envSupabasePublishableKey
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

// The Curio account site (auth-web/) — where Supabase confirmation and password
// reset emails send people. Without it the app sends NO redirect, so Supabase
// falls back to the project's own Site URL (which is what sent members to
// localhost), and the sign-in form hides its "Forgot your password?" row.
// Trailing slashes are trimmed here because every caller appends a path.
val envAuthSiteUrl: String? = System.getenv("CURIO_AUTH_SITE_URL")
    ?.trim()
    ?.trimEnd('/')
    ?.takeIf { it.isNotEmpty() }
val authSiteUrlEscaped: String = envAuthSiteUrl
    ?.replace("\\", "\\\\")
    ?.replace("\"", "\\\"")
    .orEmpty()

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
        //
        // v458 — 20260923 / 1.4.0: the +1 bump over 20260922 (which shipped as
        // v1.3.0 and its hotfix). The version NAME is a minor step because this
        // release carries new behaviour as well as fixes — the film and Incursion
        // posters are TMDB's (with the year searched for, not guessed), and a
        // topic whose year this app states wrongly is re-asked without it.
        //
        // v471 — 20260924 / 1.4.1: **EVERY PUSH BUMPS THE VERSION** (the member's
        // own rule, 2026-09-24 — see root AGENTS.md). +1 on the date-based code,
        // +0.0.1 on the name, and the fastlane changelog for the new code is a NEW
        // file (`changelogs/20260924.txt`): `20260923.txt` is what v1.4.0 shipped
        // with, so it is left exactly as it was rather than renamed out from under
        // the release that owns it.
        //
        // v472 — 20260925 / 1.4.2: the +1 / +0.0.1 bump for this push, and
        // `changelogs/20260925.txt` is its notes file (copied forward from
        // 20260924.txt, which stays exactly as it was — it is the record of the
        // build that code shipped as). What it carries: completing a topic from
        // its page marks it explored (a recents row, and no more stale
        // **Unexplored** tag), and Recents lists the topics already finished.
        // v483 — 20260926 / 1.4.3: the +1 / +0.0.1 bump for this push, and
        // `changelogs/20260926.txt` is its notes file (copied forward from
        // 20260925.txt, which stays exactly as it was — it is the record of the
        // build that code shipped as). What it carries: the drawer's sky is
        // territories now (no branch runs through another's stars, no two dots or
        // halos overlap), and its connections wear one of three styles — threads,
        // bones or swept arcs — cycled by a HOLD on the sky and remembered.
        versionCode = 20260926
        // v406 — the local/PR default matches the version now being tagged, so
        // a build from main reports the release it belongs to (a v* tag still
        // overrides it through RELEASE_VERSION).
        versionName = envReleaseVersion ?: "1.4.3"

        // v354 — optional Google Books API key baked into BuildConfig so the
        // keyless fetchers can upgrade to keyed (higher-quota) calls when the
        // repo secret is present; empty string otherwise (no behaviour change).
        buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$gbkEscaped\"")

        // v356 — optional LibraryThing key: enables the LibraryThing cover
        // provider in the hub (hidden without a key); empty string otherwise.
        buildConfigField("String", "LIBRARY_THING_API_KEY", "\"$ltkEscaped\"")

        // v358 — optional Spotify developer credentials: when set, music
        // topics deep-link to real open.spotify.com items (client-credentials
        // flow); empty strings keep the search links.
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyIdEscaped\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$spotifySecretEscaped\"")

        // v389f — optional TMDB key: film/anime artwork and facts, and the
        // episode list of a title that maps to a show. Empty string otherwise.
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbEscaped\"")
        // v428 — the v4 read token ("API Read Access Token"), when the account
        // has that instead of (or as well as) the v3 key. Empty string otherwise.
        buildConfigField("String", "TMDB_READ_TOKEN", "\"$tmdbTokenEscaped\"")

        // v426 — optional Comic Vine key: the comics sources ask it first for a
        // Western comic and last for a manga; empty string otherwise (the
        // keyless cascade is unchanged, and no request is ever made without it).
        buildConfigField("String", "COMIC_VINE_API_KEY", "\"$cvkEscaped\"")

        // v428b — optional OMDb key: a film/series description and poster of
        // last resort, asked by name; empty string otherwise (no request is ever
        // made without it, and the keyless cascade is unchanged).
        buildConfigField("String", "OMDB_API_KEY", "\"$omdbEscaped\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrlEscaped\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabasePublishableKeyEscaped\"")

        // Empty on a build whose account site is not deployed yet: the app then
        // keeps Supabase's own Site URL in charge of email links and hides the
        // password-recovery row, rather than opening a door that goes nowhere.
        buildConfigField("String", "CURIO_AUTH_SITE_URL", "\"$authSiteUrlEscaped\"")

        // Only include English locale — saves ~5-8 MB of APK size.
        // Curio ships as a single-language app. Add others as needed.
        androidResources.localeFilters.clear()
        androidResources.localeFilters.add("en")
    }

    // ── v465 — TWO EDITIONS: CORE AND FULL ─────────────────────────────────
    //
    // The member: *"lets do a double build, one with advance feature focising on
    // online and all, one smaller with the core curio features"*. Asked where the
    // line falls, their answer was narrow and deliberate: the CORE edition drops
    // **only the two things that are large BINARY rather than large FEATURE** —
    // the on-device speech-to-text stack (Vosk: ~19 MB of arm `.so` in a release
    // APK) and the neural read-aloud voice packs that will follow it — and keeps
    // everything else, online layer included. The FULL edition keeps both, and
    // takes back the ISBN scanner (CameraX + ML Kit) the app gave up in v458 now
    // that size is no longer the thing being optimised for.
    //
    // ── TWO PACKAGE NAMES, SIDE BY SIDE ────────────────────────────────────
    //
    // On the member's own instruction (*"two package names, side by side"*): an
    // edition is an app the member CHOOSES, not a variant that overwrites the
    // other. `com.curio.app` stays the core edition — the identity that already
    // exists, so an existing install is never renamed (and never opens as a
    // different app with a different data directory) — and `com.curio.app.full`
    // is the full one. Both are signed by the SAME keystore: a signing key is not
    // bound to a package name, and `signingConfigs.release` (below) already
    // applies to every variant, so no new CI secret is involved.
    //
    // ⚠️ THE ONE THING THAT DOES BITE: a credential restricted by PACKAGE NAME
    // (a Google Books key locked to a package + SHA-1) rejects the second package
    // until that pair is added to the key's restrictions in its console. Plain
    // keys (TMDB, OMDb, Comic Vine, LibraryThing, Spotify) do not care, and
    // Supabase's anon key is the same project key for both.
    flavorDimensions += "edition"

    productFlavors {
        create("core") {
            dimension = "edition"
            applicationId = "com.curio.app"
            // The edition's own name, and it is not decoration: every published
            // APK is named `Curio-<version>-<code>-<edition>-<abi>-...`, and the in-app
            // updater matches THIS token to pick the right file out of a release's
            // assets (see UpdateChecker.parseApkAsset). Both editions publish from the
            // same tag, so an updater that took "the first .apk" would hand a member
            // the other edition — an install that fails on the last tap.
            buildConfigField("String", "EDITION", "\"core\"")
            buildConfigField("boolean", "EDITION_OFFLINE_TRANSCRIPTION", "false")
            buildConfigField("boolean", "EDITION_ISBN_SCANNER", "false")
            buildConfigField("boolean", "EDITION_NEURAL_VOICES", "false")
        }
        create("full") {
            dimension = "edition"
            applicationId = "com.curio.app.full"
            buildConfigField("String", "EDITION", "\"full\"")
            buildConfigField("boolean", "EDITION_OFFLINE_TRANSCRIPTION", "true")
            buildConfigField("boolean", "EDITION_ISBN_SCANNER", "true")
            buildConfigField("boolean", "EDITION_NEURAL_VOICES", "true")
        }
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
            // v204 — native-lib DIET: every real device since ~2017 is arm64
            // (or an older 32-bit armeabi-v7a); x86/x86_64 are emulator-only
            // legacy. Restricting the RELEASE native libs to the two arm ABIs
            // halves the bundled Vosk libvosk.so footprint (~4 ABIs ≈ 38MB →
            // 2 ABIs ≈ 19MB in the universal release APK — the PR/push CI
            // artifact and the release universal both shrink by ~20MB).
            // Debug builds keep ALL four ABIs so x86_64 emulator testing
            // still works locally.
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
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
    // Liquid-glass pills experiment (v227): real-time backdrop
    // vibrancy/blur/lens for the floating nav-style capsules.
    implementation(libs.backdrop)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Baseline profiles: applies app/src/main/baseline-prof.txt on first
    // launch so ART AOT-compiles the hot startup/UI paths at install instead
    // of JIT-compiling giant composables at runtime (logcat showed single
    // methods compiling at up to 7.7 MB). Already present transitively via
    // Compose; declared explicitly per the baseline-profile docs.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    // v458 — the Compose tooling pair is gone: `ui-tooling-preview` (the
    // `@Preview` annotation) and debug-only `ui-tooling` (the preview
    // renderer) backed nothing, because this module has NO `@Preview` and no
    // `androidx.compose.ui.tooling` import. `ui-tooling-preview` is an
    // `implementation`, so it was shipping in the release APK. Both are still
    // in the catalog — two lines to re-add with the first preview.
    implementation(libs.androidx.material3)
    // Window-size-class breakpoints for the adaptive tablet/landscape layouts
    // (compact < 600dp, medium 600-839, expanded >= 840).
    implementation(libs.androidx.material3.window.size)
    // Icons are rendered via Material Symbols font ligatures (CurioIcon), NOT
    // the bundled M2 vector set, so androidx.compose.material.icons.core is
    // intentionally absent. Re-add only if a screen needs an M2 vector icon.
    implementation(libs.androidx.compose.animation)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.io.coil.kt.coil.svg)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ExoPlayer for audio playback
    implementation(libs.androidx.media3.exoplayer)

    // Gson for JSON serialization (CaptureData -> Room blob)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.com.squareup.okhttp3.okhttp)

    // v389 — the book reader's PDF text layer (see libs.versions.toml): the
    // words on a PDF page, so a PDF can be selected, searched and highlighted
    // like a reflowable book. Used LAZILY, one page at a time, on the IO
    // dispatcher — never while a book is opening.
    implementation(libs.com.tom.roush.pdfbox.android)

    // Vosk — on-device speech-to-text for pre-recorded sound bites (offline
    // transcription in the entry detail page; model downloaded in Settings).
    //
    // ⚠️ v465 — FULL EDITION ONLY (see the edition block above). This is the
    // core edition's single largest binary cost (~19 MB of arm `.so` in the
    // release APK) and the member's own line for the split is that the core
    // edition drops it and nothing else. That is why the implementation moved to
    // `app/src/full/java/.../OfflineTranscriber.kt` with an identical-API twin in
    // `app/src/core/java/.../OfflineTranscriber.kt`: `main` is compiled for BOTH
    // editions, so no file under `main` may import `org.vosk.*`. Any future
    // Vosk-based work goes in `src/full`, never in `main`.
    "fullImplementation"(libs.com.alphacephei.vosk.android)

    // ── v465c — THE NEURAL READ-ALOUD VOICE PACKS, FULL EDITION ONLY ──────
    // The vendored runtime, as a LOCAL AAR: sherpa-onnx publishes
    // `sherpa-onnx-1.13.8.aar` (~48 MB) from its own GitHub release and has NO
    // official Maven Central coordinate — the Maven hits are third-party
    // repackages — so the binary is committed under `app/libs/` rather than
    // resolved. It carries all four ABIs' `.so` files on purpose: the RELEASE
    // build's `ndk.abiFilters` (see the release buildType) keeps only the two
    // arm ones in the APK, while debug builds keep all four so an x86_64
    // emulator can still exercise the neural voice path.
    //
    // ⚠️ THIS AAR NEEDS `app/proguard-rules.pro`, NOT JUST A DEPENDENCY LINE.
    // The native side looks its config up by FIELD NAME (`GetFieldID(...,
    // "vits", ...)`) — verified by reading the literal strings out of
    // libsherpa-onnx-jni.so — so R8 renaming those fields would leave a release
    // APK whose voice silently does nothing while the debug build worked. The
    // keep rules are already written there; do not remove them.
    "fullImplementation"(files("libs/sherpa-onnx-1.13.8.aar"))
    // BZIP2 + TAR, and it is not optional: every sherpa-onnx TTS pack is a
    // `.tar.bz2` and Android cannot decompress bzip2 (see libs.versions.toml).
    "fullImplementation"(libs.org.apache.commons.commons.compress)

    // ── v465b — THE ISBN SCANNER'S LENS STACK, FULL EDITION ONLY ──────────
    // Five dependencies, and every one of them is FULL-only for the same reason
    // Vosk is: they exist solely to serve the scanner, and the member's line for
    // the split was that the core edition stays the smaller app. They reach the
    // build as `fullImplementation`, never `implementation` — `implementation`
    // would put them in BOTH editions and quietly undo the split.
    //
    // The scanner is the one feature that comes BACK in the full edition: v458
    // removed it (and its CAMERA permission) because it was the app's largest
    // remaining binary cost, which was the right call at the time and the wrong
    // one once size stopped being what the build optimises for.
    //
    // ⚠️ Same structural rule as Vosk: the scanner's screen imports
    // `androidx.camera.*` and `com.google.mlkit.*`, so the REAL screen lives in
    // `app/src/full/java/.../IsbnScannerScreen.kt` and `app/src/core` carries an
    // identical-signature no-op twin. `main` (the add-a-book sheet) may only
    // call the composable and read `BuildConfig.EDITION_ISBN_SCANNER` — never
    // import a camera or ML Kit type.
    //
    // `kotlinx-coroutines-play-services` is deliberately NOT re-added: the
    // scanner's ML Kit call is driven by `addOnSuccessListener` /
    // `addOnCompleteListener`, and the one `kotlinx.coroutines.tasks.await`
    // import the old file carried was dead (no `.await()` call existed in it).
    "fullImplementation"(libs.mlkit.barcode.scanning)
    "fullImplementation"(libs.androidx.camera.core)
    "fullImplementation"(libs.androidx.camera.camera2)
    "fullImplementation"(libs.androidx.camera.lifecycle)
    "fullImplementation"(libs.androidx.camera.view)

    // v458 — the test scaffolding is gone with the tests it never had: there is
    // no source in `app/src/test` and no `androidTest` source set at all, so
    // `testImplementation(junit)` and the debug UI-test manifest backed
    // nothing. CI runs `lintCoreRelease lintFullRelease validateTopics
    // assembleCoreRelease assembleFullRelease` (per edition — v465b) and no
    // test task, so neither could have run even if a file appeared. Re-add both
    // (they are still in the catalog) with the first real test.
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
val hasTopicFiles: Boolean = file("src/main/assets/topics").let { d ->
    d.exists() && d.listFiles { f -> f.extension == "json" }?.isNotEmpty() == true
}

tasks.register("validateTopics") {
    group = "verification"
    description = "Validates assets/topics/*.json against the CurioTopic schema (CURIO_DATA_PLAN.md §2)."
    // Resolve the directory as a task-local value (not a script property) so
    // the doLast action captures a plain File and serializes cleanly into
    // the configuration cache.
    val topicsDir = file("src/main/assets/topics")
    inputs.dir(topicsDir)
    // v406 — DECLARE AN OUTPUT, SO THE TASK CAN BE SKIPPED.
    //
    // This task has inputs but produced nothing, and Gradle can only mark a task
    // UP-TO-DATE (or restore it from the build cache) when it has both: a task
    // with no declared output is never up to date, so every build re-parsed the
    // whole catalog. The stamp below is what the task produces. Unchanged JSON
    // leaves the stamp good, the parse is skipped, and with org.gradle.caching
    // on it can even be restored on a fresh CI runner.
    val validationStamp = layout.buildDirectory.file("curio/validate-topics.stamp")
    outputs.file(validationStamp)
    outputs.cacheIf { true }
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
            // Filename is the category SLUG (animated-movies.json); the
            // topics' categoryId is the enum name (ANIMATED_MOVIES) — so
            // hyphenated slugs map to underscores. Single-word filenames
            // (films.json → FILMS) are unaffected.
            val expectedCategoryId = json.nameWithoutExtension.uppercase().replace("-", "_")
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
        // The stamp is written LAST, so a failing validation fails before it is
        // written and the next build checks the catalog again instead of
        // treating a bad catalog as validated.
        validationStamp.get().asFile.apply {
            parentFile?.mkdirs()
            writeText(
                "validated=" + System.currentTimeMillis() +
                    " files=" + jsonFiles.size +
                    " populated=" + populatedFileCount + "\n"
            )
        }
    }
}

// Only hook validateTopics into preBuild when there's actually JSON to check.
// Keeps placeholder-UI builds (no topics yet) friction-free.
if (hasTopicFiles) {
    tasks.named("preBuild") {
        dependsOn("validateTopics")
    }
}

