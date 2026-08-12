# Prompt.md — Request log

## Current request (v26 — Topic Browser header rebuild + back-to-top arrow)

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
