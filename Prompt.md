# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "lets do some chnages with theme, so the backgroud glyph watermark make it very very
> subtle by default as its distracting, and in apperance option add an option to make it
> look like current deep look. bu tby default it will be very subtle"

Two clear halves, no ambiguity to ask about:

1. The page-wide category glyph collage behind every screen becomes **very very subtle by
   default** (it reads as distracting at full strength).
2. **Appearance** gains the option to bring back **today's deep look**. The default stays
   subtle.

The user asked for the settings option themselves, so the "new feature — toggleable?"
question of the root AGENTS.md was already answered by the request: it ships as a
user-facing Appearance choice with the subtle path as the default.

---

## 2. What was found

- **Exactly ONE component draws the page-wide glyph backdrop:**
  `app/src/main/java/com/curio/app/ui/components/CurioWatermarkBackdrop.kt` —
  `CurioWatermarkBackdrop(activeCat, topClearance, alphaScale)` scatters the 11 category
  glyphs, each tinted with its own category accent, and hands each glyph an alpha from a
  private `watermarkAlpha(active, isDark, pastel)` (dark: 0.22 active / 0.11 inactive,
  light: 0.30 / 0.15, pastel raised a step further). `alphaScale` is per-screen tuning —
  40+ call sites pass 0.45, Spin 0.5, Stats 0.40, the wide-window NavHost collage 0.55.
- **Not this backdrop, deliberately left alone:** `CurioMoodBoardBackdrop` (the seeded
  mood-board collage — board ART), the hero banners' mirrored watermark pairs
  (`heroWatermarkSymbols`), and the capture paper's `WATERMARK` paper style.
- **The preference pattern in `data/AppPreferences.kt`** is a private `KEY_*` const, one
  `var …State by mutableStateOf(default) private set` seeded in `initThemeMode(context)`,
  and an `is…Enabled` / `set…Enabled` pair — that is the shape every Appearance switch
  already uses (e.g. `darkGlowState`), so the new option follows it exactly.
- **The Appearance page is `AppearanceSection` in
  `features/settings/SettingsSectionScreen.kt`**, its rows are `CompactSwitchRow` /
  `CompactSegmentedRow` / `SettingsOptionRow` wrapped in `SettingsRowPulse(highlightKey == …)`,
  and the hub's deep-search index (`SettingsDeepIndex` in `SettingsHubScreen.kt`) carries
  one `SettingsDeepRow` per searchable row key.

---

## 3. What was decided

- **One multiplier, not new alphas.** `watermarkAlpha`'s current values ARE the deep look,
  so they stay untouched and the depth is applied as a scale factor in the composable:
  `depthScale = if (deep) 1f else GlyphBackdropSubtleScale` (0.32f), multiplied by the
  call site's own `alphaScale`. Every one of the 40+ call sites therefore keeps its own
  tuning and none has to know the toggle exists, and "Deep" is bit-for-bit today's look.
- **Reactive state, not a one-shot read.** The composable reads
  `AppPreferences.glyphBackdropDeepState` (a snapshot state seeded at startup), so flipping
  the switch repaints every screen's collage immediately.
- **A segmented row, not a switch.** "Subtle / Deep" shows which one is live and matches
  the Theme / Hero rows beside it; a switch labelled "Deep" would hide the default.

---

## 4. What was changed

- **`data/AppPreferences.kt`** — `KEY_GLYPH_BACKDROP_DEEP` (`glyph_backdrop_deep`, default
  OFF), `glyphBackdropDeepState` seeded in `initThemeMode`, and
  `isGlyphBackdropDeepEnabled` / `setGlyphBackdropDeepEnabled`.
- **`ui/components/CurioWatermarkBackdrop.kt`** — `internal const val
  GlyphBackdropSubtleScale = 0.32f`, the `depthScale` / `glyphScale` pair in the
  composable, and the scaled value handed to both the scattered and lower-band glyphs
  (signatures unchanged).
- **`features/settings/SettingsSectionScreen.kt`** — Appearance row `"Glyph backdrop"`
  (`CurioIcons.Wallpaper`, Subtle / Deep) under Pastel colors, with the hub search key
  `appearance-glyph-backdrop`.
- **`features/settings/SettingsHubScreen.kt`** — the deep-search index entry for that row.
- **`fastlane/metadata/android/en-US/changelogs/20260922.txt`** — one ADD line for the
  subtle default and the option.
- **`app/AGENTS.md`** — a durable "Background glyph backdrop" contract (one component, the
  multiplier, the subtle default, and the warning not to raise `watermarkAlpha` to
  compensate).

---

## 5. Still open (carried over, unchanged by this request)

- **The forms SQL has not been pasted into Supabase**, so the ban ladder, the queue's Clear,
  and publish / read / vote / skip for a form have nothing to talk to yet; the `forms`
  permission also still needs granting to the team.
- **Stop-the-poll in moderation** (asked for, not built) needs that schema first.
- **Journal entries being slow** still needs a measurement: list open, or page load.
- **Episode lists**: only 2 of the topics carry an authored `episodes` array; content, not
  code.
- **The deprecation sweep** (about 30 `rememberModalBottomSheetState` sites, 4
  `LocalClipboardManager`, 1 `LocalLifecycleOwner`, and roughly 150 site-specific warnings)
  is unstarted; each needs a compile to verify, and Material3 here is `1.5.0-alpha20`.
- **The take editor's per-keystroke rebuild** was found by inspection only and never
  profiled.
- **The subtle default is a judgement call on a number**: `GlyphBackdropSubtleScale =
  0.32f`. It is one constant in `CurioWatermarkBackdrop.kt` if the member wants it quieter
  or stronger after seeing it on a device.
- **`web/` still carries its own `CurioWatermarkBackdrop`** (React mirror). Out of scope per
  the root AGENTS.md scope rail (web/ is on hold) — it was NOT touched.

---

## User prompts

Status: this request is complete and pushed. No pending prompt below.

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and
its status stay at the top, and the empty slot below is where the next instruction lands. -->
