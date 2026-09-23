# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 0. THE CURRENT REQUEST — §58 — read-aloud: a read-along mark, skip controls, and a voice engine of the member's own choosing (v463 + v464 + root cleanup; PHASES 1–2 SHIPPED)

> i want you to improve book reader, with highlight of which sentence row its reading, and then customisation play pause skip and a custom voice download option except system voice, research hats a better natural reading voice donload available.

**FOUR QUESTIONS WERE ASKED FIRST, and the answers drive everything below** — this is a new measure with a native-dependency decision inside it, which is exactly the case the root AGENTS "ask when unsure" rule is for:

| The question | The member's answer |
| --- | --- |
| The voice engine | **On-device neural downloads as the DEFAULT; cloud optional** |
| What a skip steps | **Both — sentence AND chapter** |
| The mark | **Sentence wash, with the rest of the page standing back** |
| The cloud door, asked again once it was shown to be PAID | *"research any free cloud one and for offline downloaded voice packs, add some more research and find the best one for book reading"* |
| Then, at the choice | **Piper medium + Kokoro as the optional "best quality" pack**, and **Edge TTS as a hidden Dev-page experiment** |

**THE RESEARCH (asked for explicitly — this is the reason for every choice below):**

- **Cloud is all paid, per character.** ElevenLabs free = 10k credits/mo (~10 minutes of audio) and **no commercial licence**; paid from $6/mo, API ~$0.05–0.10 per 1k characters. OpenAI `tts-1` **$15 / 1M chars**, `tts-1-hd` $30 / 1M. Google Cloud **$60 / 1M** (Neural2/Studio). **A 300-page novel is ~500,000 characters of speech: ~$7.50 for one read with OpenAI, ~$50 with ElevenLabs** — and re-billed on every replay unless the audio is cached to disk.
- **Genuinely free and legitimately sanctioned: Google Cloud TTS** — 1M chars/mo on Chirp 3 HD (≈1.5 novels a month), needs a key **and a billing account**. Azure is 500k/mo free; Amazon Polly's free tier is account-age dependent (pre-15-Jul-2025 accounts only).
- **Free, keyless, but UNSANCTIONED: Edge TTS** (the Edge browser's own read-aloud endpoint — `rany2/edge-tts`). Excellent voices, no signup at all, but unofficial and undocumented, so it can break or be blocked: hence a HIDDEN Dev-page experiment and never a shipping path.
- **Offline packs, against sherpa-onnx's own RTF benchmark** (Raspberry Pi 4, 4 threads — a phone-class proxy; **RTF must be < 1 or narration cannot outrun playback**):
  - **Piper medium — 61 MB, per-model voice licence, RTF 0.357** → fastest and smallest; **the long-form workhorse, and the default pack.**
  - Matcha-TTS — 71 MB, RTF 0.411.
  - **KittenTTS nano v0.8 — ~25 MB, 15M params, 8 voices** (sherpa's docs suggest it as an Android system-TTS *replacement*).
  - **Kokoro-82M — 311–330 MB, Apache-2.0, RTF 2.77–3.19** → the best voice in the class and **~3× SLOWER than real time** on Pi-class hardware. It synthesises more slowly than it speaks, which on a mid-range phone means stutter, heat and a flat battery — offered second with a size warning, never the default.
  - **Licensing:** sherpa-onnx and Kokoro-82M are **Apache-2.0** (commercially clean). Piper's training codebase moved to **GPL-3.0** (`OHF-Voice/piper1-gpl`) but is **not embedded** — sherpa-onnx's Piper runtime is its own Apache-2.0 code, so Piper *voices* do not drag GPL into Curio.

**PHASE 1 — DONE IN THIS COMMIT (v463), no new dependency.** The voice is driven one SENTENCE at a time (`ReaderSentence`/`speechSentences`/`ReaderSentenceScanner` — a SCANNER because the name `ReaderSentenceSplit` was already taken by a `Regex` in this file, which cost a red build: **grep the NAME, not the declaration shape, before adding a top-level name to `BookReaderScreen.kt`**) instead of four paragraphs; that same sentence index draws the read-along wash (`spokenRange`, accent 0.18) and dims the rest of the page (`READ_ALOUD_DIM` 0.38, only while actually running); the session gained a PAUSED state (`voiceOn`/`voicePaused`) so a pause keeps the mark and hands the page its contrast back; the one-glyph pill became a five-control bar (`ReaderSpeakBar`) with chapter up/down, sentence back/on, and the state word. Also fixed on the way: **a PDF read aloud used to re-read its last page for ever** (the page callback left the cursor at `pageCount`, which the next pass's `coerceIn` pulled back to the final page).

**PHASE 2 — SHIPPED IN THIS COMMIT (v464), and the member CHOSE ITS SHAPE.** The research above had left two ways to give a better voice, and the member picked the second: **point the reader at any speech engine the phone already has.** Android's `TextToSpeech(Context, OnInitListener, String engine)` takes an engine PACKAGE, and Android lets any app supply speech by answering `android.intent.action.TTS_SERVICE` — so a better voice is a **better ENGINE the member installs themselves**, with **no vendored binary, no bundled runtime, no ~27 MB AAR and no model downloader inside Curio**. Built: `ReaderSpeaker.prepare(context, enginePackage)` (idempotent on the package, releases the old engine first), `ReaderSpeaker.engines(context)` (queries the TTS intent directly — `getEngines()`'s static/instance signature is ambiguous across API levels, and the intent query is the same door AOSP's own `TtsEngines` uses), `ReaderLook.speakEngine` (stored + in `rememberKey()`), the Settings **Engine** row with its own picker (clearing `speakVoice` on a change, because a voice name belongs to its engine), and a **`<queries>` entry for `TTS_SERVICE`** in the manifest — Android 11+ package visibility would otherwise list only the engine that already had it, which is the exact opposite of the feature.

**Fixed on the way (a real v440 bug):** the Voice row read `voices()` on the line after `prepare()`, which is ALWAYS too early because binding a speech engine is asynchronous — so the picker said "No voices are installed on this phone yet" on its first open and only filled on the second. `prepare` now takes an `onReady` callback (immediate when the engine is already up, otherwise from the binding callback on the main thread, dropped when it fails) and the voice row uses it.

**NOT TAKEN: the vendored neural packs (Piper/Kokoro through sherpa-onnx).** Still the highest-quality option and still available, but it means committing a **~27 MB native AAR** with no official Maven coordinate, and **this environment cannot build or validate it**. Revisit only as its own CI-proven change.

**v465b — THE ISBN SCANNER COMES BACK, AND ONLY IN THE FULL EDITION.** The member's line for the split was explicit that the advanced edition takes it back (*"we can add the isbn sanner into the advance build as we dont have to worry about the size"*), so this is a **restore of the v458 file**, not a rewrite: `git show f4bafcdb^:…/IsbnScannerScreen.kt` recovered whole into **`app/src/full/java/com/curio/app/features/personal/IsbnScannerScreen.kt`**, with a **no-op twin of the same signature at `app/src/core/…`** — the same source-set-twin shape Vosk already uses, and for the same reason: the scanner imports `androidx.camera.*` / `com.google.mlkit.*` and `main` is compiled for BOTH editions. Five dependencies return as **`fullImplementation`** (`mlkit-barcode-scanning` + the four `androidx.camera.*`), and **`kotlinx-coroutines-play-services` does NOT** — the scanner drives ML Kit through `addOnSuccessListener`/`addOnCompleteListener`, and a grep of the recovered file for `.await()` found none, so the v458 commit's "the module's only caller of `Task.await`" was the dead import and never a call. Three dead imports also left with it (`tasks.await`, `android.content.Context`, `android.net.Uri` — all three grepped, none referenced). **The camera permission moved into `app/src/full/AndroidManifest.xml`**, which is the point of doing it as a flavor manifest: the core edition now declares NO camera at all, so its Play listing has no camera line and its APK carries no lens stack, while the door in the add-a-book sheet is gated on **`BuildConfig.EDITION_ISBN_SCANNER`** (a new per-flavor field) — the flag exists because the seam always exists and the core edition's is a no-op, so a door drawn without the flag would open a black rectangle. **The changelog's `REMOVE: the ISBN barcode scanner is gone` line was deleted rather than left to contradict the app** — and the check that settled it is worth keeping: **v1.4.0's versionCode is also 20260923** (`git show v1.4.0:app/build.gradle.kts`), so `20260923.txt` is the SHIPPED v1.4.0 file, its sibling `20260922.txt` already carries the removal in its own version's notes, and a line that a later commit reverses inside the same file is a line that lies. **That one is worth the member's eye: it is the only content removed from a shipped changelog in this change.**

**THE EDITION SPLIT WENT RED ON A TASK NAME, AND THE FIX IS ONE LINE IN EACH WORKFLOW.** The v465 commit (`3299d5a5`) failed the run in **48 seconds** with `Task 'lintRelease' is ambiguous in root project 'Curio' and its subprojects. Candidates are: 'lintAnalyzeCoreRelease', …, 'lintFullRelease', …`. **The `edition` flavor dimension makes the bare `assembleRelease` / `lintRelease` names AMBIGUOUS, and Gradle rejects the whole invocation at TASK SELECTION — before a single line of Kotlin is compiled.** That is why the failure was so hard to read: the compile step exits ~50s in, so there are no `e:`/`w:` lines for the annotation surface to lift, the APK step is skipped, and the only visible clue is the gate step's one-line "The Gradle build failed" — the log's `What went wrong` block is the entire diagnosis, and the second candidate list (`assembleRelease`) never even gets printed because Gradle stops at the first ambiguous task. Replaced with the explicit per-edition set — `lintCoreRelease lintFullRelease validateTopics assembleCoreRelease assembleFullRelease` in `android.yml` (`-PcurioAbiSplits=false` kept), `validateTopics assembleCoreRelease assembleFullRelease` in `release.yml` — and the pairing is deliberate: each `lint<Flavor>Release` runs first so the `assemble<Flavor>Release` beside it reuses that variant's compilation, which was the v412 optimisation's whole point. **The APK paths, the `*/release/*.apk` globs, the per-edition rename and `UpdateChecker`'s token match were already correct** — the only fault was the task names. Docs corrected to match: `app/AGENTS.md` (the v465 entry claimed `assembleRelease`/`lintRelease` cover every flavor — they no longer do, and that claim is exactly what would lead the next agent back into this), `.github/AGENTS.md` (both the `verify` and `release.yml` descriptions), `docs/CONTRIBUTING.md` (which told contributors to run `assembleRelease`), and two stale comments in `app/build.gradle.kts`. **The rule for the next agent: never write an unqualified `assemble*`/`lint*` task once a flavor dimension exists.**

**PHASE 3 — NOT BUILT: the Edge TTS Dev-page experiment** (free, keyless, unsanctioned).

**ALSO DONE IN THIS COMMIT — the root cleanup the member asked for.** `design.md` and `RELEASE_NOTES.md` moved into `docs/` (both workflows' literal paths and `.github/AGENTS.md` updated; `RELEASE_NOTES.md`'s "file is missing" branch is SILENT, so that path is load-bearing and moving it again means editing them). **Four files deliberately STAYED at the root**, and this is not laziness: `AGENTS.md`, `master.md` and the child `AGENTS.md` files are the DOX rail — agents discover an `AGENTS.md` by walking the path, so one inside `docs/` governs nothing; `Prompt.md` is the log the root `AGENTS.md` check-after-every-push contract points at; and `README.md`/`LICENSE` are the two standard root files.

---

## 0 (previous). §57 — the dictionary page's blank foot and its alphabet (DONE, v462 — pushed)

**What it was:** two separate causes, and the v456 pass had fixed only the first. That one was the INSET — the nav bar's height was taken by a zero-width spacer drawn over the page, so the scroll ran under an invisible strip and a fling ended in a band of nothing; it is a real `navigationBarsPadding()` on the scroll's own column. **This one was the list:** the browsable words are a `LazyColumn` with a FIXED `height(340.dp)` inside the page's own `verticalScroll` Column — a letter with forty words reserves three hundred pixels of empty paper under them, with no content and no edge. That is the blank area.

**What changed (v462, `ReaderDictionaryPage` only):** the window is a CEILING (`heightIn(max = DictionaryWordsWindow)`) so a small letter is as tall as its own words and a big one still scrolls inside the page; the alphabet left the page's scroll and became a **floating pill at the page's foot** (the reader's own language — paper colour, soft shadow, round chips inside one capsule, the letter you are on wearing the accent), stepping aside for an open search and an open word sheet; the current letter moved into the section label ("THE DICTIONARY · OPTED · B"); and a `DictionaryFootClearance` spacer keeps the last word above the floating pill. **The reason the rail had to move rather than be restyled:** a horizontal scroller nested inside a vertical one is a gesture conflict (a sideways flick over the letters can carry the page), and a rail that scrolls away is an alphabet you cannot reach while reading an entry.

**Also fixed on the way:** the word sheet's own doc block had been split from its function by the new composable (a Kotlin doc binds to the next DECLARATION) — the block now says whose it is and the sheet carries a pointer back to it.

---

## 0 (previous). §56 — the TMDB posters, the drawer sky's glow, the version bump (1.4.0), and a batch still to be scoped

> no need can u fix the tmdb api please, i need it to work on the app for the movies and incursion ui posters to work, coz the posters its fetching rn is bad. and not accurate, also the drawer costellation i can see the edges in dark mode, and also in light mode its not visible. and in collection sthe open and close is really clanky and weird looking. lets update the intro as well, and also ability to turn off journal shelf etc, in collections use 3 grid for books etc, add online in intro, with log in in that, and exlaing you can share your thoughts, also a way for user to open the added book directly without going through the book detail, how about when added a pdf in book, user can pin it in home screen shelf door and it shows with the small in icon. also lease the way our app does select all rows, why cant i do the same with select all with android it still sometimes does only 1 row oy or sometimes misses some rows, fix it. also bum version code and number both, and udate the release notes. also maybe simplifying settings, like yk some are really confusing to find

**Done this round (committed and pushed):**

1. **THE POSTERS ARE TMDB'S.** Two faults, neither in the key wiring (both secrets are set and exported by both workflows): (a) **TMDB was the LAST door** — `FilmPosterFetch` was `viaKeyless ?: TmdbFetch.posterUrl(...)`, `SeriesPosterFetch` never asked it at all, and `IncursionPoster` raced it (a race is decided by speed, and a one-request TVMaze/Wikipedia answer beats a search-plus-detail TMDB read), so the plate filled with an iTunes square or a Wikipedia lead image. With a credential present the keyed door now goes **first**, alone, on a 4s lead, with the free doors as the fallback; **keyless builds are byte-for-byte unchanged**. (b) **THE YEAR WAS STRIPPED BEFORE THE SEARCH** — `facts()` cleaned the topic name (removing `(2005)`) *before* calling `movieFacts`, so `bestHit`'s year scoring never saw a year and TMDB answered with whatever ranked first; the year is now read first (`yearIn`) and asked for **at the API** (`&year=` / `&first_air_date_year=`), with one unfiltered retry when the stated year is wrong. `clean` stays the cache key and the query text.
2. **THE SKY'S GLOW.** *"I can see the edges in dark mode … in light mode its not visible"* — both halves of the v457 wash: four stops is a piecewise-linear ramp (the eye draws a line wherever it changes slope, which a dark page makes plain) and one mix strength served both themes (22% of the accent on near-white is a slightly different white). One helper now samples an eased curve at fine steps (`glowStops`) for **both** the wash and each star's aura, and the strength is the theme's own — **more** of it on the light page, which is the opposite of how a shadow behaves.
3. **VERSION `20260922` → `20260923`, `1.3.0` → `1.4.0`**, with the new release's notes as a new file (`changelogs/20260923.txt`, per the versionCode contract) — `20260922.txt` is the version that shipped.

**Still open from the same message. THE MEMBER'S OWN SCOPE CALLS ARE IN — do not re-ask them:**

| item | the member's answer |
| --- | --- |
| *"in collection the open and close is really clanky"* + *"use 3 grid for books etc"* | **Collections = the Book shelf + the journals.** (Note the shelf is **already** `GridCells.Fixed(3)` — `BookShelfScreen.kt:189`, with `reading` and `finished` as its two `items` groups — so the 3-up work is the **journals list**, still a `LazyColumn` (`JournalListScreen.kt:437/444`) — and the "open and close" is the shelf/journal **open-close motion**, not the grid itself.) |
| *"select all … sometimes does only 1 row or misses some rows"* | **the writing editor** (the journal page, i.e. `PersonalCanvas`), not the Cabinet/shelf batch select. |
| *Simplifying Settings* | **re-cut, move rarely-used rows into one "Advanced" page, AND remove what's concluded.** (The verified inventory of dead rows, flags nothing can write, 34 dead strings and 2 dead colours is already recorded in the v458/v459 notes and §0.6 above.) |
| *"ability to turn off journal shelf etc"* | **each row its own switch** — one for Home's **Pages** row, one for **My shelf**, not one switch for both. |

**The Select-all bug, with the diagnosis already in hand (do NOT re-derive it):** the journal page is a single composed surface that installs its own `TextToolbar` (`PersonalCanvas.kt:3400–3469`) precisely so it can re-point Select all — `onSelectAllRequested` branches on **`state.pageIsOneField()`**: a one-field page keeps the platform's native select-all (the whole entry really is that field), and a multi-field page calls **`state.selectPage()`** (the page wash). So the two failure shapes the member describes map onto the two branches — *"only 1 row"* is either `pageIsOneField()` answering **true** for a page that is really several fields (the native select-all then takes only the focused field) **or** a Select all that arrives **without** the toolbar at all (Ctrl+A on a hardware keyboard, an IME's own select-all, the paste-menu), which never reaches `showMenu` and therefore keeps the native row behaviour; *"misses some rows"* is `selectPage()`'s own coverage. The pieces to read before editing: `pageIsOneField()`, `selectPage()`, `selectWholePage()` (`:1944`, the copy bar's own All rows — reached from `:5729` and the `CopyChip("Select all", …)` at `:5935`), and the page's own `BasicTextField`s (`:4230`, `:4876`). The rule to land: **one meaning for Select all on a page — the whole page — whichever door it came through**, with the app's own bar and Android's action agreeing.

**Landed in the v461 pass ("doo all of them in one pass"):**

- **Select all, from every door** — the catch is in `PersonalCanvas.onFieldChange`: a whole-row selection arriving from a CARET (which no drag can produce) on a multi-field page is the platform's Select all and becomes `selectPage()`. The toolbar override and the copy bar were already right; this closes the doors that never reach them (an IME's own, the paste menu's).
- **Home's two rows, each its own switch** — `AppPreferences.homePagesRowState` / `homeShelfRowState` (both default on, seeded by `initHomeRows` from the same three places `initThemeMode` is), read directly by `PersonalChipsRow` and by the two switches in Settings → Preferences. Hiding a row hides the row, never the data.
- **The intro's online step** (`OnlineSlide`) — sits before setup, one Sign in door that finishes the intro first and opens the account page over Home, and says what sharing your thoughts means.

**Also landed in the same pass ("do the rest plis"):**

- **A book you can open opens** — the shelf card's tap asks `BookFiles.documentOf(...)` and goes to the reader when a document is attached, to the detail page when it is not (which is where a file gets attached).
- **A held book has actions** — Open in reader / Pin to Home / Remove, instead of one destructive question; the pin is ONE id (`pinned_book_id`), sorts the pinned book to the front of Home's shelf row, and wears the same corner disc on the chip and the shelf card.
- **The journals are a 3-up grid** (`JournalGridCell`), matching the shelf; month heads keep the full span.
- **Settings is re-cut with an Advanced page** — Recording, Experiments and the Pet designer moved there (routes, page, content and the nav host all wired), so the hub shows the rows members actually change.

**Deliberately NOT done, with the reason on the record:** (1) **removing the concluded experiment flags** — nine preferences the UI still reads but nothing can write; each read site has to be walked before deleting, and a settings row removed on a guess is a feature someone loses quietly; (2) **a nav-transition change for the shelf/journals** — both routes already inherit the app's own transition, and the reachable candidates left for *"the open and close is clanky"* (the filter panel's `pillArrive`/`pillLeave`, the create launcher's single-progress scale) are already on `CurioMotion`, so a change there would be a guess rather than a fix — worth one sentence from the member about which part of the open/close feels wrong.

---

## 0 (previous). §53–§55 — the scanner and its camera permission, a sweep of everything else that earns nothing, then dictation (fixed and live) plus a redundancy audit

> remove the isbn scanner feature along with its camera ermission,

**What it was:** the shelf's add-a-book sheet had three doors — *Search* (title and author against the catalogues), *Scan ISBN* (a full-screen camera preview with barcode detection, then a keyless Open Library look-up by ISBN that saved the book), and *Type* (title, author, chapters by hand). The scan door is the one that asks for the camera.

**Asked?** No, and none was needed: the request names both the feature and its permission in its own words, which is the confirmation the house rule wants. What was checked *before* the removal is that nothing else in the app earns that permission — every other image door (an avatar, a pet, a note's photo, a gallery wall) hands the job to the system picker, which needs nothing declared here.

**Files:** **deleted** `features/personal/IsbnScannerScreen.kt` (the scanner sheet, its ISBN normalisation, its Open Library fetch and `saveScannedBook`); `features/personal/BookShelfScreen.kt` (the door and its state); `AndroidManifest.xml` (`CAMERA` + the `android.hardware.camera` feature); `res/xml/file_paths.xml` (`isbn_camera`); `app/build.gradle.kts` + `gradle/libs.versions.toml` (the six dependencies only it used).

### 0.6 §55 — the current batch (dictation done and pushed; the rest open)

> suggest me things to remove from settings that are unncessary, also reworking on some of them again which are left behind also i saw a bug when i turn on voive to text and try to use it in save your take the dialog opens and then it closes again because of keyabord, fix it and also mak eit tye the words live in the note, also find ways to decrease more gpu and lag for the aer style. lowe rendering or maybe pre rednering maybe also for liquid glass . also is it possible for you to remove some of the contributers, see if i m not violating any policies or anything and ive clearly mentioned everything. also list all the used ai in readme.
>
> nvm, do a redundacy audit chekc, also did the mt kit or something removed too right along with camera isbn? and commit and push all.

**Answers gathered before touching anything:** *"the aer style"* is the **paper style**; the AI list is **Codebuff (Buffy) + Freebuff Agent** (v0 authored 156 commits and was deliberately left out); the credits change is **AI/bot identities only**; and the settings-removal groups came back empty twice and were then dropped by the member (*"nvm"*) — the verified inventory stays recorded above (and in the v458/v459 notes of `app/AGENTS.md`) for whenever it is wanted.

1. **THE DICTATION BUG IS FIXED, AND THE WORDS NOW TYPE THEMSELVES IN.** The dialog was never the problem: the mic rode the field's tool dock, and in DOCK mode the dock exists only while the field is focused with the keyboard up (v389/v391). The dialog took the focus, the field blurred, the dock folded, and the mic — which owned `open` — went with it. The FIELD hosts the session now (`DictationHost` + `LocalDictationHost`), so the dock stays composed while a session is live, and every partial is typed into the note as it is said (`base + "\n" + transcript`). Cancel restores exactly what the note held; the door is Done, not Insert. Eight boxes, one implementation, **no call-site changes**.
2. **ML KIT WENT WITH THE SCANNER** (the member's question): `mlkit-barcode-scanning`, the four `androidx.camera` libraries and `kotlinx-coroutines-play-services` were all removed in `f4bafcdb` along with the screen, the permission, the feature and the file-paths entry — `grep -i "mlkit\|barcode\|camera"` over the catalog, the module and all source now finds nothing.
3. **STILL OPEN IN THIS BATCH:** the redundancy audit, the paper-style + liquid-glass GPU pass (paper cards draw every ruled/torn line as its own `drawLine`, per card, per frame — the fix is to compute that geometry once and blit it), and the README's AI list + credits wording.

### 0.1 What was done

1. **THE SCANNER IS DELETED WHOLE, AND THE SHEET HAS TWO DOORS.** The CameraX `PreviewView`/`ImageAnalysis` pipeline, the ML Kit barcode model, the `EAN_13`/`EAN_8`/`UPC_A` normalisation, the Open Library look-up and the door that opened it are all gone; **Search and Type** are what remains of the three.
2. **THE CAMERA PERMISSION GOES WITH ITS FEATURE.** `android.permission.CAMERA` and the `android.hardware.camera` feature both leave the manifest, and the `isbn_camera` cache-path leaves `file_paths.xml`. Nothing else in the app opens a camera of its own — verified first, not assumed.
3. **AND ITS DEPENDENCIES.** `com.google.mlkit:barcode-scanning`, the four `androidx.camera` libraries (`core`, `camera2`, `lifecycle`, `view`) and `kotlinx-coroutines-play-services` (whose `Task.await` the scanner's ML Kit call was the module's only caller of) leave the catalog and the module's block, with their three version entries.
4. **NOTHING IS LEFT ON A PHONE, AND NO MIGRATION IS NEEDED.** The scanner never wrote the `isbn/` directory it declared — detection ran on the in-memory `ImageProxy` — so there is no directory to purge, and a scanned book was an ordinary `personal_books` row, so the schema is untouched.

### 0.2 What was NOT removed

- **The ISBN itself** stays everywhere a lens was never needed: `BookCoverFetch`'s keyless Google Books search still resolves a book's ISBN for the LibraryThing/Open Library cover row, and the source lab still probes those URLs. Only the camera path to an ISBN is gone.
- **`CurioIcons.Screenshot`** ("photo_camera") stays — it is a glyph in the category-icon pool, not the scanner's.

### 0.4 The red build that was waiting on the last round (fixed before answering)

The single quick `gh run list` before this push found the run for §52's second commit (`b80785de`) **failed**. It was §52's own defect, and it was one line of placement: a `@Composable` sat between `PersonalChipsRow`'s KDoc and the snapshot object's KDoc, so it **bound to `internal object PersonalShelfSnapshot`** (*"This annotation is not applicable to target 'standalone object'"*) and left the row **unmarked** — which cascaded into eight *"@Composable invocations can only happen from the context of a @Composable function"* errors under it, the first at the `backdrop` default, because a default expression in a `@Composable` function is a composable context. The object keeps the v457 doc, the row gets its own doc and its annotation back, and the same shape (**an annotation whose next line is a KDoc**) was then searched for across the tree: none left. The scanner removal went out first (`f4bafcdb`) and this fix carries the previous round's features to a green build.

### 0.5 The sweep (§54) — what else earns nothing

> Sweep the app for any other permission or dependency nothing earns, the way the camera one just went

**Asked before removing anything** (the house rule), with the findings already researched rather than guessed: the member took the all-files permission, androidx Palette and the stillborn test scaffolding, then — asked again about the Compose tooling pair, which turned up while I was checking the edits — took that too. They **declined** pruning the 36 unused catalog aliases.

5. **ALL-FILES ACCESS WENT, WITH ITS STALE COMMENT AND ITS NAMESPACE.** `MANAGE_EXTERNAL_STORAGE`'s only user was the **Glass Widget Lab's wallpaper auto-detect**, and `4e6d184c` (Sep 8, "remove concluded experiments…") deleted that lab — 766 lines of screen — while **leaving the declaration behind**, where it sat for two weeks as the most review-sensitive permission the manifest carried (All-files access is a store *policy* declaration, not just a runtime prompt). Nothing earns it: no `Settings`/`AppOps` all-files check exists, `WallpaperManager` appears nowhere in the tree, and the surviving glass widget paints its own Canvas art. `xmlns:tools` went with it, because `tools:ignore="ScopedStorage"` was its only use. **A permission is referenced only by the code that REQUESTS it, so a deleted feature leaves a declaration that no usage-grep can ever flag — a permission sweep has to run manifest → code, never the other way.**
6. **THREE DEPENDENCIES THAT BACKED NOTHING.** `androidx.palette:palette-ktx` (its v338 comment described a swatch feature that is gone; the "palette" names in the tree are the app's own `CurioPalette`/`ShareCardPalette`), the **Compose tooling pair** (`ui-tooling-preview` was an `implementation`, so it was SHIPPING in the release APK, plus debug-only `ui-tooling` — this module has no `@Preview` and no `androidx.compose.ui.tooling` import), and the **test scaffolding** (`testImplementation(junit)` + the debug UI-test manifest: `app/src/test` is empty, there is no `androidTest` set, and CI runs `lintRelease validateTopics assembleRelease` — no test task). **All stay in the catalog**, so re-adding any of them is one line.
7. **AND THE CENSUS OF WHAT EARNS ITS PLACE — checked, not assumed.** RECORD_AUDIO (sound bites, the dictation mic, voice notes), POST_NOTIFICATIONS, INTERNET, REQUEST_INSTALL_PACKAGES (the updater installs the downloaded APK), RECEIVE_BOOT_COMPLETED (two boot receivers re-arm the alarms), SYSTEM_ALERT_WINDOW (the pet overlay and the explore bubble), FOREGROUND_SERVICE + FOREGROUND_SERVICE_SPECIAL_USE (the two `specialUse` services), all three FileProvider paths (`share/`, `downloads/`, `exports/`), the http/https `<queries>` (the browser doors) and `profileinstaller` (it is what installs `baseline-prof.txt`). The 36 unused catalog aliases are build-time only and were left alone on purpose.
8. **A REMOVAL EDIT ATE A NEWLINE, AND THE SEAMS CAUGHT IT.** The replacement that took the three test lines out left its `newString` without a trailing newline, so `implementation(libs.com.alphacephei.vosk.android)` was pulled into the Vosk comment above it: **the dependency was commented out** and the next build would have failed on unresolved `org.vosk` symbols — with the text still on the page, so no grep for "vosk" would ever have noticed. Reading the cut's seams found it before the commit; **root `AGENTS.md` now carries compile-safety rule 12, "RE-READ THE SEAMS AFTER A REMOVAL".**

### 0.3 Still open from the previous request (§52)

- The star map's glow, twinkle and arrival are CI-green, but **the glow itself is the member's to walk on a device**.
- **A legacy highlight's first re-draw** still searches for its words (it has no offsets); the first re-highlight of those same words upgrades the mark in place.
- **The remaining `produceState(initialValue = emptyList())` reads** (`ChapterScreen`/`BookReviewScreen` notes, `BookDetailScreen`'s marks, the cover hub and browser catalogs): the same unload-then-fill shape the member named, on surfaces they did not.

---

## 0z. §52 — a dictionary with two doors, highlights that stay where they were drawn, the topic browser's place, the star map's glow, and the zoom-locked scroll (DONE, v457 — pushed)

> some animations still feel clanky without the lite mode, and mak ethe star pattern animation more better and also more noticable shade of it, with a sligh glowish backgroud, not a sloid box but a slight glowish rounded or side rounded bacgroud, then the dictionary page is bad buttonm area is covered with something, fix it please. , the words view is weird with weird words, also remove the webster's 1913 one keep the full one,and a glitch discoevred for highlight when selecting text it perfectly selects and shows the selected textts and when hihglighted the hihglight goes to a totally differnt line or text but in highlight it shows correctly the one i hihglighted but the vie of highlight is at a wrong text or line, fix this weird behavrior,
>
> now lets fix the weird scrolling when zoom locked so the scroll isnt like scrolling but it lets me drag to side too, weird behavrior, and also amany ui elements haev loading unloading behaviors unncecessarily.

**Asked before touching anything, and the answers shape the work:** the clanky motion is **all of it** (screen transitions, the drawer and its star map, the sheets, lists and scrolling) plus a new report — *"in topic browser the scrolled position isnt rememebered anymore … when i opne a toic and exit it the scroll goes back to the to, why even reload the page when i open something kee it loaded"*; the star map's background is a **radial glow from the centre** (not the rounded panel, not the side rails); the dictionary page's covered foot is **"a blank strip in a different colour at the very bottom"**; the offline door to remove is the **abridged Webster's 1913 (9 MB)**; and the words list should browse **WordNet (modern words)** — clean, not replaced.

**Files:** `data/PersonalEntity.kt` + `data/CurioDatabase.kt` (the highlight offsets and `MIGRATION_23_24`); `features/personal/BookReaderScreen.kt` (the offsets saved and drawn, the sheet's layer move, the volumes list, the purge); `features/personal/ReaderOfflineDictionary.kt` (one door fewer, WordNet's words as English); `features/personal/ReaderDictionaryPage.kt` (the volumes list, the purge); `navigation/CurioNavHost.kt` (the page's full-bleed foot); `features/database/TopicDatabaseScreen.kt` (the restore race); `features/home/HomeScreen.kt` (the star map).

### Record — what was built

1. **A HIGHLIGHT REMEMBERS WHERE IT WAS DRAWN.** The root cause of *"the highlight goes to a totally different line or text … but in highlight it shows correctly the one i highlighted"* is in the mark itself: `ReaderMarkEntity` stored the WORDS and nothing else, so drawing one back meant `block.text.indexOf(passage.text)` — the FIRST occurrence — and a phrase a paragraph carries twice washed the earlier run. The entity gains **`startIndex`/`endIndex`** (the swept character offsets, `-1` = unknown), `MIGRATION_23_24` backfills every existing row to `-1`, the sweep saves its own `from`/`to`, and both renderers (the reflowable block and the PDF glyph layer) use the stored run when the words it points at are still the mark's words, falling back to the old search for a legacy row. Two runs of the same words are now two marks: the identity check compares the run, and a legacy row still matches on its words so the member's first re-highlight upgrades it in place instead of stacking a second wash.
2. **THE DICTIONARY'S DOORS ARE TWO.** The abridged `WEBSTER` volume is removed (it was the same public-domain 1913 text as `FULL` in a lighter conversion — two rows that read as one dictionary twice, the smaller a strict subset), both door lists carry `MODERN · FULL`, and **`purgeRetired`** deletes a phone's copy of the retired file from the two surfaces that read the volumes, so 9MB of unsearchable dictionary comes back rather than sitting there forever.
3. **AND ITS WORDS ARE WORDS.** *"The words view is weird with weird words"* — WordNet's lemmas are database keys (`alarm_clock`) and its index also holds symbol strings. The index is now written with the underscores turned into spaces (so a lookup and a walk read the same English, and `define` accepts a space as part of a headword), and the browsable list passes every head through `isWordShaped` — letters, an internal space, hyphen and apostrophe, starting and ending with a letter — which is applied to the LIST only, never to the lookup, so nothing the member already knows can be lost to the filter.
4. **THE DICTIONARY PAGE'S FOOT IS THE READER'S PAPER.** The route is a push screen, so the NavHost's own `navigationBarsPadding` stopped the page's paper above the gesture bar and left a strip of the APP's background under it — the member's *"blank strip in a different colour"*. It cannot join the full-bleed **prefix** set (its prefix `reader` is shared with the reader's settings page), so `fullBleedBottomRoutes` names it by its exact route, and the page's own `navigationBarsPadding` keeps its content above the bar.
5. **THE TOPIC BROWSER KEEPS ITS PLACE.** The restore was right and the SAVER was racing it: `snapshotFlow { firstVisibleItemIndex }` emits the current value the instant it is collected, and that collector was launched in the same frame as the restore — so on a return trip it wrote index 0 into `savedScrollIndex` and into `TopicBrowserSession` a beat before `scrollToItem` ran, and the "restored" spot WAS 0. A `settled` flag now makes the restore land first and the saver ignore every emission until it does, and the two reset effects (a new category, a new page) carry a seen-value guard so the entry frame — which is not a user action — no longer wipes the restore either.
6. **THE STAR MAP STANDS IN A SOFT LIGHT.** One radial gradient behind the sky (a warm whisper of the brand colour at the hub, out to the page's own colour — no plate, no edge, no corner), its stars and joins a notch deeper, and the bloom rebuilt as **one cached radial-gradient brush per star** instead of four concentric circles repainted every frame: a quarter of the draw ops, a real falloff instead of stepped rings, and the twinkle and the arrival reduced to a translate+scale over a brush that never changes. Each star also comes OUT OF THE HUB as it lights (a tenth of its distance inward, sliding to its place while it grows) and gathers back the same way as the drawer closes.
7. **THE READER'S SHEETS MOVE AS ONE LAYER.** The panel's `offset { }` is read in the LAYOUT phase, so every frame of a drag, an arrival or a departure re-laid-out the whole sheet and re-rendered its 16dp shadow with it; `graphicsLayer { translationY }` moves the same pixels with no relayout.

### Record — the second message in the same round (done)

> now lets fix the weird scrolling when zoom locked so the scroll isnt like scrolling but it lets me drag to side too, weird behavrior, and also amany ui elements haev loading unloading behaviors unncecessarily.

**Asked which elements load and unload**, the member answered **"Screens that re-read everything when you come back, the books and journals in home screen"** — so both halves have a surface, not a guess.

8. **A LOCKED PAGE SCROLLS LIKE A PAGE.** `thawed` was recomputed on EVERY event from the gesture's accumulated travel, and both halves of the member's report were that one line: a drag whose first pixels went a hair sideways was swallowed whole (and consuming is what cancels the scrolling column's own slop wait, so the page could not scroll at all — *"the scroll isnt like scrolling"*), while a drag that began vertically thawed the lock and let the sideways move it went on to make turn the page (*"it lets me drag to side too"*). The axis is settled **once**, on the event the finger crosses the touch slop, and `lockVerdict` holds it until the lift; a pinch and a sweep in flight still never thaw, and the v448 rule (only a Wide or magnified page can thaw at all) is unchanged.
9. **THE UNLOADS ARE GONE FROM THE WRITING SURFACES.** The dictionary page's word list was REPLACED by a "Reading the “B” pages…” line on every letter tap — a whole panel unmounting and remounting for a local file read; the list is one object through a letter change now and only its items change (the quiet line moved above it and is the only text state left). And the member's own two: the journals/books rows (and the journals list, and the shelf) read through `produceState(initialValue = emptyList())`, so every return to Home composed an EMPTY row and filled it a frame later — `PersonalShelfSnapshot` (one process-scoped snapshot for all three, since they read the same two flows) is the first frame now and the flow only corrects it; `HomeFeedSnapshot` does the same for Home's recents feed and the drawer's own knowledge map, which used to come up dark and light a beat later. A **failed** read keeps what is on screen instead of blanking it.

### Record — what was still open from that request

- **The star map on a device** — CI compiles the map, it cannot see the glow; the arrival, the twinkle and the halo's falloff are the member's to walk.
- **A legacy highlight's first re-draw** still searches for its words (it has no offsets); the first time the member highlights those same words again the mark is upgraded in place.
- **The remaining `produceState(initialValue = emptyList())` reads** (`ChapterScreen`/`BookReviewScreen` notes, `BookDetailScreen`'s marks, `BookCoverHubScreen`/`BookBrowserScreen`'s catalogs): the same unload-then-fill shape, on surfaces the member did not name. The two the member named are fixed; these are candidates, not findings.

---

## 0z. §51 — the keyless film door, a word's meanings in a sheet, the Share hub on the Dev page, and Felicity credited

> in dictionary, the buttom area seem sto have some glitch, also the badge to switch is kinda weird.. make the share hub hide from settings and its only
> accessible from the dev settings. akso from dictionary dont show the provider removing in dictionary age, and fix the look and open the meanings in
> buttom sheet. then a full remvam of the invcursion ui, and still the movies doesnt load, without tmdb or obdmdb key, do something about it please.
> also is the imlemetaion of previous one is fully done? push the revious one
>
> also add felicity redits in readme and a info for the help

**Asked before touching anything, because the request both removed and fixed the same area:** the Incursion question (*"a full removal"*) and which movie
surface fails. The answers: **"not removal full redesigning"** — so the Incursion UI is a REDESIGN, not a deletion, and nothing of it is removed — and
**"All of them"** for the movies, so every film surface has to work with no key. **The previous CL was pushed first, as asked** (`ac806765`, the run before
it green). **Is the previous one fully done?** Phases 1 and 2 of the motion work are shipped and green; phase 3 shipped the panel clock with Material's own
~140 `ModalBottomSheet`s gated on Material3 1.5 (recorded in `MOTION_PLAN.md` §4 and marked in `CurioTheme`); phase 4's arrivals rule is live on Home's
recents, the Cabinet grid and the journals list.

**Files:** **new** `features/reveal/WikidataFilmFetch.kt`; `features/incursion/IncursionSources.kt`; `features/reveal/TopicRevealScreen.kt`;
`features/personal/ReaderDictionaryPage.kt`; `features/personal/BookReaderScreen.kt` (`ReaderSheetFrame` → `internal`);
`features/settings/SettingsHubScreen.kt` + `ExperimentsScreen.kt`; `features/support/SupportScreen.kt`; `README.md`; `app/AGENTS.md`; this file.

### 0.1 What was built

1. **A FILM'S FACTS EXIST WITH NO KEY.** `WikidataFilmFetch` = Wikipedia decides WHICH work (the bracket rule), Wikidata states the facts (`P2047`
   runtime, `P136` genres, `P57` director, `P161` cast, `P444` score, `P577` year, `P31` film-vs-show, `P18` as the image fallback), with ONE batched
   `wbgetentities` read for every referenced label, a 9s whole-record budget, and answers AND misses memoised per title+year. Wired: `IncursionSources`'
   keyless stage (in FRONT of the article door — it is the only keyless door with facts as well as prose), its artwork race, the reveal's film card
   (`factLine` under the poster) and the film sheet (the fact line in its meta row, *Directed by* + the cast, and the article's prose as the about-text
   only when the topic has no synopsis or teaser). A runtime stated in seconds (`Q11574`) is divided; every early exit is a branch, never a non-local return.
2. **A WORD'S MEANINGS ARE A SHEET.** `DictionaryWordSheet` on the reader's own `ReaderSheetFrame` (now `internal`) — the same paper, drag, flick,
   keyboard inset and motion clock as every other reader sheet. The **version switch moved into it**, labelled "SHOWING" (the member's *"the badge to
   switch is kinda weird"*: it stood directly under the door badges and read as the same furniture twice), and it is shown only when more than one volume
   is on the phone. `askSeq` is the new page state that lets a word be asked for TWICE — closing the sheet clears `sheetWord`, and `word` alone cannot say
   "again".
3. **THE PAGE'S FOOT IS A REAL INSET, AND IT NO LONGER REMOVES A DICTIONARY.** The nav bar's height was taken by a zero-width SPACER drawn OVER the
   page, so the scroll ran under an invisible strip (the member's *"the buttom area seems to have some glitch"*); it is `navigationBarsPadding()` on the
   scroll's own column now. The Remove control left the page (a search surface is the wrong place for a destructive one-tap) and the volume's fact line
   stayed; removing still lives in the reader's dictionary sheet.
4. **THE SHARE HUB IS A DEV DOOR.** Both Settings entries are gone — the "Personalize" row AND the settings nav rail's `share` entry, because a hidden page
   still offered by the rail is not hidden — and the Hub sits on the **Dev page** under a new "Sharing" heading.
5. **THE README'S OPEN-SOURCE LIST STOPPED BEING EMPTY.** It was two words and a tagline; it now names what the app ships — AndroidLiquidGlass, blobatar
   (with its MIT notice), TMDB (with the notice its terms require), the keyless doors around it, Supabase and the app's own foundations — with the same list
   already in **Support & diagnostics → About Curio**. A Felicity credit was added here first and then **removed again with the motion revert below**, so the
   list names only what is in the build today.

### 0.2 What the member changed their mind about, in the same round

- **THE INCURSION REDESIGN WAS ASKED FOR AND THEN DROPPED.** Asked which direction it should take (editorial / poster-forward / simpler structure) the member
  answered *"forget that revert the felicity smooth transition chnages. only that"* — so **no Incursion redesign was made** and nothing of that page was
  touched. **If it is ever picked up again, ask for the direction first:** `features/incursion/IncursionScreen.kt` is 2,451 lines (header, progress, filter
  row, list + grid, group headers, phase chips, entry rows/tiles, the detail sheet, the nav bar), and a guess there is a full cycle on the app's largest screen.
- **THE WHOLE FELICITY MOTION WORK WAS REVERTED, AND THE CREDIT WITH IT.** Asked exactly how far back, the member chose **everything from the motion work**, and
  **remove the credit too**. Out: the four screen transitions, the seeked back gesture, the `navigationCompose` 2.10.1 bump (back to **2.9.8**), the panel clock
  in the reader's sheets (back to `CurioMotion.ENTER_MS`/`EXIT_MS`), the arrivals on Home / the Cabinet grid / the journals, the Experiments switch and
  `AppPreferences.motionSystemState`, `ui/theme/CurioMotionSystem.kt` and `app/MOTION_PLAN.md` (both deleted), `CurioRevealNav.screenRevealActive`, and the
  README + Support credit rows. **The revert was done FILE BY FILE, never with `git revert`,** because the v455 commit also carried **Home's recents going
  tap-only** — a separate member request that had to survive, and which a whole-commit revert would have undone (it would restore the buggy hold). The files
  that only the motion work had touched were restored from `aed925a5` (the commit before it); `HomeScreen` (hold out, `curioItemIn` and `order` in),
  `ExperimentsScreen` (switch out, the Share-hub row in) and `BookReaderScreen` (pill clock back, `ReaderSheetFrame` stays `internal`) were hand-edited.
  `app/AGENTS.md`'s v455/v455b entries were replaced by one withdrawal entry that names what must not be re-reverted.

### 0.3 Still open

- **The Incursion redesign** — dropped at the member's request (see 0.2); start it only with a direction.
- **Phase 3's remaining half:** Material's own sheets stay on Material 3's clock until Material3 1.5 is stable (the scheme is designed in `MOTION_PLAN.md` §4,
  the one line it wants is marked in `CurioTheme`).
- **The audit's UNVERIFIED leads** (`app/APP_AUDIT.md`) — touch targets, icon-only content descriptions, `Surface(onClick)` pressed states, and whether any
  `Color.White` is left in a themed surface. Countable, each with the command to re-check it.

## 0z. §50 (finished — kept for reference) — the Felicity motion system, and Home's recents go tap-only

> the tap and hold is buggy in home screen recent topics, even after when im not holding and im releasing it continues the holding and its buggy,
> ykw remove the tap and hold action from home screen recents anthen do a full plan to improve aps transtions oening animations and evetything
> smooth https://github.com/firefly-sylestia/Felicity from this repo, make a full plan of it with the instructions and then check if the previous cl
> is failed fix it and push, and then fully start you rplan and make this a new option for smoother animation in experiments, and no old app
> animation will be used. start the implemetation and finish it and only then ask if you have something to ask and remeber full animation opening
> system is chnaging, beautiful trasntions etc.

**Asked first, as instructed** (*"did the grid theme selector done? answer this first with ask user"*): the grid theme picker is live — v453 made it a
**two-column grid inside the "Color theme" sheet** reachable from Appearance, each card drawing the theme it offers, the live one ringed in its accent,
with **no experiment switch** on it. The member's answer: *"It's there — I just hadn't opened the Color theme row"* — so nothing was changed there.

**The previous CL was checked first, as instructed:** the Lite-mode run (`35712432059`) finished **success** — nothing to fix.

**Files:** `features/home/HomeScreen.kt` (the hold out, the radial menu deleted, `curioItemIn` on the rows), **new** `ui/theme/CurioMotionSystem.kt`,
**new** `app/MOTION_PLAN.md`, `data/AppPreferences.kt` (`motionSystemState`), `navigation/CurioNavHost.kt` (the four transitions),
`navigation/CurioRevealNav.kt` (`screenRevealActive`), `features/settings/ExperimentsScreen.kt` + `UserExperimentsScreen.kt` (the switch).

### 0.1 What was built

1. **HOME'S RECENTS ARE TAP-ONLY.** The hold is *removed*, not re-tuned — a hold that outlives the finger is the radial picker arming from its own
   cancellation window, and no timeout tuning removes a race that lives below it. The three hold actions are the Recents page's own rows. Only one
   `CurioPatientHold` call site remains in HomeScreen.kt (the pet's bed).
2. **THE FELICITY PLAN IS A FILE** (`app/MOTION_PLAN.md`): provenance table (its file → our symbol), the numbers it actually uses, the four rules
   the port is held to, five phases, the deliberate deviations, what is NOT ported and why, and a verification checklist.
3. **THE MOTION SYSTEM SHIPPED AS PHASE 1** — `ui/theme/CurioMotionSystem.kt`: shared axis **X** (drift a quarter of the width + crossfade, 500ms),
   **Z** (scale + fade, mirrored on the way back), **F** (pure fade for peers and for routes whose shared element is the animation), the
   `1 − (1−t)⁶` settle curve, the linear `Track` curve, and `Modifier.curioItemIn` (Felicity's item ADD, draw-phase, staggered).
4. **THE NAVHOST CONSULTS NONE OF THE OLD DURATIONS WHEN IT IS ON** — each of the four transitions opens with the new system's branch. The
   shared-element routes (Reveal, Pet Designer, the settings family, tab switches) take the fade *on purpose*. The screen reveal **stands down**
   (`screenRevealActive`) because two screen-switching experiments together freeze a bitmap over a page that is also drifting.
5. **THE SWITCH IS AN EXPERIMENT, DEFAULT OFF** (Experiments → Motion → "Smoother transitions"), matching the house rule that a behaviour swap is
   opt-in and one tap from the old feel. The interaction layer (`CurioMotion`'s springs and pill clock) is deliberately NOT moved — see the plan.

### 0.2b Phases 2 and 3 (same request, immediately after)

**The red CL was fixed first:** the v455 run failed on **one line** — a KDoc in `CurioMotionSystem.kt` contained a shell glob
(`transitions/…*`), and **Kotlin block comments NEST**, so the glob opened a comment that was never closed; every symbol in the file then read
as unresolved across three files. Fixed, pushed as `96ec47ad`, and the lesson is written into the file itself.

1. **PHASE 2 SHIPPED.** `navigationCompose` 2.9.8 → **2.10.1**, and the NavHost now passes `predictivePopEnterTransition` /
   `predictivePopExitTransition` — six new `predictivePop*X/Z/Fade` factories repeat the pop shapes on the **linear** `Track` curve (Felicity's
   "linear while seeked"). Verified BEFORE the bump, from the artefacts: nav 2.10.1 needs compose **1.10.5**, the BOM pins **1.11.2** — the safe
   direction. OFF hands back `DefaultNavTransitions.*` (the library's own defaults).
2. **PHASE 3, PART SHIPPED.** The panel clock (`PanelEnterMs` = `NavMs`, `PanelExitMs` shorter, both on `Settle`) and the reader's
   `ReaderSheetFrame` re-timed with it — structure untouched (it still travels by its own measured height and the drag is still never
   interpolated), OFF keeping the pill clock's 190/130ms pair.
3. **PHASE 4 SHIPPED (the lists that suit it).** `CurioArrivals` — the rule a lazy list must obey: an item there is composed when it SCROLLS INTO VIEW, so `curioItemIn` alone would make a row animate again on every scroll back to it. One record per list, a key animates once, and the record is not snapshot state. Adopted on the **Cabinet grid** (`itemsIndexed` + record) and the **journals list** (`JournalRow` gained a `modifier`), with Home's recents already on it (a scrolling `Column`, so nothing extra was needed). **The Topic Database is deliberately left out** — 16k rows with section headers, where headers would pop while scrolling. Recorded in `MOTION_PLAN.md` §4.
4. **PHASE 3, THE PART THAT CANNOT BE DONE YET — WITH THE REASON.** The ~140 `ModalBottomSheet` sites are animated by Material 3 itself via
   `MaterialTheme.motionScheme`; **in M3 1.4.0 `MotionScheme`, `LocalMotionScheme`, `MaterialExpressiveTheme` and the `motionScheme` parameter are
   all `internal`** (read from the 1.4.0 sources jar, not the docs), and the public API is 1.5+, still alpha. The scheme is designed in
   `MOTION_PLAN.md` §4 phase 3 and the one line it goes in is marked in `CurioTheme`. **No Material3 alpha was pulled** — that is the app's
   top-level theme.

### 0.2 Open (all in `MOTION_PLAN.md` §4–5)

- **Phase 2** is the predictive-pop overload: `navigationCompose` 2.10's `predictivePopEnterTransition`/`predictivePopExitTransition` with the
  linear `Track` easing. **Deferred on purpose:** a dependency bump must be paired with the Compose BOM on a real device, and a nav library newer
  than the BOM links against APIs the runtime may not have — which CI cannot see. 2.9.8 already seeks the transitions natively, so today's build
  is not missing the gesture, only the linear-while-seeked curve.
- Phases 3–5: sheets and dialogs; the item animator on the other lists; retiring the old branches once lived with. **Not ported on purpose:**
  REMOVE, the rotationX flip, the View helpers and the IME/insets View callbacks (Compose-native equivalents are already in place).
- **Device verification is the member's** — CI compiles the app and cannot see a gesture. The checklist in §6 of the plan is what to walk.

---

## 0c. §49 — Lite mode, and the loops that were running for nothing (DONE, v454 — pushed, CI green)

> now without the liquid glass, the app lags a little. do something about it check properly if useless things running, and introduce a lite
> mode off by default which makes the app less laggy disables useless animation without making it clanky. also why the reader liquid glass
> is on when the option liquid glass is off. what u found in audit save it in n file

**Then, mid-work:** *"no no the liquid glass is fine continue"* — read as the answer to the reader-glass question (**leave the reader's
glass as it ships**, it is the reader's own identity and not a bug) and as *carry on with Lite mode*. Nothing in the reader's glass was
removed; the only place Lite mode touches glass is when the member has switched Lite mode ON themselves.

**Files:** `data/AppPreferences.kt` (`liteModeState`, `KEY_LITE_MODE`), **new** `ui/theme/CurioLiteMode.kt`
(`isLiteMode`, `isAmbientMotionOn`, `rememberAmbientTransition`), `ui/components/LiquidGlassPills.kt` (the one glass gate),
`CurioAnimations.kt`, `CurioSkeleton.kt`, `CurioScrollIndicator.kt`, `CurioIcons.kt` (`Bolt`), `CurioPetSprite.kt`,
`features/splash/SplashScreen.kt`, `features/home/HomeScreen.kt`, `features/spin/SpinScreen.kt`,
`features/incursion/IncursionSurfaces.kt`, `features/settings/SettingsSectionScreen.kt` (+`SettingsHubScreen.kt`),
**new** `app/APP_AUDIT.md`.

### 0.1 What was built

1. **LITE MODE, OFF BY DEFAULT (Appearance).** One switch; the gating rule is written into the file and is the whole design:
   **gate what only exists to be looked at** (ambient clocks, refraction passes), **never gate what carries meaning** (a transition, a
   sheet, a press, a state change, a progress bar, a page turn). That second half is the member's *"without making it clanky"*.
2. **THE GLASS GATE IS ONE LINE.** `isLiquidGlassRequested()` — the predicate every one of the ~33 glass sites already funnels through
   (`Modifier.liquidGlassCapsule` returns `this` without it, `curioAmbientGlass` too, the toolbars branch on it). Only the pass is
   skipped: each site's non-glass branch is its old solid fill, so nothing vanishes and no layout moves.
3. **THE AMBIENT CLOCK DOOR.** `rememberAmbientTransition(label)` returns `null` when parked; the splash mark, the drawer sky's twinkle,
   the Incursion mark, Spin's idle die and the pet's flourishes park. **The pet keeps its bob and blink** (only breath/glance/ear-flick
   stop) so it still reads alive. Spin's orbit, the shuffle glyph, every transition and every recording pulse stay — they are state.
4. **A REAL PERF BUG, FOUND BY ASKING WHAT IS RUNNING.** `CurioScrollIndicator`'s drain loop woke **every frame** for as long as the
   indicator existed just to find `pendingDelta == 0` — 60 wake-ups a second on an idle knob, on every screen with a rail. It parks on
   `snapshotFlow { pendingDelta }.first { it != 0f }` now.
5. **THE AUDIT IS A FILE.** `app/APP_AUDIT.md`: what was found and fixed, the Lite-mode rules, an **UNVERIFIED** list with the exact
   re-checkable command beside each lead, and an honest **not audited at all** list (accessibility, RTL, empty/error states, realtime on
   a bad line, battery).

### 0.2 Open, deliberately

- **The reader's glass with the app switch off** — reported, then the member said the glass is fine; recorded in `APP_AUDIT.md` §3.5
  rather than changed.
- **§48's word-page sheet** (the per-version answer moving into a bottom sheet) — still the one piece of that request not built.
- The UNVERIFIED leads in `APP_AUDIT.md` §3 (touch targets, icon-only descriptions, `Surface(onClick)` pressed states, `Color.White` in
  themed surfaces, `while (true)` timings) are leads, not findings. Say the word and they become a focused pass.

---

## 0d. §48 — a dictionary you can walk, a theme grid, and the audit (DONE, v453 — pushed with §49)

> make the theme select 2 grid based with beautiful view fix the dictionary page search box always open show it as a search pill to the
> right and show the dictionary words by alphabetical order, and only opening the word shows both of the version with badge to switch,
> and in edit profile screen the change photo and use photo/use blob they dont have elevation or proper pill fix it. in chats the theme
> dark chat bubble color is a little bad sometimes the texts blend, find more flaws which you can notice, and report it to me, do a full
> app audit analysis. and dont push it yet

**Confirmed with the member before editing** (the ask round): the dictionary page is a **physical-dictionary browse of the words the volume
holds**, and the per-version badge belongs to **the word's page** ("in bottom sheet").

**Files:** `ReaderOfflineDictionary.kt` (`headwords`), `ReaderDictionaryPage.kt` (the rail, the list, the search pill, the version badges),
`SettingsSectionScreen.kt` (`ColorThemeCard`), `ProfileEditScreen.kt` (`EditQuietAction`), `DirectMessageScreen.kt` (`bubbleFill`/`bubbleInk`),
`JournalListScreen.kt` (the colour filter out, labels in).

### 0.1 What was built

1. **A DICTIONARY YOU CAN WALK** — one bucket per letter, sorted case-insensitively; the field is a search pill on the head that opens on the
   pill clock and turns into its own cross; a word row sets the field, so list, search and answer are one machine.
2. **WHICH DICTIONARY ANSWERED** — the page keeps its volumes separate (unlike the sheet's merged answer): one badge per volume on the phone,
   `version` a key of the lookup effect so switching re-asks.
3. **THE THEME GRID** — two cards to a row, each drawing the theme it offers (page, hero, ink), live one ringed in its accent.
4. **THE PILLS** — `EditQuietAction` is a capsule, opaque, 2dp lift, equal halves; the camera disc lifted too.
5. **THE CHAT INK** — one `bubbleFill`, one measured `bubbleInk` for both sides (body, quote, timestamp, edited, ticks) and the surviving
   hardcoded white removed.
6. **THE JOURNALS** — the colour filter and its state/imports are gone; the mood and length rows are labelled.

---

## 0e. §47 — one Offline door, six ⋯ doors, and the header that matches the app (DONE, v452)

> merge the two modern and full 1913 in offline as offline shows nothing, only modern and full 1973 does. also from
> the 3 dot menu remove the share button, also the profile and home glass header is bad, they dont look like other
> glass header with that curve look

**Files:** `features/personal/BookReaderScreen.kt` (the doors, `ask`, the badge liveness, the Remove branch, the chip
labels, the ⋯ grid) and `ReaderDictionaryPage.kt` (the same door + a chips row it never had),
`ui/components/CurioGlassToolbar.kt` (the morph's single state), `features/home/HomeScreen.kt` +
`features/profile/ProfileScreen.kt` (the reservations, and the page stats card removed again).

### 0.1 What was built

1. **THE OFFLINE DOOR IS A DOOR, NOT A DICTIONARY.** `DictionaryDoor.volumes` owns all three volumes best-first; the
   badge is the INTENT ("answer me without a connection") and the volumes are what it draws on; `ask` returns the first
   volume that carries the word, `null` only when none of them is on the phone. Nothing was deleted — all three keep
   their download, progress and Remove. Badges dim per-DOOR, Remove keeps the member where they stand, chips name the
   source. Same in the sheet and on the page.
2. **SIX ⋯ DOORS IN TWO FULL ROWS.** Share leaves the grid (the selection's own Share stays); Settings moves up so no
   row is half-width.
3. **THE HEADER IS THE APP'S OTHER GLASS HEADERS.** `eased = 0f`: one state, the bar's natural content height, the
   26dp curve and the deep frost under all of it; the reservations follow the bar (never a lerp it does not drive),
   and Home's stats row is back in the bar.

---

## 0f. §46 — liquid glass app-wide, and the CL (DONE, v451 — pushed, CI green)

> liquid glass to more buttons and things app wide. many doesn't have it. fix cl fail

**Confirmed with the member before editing** (the ask round): the recipe is **real refraction, capture per
screen**; the surfaces are **all four** — the reader's pills, the shared controls, the Settings/list screens'
floating controls, the journal dock.

**Files:** `ui/components/LiquidGlassPills.kt` (the ambient architecture), `features/personal/BookReaderScreen.kt`
(the capture + seven pills), `ui/components/CurioTopBar.kt` (`CurioBackButton.ambientGlass`) and the four sites
**The CL** was the v449 route import — fixed and pushed as `feea6cf3`.

### 0.1 What was built

1. **THE AMBIENT GLASS DOOR.** Real refraction requires the pill to be OUTSIDE the captured subtree, which is why
evidence, and inert until adopted. A screen adopts in three lines; a screen that already keeps a capture
(`~30` of them) hands it over with the one-line `ProvideCurioGlass`.
2. **THE READER.** The page is the captured layer (with its paper painted INSIDE the capture, so a pill over a
margin refracts the page rather than nothing), the chrome is a sibling, and all seven pills refract: head (out /
name / search), foot, search bar, motion lock, listen, pinned count, scrubber. Fill → Transparent, lift → 0, and
a state-tinted pill hands the glass its own tinted container. 
3. **`CurioBackButton`.** Refracts on any screen that has adopted — `ambientGlass = false` at the four sites that
already bring their own glass (two `drawBackdrop` passes is not a stronger refraction).
4. **REMAINING, RECORDED NOT HALF-BUILT.** The Settings/list screens' and the journal dock's own adoption: the
same three lines each, but every one needs its content capture identified by reading that screen.

---

## 0g. §45 — one header state, and a pill's glyph in the accent (DONE, v450)

> more unification and polish of button and pills icon colors also fixing the glass header of home and
> profile screen, glitchy scroll and 2 differnt state so kee it 1 simplify and smooth

**Confirmed with the member before editing** (the ask round): keep the **COMPACT** bar always (one of the two
states goes); the glyph colour rule is **all glyphs in the accent**; the scope is **app-wide**.

**Files:** `ui/components/CurioGlassToolbar.kt` (the morph's `eased`, four pills), `ui/components/CurioTopBar.kt`
(`CurioBackButton`'s default ink), `features/personal/BookReaderScreen.kt` (the missing `CurioRoutes` import that
was the red build). `web/` and `desktop/` untouched.

### 0.1 What was built

1. **ONE BAR, THE COMPACT ONE.** `CurioGlassToolbarMorph` cross-faded two contents (a full hero and a compact
   identity row) with an ANIMATED HEIGHT while the finger scrubbed `progress` — the glitch the member reported.
   `eased` is now pinned to `1f`: the full content's alpha ramp multiplies by zero, the reported height never
   lerps, and the compact row rides the scroll at every position. `progress` is still taken (the screens still
   report their scroll through it) so no call site changed shape, and the full state's composables still render
   into the pinned-hidden column so a caller passing them keeps its layout.
2. **A PILL'S GLYPH IS THE ACCENT, ITS WORDS ARE INK.** Enforced at the SHARED components, not site by site:
   `CurioBackButton`'s default `contentColor` is `MaterialTheme.colorScheme.primary` (so every screen's back
   chevron follows in one change; a caller that deliberately recolours — the detail hero's frosted controls —
   still passes its own), plus the glass toolbar family's menu pill, back pill, streak fire and Edit pill. The
   streak pill is the shape of the rule: an accent fire beside an ink count.
3. **THE PAGE'S RESERVED HEIGHT IS THE COMPACT BAR'S, AND HOME'S STATS MOVED TO THE PAGE.** The header is
   always short now, so both screens' reservations follow it: Home's `glassHeaderReserve` and Profile's are the
   compact floor instead of a lerp from the tall hero driven by a `progress` that no longer draws anything (a
   lerp there would leave ~210dp of empty paper above the first card). And because Home's GLASS style replaces
   its torn hero with a `Spacer` — its Streak · Cabinet · Topics row lived ONLY in the header's full state — the
   row is drawn on the page now, on its own rose pane, so nothing was taken away from Home.
4. **THE RED BUILD FROM v449.** `BookReaderScreen`'s ⋯ door navigated `CurioRoutes.READER_DICTIONARY` with no
   `import com.curio.app.navigation.CurioRoutes` (the reader lives under `features/personal/`, so no
   package-level access saved it). Added, and every file that names `CurioRoutes.` was swept for the same gap.

**Open, honestly:** the glyph rule is enforced at the shared components (the back button and the header family),
not at all ~148 `tint = ink` call sites across the app — the remaining surfaces follow the rule as they are
next touched.

---

## 0h. §40 — the dock's panels, the reader's sheets, the dictionary's three doors, and the sky (DONE)

> the bulletpoint and highlight so tapping it again opens the collapsed options, and then i need
> to tap that last cross to close it, and theres one at the first to dismiss the picked, so make
> the close button the first one both dismiss and deselect the pick, and also the collapse auto
> closes when i start typing or i tap the page. also for dictionary use both the provider add show
> it a a badge option to switch between, also the buttom sheet can be scrollable and a little up.
> also the text tap and hold selection is bad, like when the line end and i copy 2 line then the
> both lines are touching each other with no space. also the online dictionary is bad, add a
> downloadable dictionary inside the app in the dictionary bottom sheet, but when the dictionary is
> opened from the 3 dot one it [is] more longer and let user search any word, and also fix the
> search box hiding below the keyboard for dictionary. and then for the drawer star map graph,
> animate it with star twinkle, and animate every time it closes and opens, with beautiful mesh
> like animation dont change the design, just beautifully animate it, also fix the light glow of
> the category tint when one is selected, and dont grow the dot too much. also the reader dropdown
> closes fast and good when taping outside but the swipe down to close is buggy it stays as an
> overlay for some time fix it.

**Decisions confirmed with the member before editing** (the ask round): the offline dictionary is
**a full one** (~100k headwords, one download, searched offline); the badge order is
**Offline · Wiktionary · Free, offline first once downloaded**; and the glued-lines copy report is
**the reader's long-press selection**, not the journal's copy box.

**Files:** `ReaderOfflineDictionary.kt` (new), `BookReaderScreen.kt` (the frame, the sheet),
`BookPdfText.kt` (the join), `PersonalCanvas.kt` (the dock's panels), `HomeScreen.kt` +
`CurioNavHost.kt` (the sky). `web/` and `desktop/` untouched.

### 0.1 What was built

1. **The dock's panels** — the marker's "No marker" and the bullet's "Remove list" now clear the
   pick AND close the panel; the trailing cross is gone from those two (it survives on format,
   alignment and export, whose first control is a real choice); and a panel closes itself when the
   focused line changes (which is what tapping the page does) or its text changes (typing).
2. **The reader's sheets** — the frame takes the IME inset on its root box (so a bottom-aligned
   sheet lifts clear of the keyboard) and it settles a body drag the instant no finger is left
   down, on the same flick the head reads; the debounce stays only as a fallback.
3. **A passage over a line break** — `PdfPageText` joins glyphs with a space where the GLYPHS'
   geometry says the page broke the line (a moved baseline, or a real gap), never after a hyphen,
   and never doubling a space that is already there. The page's own `text` and `textBetween`
   share one builder, so the copy, the context line, a share and a stored highlight all agree.
4. **The dictionary's three doors** — a badge row (Offline · Wiktionary · Free) with the offline
   file first once it is there, the sheet re-asking on a door switch (the online doors memoise, the
   offline one is a local stream), a taller panel for the ⋯ menu's search mode `0.62f` vs `0.55f`,
   and the download row (progress, licence, Remove) inside the sheet.
5. **The offline dictionary** — `ReaderOfflineDictionary`: Webster's 1913 (public domain, ~9MB,
   ~86,000 headwords) streamed from the source's own JSON with `android.util.JsonReader`, a
   `.part` file renamed into place only when whole, alphabetical early stop, and `null` for "no
   dictionary" against `emptyList()` for "no such headword".
6. **The sky** — one `reveal` progress driven by the drawer's own state: the panel eases and fades
   in, every hairline runs out from its star (the mesh draws itself), both reverse on close; a
   `withFrameNanos` twinkle runs only while the drawer is open and is read inside the draw block;
   the picked aura wears the lane's own tint at deeper mixes and tighter radii, and the dot grows
   18% instead of 50%.

### 0.2 Checks run

- **No Gradle command** (root `AGENTS.md` forbids it here); CI validates on push.
- `android.util.JsonReader`/`JsonToken` are framework APIs; `DictionaryDoor` keeps the existing
  `ReaderDictionarySource` and its two-option settings row untouched (no `when` had to change);
  `withFrameNanos` and the four badge-row imports were added after checking what the file already
  imported; the download callback writes Compose state (thread-safe) and reports progress in the
  row's own text rather than adding a progress-bar API that this Material version may not carry.
- A string/comment-aware bracket-balance pass over all six touched files: balanced.

### 0.3 Still open

- **The offline file is ~9MB, not the 20–30MB guessed** — Webster's 1913 is the full dictionary at
  that size, which is a better download rather than a smaller dictionary.
- **A tap on the SAME line's whitespace** does not close a dock panel (only a focus MOVE or
  typing does): the editor has no page-tap signal of its own yet; if the member wants that exact
  case, the page's tap handler is where the signal would come from.

---

# (previous session, kept for the state it records)

## 0. §39 — the Edit profile page, as a full screen

> this ia the desin specification of edit profile and instead of dialog box make it a full screen
> with this style kee the backgorud and color theme aware and kee the backgroud plain thi sis the
> modified only and ask question of anythign else, + the spec pasted below.

**The spec, in one line each:** editorial, calm, minimal, tactile; theme-aware plain background;
no gradients, no glassmorphism, no decorative cards; a small back button, a large quiet
"Edit profile" title and one supporting sentence; the picture centred with a small camera disc
overlapping it and exactly two compact actions under it; NAME and BIO as OPEN FIELDS with a
subtle bottom border (never a box in a box); an ACCOUNT section of flat EMAIL (muted/locked) and
USERNAME rows with a small supporting terms line; ONE tappable PRIVACY row; Cancel + Save
changes fixed at the foot (quiet + solid berry, same height, same radius); one typeface with a
calm hierarchy (nothing every-heading-bold); and an 8dp spacing base (8/16/24/32/40) where
important elements get more SPACE rather than another card.

**Decisions, all confirmed with the member before editing** (the ask round for this request):

- **Signed OUT** → one calm row ("Sign in to Curio") that opens the existing account surface
  (`CurioRoutes.SETTINGS_ONLINE`), never a form squatting inside the page.
- **Sign out** lives at the BOTTOM of this screen (row + the existing confirm dialog).
- **The picture**: exactly TWO actions (Add/Change photo · Use my blob / Use my photo). Tapping
  the picture itself expands it over the page, and the ⋯ in that expansion offers removal
  (the deletion door the dialog used to have).
- **Terms** → the SAME dialog the account card already shows (one `private` → `internal` flip on
  `CurioTermsDialog`, no second copy of the text).
- **A real route** (`profile/edit`), not an overlay: the page is a page, and Cancel simply
  leaves.
- **The handle folds into Save changes** and is checked AS IT IS TYPED (the same rules the
  server enforces, stated before the press) — no separate "Save username" button.

**Files:** new `features/profile/ProfileEditScreen.kt` (the page + the avatar pipeline moved out
of `ProfileScreen.kt`), `navigation/CurioRoutes.kt`, `navigation/CurioNavHost.kt`,
`features/settings/CurioAccountComponents.kt` (the terms dialog's visibility),
`features/profile/ProfileScreen.kt` (the old `ProfileDialogs` + its helpers retired; all four
entry points navigate). `web/` and `desktop/` untouched, per root `AGENTS.md`.

**Plan:** routes → the new page → the dialog's exit → verification (imports, references, bracket
balance) → DOX + changelog + commit/push.

## 0.1 What was built

1. **The route** — `CurioRoutes.PROFILE_EDIT = "profile/edit"` and a plain `composable` in
   `CurioNavHost`. It sits behind the profile page's own prefix, so the shell treats it as a
   pop screen (the same arrival Profile itself has) and shows no bottom bar; it inherits the
   nav host's `Push` clock, so nothing new was added for its opening.
2. **`features/profile/ProfileEditScreen.kt` (new)** — the spec's page: back disc, a 31sp
   Medium title, one 15sp sentence; the 104dp picture with a 34dp accent camera disc on its
   corner and the two quiet actions under it; **open fields** (a hairline, never a box —
   `EditOpenField`) for NAME and BIO and for the prefixed USERNAME; a muted, locked EMAIL row;
   the handle's one-line notice; the terms as a supporting line; a plain tappable PRIVACY row;
   a quiet SIGN OUT row; and Cancel / Save changes (52dp, 22dp radius, one each) held at the
   foot. `EditSpace` holds the 8/16/24/32/40 ruler. The avatar pipeline
   (`decodeAvatarSource`, `centerSquareCrop`, `scaleToMax`, `saveAvatar`, `removePhoto`) moved
   here from `ProfileScreen`. Tapping the picture expands it over a **blurred** page
   (`Modifier.blur` on an animated veil) with the ⋯ that removes the photo; `BackHandler`
   closes the expansion before the page.
3. **Saving** — `commit()`: name + bio written locally and mirrored best-effort; a changed
   handle validated from the same rule the server uses, then claimed (`SocialApi.updateUsername`)
   with the terms accepted through the shared dialog first (`pendingClaim`). A refusal is shown
   on the page; a success pops.
4. **The dialog retired** — `ProfileDialogs`, `EditSectionLabel`, `DialogPillAction` and the
   avatar helpers are gone from `ProfileScreen.kt` (390 + 75 lines), all three "Edit profile"
   doors navigate, the sign-out confirm moved with the page, and the screen keeps only the
   avatar path it draws (re-read on entry and on `ON_RESUME`, so the hero can never lag an edit).
5. **`CurioTermsDialog`** is `internal` now instead of `private` — one dialog, two callers, no
   second copy of the disclosure text.

## 0.2 Checks run

- **No Gradle command** (root `AGENTS.md` forbids compile/build/lint here); CI validates on push.
- Every API checked against its real definition before use: `CurioTermsDialog`'s signature,
  `CurioContentFilter`'s package (`data`), `SocialApi.updateUsername/updateDisplayName/updateBio`,
  `settingsRoseAccent`/`settingsReadableInk`/`settingsCardAccentInk` (public in
  `SettingsHubScreen.kt`), `CurioIcons.Screenshot` (= "photo_camera"), `AvatarCropDialog`'s
  three params, `SocialConfirmDialog`'s named params, `CurioMotion.Durations`, and
  `popScreenRoutePrefixes`' prefix matching (which is what makes `profile/edit` arrive like
  Profile). The module globally opts into `ExperimentalMaterial3Api`, so `Surface(onClick = …)`
  needs nothing.
- **A string/comment-aware bracket-balance pass** over all five touched Kotlin files: balanced.
- **The unused-import sweep** over `ProfileScreen.kt` dropped the 42 imports the retired dialog
  and its helpers owned; every remaining import is referenced (checked symbol by symbol, with
  `getValue`/`setValue` excluded as delegated names).
- Grepped the whole module for the retired names (`ProfileDialogs`, `showNameDialog`,
  `nameInput`, `taglineInput`, `cropSource`, `saveAvatar`, `removeAvatar`, `confirmingSignOut`,
  `decodeAvatarSource`, …) — no reference anywhere survives, and nothing else in the app opened
  that dialog.

## 0.3 Still open

- **The ⋯ in the expanded picture** offers "Remove photo" (and the blob switch when there is no
  photo, so the expansion is never a dead end). If the member wants more there — "Save to
  device", sharing — the menu is the place for it.
- **The bio is one line**, exactly as the dialog had it: the hero draws it as a single line, so
  a multi-line bio would need the hero to grow a rule first.
- **No SQL change and no migration**: everything here is UI, navigation, preference reads and
  one existing network call.

---

# (previous session, kept for the state it records)

## 1. The request (previous session) — §38

> fix the reading progress accidental touch and in journal the pain the page remove that
> option, and then mak ethe coloring smart so that chnaging color automatically adjusts the
> text color as well so the today the date pill or anything else doesnt get the weird
> unredable text, also i think dont color the today are,a, and also for the copy arrow the
> behaviror is unexpected fix it, also for the hihgligh selecter remove the frst x and when
> tappin git again the color it should deselect, and then the menu the drawer menu icon on
> home screen its gteeing the profile pic so fix that.

> continue

Seven things across three surfaces (the book page, the journal, the reader's dock and Home),
plus the one the member added while answering the questions: the dictionary "wasnt working".

1. **The reading-progress bar** — an accidental touch moves it (and stole the scroll).
2. **The journal's "Paint the page too"** — remove the option.
3. **The journal's colouring** — the ink on anything the member's colour fills must adjust
   itself, so nothing comes out unreadable.
4. **The Today/date area** — does not take the page's colour.
5. **The copy box's ← / → arrows** — the behaviour is unexpected.
6. **The highlight dock's ×** — remove it, and the applied colour should deselect.
7. **Home's menu pill** — it must not wear the member's picture.

Touched: `features/personal/BookDetailScreen.kt` (the gauge), `PersonalTheme.kt`,
`JournalAccent.kt`, `PersonalPage.kt`, `JournalEditorScreen.kt`, `PersonalCanvas.kt`,
`ReaderDictionary.kt`, `BookReaderScreen.kt`, `data/PersonalEntity.kt` (a dead helper),
`features/home/HomeScreen.kt`. `web/` and `desktop/` untouched, per root `AGENTS.md`.

## 2. What was actually wrong (found by reading, not by guessing)

1. **The progress bar** — `ReadingGauge`'s handler consumed the DOWN and treated ANY movement
   as a scrub (`if (change.positionChanged()) moved = true`). A finger running down the page
   that passed over the gauge therefore moved the reading place AND, because the down was
   consumed, stopped the page from scrolling: one false move cost both gestures.
2. **"Paint the page too"** — v429 added it, v439 withdrew it, v440b restored it. The switch
   lived in `JournalAccentSheet`, and the paper it painted came from
   `JournalPagePaint.painted` through `journalPaper()` (a 0.20 light / 0.28 dark tint) with
   `journalInk()`'s own contrast branch answering for the result.
3. **The unreadable text** — `journalOn(fill)` was `settingsReadableInk(fill)`, and
   `settingsReadableInk` answers from the THEME (a named theme's `onPrimary` pair, the pastel
   flag, the light/dark branch) and **never looks at `fill` at all**. So the one case
   `journalOn` exists for — text on a colour the member picked — was the one case it got
   wrong. The colour sheet's own tick had the same defect (`settingsReadableInk(color)`), and
   `PersonalCanvas.readableOnFill` was the only place in the journal with the right rule.
4. **The Today pill** — `JournalTopBar` was handed the page's `accentArgb` (v437: "the day IS
   the page's own title") and filled the day capsule with it, so a deep page colour left the
   date's ink at whatever the theme happened to be — the unreadable pill.
5. **The letter arrows** — `nudgePageLetters(more)`: **→ WALKED** the window along the row
   once it reached the row's end (`TextRange(range.min - 1, range.max)`) and **← SHRANK** it
   from the right (walking the other way at a single letter). Between them the two arrows
   could never hand the member more than the one character they started with.
6. **The highlight dock** — its colours were a 35% wash of themselves whether the passage wore
   them or not, so a marked passage's own colour was indistinguishable from the three it did
   not wear, a second press re-wrote the same mark, and the only way out of a mark from the
   dock was the × that the member asked to be rid of.
7. **Home's menu pill** — `TopBarPill` drew the member's face whenever
   `hasOwnPicture(avatarPath)` was true, and **a member wearing their blob as their picture
   has no photo path at all**, so `hasOwnPicture(null)` is TRUE for them: the drawer's
   hamburger, handed no path, drew their face instead of its own glyph.
8. **The dictionary** — every word Wiktionary does not carry answers **404**, the old getters
   turned any non-200 into `null`, and `null` is the sheet's own word for UNREACHABLE. A rare
   word, a name or a misspelling was therefore reported as a dead network, and v442's spelling
   suggestions could never run at all (they are only asked for after an EMPTY answer, which
   the 404 path never produced). The sheet's single nullable list also carried "not asked yet"
   and "unreachable" at once, so its first frame flashed "The dictionary could not be
   reached." whatever the word was.

## 3. Decisions, all confirmed before editing

Seven questions were asked and answered across the session ("Two times — from / until",
"Chips + context line", "scrink it but dont make it too lose th buttom", "Never paint the
page", "A scroll over the bar must not move it", "kee adding letter never walk shrink", "The
× in the selection dock", "The hamburger pill shows my photo").

- **The journal's paper**: **never paint the page** — the switch goes and the paper is the
  theme's (the member's own answer to "what should removing it do").
- **The Today area**: keeps the theme's colour (the member's own instruction).
- **The progress bar**: a scroll over it must not move it.
- **The letter arrows**: only add — never walk, never shrink.
- **The dock's ×**: removed, and the applied colour is what deselects.

No new feature here is toggleable-or-not: six of the seven are fixes, and the seventh
(removing "Paint the page too") is a removal the member asked for by name — root
`AGENTS.md`'s "ask before deleting" rule is satisfied by their own words plus the answer they
chose when asked what removing it should do.

## 4. What was built

### 4.1 The reading-progress bar (`BookDetailScreen.kt`)

The gauge now **wears in** like the reader's own magnified page: `awaitFirstDown` takes the
press and consumes nothing, and nothing is taken until the finger crosses the touch slop
**SIDEWAYS** (`dx > slop && dx > dy`). A finger that crosses it DOWNWARD has announced it is
scrolling, so the handler leaves the whole gesture to the page. The knob is lit by the CLAIM,
not by the touch (a scroll over the bar shows nothing at all), and a press that never travels
is still a tap that seeks on the release.

### 4.2 The journal (`PersonalTheme.kt`, `JournalAccent.kt`, `PersonalPage.kt`,
`JournalEditorScreen.kt`, `PersonalCanvas.kt`, `data/PersonalEntity.kt`)

- **The page never paints its paper.** `JournalPagePaint` lost its `painted` flag;
  `journalPaper()` is the theme's parchment with the app's accent whisper whatever colour the
  page was given; `journalInk()` is simply the theme's `onSurface`; the page's own box is the
  plain `background` again. The colour still reaches the page's doors (`journalDoorAccent`)
  and the tint its own controls wear (`journalPaperRaised`).
- **The switch is gone** from `JournalAccentSheet` (with its `painted`/`onPainted` params and
  the now-dead plumbing through `PersonalWritingPage` and `PersonalToolDock`), and the dead
  `PersonalNoteEntity.paintsOwnAccent` with it. **`pagePainted` is still READ and still
  WRITTEN** through `PersonalPageMeta` — the writer rebuilds the whole row from the meta, so
  dropping it would erase a member's earlier answer from their own file on the next
  keystroke. No SQL, no migration.
- **The day keeps the theme's colour.** `JournalTopBar` no longer takes `accentArgb` at all
  and asks for `journalDoorAccent(JOURNAL_ACCENT_THEME)`.
- **Smart ink.** `journalInkOn(fill)` is the measurement
  (`fill.luminance() > 0.55f` → the journal's near-black, else its cream) as a PLAIN function
  (a draw pass and a gesture want it as well as a composition); `journalOn(fill)` is it as a
  composable; `PersonalCanvas.readableOnFill` delegates to it; the colour sheet's tick asks
  it instead of the theme role.

### 4.3 The copy box's arrows (`PersonalCanvas.kt`)

Both arrows only ever ADD: → takes the letter to the RIGHT of the window, ← the letter to its
LEFT — never walking and never shrinking. Either arrow may OPEN a fresh window (← used to
refuse, "nothing to give back"), `canNudgePageLetters` is live for both on an empty reach and
dead only at the row's own edge, and the pills' labels are "One more letter to the left" /
"…to the right". A wrong reach is started over by a row arrow, "Line" or "Select all", all
of which reset `pageCharRange`; the box's own cross stays.

### 4.4 The dictionary (`ReaderDictionary.kt`, `BookReaderScreen.kt`)

- **The status is kept.** `Fetch(code, body)` replaces the two old getters' nullable String
  (one shared `fetch()` with the same 4s / 6s budget), and `define()` answers **404 →
  `emptyList()`** ("no such headword" — the answer that puts the suggestions on screen),
  anything else non-200 or a thrown call → `null` (unreachable), and a term that is not a word
  at all → `emptyList()` (the contract's own line, which the old code contradicted by
  returning `null` and drawing "could not be reached" for `1234`).
- **Three states, not one nullable list.** `ReaderLookup.Idle` / `.Answer(senses)` /
  `.Unreachable`. The sheet flashes nothing while it opens (it used to flash "The dictionary
  could not be reached." on its first frame, and a blank field sat on that message for good),
  and an empty field now draws nothing under the input.
- **The miss path stops at the first `null`** — the source is not answering, so four more
  neighbours would only buy four more timeouts before the same sentence.

### 4.5 The highlight dock (`BookReaderScreen.kt`)

The passage's own ink is resolved in the screen (`marks.firstOrNull { it.isHighlight && … }`)
and handed to the bar as `appliedInk`; that swatch is drawn as TAKEN (opaque, a 2dp rim, a
`Check` tinted with the ink that reads on it — `journalInkOn`), and pressing it **REMOVES the
mark** (`deleteReaderMark`) instead of writing it again. With that switch in the row the ×
("Clear the selection") is redundant and gone: the ⋯ door is the way to everything else, and
a tap on the page puts the dock away (`tapPage` clears a selection first).

### 4.6 Home's menu pill (`HomeScreen.kt`)

`TopBarPill` gained `showingPicture` (default false); only the profile pill passes true, so
only it ever resolves a face, and a pill handed no path draws its own glyph.

## 5. Checks run

- **No Gradle command**: this environment forbids compile / build / lint (root `AGENTS.md`).
  Validation is CI on push.
- Every API verified against its real definition in the repo before use:
  `viewConfiguration.touchSlop` (`GalleryWallFormat.kt` already uses it, so it needs no new
  import — only `kotlin.math.abs` was added to `BookDetailScreen.kt`), `CurioIcons.Check`
  (`ReaderMarkSheet` already draws it inside a colour swatch), `ReaderMarkEntity.colorKey`,
  `deleteReaderMark` (already used by the marks sheet), `PersonalPageMeta.pagePainted` (kept),
  and `hasOwnPicture`'s exact semantics in `ProfileAvatar.kt`.
- **A bracket-balance pass over all ten changed files** (string/comment aware, the one syntax
  fault a large hand-edit hides): zero unbalanced brackets.
- **Every removed import checked for its other references**: `luminance` out of
  `PersonalCanvas.kt` (its only use was `readableOnFill`, which now delegates),
  `settingsReadableInk` and `rememberCurioControlTick` out of `JournalAccent.kt`.
- The v443 block is in `app/AGENTS.md`, and the release notes are in
  `fastlane/metadata/android/en-US/changelogs/20260922.txt` — with the stale "Paint the page
  too" bullets dropped, because the feature never reached a release and the notes are edited
  in place.

## 6. Still open

- **How a letter reach is taken back.** The member asked for "keep adding letter never walk
  shrink", so neither arrow gives a letter back; a wrong reach is started over by a row
  arrow, "Line" or "Select all". If they want a way to take one letter back without the
  window moving, that is one more control on the panel.
- **The journal's paper following its colour** is settled as "never", and the stored
  `pagePainted` column is deliberately left in place (a Room column is a migration).
- **No SQL change and no migration anywhere in §38** — every change is UI, gesture, colour or
  network, and the two look fields that DID need a store were §37's.

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- **§59 — "for instance option remove the dictionary settings option. we can ship with it dw about the ap size, lets do a double build, one with advance feature focusing on online and all one smaller with the core curio features" + "also can it work with the same key" (IN PROGRESS — the two reader items are DONE and pushed; the two EDITIONS are the open work).**

  **The decisions, all from the member, asked before any edit** (this is a new measure with a permanent repo consequence inside it): the CORE edition drops **only two things — the neural read-aloud voice packs and the offline voice-to-text stack (Vosk, ~19 MB of arm `.so`)** — and keeps everything else; **two package names, side by side** (`com.curio.app` stays core, `com.curio.app.full` is the full one), chosen over one package with two variants; and the full edition **takes the ISBN scanner back** now that size is no longer what is being optimised (*"we can add the isb sanner into the advance build as we dont have to worry about the size"*) — the scanner that left with its camera permission and six dependencies in v458.

  **The "same key" question, answered:** YES — one keystore signs both editions. A signing key is not bound to a package name, `signingConfigs.release` already applies to every variant, and no new CI secret is needed. The two caveats recorded: **Play App Signing is per app record** (two listings if both go to Play, though the same UPLOAD key serves both), and **package-restricted API keys are the real trap** — a Google Books key restricted by package name + SHA-1 rejects the second package until that pair is added (TMDB / OMDb / Comic Vine / LibraryThing keys are unrestricted).

  **The dictionary, clarified by the member after being asked WHAT to remove:** two different surfaces were meant. (a) *"just its option from the reader appearance bottom sheet"* — the `ReaderDictionarySource` row left `ReaderAppearanceSheet` (**DONE, v465**; it is a set-once choice and it still lives in `ReaderSettingsScreen`). (b) *"when i select a paragraph the dictionary option doesnt show fix that"* — a real REGRESSION from v448: the dictionary was moved out of the selection row into a pill that renders ONLY for a single word, so a multi-word sweep had no dictionary door at all (**DONE, v465**; a passage now gets the door in the row, a single word keeps its pill — one door per selection, never two).

  **The edition split is BUILT (this push):** an `edition` flavor dimension with `core` (`com.curio.app`) and `full` (`com.curio.app.full`); `"fullImplementation"(vosk)` with **a real source-set seam** — the true `OfflineTranscriber`/`VoskModels`/`VoskModelDownloads` moved to `app/src/full/java/...`, an identical-API empty twin written at `app/src/core/java/...`, so no file under `main` imports `org.vosk.*`; `BuildConfig.EDITION` and `EDITION_OFFLINE_TRANSCRIPTION`; the Settings "Offline model" row gated on the flag (everything else closes by itself because the core catalog is empty); `src/full/res/values/strings.xml` giving the full edition its own launcher name.

  **Three real breakages found and fixed with it, each of which would have shipped silently:** (1) **the APK output path moved one directory deeper** — `app/build/outputs/apk/<flavor>/release/` — so the `apk/release/` globs in `android.yml`, `release.yml` AND `.github/scripts/build-summary.sh` all matched nothing (and the failure reads as "No signed release APK was produced", i.e. a signing fault, not a path fault); both job timeouts went 30 → 45 min for the doubled compile+lint. (2) **the in-app updater took "the first `.apk`" of a release** (`UpdateChecker.parseApkAsset`) — with two editions that is whatever GitHub lists first, and the wrong file fails at the last tap of the install with "App not installed"; it now matches the `-<edition>-` token in the published file name, with a deliberate fallback to the first `.apk` so installs updating from a PRE-SPLIT release (no token) still work. (3) the release's "expected splits" guard now checks **per edition × ABI**.

  **The ISBN scanner is DONE and pushed (v465b, `0ab7a506`)** — restored into `src/full` only (screen + five `fullImplementation` deps + `src/full/AndroidManifest.xml` carrying the camera permission, with the no-op `src/core` twin and the `BuildConfig.EDITION_ISBN_SCANNER`-gated door in `main`). **The edition split itself went GREEN on CI in the same session** (`b38858d7`, 26m51s, both editions compiled and linted) after a task-name fix: `lintRelease`/`assembleRelease` are AMBIGUOUS once a flavor dimension exists and Gradle killed the run at TASK SELECTION in 48 seconds, before a single line of Kotlin compiled.

  **Still open — ONE item, and it needs the member's answer before it can be built:** **the neural read-aloud voice packs**. They are now simply "a full-edition feature" rather than a size problem, since the member accepted the size (*"dw about the ap size"*), but the *route* is still an open fork: shipping Curio's OWN voice downloader means **committing a ~48 MB native `sherpa-onnx` AAR** (k2-fsa's official Android release, no official Maven coordinate — the Maven hits are third-party repackages), and **this environment cannot compile or validate it** (no NDK/SDK; Gradle is off-limits here by the root AGENTS), so it lands over several CI cycles. The alternative already ships and needs no code: the **v464 engine picker** lets the member point read-aloud at a neural speech engine they install themselves (F-Droid's SherpaTTS and k2-fsa's own ready-made TTS-engine APKs download Piper/Coqui voices themselves), which reaches "a better voice than the system one" today. Also open from §58: **Phase 3, the Edge TTS Dev-page experiment.**
- **§58 — "i want you to improve book reader, with highlight of which sentence row its reading, and then customisation play pause skip and a custom voice download option except system voice, research hats a better natural reading voice donload available" (PHASE 1 DONE, v463 — pushed).** Four questions asked before any edit (engine, skip unit, mark shape, cloud) because this is a new measure with a native-dependency decision inside it; then, shown that cloud is paid, the member asked for research on "any free cloud one and for offline downloaded voice packs … the best one for book reading", and chose **Piper medium + Kokoro** for the packs and **Edge TTS as a hidden Dev-page experiment**. Phase 1 shipped with no new dependency: sentence-level driver + read-along wash + standing-back page + a five-control voice bar, plus the PDF last-page infinite-loop fix.  **The engine door shipped in v464** (`feat(reading): read aloud can use any speech engine on the phone`) — asked as a binary choice against vendoring a neural runtime, because sherpa-onnx for Android is a large native AAR with no official Maven coordinate. **The member has since answered the other way for the packs** (*"dw about the ap size"*, plus the double build in **§59**), so Phase 2 is now "the full edition carries the packs", not "find a way to avoid them"; **Phase 3 (Edge TTS, a hidden Dev-page experiment) is still open.** See §59 and the v463/v464 sections of `app/AGENTS.md`.
- **§55 — "suggest me things to remove from settings … the voice-to-text bug … type the words live … decrease gpu for the aer style / liquid glass … remove some contributors … list all the used ai in readme", then "nvm, do a redundacy audit chekc … did the ml kit or something removed too right along with camera isbn? and commit and push all" (IN PROGRESS — the dictation fix and its live typing are DONE and pushed; the redundancy audit, the paper/glass GPU pass and the README/credits work are open).** Asked first, and the answers shaped the work: *"the aer style"* is the **paper style**; the AI list is **Codebuff (Buffy) + Freebuff Agent**; the credits change is **AI/bot identities only** (a human's attribution for code still in the repo would break AGPL §5, and is not what was asked); the settings-removal scope was dropped by the member before I touched it. **The dictation bug:** the mic's dialog lived inside the field's dock — a dock that exists only while the field is focused with the keyboard up — so the dialog's own focus folded the dock and unmounted the mic that owned it. The field hosts the session now, the dock stays composed while dictating, and every partial transcript is typed into the note live (Cancel restores the note; the door is Done). **ML Kit:** it left with the scanner in `f4bafcdb`, along with the four CameraX libraries and `kotlinx-coroutines-play-services`.
- **§54 — "Sweep the app for any other permission or dependency nothing earns, the way the camera one just went" (DONE, v458).** Asked first, with the findings already researched: the member took **all-files access** (its only user, the Glass Widget Lab's wallpaper auto-detect, was deleted on Sep 8 while the declaration stayed — the manifest's most review-sensitive permission, earning nothing since), **androidx Palette** (no import, no `Palette.from()`, its comment describing a feature that is gone), **the empty test scaffolding**, and — asked a second time once I found it — the **Compose tooling pair** (no `@Preview` in the module, and `ui-tooling-preview` was shipping in the release APK). They **kept** the 36 unused catalog aliases (build-time only). Every permission and dependency that remains was walked against its code and earns its place; the census is in the v458 section of `app/AGENTS.md`. One defect on the way: a removal edit joined the Vosk dependency into the comment above it (unresolved symbols on the next build), caught by re-reading the seams — now a root compile-safety rule.
- **§53 — "remove the isbn scanner feature along with its camera ermission," (DONE, v458).** The shelf's add-a-book sheet had three doors and the middle one was a whole screen (`IsbnScannerScreen.kt`: CameraX preview + ML Kit barcode + a keyless Open Library lookup that saved the book). It is deleted entire, **the camera permission goes with it** — `android.permission.CAMERA` and the `android.hardware.camera` feature out of the manifest, the `isbn_camera` cache-path out of `file_paths.xml` — and so do the six dependencies only it used (mlkit barcode, the four `androidx.camera` libraries, and `kotlinx-coroutines-play-services`, whose `Task.await` the scanner was the module's only caller of). Checked BEFORE removing the permission that nothing else earns it: every other image door in the app hands the job to the system picker. No migration (a scanned book was an ordinary row), and nothing is left on a phone (the scanner never wrote the `isbn/` path it declared). Search and Type are the two doors now; the ISBN itself stays wherever no lens was needed (`BookCoverFetch`). See the v458 section of `app/AGENTS.md`.
- **§50b — "cl failed and then start phase 2 and phase 3" (DONE, v455b — pushed; then REVERTED in §51 at the member's request — see 0.2).** The CL failed on a **shell glob inside a KDoc** (Kotlin block comments nest → an unclosed comment → every symbol in `CurioMotionSystem.kt` unresolved elsewhere); fixed and pushed. **Phase 2:** `navigationCompose` 2.9.8 → 2.10.1 and the predictive-pop transitions wired on the linear `Track` curve — the back gesture now seeks the page — with the compose/BOM pairing *verified from the POMs before the bump* (nav wants 1.10.5, the BOM pins 1.11.2) and `DefaultNavTransitions.*` handed back when the experiment is off. **Phase 3:** the panel clock shipped (`PanelEnterMs`/`PanelExitMs` on `Settle`) and the reader's `ReaderSheetFrame` re-timed with it, structure untouched; Material's own ~140 `ModalBottomSheet`s are **gated on Material3 1.5** — in 1.4.0 `MotionScheme`/`LocalMotionScheme`/`MaterialExpressiveTheme`/the `motionScheme` parameter are all internal, and no alpha was pulled for the app's top-level theme. The scheme is designed in `MOTION_PLAN.md` §4 and the one line it wants is marked in `CurioTheme`.
- **§50 — "the tap and hold is buggy in home screen recent topics … remove the tap and hold action from home screen recents … do a full plan to improve aps transtions oening animations … [the Felicity repo] … make this a new option for smoother animation in experiments, and no old app animation will be used" (DONE, v455 — pushed; then REVERTED in §51 — see 0.2. The recents' tap-only half SURVIVES on purpose).** Asked first about the theme grid (it was already live — v453's two-column sheet; the member just had not opened the row) and checked the previous CL (Lite mode: **green**). Then: **Home's recents are tap-only** — the hold is removed rather than re-tuned, and the radial menu with it; and **the motion system** shipped as phase 1 of `app/MOTION_PLAN.md`: shared axis X (a quarter-width drift + crossfade, 500ms), Z for modal pushes, a pure fade wherever a shared element is the animation, the `1 − (1−t)⁶` settle curve, and Felicity's item ADD as `Modifier.curioItemIn`. With the switch on, the old `CurioMotion.Durations` nav branches are unreachable and the screen reveal stands down. **Switch:** Experiments → Motion → "Smoother transitions", default OFF per the house experiment rule; the interaction layer (pill clock) is deliberately untouched and recorded as the next step. Phase 2 (navigation 2.10's predictive-pop overloads + the Compose-BOM pairing check on a device) is deferred with the reason written down.
- **§49 — "without the liquid glass, the app lags a little … introduce a lite mode off by default … also why the reader liquid glass is on when the option liquid glass is off. what u found in audit save it in n file" (DONE, v454 — pushed with §48).** Lite mode is a *performance* profile (Appearance, default OFF): the glass pass is skipped at the one predicate every glass site already asks, and the decorative clocks park through `rememberAmbientTransition`. Meaning-carrying motion is never gated (that is the *"without making it clanky"* half). A real find on the way: `CurioScrollIndicator`'s drain loop woke every frame on an idle knob; it parks on the delta now. The audit lives in **`app/APP_AUDIT.md`**, split into fixed / UNVERIFIED leads (with the command to re-check each) / not audited at all. The reader's glass with the app switch off: **the member said the glass is fine**, so it was left alone and recorded.
- **§48 — the browsable dictionary, the theme grid, the chat ink, and a full app audit (DONE, v453 — pushed together with §49).** Asked which list "alphabetical order" meant and where the version badge goes: the answers were *"the
  dictionary from the home screen shows the full words it have, like a physical dictionary"* and *"dictionary page inside the word page in a
  bottom sheet"*. Built: **the dictionary page browses** (`headwords` per letter — one bucket per letter, sorted case-insensitively, nothing read
  until a letter is stood on), the **search is a 50dp pill at the right of the head** whose glyph becomes the cross that closes it, a word row
  sets the field, and **a word shows one badge per dictionary on the phone** with the answer belonging to the badge you are on. Also: the theme
  picker is a **two-column grid of real previews**, `EditQuietAction` is a real capsule with a 2dp lift, the chat bubble's ink asks its own fill
  **on both sides** (plus one surviving hardcoded white killed), and the journals' colour filter is gone with the two remaining rows labelled.
  **Open, and deliberately not half-built: the word's own sheet.** The answer is still inline on the page (with the version badges working);
  moving it into a bottom sheet is the remaining piece of the member's answer. The audit findings are in the response and in `app/AGENTS.md`.
- **§47 — "merge the two modern and full 1913 in offline as offline shows nothing … also from the 3 dot menu remove the share button, also the profile and home glass header is bad, they dont look like other glass header with that curve look" (DONE, v452).** Three things. **The dictionary's Offline door is a DOOR now**, not a dictionary: it owns all three volumes (modern
  senses first, then the complete 1913, then the abridged one), the badge row is **Offline · Wiktionary · Free**, and `ask`
  walks the door's volumes and returns the first that carries the word — with "no volume here" (`null`) and "no such
  word" (`emptyList()`) still told apart. A badge dims only when NONE of its volumes is here, a Remove keeps the member
  on Offline while any volume remains, the chips name the SOURCE ("Download WordNet 3.1"), and the dictionary PAGE got
  the chips row it never had (with one badge its other two volumes would have been unreachable from the page).
  **The ⋯ menu is six doors in two full rows** — the Share tile is gone (the selection's own Share and the hold dock's
  are untouched) and Settings moved up so every row is as wide as the others. **The Home/Profile glass header is the
  app's other glass headers now**: the single state is the bar's natural content height (title, subtitle, trailing
  pills, action row, content row, 26dp bottom curve, 1.6× frost), with both screens' reservations following it, and
  Home's Streak · Cabinet · Topics row back in the bar (the v450 page card removed).
- **§46 — "liquid glass to more buttons and things app wide, many doesn't have it. fix cl fail" (DONE, v451 — pushed as `dc3aedef`, CI green).**
  **The CL:** the failure was the v449 dictionary route (`BookReaderScreen` navigated `CurioRoutes.READER_DICTIONARY`
  with no `CurioRoutes` import); the fix went out in `feea6cf3` and its run was still `in_progress` when this
  pass ended. **The glass:** asked what recipe — **real refraction, capture per screen** — and which surfaces:
  **the reader's pills, the shared controls, the Settings/list screens' floating controls and the journal dock**.
  Built this pass: the AMBIENT architecture (`rememberCurioGlassScreen` / `glass.capture` / `glass.Provide` /
  `ProvideCurioGlass` / `ambientGlassOn` / `Modifier.curioAmbientGlass` in `LiquidGlassPills.kt`) so a screen
  adopts in three lines and every shared component underneath refracts with no parameter; **the reader** (the
  page is the capture, the chrome is its sibling, all seven pills refract — head, foot, search bar, motion lock,
  speak, pinned count, scrubber); and **`CurioBackButton`** (refracts wherever a screen has adopted, with
  `ambientGlass = false` at the four sites that already bring their own glass). **Remaining (recorded, not
  half-built): the Settings/list screens' and the journal dock's own adoption** — each is the same three lines
  (or the one-line `ProvideCurioGlass` where the screen already keeps a capture) but each needs its content
  capture identified by reading that screen, which is the work left.
- **§45 — "more unification and polish of button and pills icon colors also fixing the glass header of
  home and profile screen, glitchy scroll and 2 differnt state so kee it 1 simplify and smooth" (DONE, v450).**
  Asked which of the two header states to keep: **the compact bar, always**; asked what the icon-colour rule
  should be: **all glyphs in the accent**, **app-wide**; and asked where the transcripts page opens from.
  Built: `CurioGlassToolbarMorph`'s `eased` is pinned to `1f` (ONE row at every scroll position — the full hero
  is never faded into and the height never lerps, which is what removed the glitch), and a pill's glyph takes
  `MaterialTheme.colorScheme.primary` while its label stays ink, enforced at `CurioBackButton`'s default
  `contentColor` plus the glass toolbar family's menu / back / streak / Edit pills. Also fixed the red build that
  followed v449: `BookReaderScreen` navigated `CurioRoutes.READER_DICTIONARY` without the
  `com.curio.app.navigation.CurioRoutes` import. See the v450 section of `app/AGENTS.md`.
- **§44 — the dictionary's own page (DONE, v449).** The lookup is a PAGE now (`reader/dictionary`,
  `ReaderDictionaryPage`) as well as the reader's sheet: the reader's **⋯ menu**'s Dictionary tile and
  Home's **"+"** sheet (a new `CreateEntrySheet` door) both open it, it is search-first (one field at the
  top, always), stands taller than the sheet, rides clear of the keyboard (`imePadding`), carries the same
  five doors with each offline volume downloaded or removed from the page, groups the answer by part of
  speech, offers the nearest spellings on a miss, and keeps the visit's own list of words looked up. The
  sheet keeps the SELECTION's door, which is the answer-shaped use it was built for. See the v449 section
  of `app/AGENTS.md`.
- **§43 — the dock, the Wide that did not stay, the lock, the ⋯ menu, the Snapshot and back (DONE, v448).**
  Seven items: the highlight dock is a **fixed, bigger toolbar** (42dp discs, 44dp doors, a real gutter) and the
  **dictionary is its own pill above it** naming the word, shown only for a one-word selection; **"Wide" stays
  wide** (a race — the reader's rebuild read a store whose 400ms-debounced save had not landed, so the store now
  loads once per process AND an orientation change is written the moment it is applied); **a locked page keeps
  its vertical move** when it is Wide or magnified in pages mode (the lock's guard now measures travel per axis
  and lets a vertical gesture through, freezing only the pinch and the sideways claim); the **⋯ menu breathes**
  (three rows, 18dp/12dp rhythm, the last row centred, a 0.38 floor); **Snapshot** is a seventh ⋯ door that puts
  the chrome away, copies the SCREEN (`PixelCopy`, with a view-draw fallback — page colours and all), and hands it
  to a fraction-based crop frame whose crop shares as a PNG (the member's answer: a second door beside Share,
  always on, both PDF and reflowable); and **back puts a selection down** instead of leaving the book. See the
  v448 section of `app/AGENTS.md`.
- **§42 — WordNet and the fuller 1913 as extra offline doors (DONE, v447).** The member's answer to the
  data question ("wordnet and fuller please") is built: the sheet's badge row is **Offline · Modern ·
  Full 1913 · Wiktionary · Free** and the offline trio are three real, verified sources — **WordNet 3.1**
  (`fluhus/wordnet-to-json`'s release asset `wordnet.json.gz`, ~11.4MB, the door with MODERN senses) and
  **the full OPTED 1913** (`CloudBytes-Academy/English-Dictionary-Open-Source`'s `csv/dictionary.csv`,
  ~14MB, 176,023 definitions, public domain) alongside the existing Webster's 1913 JSON object. Both new
  volumes are **parsed from their own release format**: WordNet's synset-keyed gzipped JSON is read in one
  pass over `synset` (skipping `lemma`/`example`) and the OPTED table through a real CSV reader (the 1913
  definitions carry commas, doubled quotes and wrapped line breaks) — and both are **translated once, as
  they download, into a per-first-letter bucket index** (`word \t label \t definition`) so a lookup opens
  ~0.5MB instead of the whole dictionary, with no sorting and no dictionary-size buffer. The v445 rules
  hold: a `*.part` index is never searchable, the download bytes are consumed and never kept, and a missing
  volume answers `null` ("no dictionary") rather than `emptyList()` ("no such word"). The sheet keeps ONE
  download row — the door you are standing on (source · licence · size · progress bar · Remove) — with
  chips for the volumes you are not on. **Still open from §41:** the dictionary's own PAGE (a route opened
  from the reader's ⋯ menu and from Home's `+` sheet). See the v447 section of `app/AGENTS.md`.
- **§41 — download polish, Edit profile in the reader's floating pills, and the dictionary data question (PARTLY DONE, v446).**
  **Done:** the offline dictionary's download row carries a real 4dp progress bar under its percentage
  (two boxes, the reader's own ink and accent); Edit profile was re-skinned onto the reader's chrome
  objects — a round way-back and a floating `Edit profile` capsule at the head, Cancel / Save changes
  as floating pills of the same 50dp height at the foot, and the spec's supporting sentence moved into
  the body; and the page gained real badges (`EditBadge`) — ACCOUNT says `Curio` / `Signed out`, the
  locked EMAIL says `Locked`, and the username's one state line is a badge in error or accent.
  **Still open (not built):** (1) the dictionary's own PAGE — a route opened from the reader's ⋯ menu
  and one more entry from Home's `+` sheet, taller/standalone, allowed to search any word; (2) the
  data swap the member asked for ("wordnet and fuller please") — the two sources were checked and
  **both are ZIP releases, not the single raw file the current Webster's 1913 door needs**
  (`globalwordnet/english-wordnet`'s `english-wordnet-*-json.zip`; `fluhus/wordnet-to-json`'s release
  asset; the full OPTED 1913 is a 183k-article dump). A parser for either must be written against the
  real bytes, which is its own pass: the honest answer to *"is the current better or are there more
  better ones"* is that **Webster's 1913 (shipped) is still the cleanest licence-safe single file**,
  WordNet/OEWN is the one that carries modern vocabulary (attribution required, zip release), and
  Wordset has the nicest data but **states no licence**, which is why it was not shipped.
- **§40 — the dock's panels, the reader's sheets, the dictionary's three doors, and the sky (DONE, v445).**
  The marker and bullet panels close themselves (the first cross takes the pick off the line AND shuts
  the panel; typing or moving lines puts it away); a reader sheet rides above the keyboard and its
  swipe-down settles the instant the finger leaves; a passage over a line break keeps its words apart
  (the copy, the context line, a highlight); the dictionary carries all three doors as a badge row
  with a **downloadable offline dictionary** (Webster's 1913, public domain, ~9MB, streamed and
  searched locally) and a taller panel when the ⋯ menu opens it to search any word; and the drawer's
  star map twinkles, draws itself in and out with the panel, wears its own category tint when a lane
  is picked, and no longer swells the dot. See §0–§0.3 above and the v445 section of `app/AGENTS.md`.
- **§39 — Edit profile as a full screen, to the member's own design spec (DONE, v444).** The
  identity editor is a PAGE now (`profile/edit`, `ProfileEditScreen`) instead of a dialog: a
  small way back over a large quiet title and one supporting sentence, the picture centred with
  a small camera disc and exactly two compact actions under it, NAME and BIO as OPEN FIELDS with
  a hairline under them, a flat ACCOUNT section (muted locked EMAIL, prefixed USERNAME), ONE
  tappable privacy row, sign out at the bottom, and Cancel / Save changes held at the foot — all
  on a plain theme background with the spec's 8dp ruler and no cards inside cards. The handle is
  claimed with **Save changes** and checked as it is typed, the terms are the SAME dialog the
  account card shows, tapping the picture expands it (with the ⋯ that removes the photo) over a
  blurred page, and a signed-out account is one calm row. What the dialog could do — the photo
  and its crop editor, the blob, the name, the bio, the handle, Privacy, signing out — all
  survives the move; the old `ProfileDialogs` and its helpers are gone. **No SQL, no migration.**
  See §0–§0.3 above and the v444 section of `app/AGENTS.md`.
- **§38 — the seven fixes across the book page, the journal, the reader's dock and Home
  (DONE, v443).** The progress gauge wears in sideways so a scroll over it neither moves it nor
  steals the page's scroll; the journal's "Paint the page too" is gone and the paper is the
  theme's again; the day pill keeps the theme's colour; ink on any colour the member picks is
  measured (`journalInkOn`, so nothing comes out unreadable); the copy box's letter arrows only
  ever ADD a letter on their own side; the reader's highlight dock draws the passage's own
  colour as taken and pressing it takes the highlight back, with the × gone; and Home's menu
  pill no longer wears the member's picture. Plus the one that surfaced while answering the
  questions: the dictionary treated the source's own 404 as "could not be reached", so a
  misspelled or rare word read as a dead network and the spelling suggestions could never run
  (the sheet now tells "not asked", "no such word" and "the source is down" apart, and stops
  asking a source that has stopped answering). **No SQL, no migration** — the stored
  `pagePainted` column stays, the UI simply no longer offers it. See §1–§6 above and the v443
  section of `app/AGENTS.md`.
- **§37 — the nine reader fixes (DONE, v442, pushed as `fe22bfdc`).** The ⋯ sheet sized to its
  own tiles; the dim's FROM/UNTIL window with one shared clock row and picker; the motion
  lock's wear-in so a locked page still hears a tap, a hold and a sweep; the side taps answered
  by their own innermost handler (fast, repeatable, and never read as a double tap); the
  dictionary suggesting the passage's words as chips with the sentence quoted, plus spelling
  suggestions on a miss; `headword()` so a comma, a possessive or a stray quote no longer hides
  a real word; the sheet shutting on a flick as well as a distance, with a longer pull needed
  nowhere; the highlight dock wearing the reader's own opaque pill body instead of a shadow
  over near-identical paper; and the header's exit reduced to one motion per reason, written as
  `CurioMotion` tokens. **No SQL, no migration.** See the v442 section of `app/AGENTS.md`.
- **§36 — the journal's open animation, and the page slider's arrows + shadow (DONE, v441, pushed as `03bd2e44` and the v441 commit).**
  1. **"the animation open nimation of journal is clanky and the pass u did for motion i
     think that also cause this" — the member was right about the second half and right
     for the wrong reason about the first.** The journal was NOT the cause: every plain
     forward navigation (the journal editor, a chapter, a book, a profile) falls into the
     nav host's generic branch, which glided the new page in over `Durations.Deliberate`
     — **500ms** — with the outgoing page drifting for the same half second. That branch
     has its own token now (`Durations.Push` 260ms / `.Pop` 220ms), the travel untouched
     (1/6 in, 1/8 out), and `Deliberate` goes back to what it was written for. The part
     the member was right about: v439's pill clock was 220/140 where the furniture it
     replaced was 180/120 — **slower by 40ms** — so it is 190/130 now. One clock, one
     tempo set, no second clock for the journal.
  2. **"page slider ui is bad with tha weird shadow — and next and previous button doesnt
     work on rapid click only goes 1 and stops working" — one root, two symptoms.**
     (a) **The arrows (`stepFrom`, `stepLedger`):** every arrow and zone computed its
     target from a place that is only true once the turn has FINISHED
     (`pagerState.currentPage`, `shownPage`, `listState.firstVisibleItemIndex`,
     `textPager.currentPage`), so four quick taps all computed the SAME next page and
     re-asked for the turn already in flight — one page, then dead until it settled.
     `stepLedger` remembers the hop; a tap steps from the destination while the reader is
     still on either END of it. **The two-end match is the safety property, not an
     oversight — a step hop is one page, so no integer lies strictly between its ends,**
     while a range rule would let a chapter or mark jump that lands inside an old run of
     taps resume from the run's end. Declared ABOVE `stepPage` (a local function cannot
     reach a local declared later in its own body — the file says so twice already).
     (b) **The same defect one layer up:** `ReaderHoldButton` is built once with
     `pointerInput(Unit)`, so it would have kept calling the closure it was first built
     with forever; its `step` is read through `rememberUpdatedState` now.
     (c) **The shadow:** the pill's fill was `surface` F5F0E8 over `paper` FBF6EC — two per
     cent apart — so the only visible part of the capsule WAS its shadow. Opaque
     `lerp(surface, ink, 0.06f)` fill + hairline edge + the pinned page's 8dp lift,
     `animateContentSize` deleted (it animated nothing and cost a layout pass per frame),
     and the count in a **fixed 72dp right-aligned slot** so the track never resizes under
     the member's finger while they drag.
  3. **No SQL change and no migration anywhere in §36** (UI, gesture and motion tokens only).
- **§35 — the four bugs from the crash report, the journal's page colour, and read-aloud (DONE, pushed as `1fcc1d00`/`621c7207`).**
  1. **THE CRASH, FOUND IN THE CODE (`drawVoicePulse`, `MIN_WAVE_HEIGHT_PX`).** The
     report — *"Cannot coerce value to an empty range: maximum -0.9 is less than minimum
     0.9"*, thrown during `dispatchDraw` — is the voice note's WAVE: the stroke is floored
     at 1.8dp so `bandTop` is exactly 0.9, and `bandBottom` is `size.height - halfStroke -
     depthDrop`, so on a canvas with no height the band CLOSES AND INVERTS and
     `coerceIn(bandTop, bandBottom)` throws on the empty range. A row that has not been
     measured yet reports exactly that size for a frame. Two guards now: a canvas under
     12px is left alone, and the band can never close.
  2. **THE PDF IS BACK IN COOLER (`renderPdfPage`).** v439 rendered pages into `RGB_565`
     to save the alpha channel — **`PdfRenderer.Page.render` accepts nothing but
     ARGB_8888** — so "Cooler" stopped rendering pages at all (member: *"pdf isnt loading
     now in cooler"*). ARGB_8888 always now; the savings that are real (the 1.5× upscale
     cap and `beyondViewportPageCount = 0`) stay. Never make that config conditional again.
  3. **THE JOURNAL OPENS ON THE TAP (`rememberJournalDoor`, `todayEntryId`).** v440's door
     awaited a database query before navigating (*"journal opening is clanky too"*). The
     list already holds every journal it draws, so today's page is handed in from memory.
  4. **THE SHEETS ARE PANELS AGAIN, AND THEIR SCRIM IS DRAWN (`ReaderSheetFrame`).** Every
     reader sheet keeps 45% of the screen (the ⋯ grid and the dictionary were the two
     named), and the scrim no longer fades through `graphicsLayer` — a full-screen
     offscreen layer re-blended every frame of every arrival and departure, which is a
     real part of "clunky and not smooth". **No motion restriction was added anywhere:**
     the only animator-scale check in the app is the blob faces obeying the phone's own
     "remove animations" switch, which cannot touch app motion.
  5. **THE JOURNAL PAGE TAKES ITS COLOUR AGAIN.** `git revert` of `147a2516` (the v439
     withdrawal), resolved by hand in `PersonalPage.kt` — the page paints itself as it did
     at `e869bac5`, and the "Paint the page too" switch is back in the colour sheet.
  6. **READ-ALOUD, FINISHED (`ReaderSpeaker`, the speak pill, `speakSpeed`/`speakVoice`).**
     Reads the visible page and follows on (blocks for a reflowed book, page text for a
     PDF), with a speed slider and a voice picker in reading settings. The engine is
     prepared on the first tap and released when the reader closes.
- **§34 — "do the reder, journal additions and also for journal ad time note too its only note date, and in journals view dont update the time if its edited again late, and add search for journals and also sorting by date by tapping the date in journals date" (THE THREE JOURNAL ITEMS ARE DONE; THE "READER, JOURNAL ADDITIONS" GROUPS ARE AWAITING ONE ANSWER).**
  **Built and committed (unpushed, per the member's "dont push anything now") — v440:**
  1. **A row names the moment the page was WRITTEN.** `PersonalNoteEntity.writtenAtMillis()`
     (= `createdAtMillis`, falling back to `updatedAtMillis` for pre-v389 rows) is printed
     beside the word count by `Long.prettyTime()`. `saveNote` already preserved
     `createdAtMillis` and only re-stamped `updatedAtMillis`, so a late edit cannot move it —
     the DAO's tiebreaker (`COALESCE(NULLIF(createdAtMillis, 0), updatedAtMillis)`) says the
     same thing for the same reason. **No SQL, no migration.**
  2. **Search** (`JournalSearchPill` + `PersonalNoteEntity.answers`): a pill under the head
     that becomes the field, on the `CurioMotion` pill clock, taking focus as it opens. It
     reads only what a row already shows — **never `doc`**, which re-parses JSON per access —
     and the head's subtitle counts ``matched of journals`` during a search. A miss is a
     cause ("Nothing matches" + the query), not a bare dash.
  3. **The date pill orders the collection** (`PersonalHeaderDate(onToggleSort, newestFirst)`):
     the shelf passes nothing and keeps its plain label (`enabled = onToggleSort != null` — a
     disabled `Surface` takes no presses and draws no ripple, so one composable is both); the
     journals list reverses on tap with an arrow saying which end is up. Ordering happens in
     the SCREEN (`sortedWith`, tie on `writtenAtMillis()`), so reversing costs no DB trip, and
     the month groups reverse with it.
  4. **The gestures box stands down, three ways** (`ReaderTapZoneEditor`): while an edge is
     being placed (`ReaderZoneHandle.onAdjust` — drag start/end/cancel), by a collapse door on
     the panel, and by v434's eye for the washes. Standing down leaves a "Gestures" pill in
     the corner that opens it again, and every movement is a `CurioMotion` factory.
  **§34b — the additions the member then ticked (DONE EXCEPT TWO).**
  Reader: **night dim on a schedule** (`ReaderLook.dimAuto` — "At sunset" = the phone's own
  dark theme, which is what a phone set to automatic switches at sunset; Android has no
  sunset to ask for and computing one needs the location, which this app holds for nothing
  else — **superseded by §37's FROM/UNTIL window**); **pages left in the chapter** in the
  progress card (a book with pages answers from `ReaderOutlineEntry.page`, a reflowed book
  from `TextPagedReader`, and a page-less flow says nothing rather than a made-up zero).
  Journal: **filter by mood / colour / length** (mood and colour are columns; the length
  counts are taken once per (list, bucket) on `Dispatchers.Default` and only while a length
  is on — a word count decodes a document); **a writing goal with its own evening nudge**
  (`AppPreferences.getJournalGoal`, `JournalGoalReminderScheduler` + `JournalGoalReminderReceiver`
  + manifest, and the day's rail in the journals head); **"open today's page"**
  (`rememberJournalDoor` — today's page if the day was written, a new one if it was blank, or
  the last page opened; the editor records the last id as a page loads). **No SQL change and
  no migration anywhere in §34.**
  **STILL OPEN:** *read-aloud with a speed and voice picker* — **done in §35**.
  *dictionary provider choice* — **closed by §37** (Wiktionary stays; it now suggests
  spellings and reads the selected passage). Also closed: which part "the highlight pill
  selector in a pdf" means — §37 answered it (the dock after a selection, fixed above).
- **§32 — "continue and still the pdf reader buttom sheet close is weirdly slow ... do the motion token set and one arrival for every floating pill ... and in pdf reader, a high charge save turns on" (DONE, v439).** Built: the sheet close now travels its OWN height (the "weirdly slow" was 60%-of-screen travel on a 200dp sheet, not the clock — see `app/AGENTS.md` v439); the pill clock added to `CurioMotion` and spent across the reader's and journal's floating furniture; the back button is its own 50dp circle pill; press feedback on the reader's two control builders; and **low power reading** (`ReaderLook.lowPower`, on by default, a real "Power" row in the reader's settings — RGB_565 pages, a 1.5× upscale cap, `beyondViewportPageCount = 0`, and `cacheDir/book-images` pruned as the reader closes). **Also fixed the red build that was pushed as `147a2516`**: `PersonalPage.kt:722` had an orphan `else MaterialTheme.colorScheme.background` left by v438's edit — the file's own paper is `journalPaper()`. No SQL change for anything in either round.

- **§31 — the reader's polish round two (PARTLY DONE).** Done from it: back
  no longer exits the reader (`BackHandler`), the settings head wears the reader's top
  floor, the night dim covers the tools, the ⋯ tiles are a glyph-only capsule with the
  name outside it, an empty marks list is an em dash, one word gets the whole selection
  bar back, and the page slider has the foot to itself. **Committed as `2db57fc0` and
  deliberately NOT pushed** (the member's instruction). Not built: the motion-lock pill +
  removing the Zoom slider, the copy box's restyle to the dock's language, the motion
  token set / one-arrival / press-feedback passes, one empty state everywhere, and the
  two zoomed-gesture bugs. **(All of those landed in v439–v442.)**
- **§30 — the writing page always keeps a line to type in (DONE).** The member's rule from §28,
  implemented at the state level rather than inside the copy box: one `publish()` choke point and
  one `keepLineToTypeIn()` guard, so cut, the row tools and the gesture tools cannot empty a page.
  Known and accepted: undoing a full-page cut restores the rows and keeps the added line.
- **§29 — the ⋯ menu, the sheets' speed, and the reader's animations (DONE).** *"the 3 dot menu
  in pdf reader is bad like too huge and also the drop downs of each is slo, lie it takes a
  secdond to close, and also the highloght and notes dropd won is so small"* plus *"for the
  floating title in pdf reader use smooth fade animatuio n for search pil use merge and smoth
  morphe, for buttom sheet use proper animation fast animation, and more similiar pass smoth
  animattions"*. All shipped. **The one item carried forward: the copy box's cut-everything dead
  end (an empty line must always remain) — the member's rule, still unimplemented.**
- **§28 — the journal dock pass, the reader's scrubber, and the build (DONE).** Both red-build
  fixes (the swallowed `Column(` and the `@Composable` display-cutout getter); the dock's
  thickness and its two remaining dropdowns; leaf + crystal removed; quote/bullet shades split
  wider for the light page and the night; the bullet axis lifted to the words' centre; the
  journal's colour reaching the raised paper, the dock's tools and the date/title bar; the copy
  box as two rows with the dock stepping aside; the reader's scrubber as a floating pill; the
  blobatar idle motion; the SQL question (answered: no change). **Open: the copy box's
  cut-everything dead end.**
- **§27 — the portrait port, the root clutter, and the reader's head (done).**
  Asked permission before every deletion and confirmed the four decisions before editing.
  Four asks: remove the useless root node modules + manifest/doc clutter (done); use blobatar
  for the social portraits instead of the hand-drawn set (ported to Kotlin, verified against
  upstream, picker removed, all call sites migrated); the reader's search/back/title pill too
  close to the status bar (own top floor); separate search as a circle pill that merges into
  the header when opened (done). The journal dock is the member's next step, not this one.
- **§33 — the voice note's wave, and the failed CL (DONE, v439).** The member: *"make the
  wave and bar of the sound in vn more accurate depiction also fix the failed cl"*.
  **The CL:** the red run was `feat(profile,reading)` — two errors — then the one before it,
  a third: `LocalContext.current` (which is @Composable) read inside the blob door's plain
  `onClick`, plus `positionChanged()` unresolved in the reader (needs an import there) and
  `context` resolving to a function inside `ProfileDialogs`. All three fixed and pushed
  (`c3418e83`, `6194f2c6`). **Note for next time: the 6ff62b80 run proved the copy box,
  the gesture wear-in and the empty state all COMPILED — only that one line was wrong.**
  **The wave:** the inaccuracy was two layers deep — every bar normalized against 16-bit
  FULL SCALE and then squared (a real sentence drew at ~4% of the band), and `bucketLevels`
  dropping the tail of every drawing (42 columns from 72 samples = 36 real + six stale
  repeats). Both fixed at the source, plus `sqrt(peak·rms)` per bar, a linear reach, the
  live meter read against a decaying reference, the BEADS radius un-squared, and the
  decode loop's `MutableList<Short>` (sixteen million boxed samples per 3-minute note)
  replaced with a primitive sink. **Storage is unchanged: one hex byte a bar, no migration,
  no SQL.** See the v439 section in `app/AGENTS.md`.
- **§26 — the reader chrome pass (done).** Sheet scroll + swipe-close from anywhere; appearance
  as capsule/segmented controls with text size AND a PDF's zoom; the ⋯ menu as a six-capsule
  grid (Share / Gestures / Settings); the Gestures editor's eye, on-demand depth and
  tap-a-zone-to-select; zoomed long-press selection; the hold dock carrying the dictionary and
  Share; six new look settings, persisted. Design confirmed by four questions before any edit.
- **§25 — the double tap, and the tools' appear/disappear (done).**
- **§24 — the journal dock pass (done).**
- **§23 — the reader redesign + the vertical-PDF zoom (done).**
- (empty slot)
