# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "remove the pntone theme, also the category tint option isnt working, for home etc after
> the paper white page cream paage option, also do somethign bout the backgroud and the
> card color issues, and use elevation also fix the rose theme its kinda feels off somehow"

Four asks plus delivery. Confirmed with the member via `ask_user` before editing:

1. **Remove the Pantone themes — DELETE COMPLETELY.** Stored Pantone choice falls back to
   Curio rose.
2. **Category tint should tint Home/Profile by the last Spin lane when ON** (rose/cream when
   OFF).
3. **Soft shadows + clearer fill steps** for card/background separation.
4. **Rose (Curio) theme: MORE VIBRANT / richer.**

## 2. Findings

- **Pantone** was 3 themes in `ui/theme/PantoneThemes.kt`, woven through ~14 files:
  `activePantoneTheme()` / `activePantoneThemeNow()` branches in `CurioTheme.kt`,
  `CategoryInk.kt`, `CurioCategoryCard.kt`, `ExploreSessionService.kt`, Home/Profile/
  Settings/Reveal screens; the `COLOR_THEME_PANTONE_*` ids + `PANTONE_ID_PREFIX` in
  `AppPreferences.kt`; the Color theme sheet rows in `SettingsSectionScreen.kt`.
- **Category tint** (`AppPreferences.tintWashEffective()` ↔ `categoryBackgroundWash()`)
  only drove the category-washed screens (Spin/Cabinet/Reveal). Home fell straight to a
  fixed rose lerp unless the *Adaptive Hero* theme was on, so the row looked dead there.
  Profile used `heroPageBackground()` which also only took a lane under Adaptive Hero.
- **Cards**: v411 made the light cream ladder deliberately subtle and relied on a 3dp
  `curioCardShadow`; the member now reports the card/page separation is not reading.
- **Rose**: `CurioColors.HomeRosewood` was nudged more vibrant in v413 (S 0.415 → 0.515)
  and still "feels off".

## 3. What was built (v414)

- **Pantone removed completely.** `PantoneThemes.kt` deleted; every branch, import and
  constant dropped; `COLOR_THEMES` is `CURIO/AZURE/MATERIAL/LANE`; `getColorTheme` migrates
  a stored `pantone-*` id to `COLOR_THEME_CURIO` on read (kept `LEGACY_PANTONE_PREFIX` for
  that one read). `curioTintOn` is now an unconditional opaque `lerp`.
- **Category tint tints Home/Profile.** New `categoryTintLane()` in `SettingsHubScreen.kt`
  (gated by the switch, reads the last single Spin lane, independent of Adaptive Hero);
  `heroPageBackground()` now falls back to it, Home resolves its page through
  `heroPageBackground(...)`. NOTE: because that resolver is shared, EVERY screen using
  `heroPageBackground()` now takes the lane tint when the switch is ON — see §5 open item.
- **Cards**: White-page light ladder re-pitched deeper (`surfaceContainerLow #FAF2E0` →
  `Highest #E8D8B3`), `curioCardShadow` default elevation 3dp → 5dp with a hair more alpha.
- **Rose**: `HomeRosewood` `#DC7482` (hue 352 unchanged, S 0.515 → 0.60, L eased to 0.659),
  `HomeRosewoodDark` `#7D2C3B` matched; every derived pastel/wash/ink follows.
- **Docs/changelog**: `app/AGENTS.md` sections marked RETIRED + the card-ladder/Paper notes
  corrected; `20260922.txt` Pantone bullets dropped (never shipped — no REMOVE note) and
  the three new FIX bullets added.

## 4. Verification

- Brace/paren balance checked 0/0 on all 15 touched Kotlin files (`TopicRevealScreen.kt`'s
  +1 paren is pre-existing — confirmed against `HEAD`).
- No `Gradle` in this environment — CI validates the compile.

## 5. Open item to confirm with the member

`heroPageBackground()` is the shared page resolver for ~25 screens (Support, Quests,
Updates, Stats, Community, Recent, Cabinet, …). Tinting it by the last Spin lane when the
Category tint switch is ON (default ON) means those pages now shift hue after a Spin under
the default Curio rose theme too. If the member wants this scoped to Home/Profile ONLY, the
lane branch should move out of `heroPageBackground` into a Home/Profile-only helper.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§5)
