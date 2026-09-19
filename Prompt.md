# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "Also tone down the mood-board and hero-banner glyph watermarks so they match the new
> subtle backdrop"

The follow-up to the previous change (v407: the page glyph backdrop became very subtle by
default, with Appearance → "Glyph backdrop" → Subtle / Deep). Same intent, two more
surfaces: the mood-board collage and the torn heroes' glyph watermarks.

**One reading decision was made rather than asked:** the two surfaces ride the SAME
Depth switch (Subtle by default, "Deep" restores every glyph watermark in the app at
once) instead of getting their own independent alphas. Rationale: the member's first
request was "very subtle by default, with the option for the current deep look", and a
Deep that left the heroes and mood boards loud would be an inconsistent, half-set state.
If they want the heroes independent of the backdrop, it is one line per surface.

---

## 2. What was found

- **The mood board** is `CurioMoodBoardBackdrop` in the SAME file as the page backdrop
  (`ui/components/CurioWatermarkBackdrop.kt`). Its alphas are computed at draw time:
  `baseAlpha` (dark 0.12, light 0.16; pastel 0.18 / 0.26) × the seeded `alphaBoost`
  (0.85–1.2).
- **The torn-hero glyph watermarks are SEVEN identical private composables**, one per
  screen, each documented as "one mirrored watermark glyph … the banner's readable ink at
  a soft alpha", each ending in `tint = tint.copy(alpha = alpha)`:
  - `HomeScreen.HomeHeroSymbol` (quest hero)
  - `ProfileScreen.ProfileHeroSymbol`
  - `CabinetScreen.CabinetHeroSymbol`
  - `SettingsHubScreen.SettingsHeroSymbol`
  - `TopicHistoryScreen.HistoryHeroSymbol`
  - `OnboardingScreen.OnboardingHeroSymbol`
  - `EntryDetailScreen.HeroWatermarkGlyph` (saved-entry hero)
  Their pair tables (5 pairs each, mirrored, alpha 0.10–0.21) are per-screen and were
  deliberately NOT edited — the multiplier goes at the glyph's one tint line, so the
  tables keep documenting the deep values.
- **Two more banner-side marks found on the way:** the Spin picker's filter hero carries
  two lone watermark glyphs (`filterHeroInk` at 0.10 / 0.07), and
  `CurioTopicCard.MiniHeroWatermark` is, in its own doc, "a scaled-down version of the
  torn-hero watermark". Both ride the same switch.
- **Two card-ART marks deliberately left alone:** `TopicShareCard` (an exported card
  design) and the Spin deck ticket's single large 150dp category symbol (the ticket's own
  art, not a banner).
- Verified with `grep -n "biasX = 0.93f"`: exactly the seven hero tables above — no hero
  watermark was missed.

---

## 3. What was changed

- **`ui/components/CurioWatermarkBackdrop.kt`** — new shared `@Composable
  glyphWatermarkDepthScale()` (`1f` when Appearance → "Glyph backdrop" is Deep, else
  `GlyphBackdropSubtleScale = 0.32f`). `CurioWatermarkBackdrop` now reads it instead of
  inlining the pref, and `CurioMoodBoardBackdrop` multiplies its `baseAlpha` by it. One
  function now owns "how loud is glyph decoration", and the constant + pref stay in one
  file.
- **The seven hero composables** — `tint = tint.copy(alpha = alpha * glyphWatermarkDepthScale())`
  (plus its import in each file; `CurioTopicCard` needs none, same package).
- **`SpinScreen`** — the picker hero's two banner glyphs scaled the same way.
- **`CurioTopicCard`** — `MiniHeroGlyph` scaled the same way.
- **`app/AGENTS.md`** — the v407 section is now "Glyph watermarks — very subtle by
  default", naming every covered surface, the one knob, and the "never raise the base
  alphas to compensate" rule (the old text said the mood board and heroes were NOT this
  backdrop — stale as of this change).
- **Changelog** — the v407 ADD bullet now names the heroes, the mood board and the card
  headers instead of only the page backdrop.

Result at the default (Subtle): page backdrop ≈ 0.016–0.03 alpha, hero banners ≈
0.03–0.07, mood board ≈ 0.04–0.06 — all whispers; Deep restores every one of them to the
pre-v407 values exactly.

---

## 4. Still open (carried over, unchanged by this request)

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
- **The subtle factors are judgement calls on numbers**: `GlyphBackdropSubtleScale = 0.32f`
  for everything, deliberately one knob. If the heroes should read a touch stronger than
  the page backdrop, give the hero tint lines their own factor rather than touching
  `watermarkAlpha`.
- **`TopicShareCard` watermarks and the Spin deck ticket's category symbol are still at
  their old strength** — card art, not banner decoration, so they were left alone. Ask if
  those should quiet down too.
- **`web/` carries its own React `CurioWatermarkBackdrop`.** Out of scope per the root
  AGENTS.md scope rail (web/ is on hold) — NOT touched.

---

## User prompts

Status: this request is complete and pushed. No pending prompt below.

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and
its status stay at the top, and the empty slot below is where the next instruction lands. -->
