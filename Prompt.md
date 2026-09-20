# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "add some new accents colors, beautiful and also differnt omplimenting for dark ode. ne
> accent names" → "not just accent colors but also they are proper theme"

Confirmed with the member via `ask_user`: add **five named FULL themes — Jade, Orchid,
Ocean, Sand, Ember** — each with a **deep-jewel dark twin**, listed **in the existing
Color theme sheet**.

## 2. Findings

- The Color theme system is `AppPreferences.colorThemeState` + `curioColorScheme()`
  (Material → dark → White/Cream paper). The heroes and accent inks branch off
  `materialHeroTearsOn()` / `heroLaneCategory()` / `heroBlueState`.
- The retired Pantone themes had solved "a theme paints the whole app"; that machinery was
  removed in v414, so a named theme now needs: a scheme, hero/ink resolvers, the sheet
  rows, and a page that beats the category wash.

## 3. What was built (v420)

- **`ui/theme/NamedThemes.kt`** — `CurioNamedTheme` enum (Jade 158°, Orchid 318°, Ocean
  191°, Sand 36°, Ember 12°). Everything is `tone(hue, sat, light)`; `second` = hue+38°,
  `third` = hue−46° for the scheme's 2nd/3rd fills. `schemeFor(dark)`:
  - **light**: airy page `tone(hue,0.40,0.93)`, near-white card ladder 0.995→0.86 climbing
    above it, deep hero `tone(hue,0.50,0.40)`, near-black body ink `tone(hue,0.30,0.18)`;
  - **dark (deep jewel)**: page `tone(hue,0.45,0.12)`, cards 0.16→0.28, lit hero
    `tone(hue,0.60,0.56)`, pale ink. `readableOn` picks white/the theme ink; `contrast` is
    reused from `CurioTheme.kt`.
- **`AppPreferences`** — `NAMED_ID_PREFIX` (`named-`), five ids in `COLOR_THEMES`,
  `namedThemeId()` (no ui import in the data layer).
- **`CurioTheme`** — `activeNamedTheme()`; `curioColorScheme()` returns
  `named.schemeFor(isCurioDarkTheme())` before the paper flip; `curioRoseInk()` → `accentFor`.
- **Heroes/inks** — `settingsRoseAccent` / `homeRoseAccent` / `profileRoseAccent` →
  `primary`; their readable-ink helpers → `onPrimary`; `settingsCardAccentInk` →
  `accentFor`; `settingsCardChipTint` → `primary`.
- **`CategoryInk`** — the page/surface resolvers return the theme's own background/ladder
  (so the theme's page shows instead of a lane wash); lane ACCENTS are left intact.
- **Sheet** — `colorThemeChoices` appends the five rows (page/hero/ink previews),
  `colorThemeLabel` names them, and the deep-search hint / sheet subtitle were updated.

## 4. Verification

- Brace/paren balance 0/0 on all 9 touched/new files; `activeNamedTheme` referenced from
  Home/Profile/Settings/CategoryInk/CurioTheme.
- CI on the prior two commits was checked: both had failed on the single `contrast()`
  reference, fixed in `f353b28d` (moved into `CurioTheme.kt`).
- No Gradle in this environment — CI validates the compile.

## 5. Open notes

- Deep-jewel dark was the member's choice; the `tone(...)` calls in `darkScheme()` are the
  dials to re-tune (page sat/light, hero light).
- A named theme does NOT collapse the 36 lane accents (unlike the retired Pantone themes) —
  lane chips keep their identity on a themed page. Say the word to collapse them.
- `activeNamedThemeNow()` was not added: nothing non-composable needed it.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§5)
