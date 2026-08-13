# Prompt.md — Request log

## Current request — CI fix: WiX path check failed on the runner's WiX 3.14

### What was asked
User pasted a desktop-release CI failure: `choco install wixtoolset` reports
"wixtoolset v3.14.1.20250415 already installed" (0/1 packages), then
`Write-Error: WiX Toolset not found at C:\Program Files (x86)\WiX Toolset
v3.11\bin after install` — exit 1.

### Root cause
The workflow hardcoded the WiX path to `...\WiX Toolset v3.11\bin`, but the
windows-latest runner image now ships WiX **3.14** at `...\WiX Toolset
v3.14\bin` — the `Test-Path` check failed.

### Research (JDK source)
Fetched `jdk.jpackage.internal.WixTool` (OpenJDK master): jpackage NEVER
reads the `WIX` env var. It first looks for tools on the PATH, then scans
`%ProgramFiles%` / `%ProgramFiles(x86)%` for dirs matching `WiX Toolset v*`
(newest first) and resolves `<dir>\bin\candle.exe` / `light.exe`. So the
workflow's WIX value was inert, and the whole hardcoded-path approach was
fragile by construction.

### Fix
`desktop-release.yml` — the WiX step now DISCOVERS the install folder
(`Get-ChildItem "C:\Program Files (x86)\WiX Toolset*" -Directory` sorted
by name descending, first hit), verifies `bin\candle.exe`, then sets
`WIX` to the installation ROOT (standard `%WIX%` convention) and adds the
`bin` dir to PATH (belt-and-braces — jpackage finds the toolset itself).
Comment updated. `.github/AGENTS.md` desktop-release bullet updated
(versioned folder, discovery, WIX=root).

### Validation
No local PowerShell/Windows (env rule) — the workflow runs on tag push /
manual dispatch. PowerShell syntax follows the file's existing pwsh style.

## Prior — cut-line rework + book rings in holes + paper card expansion (v27u)

### What was asked
1. "Title cut lines" experiment: the two lines need to be a little SHORTER,
   more natural stroke, and the 2-line placement feels wrong — research how
   they should be drawn and fix it.
2. Place rings inside the pin holes like book rings — only 3, tilted look —
   as a DIFFERENT look option with a NEW toggle in Experiments.
3. Expand the paper stat card to all stat-card screens too (all experiments
   stay behind their toggles).

### Research
Double-underline conventions: two strokes close together, NOT parallel —
slight convergence like one continuous pen motion; the lower line usually a
little longer; gentle waviness + rounded/felt ends read "hand-drawn". The
old implementation's deep quadratic sag (control y 0.64 on a 0.40–0.47
stroke) dipped INTO the second stroke — that crossing is the wrong
placement.

### Fix
- **PaperTitleLines.kt** — lines now span ~88% of the title width (was
  (len+3)·0.62em — stretched past the text), floor 1.8em / cap 11em +
  220dp. Two cubic-bezier strokes in the lower half with a steady gap,
  converging slightly toward the right (top 0.34→0.40, bottom 0.74→0.68),
  bottom line longer + offset right, felt-pen double pass, -2° tilt.
- **PaperStatCard.kt (new, shared)** — `paperStatCardFill(shape, fill,
  holesOn, ringsOn, ink)` draws the opaque paper fill + 3-hole EvenOdd
  punch (left edge, 5.5dp @ 14dp) + per-hole pressed rims OR tilted metal
  book rings (foreshortened ellipse rx=holeR+0.8dp, ry=0.78·rx, per-ring
  tilt -9°/-3°/3°, metal vertical gradient, white specular top arc,
  contact shade bottom-right). `paperStatCardColor(base)` = the shared
  cream/rose-brown paper blend (light lerp 0xFFF6EB 0.62 / dark 0x2A211C
  0.50).
- **AppPreferences** — new `paperHoleRingsState` (default false, KEY
  paper_hole_rings) + is/set; loaded in init.
- **ExperimentsScreen** — new "Hole rings" row under Stamped pin holes
  ("needs Stamped pin holes on"); Paper stat card subtitle now "Home +
  Profile".
- **HomeScreen** — stat card refactored onto `paperStatCardFill` (rings
  honored); paper color via `paperStatCardColor(heroFill)`; 10 dead drawing
  imports removed.
- **ProfileScreen** — Level · Saved · Lanes pane wears the same paper card
  when the toggle is on (holes/rings/tear follow the same toggles; tear
  seed 0x6B4E3E).
- Cut lines confirmed: they were already on ALL hero-title screens (Home,
  Profile, Cabinet, Entry Detail, Settings).

### Validation
Brace balance OK (6 files), `git diff --check` clean, no leftover
Path/drawWithCache/StrokeCap/Outline refs in HomeScreen. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v27u bullet.

## Prior — explore-dialog pills: clean glyphs, visible pill fill, spacing (v27u)

### What was asked
"The Watch in YouTube icon looks bad, same for Explore in browser — give the
two proper spacing and give the whole button a pill shape." (Also: drop the
Codebuff commit footer from now on.)

### Root cause
The two explore actions were `TextButton`s with `RoundedCornerShape(50)` but
`curioDialogActionButtonColors()` was TRANSPARENT (textButtonColors has no
container color) — so the pill shape was invisible; what rendered was just
the tiny 18dp v27s brand square (red YouTube tile / engine letter monogram)
floating next to the label, which read as a bad icon + no button at all.

### User decision (ask_user)
Clean glyph icons: no brand tiles. Globe (travel_explore) on the browser
button, the service's glyph (youtube_activity / play_circle / music_note)
on the watch button, tinted with the pill ink, on visible soft-tinted pills,
12dp apart.

### Fix
- `CurioTheme.kt` — `curioDialogActionButtonColors(containerColor: Color?
  = null)` so dialog actions can wear a visible container fill.
- `TopicRevealScreen.kt` — the two pills now use
  `curioDialogActionButtonColors(containerColor = pillFill)` with
  `pillFill = lerp(curioDialogContainerColor(), pillInk, 0.14f)` (opaque,
  v27n-safe), pillInk = `curioDialogActionColor()`; clean CurioIcon glyphs
  at 20dp tinted pillInk; spacing 8→12dp; contentPadding 14→16dp horizontal.
  `watchGlyph` stays service-aware via `watchService.brandTile().second`;
  removed the now-unused `SearchEngine` import + `BrandMonogram` import.
  BrandMonogram/brandTile helpers stay in CurioIcons.kt (doc updated).

### Validation
Brace balance OK (3 files), `git diff --check` clean, no leftover
BrandMonogram/engineTile/watchTile/SearchEngine refs. No Gradle locally
(env rule) — CI on push is the gate. Docs: app/AGENTS.md v27u bullet.

## Prior — main-card border removal + save-page opaque fills (v27u)

### What was asked
"Leftover border cleanup: the main card still has the border. And on 'Save
your take', the topic-name card at the top and the attach photo have a
visual transparency issue — make it opaque, no transparency."

### User decisions (ask_user)
- Border scope: remove the drawn rim border from BOTH the Spin main card AND
  the Topic Reveal hero, and ALSO remove the main card's AMOLED rim light.
- Save page: topic strip fill + icon plate BOTH opaque.

### Root causes
- **Spin main card** (`SpinScreen.kt`) drew a 1.5dp gradient rim border +
  1dp bevel (`heroBorderOn` ← `AppPreferences.heroBorderState`, default
  true) plus an AMOLED `categoryEdgeShine` rim light — leftover borders from
  the v27n border-removal pass.
- **Topic Reveal hero** (`TopicRevealScreen.kt`) mirrored the same 1.5dp
  gradient rim (must change with the ticket or the shared-element morph
  would show the border popping in mid-morph).
- **SaveCapture topic strip** used `cat.tint` = accent @ 20% alpha under a
  3dp `shadowElevation` — translucent fill bleeds the shadow (v27n rule),
  the exact transparency issue the user saw; its icon plate was
  `themedAccent().copy(alpha = 0.15f)` (also translucent). The attach-photo
  add tile was already opaque (v27n lerp) — nothing more to change there.

### Fix
- Removed the gradient rim + bevel `drawBehind` blocks and the AMOLED
  `categoryEdgeShine` from the Spin main card; removed the matching rim from
  the Reveal hero. Dropped now-unused imports (drawBehind, CornerRadius,
  Stroke) in both files; the `categoryEdgeShine` import stays (4 other uses
  in SpinScreen). `heroBorderState` pref API left dormant (nothing reads it
  now — consistent with v25's dormant-pref convention).
- SaveCapture strip fill → opaque `lerp(surfaceContainerHigh, cat.accent,
  0.20f)` (preserves the tinted look, clean 3dp shadow); icon plate → opaque
  `lerp(surfaceContainerHigh, themedAccent(), 0.15f)`.

### Validation
Brace balance OK (3 files), `git diff --check` clean, no leftover
heroBorderOn/drawBehind/CornerRadius/Stroke refs in either file. No Gradle
locally (env rule) — CI on push is the gate. Docs: app/AGENTS.md v27u note.

## Prior — CI fix: jpackage "Illegal version" for the v1.0.2-beta tag

### What was asked
User pasted two identical CI failures (Windows desktop-release + Linux
release/verify) — both fail configuring `:desktop`:

```
* Illegal version for 'Dmg': '1.0.2-beta' is not a valid version.
  Correct format: 'MAJOR[.MINOR][.PATCH]'
* Illegal version for 'Msi': '1.0.2-beta' is not a valid version.
  Correct format: 'MAJOR.MINOR.BUILD'
```

### Root cause
`settings.gradle.kts` includes `:desktop`, so EVERY Gradle invocation
configures it (Android release, per-push desktop job, desktop-release).
`desktop/build.gradle.kts` feeds the tag minus `v` straight into
`nativeDistributions.packageVersion`; on the `v1.0.2-beta` tag that becomes
`1.0.2-beta`, which jpackage rejects (numeric-only). The Android
`versionName` is a plain string and tolerates the suffix — the desktop
installer metadata does not.

### Fix
- **`desktop/build.gradle.kts`** — sanitize `envDesktopVersion`: strip
  prerelease/build suffixes (`substringBefore('-')`, `substringBefore('+')`)
  so `v1.0.2-beta` → packageVersion `1.0.2` (v27u comment).
- **`.github/workflows/desktop-release.yml`** — the release body named the
  MSI from the full tag (`Curio-1.0.2-beta.msi`) but jpackage names it from
  the numeric package version (`Curio-1.0.2.msi`); the body now derives the
  same numeric `msiVersion` while the portable zip keeps the full tag (so
  prerelease artifacts stay distinguishable from later stable ones).
- **`.github/AGENTS.md`** — desktop-release contract documents the
  numeric-only packageVersion rule and the naming split (zip = full tag,
  MSI = numeric core).

### Validation
No Gradle locally (env rule) — CI on push/tag is the gate. PowerShell and
Kotlin logic hand-verified (`1.0.2-beta`.Split('-')[0] = 1.0.2; stable tags
pass through unchanged).

## Prior — CI fix: desktop build + Android app compile errors (b669f0c…5d01a18) + desktop JAR artifact (d648350)

### Round 1 — plugin collision (b669f0c)

### What was asked
User pasted a CI failure: `desktop/build.gradle.kts` line 10 fails with `Error resolving plugin [id: 'org.jetbrains.kotlin.jvm', version: '2.3.21']` → "already on the classpath with an unknown version, so compatibility cannot be checked".

### Root cause
`org.jetbrains.kotlin.plugin.compose` (declared `apply false` in the root build) pulls the Kotlin Gradle Plugin onto the shared buildscript classpath transitively, which leaves `org.jetbrains.kotlin.jvm` on the classpath with an *unknown* version. The `:desktop` module then requests `org.jetbrains.kotlin.jvm` with an explicit version (from the catalog) and the `AlreadyOnClasspathPluginResolver` can't verify compatibility. The two prior `resolutionStrategy.eachPlugin { useVersion("2.3.21") }` attempts didn't help because `useVersion` pins the *requested* version, not the *classpath* version.

### Fix
`build.gradle.kts` — added `alias(libs.plugins.kotlin.jvm) apply false` to the root `plugins {}` block, so the Kotlin JVM plugin lands on the shared classpath with a *known* version (2.3.21) and the desktop request resolves cleanly. Committed + pushed to Alpha for CI validation.

### Validation
No Gradle locally (env rule) — the `desktop` CI job on push is the gate.

### Round 2 — script compile errors (0f63f12)
After round 1 landed, CI got past plugin resolution but `desktop/build.gradle.kts` failed to compile as a Kotlin DSL script:
- `TargetFormat` unresolved → added `import org.jetbrains.compose.desktop.application.dsl.TargetFormat`.
- `compose.material3` (String accessor) deprecated → a script error in CMP 1.11+. Replaced with a direct catalog dependency: added `composeMaterial3 = "1.11.0-alpha07"` + `compose-material3` to `libs.versions.toml` (the material3 version bundled with CMP 1.11.1, decoupled from the CMP plugin version since 1.8) and `implementation(libs.compose.material3)` in the desktop module. TOML re-validated with `tomllib`.

### Round 3 — desktop Kotlin source errors (e27615d)
Once the desktop script compiled, `:desktop:compileKotlin` surfaced three independent issues (the desktop sources had never compiled — CI was blocked at plugin resolution until round 1):
1. **DesktopCatalog.kt nested comment** — Kotlin supports NESTED block comments, so the `/*` in the doc comment's `topics/*.json` opened a nested comment, swallowing the rest of the file ("Unclosed comment" at EOF). This cascaded into every `Unresolved reference 'DesktopTopic'/'DesktopCatalog'` error. Reworded to `app's \`assets/topics\` JSON schema`.
2. **DesktopPill** — `onClick` wasn't the last param, so trailing-lambda call sites (`DesktopPill("Light", active) { ... }`) bound the lambda to `enabled` (Boolean) → "No value passed for onClick". Moved `onClick` last (Compose convention).
3. **Main.kt window API** — CMP 1.6+ sealed `WindowPosition`: `WindowPosition(x, y)` → `WindowPosition.Absolute(x.dp, y.dp)`; `state.position.x/.y` are `Dp` → `.value.toInt()` under an `is WindowPosition.Absolute` cast.

### Round 4 — "other" APK build failure (kotlin.jvm not found — TRANSIENT)
A separate CI run failed at the ROOT `build.gradle.kts` with `Plugin [id: 'org.jetbrains.kotlin.jvm', version: '2.3.21', apply: false] was not found` (could not resolve the plugin marker). Verified the marker `org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.3.21` EXISTS on Maven Central, and the sibling desktop job resolved it fine in the same push — so this is a transient resolver/network blip, not a code issue. Re-run if it recurs.

### Round 6 — upload desktop JAR artifact (d648350)
User asked why the JVM job uploaded no result / whether it built the installer. Answered: the per-push `desktop` job is a compile gate with no artifact, and the installer comes from `desktop-release.yml` on `v*` tags. User chose to upload the desktop JAR per-push: added an `upload-artifact` step (`curio-desktop-jar-*`, `desktop/build/libs/*.jar`, `if-no-files-found: error`, 14-day retention) to the `desktop` job, with a comment noting it's a compile artifact (not runnable standalone — runnable bundles stay tag-only via jpackage). Updated root AGENTS.md CI line.

### Round 5 — Android app compile error (5d01a18)
With the desktop build finally compiling, the verify job advanced to the Android app and hit a pre-existing bug: `HomeScreen.kt:593` used `Size(...)` (the `androidx.compose.ui.geometry.Size` constructor, in the diary-spiral punch-hole ring drawing) without importing it. Added `import androidx.compose.ui.geometry.Size`. This had been masked the whole time because CI never got past the `:desktop` build. Only this one error was reported (debug + release), so the app should now compile.

## Prior — desktop full parity (milestone 3) + workflow hardening (v27t)

### What was asked
"Do the rest of the desktop things full parity. And workflow build properly."

### What was done
Desktop app restructured from a single-pane shell into Android-parity four-tab layout:
- **`Main.kt`** — now just the entry: window geometry, theme schemes, shared `CurioShellState`/`shell` (internal, with a persisted `screen` tab), bottom nav (Home · Spin · Cabinet · Settings). The old sidebar + mode-pill header are gone (lanes moved to a chip bar).
- **`DesktopCommon.kt`** — `DesktopPill` (with `enabled`), `ScreenHeader`, `LaneChipsRow` (36 lanes, tap → select + jump to Spin).
- **`DesktopHome.kt`** — rose hero banner (Streak · Cabinet · Topics stat chips; streak = consecutive save-days, topics total computed off the UI thread via `produceState`+`Dispatchers.Default`), lane chips, SPIN A LANE CTA, saved-count link to Cabinet.
- **`DesktopSpin.kt`** — lane chip bar + Spin/Browse pills + deck (2 peeks) + reveal + **Save to Cabinet** pill + browse list (moved from Main).
- **`DesktopCabinet.kt`** — saved discoveries from `DesktopEntryStore` (new JSON store at `~/.curio/entries.json`, reactive via Compose state): open (jumps to Spin reveal) / remove / empty state.
- **`DesktopSettings.kt`** — Appearance (Light/Dark), Data (Clear entries, Reset all preferences via `DesktopPreferences.clear()`), About.
- **Workflow hardening** — `desktop-release.yml` now compiles `:desktop:build` FIRST (fast-fail before WiX/jpackage) so code errors surface clearly.

### Validation
Braces balanced (9 files), `git diff --check` clean, no stale refs to the removed sidebar/MainPane/ModePill, YAML still parses, imports verified per file. No Gradle locally (env rule) — the `desktop` CI job gates compile on push; the release workflow exercises packaging on tag/manual dispatch.

## Prior — Windows .exe installers in CI (v27t, desktop release workflow)

### What was asked
"Build the Windows .exe installers in CI with a desktop release workflow."

### What was done
- **`desktop/build.gradle.kts`** — `packageVersion` now follows the tag: the workflow exports `RELEASE_VERSION` (tag minus `v`) and jpackage versions the installer from it, mirroring the Android `versionName` convention; local builds keep `1.0.0`.
- **`.github/workflows/desktop-release.yml` (new)** — runs on `v*` tags + manual dispatch, `windows-latest`, `contents: write`:
  - Installs WiX Toolset via chocolatey (jpackage needs it for `.msi`), exposed via `WIX` env + PATH.
  - Runs `./gradlew.bat :desktop:packageDistributionForCurrentOS` → builds the app image (contains `Curio.exe`) + the `.msi` (Dmg/Deb are non-Windows formats, skipped).
  - Hard guards: `.msi` must exist, app image must contain `Curio.exe`.
  - Zips the app image into `Curio-Windows-{version}-portable.zip` and publishes BOTH the `.msi` and the portable zip to the GitHub release with a Windows install-guide body.
  - Same `alpha/beta/rc` prerelease detection as Android; `update_release_body: false` so it never clobbers the Android release workflow's body when both run on the same tag (race-safe both directions).
- Docs: `.github/AGENTS.md` ownership + new "Desktop release workflow" contract section; root AGENTS.md desktop CI + not-yet-ported lines.

### Validation
No YAML lint locally — the workflow is only exercised on tag push / manual dispatch. Structure mirrors the battle-tested `release.yml` (same action versions, prerelease logic, contents: write).

## Prior — desktop persistence + preferences store (v27t, milestone 2)

### What was asked
"Continue the desktop port: add persistence and a preferences store so the deck and settings survive restarts."

### What was done
- **`DesktopPreferences.kt` (new)** — tiny pretty-printed JSON store at `~/.curio/prefs.json` (Gson, same serializer as the topic loader); best-effort load (corrupt file → defaults); `get/set` + `getBoolean/setBoolean` + `getInt/setInt`.
- **Main.kt — deck persistence:** `CurioShellState.selectedSlug` now seeds from prefs; `LaunchedEffect`s persist the lane, the landed topic id, and the theme as they change; on cold start the last landed topic is re-found in the lane's pool.
- **Main.kt — settings persistence:** a Light/Dark theme toggle pill (persisted) + full dark color scheme (warm near-black paper, light coral primary); all components now read `MaterialTheme.colorScheme.*` instead of hardcoded light constants (brand `Coral` stays as a translucent tint on both themes).
- **Main.kt — window geometry:** `rememberWindowState` restores saved size; saved position restored only when non-negative (off-screen guard); `saveWindowGeometry` persists size + position on close (guards `isSpecified`).
- **CI fix (separate commit `258be7b`):** the v27t comment in `libs.versions.toml` used Kotlin `//` comments — TOML rejects them ("Unexpected '/'" at line 43), breaking catalog parsing for the whole build. Converted to `#`; verified with `tomllib`. Pushed first to unblock.

### Validation
Braces balanced (Main.kt + DesktopPreferences.kt), `git diff --check` clean, no leftover hardcoded light colors outside the palette/scheme blocks, no stale `rememberWindowStateSafe` reference. No Gradle locally (env rule) — the `desktop` CI job gates on push.

## Prior — pet studio save fix + paper experiment reworks (v27t)

### What was asked
1. "Curie custom design isn't saving" — it should apply regardless of the pet and save as a new pet.
2. "Stamped pin holes" experiment: make them DIARY-SPIRAL looking (like a ring-bound diary), only 3, proper look.
3. "Title cut lines": move more to the end of the hero text, size to the hero text, a little longer, slight curve (not too much), pen-line like, slightly tilted.

### Root cause (pet save)
`saveAsNewPet()` / `selectCustomPet()` wrote the design only into the custom SLOTS (`setCustomPet`) — but the pet sprite + floating pet read ONLY the ACTIVE design (`AppPreferences.petDesignState`). So a "saved" custom pet never appeared outside the studio.

### What was done
- **PetDesignerScreen.kt:** `saveAsNewPet` + `selectCustomPet` now ALSO `setPetDesign` (persist as active). `selectPet` persists the re-tagged custom design (`setPetDesign` when custom — custom follows the species; `clearPetDesign` when the default look is picked). `deleteCustomPet` clears the active design when the deleted slot was active.
- **PaperTitleLines.kt rework:** signature now `(ink, title, fontSize)` — length scales with text length + font size (≈0.62em/char + 3 chars past the end, cap 16em/300dp, floor 5em); two quadratic-bezier pen strokes (slight sag top, slight rise second), felt-pen edge (wide faint pass under narrow dark pass, round caps), -2° hand tilt. All 5 call sites updated (Home name 36sp, EntryDetail topic name headlineMedium, Settings/Profile/Cabinet headlineSmall).
- **HomeScreen.kt diary-spiral holes:** 3 punch holes (5.5dp @ 14dp from left) with proper two-tone pressed rims — faint 1dp lip ring + white highlight arc top-left (160°→290°) + ink shadow arc bottom-right (340°→110°). `StrokeCap` import added.

### Validation
Braces balanced (7 files), `git diff --check` clean. No build locally (env rule) — CI on push.

## Prior — start the Compose Multiplatform desktop port (v27t, pushed 40e3c21)

### What was asked
"fix the app freeze when i take a screenshot during explore session. and fix cl and start the desktop port" (CL = the pet-eyes CI log the user pasted).

### What was done
1. **CI fix (pushed `805a430`)** — `CurioPetSprite` pointer tracker used wrong APIs: `awaitPointerEventScope` doesn't exist on `PointerInputScope` (removed), `PointerEventType` vs `PointerType` swapped in the when (now compares `event.type` to `PointerEventType.*`), `isSpecified` doesn't exist on `Offset` (null checks instead).
2. **Screenshot freeze (pushed `3a52267`)** — `DeviceScreenshotWatcher.scan()` ran on the MAIN thread (ContentObserver with main Looper) and did a MediaStore query + full file copy (`SessionShots.copyFrom`) right as the system was still writing the screenshot → the app froze during explore sessions. Fixed: the observer now spawns a `Dispatchers.IO` coroutine for query+copy; the main thread only toggles a `needsScan` flag.
3. **Desktop port started (this commit)** — new `:desktop` module (Compose Multiplatform 1.11.1, JVM 17, `org.jetbrains.compose` plugin already added to the catalog):
   - `desktop/build.gradle.kts` — CMP desktop application (mainClass `com.curio.desktop.MainKt`, jpackage `nativeDistributions` for Msi/Dmg/Deb); resources point at `../app/src/main/assets/topics` so the desktop build reads the SAME topic JSON files (no asset duplication).
   - `DesktopCatalog.kt` — desktop mirror of the topic schema (nullable `byline`/`tier` + `safe*` accessors — Gson bypasses Kotlin default-arg constructors, verified 6675 missing bylines / 591 missing tiers in the real files) + the 36-lane table + wildcard merge.
   - `Main.kt` — window shell: brand sidebar (all 36 lanes with live counts), Spin/Browse mode pills, the spin deck (front ticket + 2 peek cards — kept 2 per the standing rule), reveal card (teaser, byline pill, explore action), browse list. Palette mirrors CurioColors (SoftCream/SoftSand/CreamWhite/CoralBlush/CoralInk/Butter).
   - `settings.gradle.kts` — `include(":desktop")`.
   - CI — new `desktop` job in `.github/workflows/android.yml` runs `:desktop:build` on every push so the port can't rot (native packaging deferred to a future release workflow).
   - Docs — root AGENTS.md gained a Desktop App (desktop/) section + child index entry.

### Not yet ported (milestone 2+)
Room data layer, preferences store, capture/sessions, notifications, floating overlay — each needs a desktop stub. UI parity with Android (incl. the tablet-layout pass + web parity effort) is the ongoing goal; the shuffle deck stays 2 peek cards.

### Validation
Brace balance clean on both new files; full code re-read for API correctness (desktop CMP APIs only: `rememberWindowState(width,height)`, `lightColorScheme`, `CardDefaults.cardElevation`, `Modifier.offset(x,y)`); no Gradle locally (env rule) — the new `desktop` CI job is the gate. 

## Prior this-session (pushed before the desktop port)
- Preset toggle-undo in the category picker + removed the Everything preset (wildcard covers it) — pushed `61c4396`.
- Tablet/landscape layout pass: shell cap 720→880dp (inner 640→800dp), Settings two-pane master-detail on wide windows (full nav list left + page content right), spin deck wide-fit cap 1.0→1.6 (deck + 2 peeks scale up) — pushed `e7b5600`.
- Pet eyes follow the pointer (hover/press/wheel, saturates ~200dp) — pushed `75216ef` (compile-fixed in `805a430`).
- Music-service setting (YouTube Music/Apple Music/Spotify) + engine brand monogram tiles on the Explore pill + "Watch in" pill + avoid-AI pledge copy — pushed `a17739e` (web parity included).

## Older — elevation/blurry-background saga (fe3da7a origin)

## v27r — readability/saturation audit, hero sort+search, section headers, browser UX (fifth request)

### What was asked
1. The new solid category-accent fills can cause text-readability issues and look too saturated sometimes → full app audit + fixes. **User decision: category chips/cards keep solid; only the FIXED-COLOR paper toolbar controls (amber/brown) get a moderated tint fill + readable ink.**
2. Cabinet sort arrow + search button: bigger + a good color; same in the Topic Browser.
3. Section headers in lists of both cabinet and browser: place the category a little higher.
4. Topic Browser: auto-scroll to top when switching category; the back-to-top arrow is too big — shrink it.
5. **CI compile failures from e34a79e (user pasted the log):** `OpenNotebookFormat` unresolved `tint` (5 sub-format calls), `TopicDatabaseScreen` unresolved `themedAccent` (missing import). User asked to push the fix.

### What was done
- **CI fixes (pushed):** re-added `tint: Color` to `OpenNotebookFormat` + caller (the sub-format composables still need it); added `themedAccent` import to TopicDatabaseScreen.
- **Readability/saturation audit:** verified every v27q solid-accent fill's on-accent ink (category chip, Cabinet pills, database chips, Spin chips + deck controls, format chips, sentiment buttons, notebook rows, mood chips, picker tabs/presets, pet designer, dialog rows, onboarding — all pastel-aware ✓). Found the real bugs in the FIXED-COLOR paper controls and fixed them: `FormatToolButton` (paper mode) + `CompactPaperChip` + `NotePaperColorToggle` now use a moderated tint `lerp(surfaceContainerHighest, accent, 0.45f)` with the accent as glyph/label ink (was solid accent + pastelFillInk → white-on-amber in dark paper mode, dark-on-dark in pastel light). Threaded a `paper` flag through FormatToolbar/SelectionFormatBar/SizePickerButton.
- **Sort arrow + search (shared components, Cabinet + Topic Browser):** `CurioSortDropdown` arrow 18→22dp glyph (dropped the conflicting 20dp box), chevron full-opacity ink + 16dp, pill fills deepened (emphasized 0.45→0.35, plain 0.70→0.55); `SettingsHeroActionPill` + `CabinetHeroActionPill` glyph 18→20dp, fills 0.45/0.70→0.35/0.55 (destructive 0.35→0.30).
- **Section headers higher:** Android `DatabaseSectionHeader` top padding 14→6dp; web TopicBrowser section header `pt-4`→`pt-2`; web Cabinet group gap `space-y-6`→`space-y-4` (the Android Cabinet has no section headers — flat grid).
- **Topic Browser UX:** `LaunchedEffect(sortMode, effectiveCat)` now scrolls to top on category switch too (was sort-only); back-to-top arrow shrunk 20dp/11dp/6dp-shadow → 16dp/7dp/2dp-shadow.

### Validation
Braces balanced (10 files), `git diff --check` clean, unused `size` import removed from CurioSortDropdown, `pastelFillInk` import removed from PaperCard (no longer used), `lerp` import verified in RichTextEditor. Docs: `app/AGENTS.md` v27r fixed-color rule. Committed + PUSHED (user asked to push the CI fix).

## v27r — explore dialog buttons, badge borders, passport check (fourth request)

### What was asked
1. "Explore in browser" / "Explore in YouTube" text gets cut in the explore dialog → resize or swap in a browser icon (for the user's chosen default engine) + a YouTube icon, and give the buttons a pill shape.
2. Badges look bad / "weirdly getting cut" → **return the border**. (The category-pill elevation part of the earlier message was explicitly ignored.)
3. Check the quest passport stamps are alright.

### What was done
- **Explore dialog (TopicRevealScreen)** — the two `TextButton`s are now PILL-shaped (`RoundedCornerShape(50)`) with a leading icon + short label so nothing truncates: `travel_explore` globe + "Explore" (opens the user's chosen engine via `buildEngineSearchUrl`) and `youtube_activity` rounded play tile + "YouTube" (opens `buildYouTubeSearchUrl`). Icons tinted with `curioDialogActionColor()`, 18dp + 6dp gap. Both glyphs verified present in `material_symbols_outlined.ttf`; new `CurioIcons.TravelExplore` / `CurioIcons.YouTubeActivity` constants added (no brand glyphs exist in the Material Symbols font, so the generic globe/play-tile stands in for the engine / YouTube — the Settings row already names the chosen engine).
- **Badges (CurioBadges.kt)** — restored the pre-fe3da7a ring borders and REMOVED all shadows (the outer coin shadow clipped at the shelf edges — the "weirdly getting cut"): inner glyph plate ring (1.5dp white@0.55 unlocked / 1dp outlineVariant@0.5 locked), ribbon gem rim (1dp white@0.85), earned-marker rim (1.5dp white), locked-silhouette rim (1dp outlineVariant@0.7), and the "+N" tile's sage ring (`BorderStroke(1.dp, sage@0.28)`). v27n's opaque fills kept. `androidx.compose.ui.draw.shadow` import removed (no shadows left).
- **Passport (QuestsScreen PassportStamp)** — checked and verified structurally sound (opaque fills + flat 2dp); restored the stamp's 1dp ring border (accent ring for UNSEEN, neutral outline otherwise) so stamps read like stamps again, keeping the flat 2dp elevation.

### Validation
Braces balanced, `git diff --check` clean, no leftover `shadow` refs in CurioBadges.kt, glyph names confirmed in the icon font, imports verified (BorderStroke added to CurioBadges + QuestsScreen). Docs: `app/AGENTS.md` v27r note. No build locally — CI on push.

### What was asked
After commit `fe3da7a` ("elevation over borders app-wide") many buttons/cards show **blurry + broken backgrounds**; the user listed: badges next to your name in Profile, the Profile Level·Saved·Lanes pane, the Lanes card, a "C avatar in cabinet", the category chips, and the Spin deck peek cards (which show a "boxy thing" while animating). User instruction: identify + confirm the issue, ask, then fix.

### Root cause (two Compose shadow rules being violated)

**A. `Modifier.shadow()` placed AFTER `.background()` in a modifier chain** — the shadow modifier is INNER, so its `drawBehind` runs after the background draws → the dark blurred shadow is painted **ON TOP of the fill**. Correct order is `shadow → clip → background` (shadow behind, fill covers the inner blur).
- `CurioBadges.kt` — 4 spots: locked silhouette (L350), inner glyph plate (L359), ribbon gem (L397), earned marker (L408) — all `.clip(CircleShape).background(...).shadow(2.dp, CircleShape)`.
- `ProfileScreen.kt` L657 — profile avatar `.clip(CircleShape).background(fill).shadow(2.dp, CircleShape)`.
- `SpinScreen.kt` PeekCard — `Surface(color = Transparent, shadowElevation = 2.dp)` with the fill applied via a `.background()` on the Surface's own modifier param (OUTSIDE the surface internals) → fill draws first, then the Surface shadow paints on top of the card. Also **v24 explicitly REJECTED deck-card shadows** ("weird look while the cards animate", `shadowsOn = false` hardcoded) — the 2dp Surface shadow re-introduces exactly that (the "boxy thing").

**B. `shadowElevation` on a Surface whose fill is TRANSLUCENT (alpha < 1)** — the shadow is drawn behind the shape and shows **THROUGH the translucent fill** → muddy/broken interior. Elevation only renders cleanly on opaque fills.
- `ProfileScreen.kt` L~730 — Level·Saved·Lanes pane: Surface `color = Transparent`, `shadowElevation = 3.dp`, inner Box wears a 12–55% alpha gradient → shadow visible through it.
- `ProfileScreen.kt` L~1139 — LanesCard lane tiles: `color = accent.copy(alpha = 0.14f)`, `shadowElevation = 2.dp`.
- `CabinetScreen.kt` L~1074 — CabinetHeroActionPill (Search/Sort/Select/Cancel glass pills): `color = ink@30–65%`, `shadowElevation = 3.dp`. (commit ae83130 had deliberately added the border for these to read on the rose banner; the elevation commit removed it.)
- `CurioBadges.kt` L~471 — "+N" tile: `color = sage@13%`, `shadowElevation = 2.dp`.
- `CurioCategoryChip.kt` — `FilterChip` elevation; selected container is `category.tint` = accent **@20% alpha** → the chip shadow shows through the translucent selected fill.

### Verified NOT broken (no change needed)
- `CurioSettingsCard` (opaque `surfaceContainerLow` fill), `CurioEntryCard` (opaque fill), `CurioCategoryCard` (fill is inside Surface content → covers shadow), `FilterChipLite` (same), `CurioSearchField` (opaque).

### Proposed fix (awaiting user approval)
1. Reorder shadow modifiers to be BEHIND the fill where the fill is opaque (profile avatar).
2. Remove the blurry inner `.shadow()`s in the badges and restore the crisp ring borders they replaced (the coin design) — or one clean outer shadow behind the whole medal.
3. Remove `shadowElevation` from translucent surfaces (stat pane, lane tiles, cabinet glass pills, "+N" tile, selected chips) and restore the hairline borders they had before fe3da7a (ae83130's legibility intent).
4. Deck peek cards: `shadowElevation` back to `0.dp` (Surface stays flat per its own comment; layered shadow stays off per v24 closeout) → kills the boxy animation artifact.

### User decisions (ask_user)
1. **"C avatar in cabinet"** = the round **profile avatar** (initial letter) — they misremembered its location; no avatar exists in CabinetScreen.
2. **Strategy:** keep shadows everywhere, **make translucent fills opaque** (not full revert, not border-restore).

### What was done — first pass (user's listed items)
Per the confirmed strategy — elevation stays, fills become opaque, misplaced shadows reordered, deck shadow removed:
- **CurioBadges.kt** — ONE clean shadow on the outer coin Box (behind the opaque metal); locked silhouette + gem + earned marker shadows moved BEFORE their clip+fill (was: smear on top); glyph-plate shadow removed (it had no fill — pure blur over the metal); locked fills made opaque (secret keeps a darker blend); "+N" tile fill → opaque `lerp(surfaceContainerLow, sage, 0.13f)`.
- **ProfileScreen.kt** — avatar: `.shadow(2.dp, CircleShape)` moved before `.clip().background(fill)`; Level·Saved·Lanes pane gradient made opaque (12–55% alpha → opaque lerps resolving to the same tints); LanesCard tiles → opaque `lerp(surfaceContainerLow, accent, 0.14f)`.
- **CurioCategoryChip.kt** — selected fill `category.tint` (accent @ 20% alpha, bled the chip shadow) → opaque `lerp(surface, accent, 0.20f)`.
- **SpinScreen.kt PeekCard** — `shadowElevation` 2dp → 0dp: the elevation commit re-added the v24-REJECTED deck shadow ("weird look while cards animate") — that is the boxy thing during the reel; Surface stays flat per its own contract.

### What was done — app-wide extension pass (second request, 24 files)
User asked to extend the same opaque-fill fix app-wide (hero glass pills, Quests, Reveal, Entry Detail, coming-soon tiles, the small explored/unexplored pills in Home/Recent, and everything else the sweep found). Two rules applied consistently:
1. **Opaque-lerp fill** (`lerp(backdrop, color, oldAlpha)`) wherever the element sits on a flat surface — kills the shadow bleed with a pixel-similar tint.
2. **`shadowElevation = 0`** on true frosted glass over heroes/imagery — glass can't hold a shadow (it bleeds through); the frost defines it.
3. **Reorder `.shadow()` before `.clip()/.background()`** for the order-bug chains (shadow was painting on top of fills).

Fixed: hero pills — CabinetHeroActionPill (opaque `lerp(ink, bannerFill, 1-alpha)`, banner fill threaded through the hero trailing slot + all 7 call sites), SettingsHeroActionPill (`backdropOverride ?: settingsRoseAccent()`), CurioSortDropdown (required `backdrop`, both call sites updated). Chips/pills/tiles: SaveCapture tag chips + add-gallery tile, SoundBite record card + mic tile, CaptureFormatComponents add-quote, Home explored-tag pill + currently-exploring stop button, Recent tag pill, Quests +XP pill + quest stage cards + passport stamps + dim tile (also reordered), TopicReveal tag chips + disabled action, EntryDetail #tags + transcribing note card + voice button + structured rows (opaque) + hero session pill + frosted button + frosted hero pane (shadow 0), GalleryWall inline mood board (opaque accent-lerp; full-screen keeps translucent tint, has no shadow), CategoryPicker + DeckPresets option rows, Onboarding option pills, PetDesigner palette slots + editor target + prompt + 4 swatch/canvas order bugs, PromoStatusCard + promo chips (opaque rose-lerp + reorder), Support update card, Settings/RecycleBin dialog rows (opaque selected, transparent rows get NO shadow), RichTextEditor + PaperCard toolbar chips (opaque active) + color-dot order bugs, CurioCategoryCard coming-soon tiles (opaque).

Left translucent by design (negligible or intentional frosted glass over imagery): ≥90% alpha fills (GalleryWall 780/923, EntryDetail 1071/3100/3122, MoodBoardZoom 634), OpenNotebook radio (selected opaque / unselected no shadow), Spin 3D button (gradient is content, covers the shadow), Home stats (opaque when shadowed), Profile/Reveal 18% glass pills at 0dp.

### What was done — v27q selection-flatten pass (third request)
User: "keep selected 2 and else 2 too in elevation, and use select highlight color change for selected" → confirmed **all selected-raise elements app-wide**, selected state reads as a **SOLID accent fill**. Executed:
- **Elevation flattened to a flat 2.dp in BOTH states** for every selectable chip/card/row/tile: category chip, topic card (was 8/3), category card (8/3), spin compact chips + deck control pills (6/3), database filter chips (4/2), Cabinet FilterChipLite (was `elevation.coerceAtLeast(3.dp)`), picker page tabs + preset chips, settings hour chips (3/1), onboarding theme/search chips (4/2), PetDesigner (armed slot, used swatch, color swatches, frame thumbs, picker cards, palette rows, palette dots, custom-pet card, library card), Reveal sentiment button, EntryDetail/SaveCapture section chips, mood chips, notebook choice rows, RecycleBin/Settings dialog rows (3/0), RichTextEditor + PaperCard toolbar chips (3/1), PaperCard color swatches.
- **Solid accent selected fills** (content flips to on-accent ink): category chip `themedAccent()`+`onAccent()`; picker/preset `primary`+`onPrimary`; dialog rows `curioDialogActionColor()` with `dialogRowSelectedInk()`/`recycleRowSelectedInk()` (white, black in AMOLED) + unselected rows now opaque `surfaceContainerHigh` so the flat 2dp shadow is clean; database + Cabinet pills + notebook rows `accent`+`pastelFillInk(accent)` (Cabinet category accent switched `categoryInk()`→`themedAccent()`, ink→`onAccent()`); editor/paper toolbar `accent`+`pastelFillInk(accent)`; SaveCapture wash-ON fill → opaque `lerp(surface, accent, 0.20f)`; PetDesigner armed slot `primary`+`onPrimary`, swatch selection via contrast-aware check (no shadow).
- **Existing non-elevation cues kept**: topic-card check badge, category-card solid gradient, pet "Your pet" pill, paper swatch check, palette dot size raise (38 vs 32dp).
- **Deliberately left**: non-selection toggles (3D button, paper-stats toggle, field-border, fullscreen capture) keep their own elevation; fan-deck per-card depth shadows are deck order; OpenNotebook's tiny radio dot keeps its 2dp/0dp (opaque fill, anti-smudge).

### Validation
- Braces balanced (full-repo check), `git diff --check` clean, every `lerp(` import verified present (stale imports removed from CurioCategoryChip/SettingsSharedComponents/RecycleBin/CategoryPicker), all `backdrop` call sites wired (Cabinet 25 uses, SortDropdown 3, SettingsHeroActionPill default), new `pastelFillInk`/`onAccent`/`themedAccent` imports verified used.
- No compile/build possible locally (no SDK) — CI on push is the gate.
- DOX pass: root `AGENTS.md` compile-safety rule 11 (shadow order + opaque fills + no deck elevation); `app/AGENTS.md` v27n note updated for the app-wide scope + v27q flat-2dp no-selection-raise rule.

## Previous request (v26 — Topic Browser header rebuild + back-to-top arrow)


## Previous request (v26 — Topic Browser header rebuild + back-to-top arrow)

**Status:** Implemented, uncommitted in working tree (user's standing rule: no push unless asked).

### What was asked
1. Floating arrow at top when scrolled too far down → jumps back to top.
2. Search, sorting, and filters live in the header (not scrolling in the list).
3. Category filter becomes a floating bar like the Cabinet screen.
4. Search has the same morph-open animation as the Cabinet.

### What was done
- **SettingsHeroHeader (SettingsHubScreen.kt)** — extended the shared settings hero with:
  - optional `trailing: (@Composable (ink: Color) -> Unit)?` slot (ink-glass pills on the top row beside the back pill),
  - optional morph-open search: `searchActive/searchQuery/onSearchQueryChange/onCloseSearch/searchFocus/searchPlaceholder`,
  - `AnimatedContent` scale/fade morph (search open: scaleIn+fadeIn 280ms; close: title fadeIn, scaleOut+fadeOut 200ms) — Cabinet's exact search-morph contract,
  - Cancel pill replaces trailing pills while searching,
  - new public `SettingsHeroActionPill` (with `modifier` param) — backward compatible: all 11 existing callers unaffected (defaults).
- **TopicDatabaseScreen.kt** — header rebuild:
  - search + sort pills ride the hero top row (sort = `CurioSortDropdown` emphasized ink; search = PetLandmark-wrapped `SettingsHeroActionPill`),
  - old in-list controls item (search box + chip LazyRow + sort LazyRow) removed; needle now reads `searchQuery`,
  - new `DatabaseStickyChipBar` — Cabinet-style floating category filter: rests below hero, lifts/pops/pins on scroll (`LazyListState.layoutInfo` progress, `DatabaseChipPop` stagger scale 0.90→1.0, label blooms toward accent via `popProgress`),
  - new floating back-to-top arrow: `AnimatedVisibility` (fade+scale), shown when `firstVisibleItemIndex >= 10` (~700dp ≈ one full screen), top-end below the pinned chip bar, clears the 54dp alphabet rail (end=68dp), tap → `scrollToItem(0)` + reset saved scroll state,
  - constants ordered so init order is safe (`DatabaseChipBarHeight` before `DatabaseContentTop`).

### Validation
- Braces OK (both files), `git diff --check` clean, no leftover `query`/OutlinedTextField refs, all new imports used.
- Code review passed; reviewer flagged back-to-top threshold (4 rows) → tuned to 10 rows (~one screen).

### Also uncommitted in the same working tree
- Pastel header saturation bump (Home/Profile/Settings rose accents, +5%) — from a previous request, also awaiting push.
- Topic Browser header rebuild (hero search/sort pills + floating chip bar + back-to-top arrow) — from the previous request, also awaiting push.

## Request: update check says "up to date" when a newer release exists

**Status:** Fixed in working tree (UpdateChecker.kt). Push pending.

### Root cause
`UpdateChecker.isNewer` used `split('.').mapNotNull { it.toIntOrNull() }`, which **silently dropped non-numeric segments**. The repo's real tags are `v1.0.0.1-test` and `v1.0-beta` (confirmed via GitHub API: `/releases/latest` → 404, so the tags fallback is used). `"v1.0.0.1-test"` → `[1,0,0]` == installed `"1.0.0"` → wrongly "up to date". Same for `v1.0-beta` → `[1,0]`.

### Fix (UpdateChecker.kt)
- New `compareVersions(a, b)`: parses each dotted segment into (numeric core, prerelease suffix) via `parseSegment` — numeric cores compare numerically, a bare number beats a suffixed one (1.0.0 > 1.0.0-beta), both-suffixed compare as text, missing segments are 0. `isNewer` delegates to it.
- Tags fallback now picks the max tag by version comparator (`maxWithOrNull(Comparator {...})`) instead of trusting array order.
- Verified by hand across 8 cases (the reported bug, beta-vs-release, numeric 1.2.10>1.2.9, equal, missing segments, 2.0.0>1.0.0.1-test). Code review passed.

## Request: new Preferences section + settings rearrangement

**Status:** Implemented in working tree. Push pending (user's standing no-push rule).

### What was asked
Add a new preference option inside Settings → Personalize and move preference-type settings there (search engine was named; I suggested the rest). Rearranged the settings.

### Decisions (user-confirmed via ask_user)
- Moved into new **Preferences** screen: **Search engine, Pet games, Pet chatter, Explore sessions, Floating explore bubble, Live explore notification**. (NOT moved: Voice-to-text stays in Recording; "Explore bubble option in Explore dialog" stays in Notifications.)
- Placement: **right after Appearance** in Personalize.
- Judgment call flagged: **Display over other apps** (the overlay-permission row) moved to Preferences with the bubble — it wasn't in the options list, but its grant/decline machinery (launcher + ON_RESUME observer) is inseparable from the bubble toggle, so splitting them would duplicate complex logic.

### Changes
- `SettingsPage.PREFERENCES` enum entry + `PreferencesSection` composable (search engine + SearchEngineDialog, explore sessions, live notification with POST_NOTIFICATIONS flow, floating bubble + overlay permission with launcher/ON_RESUME handling, pet chatter, pet games).
- `NotificationsSection` trimmed to daily reminder (+hour chips) + bubble-opt-in-in-dialog; keeps its own permission launcher for the reminder.
- `AppearanceSection` lost pet chatter/games.
- `SettingsHubScreen`: Preferences row (CurioIcons.Tune) after Appearance; deep-search index rows re-pointed to SETTINGS_PREFERENCES with pref-* rowKeys (pref-search-engine, pref-sessions, pref-live, pref-bubble, pref-overlay, pref-pet-chatter, pref-pet-games); Notifications deep rows now just reminder + bubble-dialog.
- `CurioRoutes.SETTINGS_PREFERENCES` + NavHost composable; `CurioIcons.Tune = "tune"` (glyph verified present in material_symbols_outlined.ttf).

### Validation
- Braces OK (5 files), git diff --check clean, no stale notif-*/appearance-pet-* rowKeys, only one exhaustive `when (page)` (updated). Code review passed.

## Request: commit and push everything

**Status:** Committed and pushed. Working tree clean.

All pending work from this session was bundled into one push:
1. Preferences settings section (new screen + hub row + deep-search index)
2. UpdateChecker version-comparison fix
3. Topic Browser header rebuild (hero search/sort pills + floating chip bar + back-to-top arrow)
4. Pastel header saturation bump (Home/Profile/Settings)
5. Warm no-AI pledge copy (onboarding + explore dialog)
6. CI APK artifact retention 14 → 1 day (android.yml)

### Not done / follow-ups
- No web app changes (user's standing rule: Android app only).

---

## v27 — Experimental paper headers (UNCOMMITTED, not pushed per user)

Request: header "cut look" (2 tilted corner strokes + 3 small lines top-right), diary-style stamped pin holes on the left edge (NOT spiral rings — user clarified), and paper-style soft rose/creamy cards for the Home Streak · Cabinet · Topics bar instead of the frosted/translucent pane. All behind experiments, OFF by default.

Shipped in working tree (6 files + 1 new):
- AppPreferences: 3 flags (paperHeaderCutsState, paperHeaderHolesState, paperStatCardsState), default false, with is/set + init.
- New ui/components/PaperHeaderAccents.kt: Canvas accents — pin-hole column (pressed rim + deeper disc + bottom highlight), two rotated corner strokes (bottom-left), three fading ticks (top-right); size-shadowing avoided (w/h).
- ExperimentsScreen: "Paper & headers" section with 3 toggles.
- Wired as FIRST child (drawn behind content, torn-clipped) in SettingsHeroHeader (12 screens), CabinetHeroHeader, ProfileHero, Home quest hero. Settings/Profile pass symbolTint (AMOLED-consistent), Cabinet/Home pass ink/questInk.
- Home stat bar: when paperStatCardsState, solid paperStatBg (light lerp(heroFill,0xFFFFF6EB,0.62), dark lerp(heroFill,0xFF2A211C,0.50)) + 3dp elevation; Box background branches at Modifier level (Color vs Brush).

Validation: braces OK (7 files), diff --check clean, code review passed (2 cosmetic fixes applied: symbolTint on AMOLED). Reviewer note: Home corner strokes sit behind the stat card (partially hidden) — acceptable for the experiment; holes + ticks still show on Home.

CI fix pushed earlier this turn: 28122f2 (Cabinet LazyGridItemInfo.offset.y — IntOffset vs LazyListItemInfo.offset Int).
- Entry Detail hero also wired (per-category heroInk). Committed + pushed on Alpha; PR Alpha → main tracks the branch, so every future push keeps it updated.
- v27b: Notifications section removed, all notification rows (daily reminder + hour chips, bubble-in-dialog) merged into Preferences; evolution level raised 7 to 15 (CurioPet gates, label, hint, comments); DEFAULT_CURLED_16/32 sleep sprites redrawn to match the standing pet (head/ears/scarf/tucked feet). Committed + pushed (PR #17 auto-updates).
- v19: pet games isolated — camouflage is now a find-me round (tap the faint ghost to win; the old visible edge-dash teleport is gone, it fades in place and slips away invisibly), taps mid-game only interact with the game (no boop/dart queued), and all three games (hide-and-seek, camouflage, spark) wind down into a ~3.2s touch-interruptible idle with pokes/peeks suppressed afterward. Committed + pushed (PR #17 auto-updates).
- v20: navigateToTab now treats a pushed tab-route instance (Cabinet opened from Profile, stack HOME→PROFILE→CABINET) like any pushed screen — pops back to HOME first so the popUpTo+singleTop navigate can no longer self-cancel into a dead Home tap; genuine tab instances (entry directly on HOME) keep save/restore tab-state behavior. Committed + pushed (PR #17 auto-updates).
- v20b: light-mode wash-out fix — new theme-aware ink helpers curioRoseInk/curioGoldInk/curioSageInk (deep CoralInk/GoldInk/SageInk on light cream, pastels on dark/AMOLED) applied to every pastel-as-ink spot: Profile XP card, shared card headers, Home drawer, onboarding permissions, topic-history bookmark, quests (trophy/progress/chips/badges/stamps/dailies), support status+links+download progress, promo card, topic-db explored chips, crash screen, badge overflows; bonusGold dedupes to curioGoldInk. Committed + pushed (PR #17 auto-updates).
- v27b: paper experiments reworked to the intended placement — PaperTitleLines (2 short lines) under the title text in all 5 hero families (settings hub, cabinet, profile, home, entry detail) gated by Title cut lines; hero-edge accents (corner strokes/ticks/left holes) removed (PaperHeaderAccents.kt deleted); Stamped pin holes now punch SEE-THROUGH EvenOdd holes into the Home Streak·Cabinet·Topics paper card (Surface transparent when holes on, pressed-rim rings, border+shadow kept); experiment labels updated. Committed + pushed (PR #17 auto-updates).
- v26c: Topic Browser scroll rework — CurioScrollIndicator now maps knob travel 1:1 onto the whole list (scrollable/travel ratio + 1..2x ramp) and drains accumulated deltas once per frame (LaunchedEffect + withFrameNanos) instead of a coroutine per drag event (fixes lag + slow scroll); gesture rewritten on awaitEachGesture/awaitVerticalTouchSlopOrCancellation so a pure tap toggles the A–Z rail (drag gestures never fire onDragEnd for a tap, so it could never open); back-to-top arrow centered on the screen with the glyph centered in the circle (M3 Surface has no contentAlignment). Committed + pushed (PR #17 auto-updates).
- v27: hero ink-glass pills deepened — SettingsHeroActionPill, CabinetHeroActionPill and CurioSortDropdown fills went from 18%/42% (55% destructive) to 30%/55% (65% destructive) alpha with the border raised 28%→42%, and the sort dropdown gained its missing border; fixes search/sort/select pills being nearly invisible on the rose banner in Cabinet and Topic Browser (and consistently across the settings-family heroes). Committed + pushed (PR #17 auto-updates).
- v27: explore-session attachments — ExploreSession gained shared note + screenshotPaths (JSON-persisted); pending-write package now carries note+screenshots and survives session clear (append/remove/set + peek accessors, hasPendingWriteFor); CurioEntry/CaptureEntity gained sessionNote + sessionScreenshots (Room v6 migration 5→6); new SessionShots (app-private PNG store), ScreenFrameCapturer (single-frame MediaProjection → PNG), ScreenCaptureRequestActivity (transparent consent host), DeviceScreenshotWatcher (MediaStore ContentObserver auto-attach, permission-gated) registered from MainActivity; ExploreSessionService: captureConsent static + ACTION_CAPTURE + captureScreenshot with Android-14 mediaProjection FGS promotion, FLAG_SECURE on the bubble window (timer never appears in shots), finishToWritePage via ACTION_STOP, note-focus window flag flip; bubble reworked — NO morph animation (instant swap + one resize burst), icon-only pause/hide, note field (local draft → setSessionNote), screenshot button with count badge, Finish & write it down; ExploreReminderReceiver ACTION_STOP hands off note+screenshots; reveal flow asks READ_MEDIA_IMAGES once; SaveCaptureScreen — floating note button + live-reactive screenshots section (add via PickVisualMedia, remove with X) + attach on save; EntryDetail shows note + lightbox-tappable thumbnails; manifest: READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE/mMediaProjection FGS permission, service type specialUse|mediaProjection, ScreenCaptureRequestActivity registered. Committed + pushed (PR #17 auto-updates).
- v27c: backup/restore v5 — session screenshots join the in-app backup: export bundles filesDir/session-shots bytes keyed by original path (deduped across shared entries) into a new BackupPayload.sessionShots map; restore rewrites each capture sessionScreenshotsJson to restored paths via a whole-restore shared index (one original path -> one restored file, preserving one-session-shared shots) using hardened SessionShots.restore(key, bytes) (path-traversal guarded like ImageStorageManager); CaptureEntity.deserializeStringList made internal for reuse; validatedCaptures now normalizes sessionScreenshotsJson to "[]" for pre-v6 backups (NOT NULL column, Gson Unsafe skips defaults — restores of old backups no longer crash); data_extraction_rules comment notes session-shots live in the excluded files/ domain like audio. Committed + pushed (PR #17 auto-updates).
- v27d: CI compile fixes + recycle-bin expiry. Compile fixes: SaveCaptureScreen ExploreSessionStore handoff calls now pass context (appendPendingScreenshot/removePendingScreenshot/setPendingNote/clearWriteSessionHandoff); removed the bare non-composable {} block around the Session screenshots section; CurioCrashScreen detectCategory is @Composable (curioSageInk); EntryDetailScreen dropped the duplicate java.io.File import, the duplicate @Composable on SessionNoteBlock and restored @Composable on GalleryWallRender; HomeScreen punched holes with addOval(Rect(center,radius)) since Path.addCircle does not exist in compose-ui 1.12 alpha (verified from the cached jar); CurioScrollIndicator drag delta now change.position.y - change.previousPosition.y (positionChange became a Boolean in the alpha); CurioRoutes uses NavController.previousBackStackEntry (NavBackStackEntry accessor removed in navigation 2.9); ScreenFrameCapturer smart-casts projection via explicit null check. Feature: recycle-bin expiry — AppPreferences gained recycleBinExpiryDaysState (default 30, 0=keep forever) with get/setRecycleBinExpiryDays; CaptureDao+CaptureRepository gained one-shot getTrashed(); new RecycleBinExpiry.purgeExpired(context) purges entries past the window with media deletion off the main thread (Dispatchers.IO); MainActivity purges on cold start via lifecycleScope; RecycleBinScreen gained an Auto-delete after row (Keep forever/7/30/90 days radio dialog mirroring SearchEngineDialog), purges on open, re-applies immediately on window change, and shows a bottom hint when the bin is empty. Reviewed, braces/diff clean.
- v27e: backup v6 — the pending (unsaved) write handoff rides the in-app backup. ExploreSession gained clearPendingWrite(context) (topic-agnostic). CurioBackupManager FORMAT_VERSION 6 with a new PendingWriteBackup(categoryId, topicName, elapsedMillis, note, screenshotPaths) + BackupPayload.pendingWrite; export reads pendingWriteTarget() once (pwTarget) and merges its screenshot paths into the sessionShots bundle (deduped with capture shots) then builds the payload object from peekWriteSessionMillis/Note/Screenshots; restore, after ExploreSessionStore.seed, remaps the pending write screenshot paths through the SAME shared shotIndexByPath (shot-$idx, so a shot shared by an entry and the pending write restores once) and re-handoffs via handoffWriteSession, else (pre-v6 backups) clears the stale prefs-resurrected package so the write page never shows dangling attachments. Reviewed; braces/diff clean.
- v27f: explore-session duration shown alongside the topic. SaveCaptureScreen topic strip: the explored row moved from under the topic to sit INLINE next to the topic name (Timer glyph + formatSessionShort, topic ellipsizes via weight(1f, fill=false) + TextOverflow, added import; comment updated). EntryDetailScreen hero: when sessionTimeMillis > 0 a centered pill (Timer + explored X, heroCardInk 12% fill + 28% border) renders between the title and the frosted meta bar, with the trailing spacer tightened to 10dp when it shows so the fixed 400dp hero never overflows. Reviewed; braces/diff clean.
- v27g: CI fix — HomeScreen paper-hole punch now uses Path.Direction.Clockwise (compose-ui 1.12 alpha renamed the enum constants from CW/CCW to Clockwise/CounterClockwise; verified from the cached ui-graphics classes.jar constant pool).
- v27h: Home paper-card + session fix batch. TornStatPaperShape (PaperCard.kt) — stat card wears a torn outline (EXTENDED bold soft tear on the top edge, corner-faded; sharper 3.5dp value-noise jitter on the other three) behind a new Torn paper edges experiment toggle (AppPreferences.paperStatTearState + Experiments row). HomeScreen: stat card shape switches torn/rounded, stamped holes moved to a VERTICAL column down the LEFT edge (EvenOdd punch reuses the Surface own outline via createOutline), Topics stat always shows the true total (produceState seeded from the warm cache, refreshed with TopicJsonLoader.countCanonicalTopics). ExploreSessionService: FLAG_SECURE removed from the bubble overlay + note-focus flags so device screenshots work during sessions. SaveCaptureScreen: attachments section also shows when a note/screenshots exist; note editor wears note-paper colors (theme-aware cream/dark + paper ink), floating note button is a solid accent pill. EntryDetailScreen: SessionNoteBlock wears the same note-paper surface (dead tintWash local removed). SoundBiteFormat: Delete recording action (AudioStorageManager.deleteAudio + idle reset). Reviewed; braces/diff clean.
- v27i: content-expansion batch B1 — 15 new category lanes. Category.kt: CategoryId grew BIOLOGY, CHEMISTRY, ANIMALS, PLANTS, TECHNOLOGIES, ASTRONOMY, HISTORY, GEOLOGY, MEDICINE, PSYCHOLOGY, MATHEMATICS, ECONOMICS, LANGUAGE, ENGINEERING, OCEANS (routeSlug + defaultOrder + newLanes set; family maps into EXISTING families SCIENCE/BOOKS so no new CategoryFamily values and the CurioIcons when(family) stays safe); 15 CurioCategory entries with 15 new color triplets in CurioColors; `visible` now excludes unshipped new lanes (id in newLanes && !isReady) so Home chips/Spin sheet/Cabinet never show dead empty decks, and HISTORY is isReady=true (117 topics). CaptureEntity verb when + ExploreSession reflectionQuestion when cover all 36 values (verified exhaustive, 36 refs each). CurioCategoryCard gained comingSoon param (dimmed border/surface/icon, label swaps to Coming soon, combinedClickable enabled=!comingSoon, edge shine intensity 0.25). CategoryPickerScreen rewritten: 2-page HorizontalPager (Original = visible minus newLanes; New = all newLanes minus hidden, coming-soon tiles for !isReady), 5 preset chips (Brainy/Stories/Screens/Sounds/Everything, Everything resolves to all visible at tap; tap enters multi-select pre-ticked so the mix is visible+editable) replace the old subtitle, page-tab pills (Original/New with counts) animateScrollToPage, tap/hold hint restored in the tabs row. Assets: 14 empty [] lane files (wildcard merge iterates every CategoryId — empty files load fine) + history.json with 117 schema-compliant topics (all Read verb, HISTORY categoryId, ≤60min, no dup IDs across all 36 files). Reviewed; braces/diff clean.
- v27k: batch — (1) in-app MediaProjection screenshot capture REMOVED entirely (freeze fix): deleted ScreenFrameCapturer.kt + ScreenCaptureRequestActivity.kt; ExploreSessionService stripped of ACTION_CAPTURE/captureConsent/captureNow/captureScreenshot/onScreenshot wiring; ExploreBubbleContent lost the Screenshot button + count badge (controls are now just Pause + Hide); manifest dropped the mediaProjection FGS type + consent activity; DeviceScreenshotWatcher auto-attach kept and doc updated; TopicRevealScreen comment updated. (2) SaveCaptureScreen: scrollable format body wrapped in a Box; new SessionNoteFloatingPill (bottom-end, imePadding, accent pill showing the note text, toggles a paper note-editor popup above the save CTA); SessionAttachmentsCard is now title + screenshots only (note moved to the pill). (3) CaptureFormatComponents ImageThumb now renders a remove × badge (onRemove was wired but never drawn — Field Notes/Reel Notes/Marginalia attachments are now deletable; Gallery Wall already had remove-with-confirm). (4) Deck presets reworked in DeckPresets.kt: Science / Entertainment / Arts & Stories / History & Ideas / Everything / Clear (clearAll flag); TopicJsonLoader.countFor(id) added; "Mixed · N" labels (Spin DeckBar, BottomCta, both pickers' Mix buttons) now show the TOTAL topic count (LaunchedEffect + countFor, seeded with lane count so no "Mixed · 0" flash); Spin's inline CategoryPickerSheet gained the Original/New HorizontalPager 2-page layout + PickerPageTab (made public, shared from CategoryPickerScreen) + Clear-chip handling in both pickers. Committed + pushed.
## Batch: Technologies lane → 1000 + header depth toggle (v27j)

- Technologies: 1000 topics authored in 50 chunks (computing, internet, AI, robotics, cybersecurity, privacy, web economy, mobile, hardware, communications, fintech, gaming, VR/AR, data, smart home, health tech, everyday tech, tech history, creators, careers, education, energy, transport, aerospace, biotech, food tech, materials, manufacturing, semiconductors, browsers/search, e-commerce). Merged into technologies.json, deduped (1 slug collision renamed), local validator 0 problems, TECHNOLOGIES flipped isReady=true in Category.kt.
- CI fix: SessionNoteFloatingPill's root Column used BoxScope-only Modifier.align — moved the align into a Box wrapper at the call site.
- New "Deeper header color" preference (default ON) in Experiments → Paper & headers: CurioCategory.headerAccent() darkens themedAccent hue-preservingly (light 0.88, dark 0.94 lightness) and is applied to the three category-colored torn-hero fills (Cabinet, Entry Detail, Topic Reveal). Watermarks/ink untouched.
- Committed + pushed (205b1d4 for astronomy; this batch on top).
- Content marathon: geology.json 1000 topics merged + GEOLOGY isReady (v27k). Next lanes: medicine, psychology, mathematics, economics, language, engineering, oceans.
- v27l: updater rewritten to hit releases LIST (prerelease-aware) + isNewer = different-tag; heroBlueState pref (OFF default) + HomeAzure/HomeAzureDark; azure branch in homeRoseAccent/profileRoseAccent/settingsRoseAccent; Quests CurrentQuestCard routed through settingsRoseAccent; Appearance toggle added; pushed.
- v27l teaser rewrite: astronomy.json done (1000/1000 rich, 74-134w each, avg 103w; 0 validation problems; committed). Next: technologies (1000), geology (1000).
- v27m teaser rewrite: technologies.json done (1000/1000 rich, 101-260w each, 0 validation problems). Next: elevation-vs-borders app-wide conversion (user: use elevation everywhere, all themes incl AMOLED).
- v27n elevation pass: app-wide border→elevation conversion complete (37 files). All BorderStroke/Modifier.border card outlines replaced with shadowElevation (+ tonal steps); AMOLED black fills → surfaceContainerLow step (shadows invisible on pure black); selected states raise elevation (4–8dp). Only exception: M3 FilterChip's required border param is 0dp transparent (canonical no-border). Technologies 1000/1000 teasers done + pushed earlier. CI-compile checks (braces, imports, remnants) all pass.
- v27o CI-compile fixes for the elevation pass (10 spots): duplicate shadowElevation args in EntryDetail mood board, Home floating pill + session card, TopicReveal tags pill, Spin deck surface, ExploreBubble icon button; OutlinedButton (Home) now uses ButtonDefaults.buttonElevation + 0dp border; PetDesigner Card uses CardDefaults.cardElevation; CurioCategoryChip drops the unsupported selectedElevation; ExploreBubble overlay stays flat (windows clip shadows). Plus the "Explore in YouTube" dialog button no longer wraps (maxLines=1 + ellipsis + tighter padding). All brace/dup/elev checks pass.
- v27p ink-contrast fix (CategoryInk.kt): computed WCAG contrast for every old+new accent in light/pastel — mid-lightness accents (green/lime/sky/amber/emerald/teal/red/fuchsia/blue/coral) read 2.0-4.0:1 as text ink. Added readableLightInk (same-hue, L=0.24) + needsLightDeepInk (luminance>0.105) rule; categoryInk/readableAccentInk/categoryInkFor deepen only mid-lights, onAccent pastel-light always uses the deep twin, pastelFillInk light branch deepened 0.30->0.24. Verified all 22 accents >= 4.5:1 on wash/card/pastel fills.
