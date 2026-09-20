# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> voice note: the play button doesn't match the wave look (it's a pill), also the number,
> also the progress drag of it; the highlight while it plays is bad — if the waves are too
> big it cuts from the top, and from the start it looks cut; and add some more voice note
> styles. Book detail: the progress pill teleports to where I touch and dragging does
> nothing (only tap works) — fix it; the "Your shelf" text isn't properly bigger and isn't
> properly aligned. Themes: the hero feels too deep in light mode (sand, ocean, ember,
> jade); in dark mode jade is worst, then orchid, ocean, sand, ember — not just the hero,
> everything feels off, and the hero is too light; the dark Curio rose is too vibrant,
> mute it; Ocean's background cards are perfect but some buttons and texts blend (Home's
> stat text and icon, Profile's stats); and remove the paper cream colour and its Paper
> option from the default rose and azure, introducing a soft silverish grey.

Confirmed with the member via `ask_user`:

- **Voice note looks are PICKABLE per note** (a control on the note itself), **and the
  recording capsule is restyled too**.
- **The play mark is hand-drawn in the wave's own ink**, and the clock goes with it.
- **Curio rose and Azure get the silver-grey cards**; Material / Adaptive Hero are
  untouched, and the Paper row stays for Adaptive Hero.
- **The dark twins become a deeper, calmer jewel** (not a neutral black).
- **"Your shelf" should be bigger and centred with the back button.**
- The styles ship as **the picker on the note only** — no Settings row.
- The looks are **different waves and different button styles**.

## 2. Findings

- The book gauge's drag died on `Modifier.pointerInput(onScrub)` **twice**: `ProgressCard`
  mints a fresh lambda every recomposition, so the first scrub restarted the
  `pointerInput` and cancelled the gesture in flight. Only the tap survived, because
  `detectTapGestures`' `onPress` seeks on the DOWN, before any recomposition — which is
  also why the fill "teleported".
- The voice wave was measured from the raw canvas: a peak could reach within 2% of the top
  with an 11%-of-height round-capped stroke drawn on it, and the first point sat at x = 0
  so its cap was sliced. That is both halves of the member's clipping report.
- The dark named hero (`tone(hue, 0.60, 0.56)`) could not carry a pale ink at all — at
  hue 158 it landed near 2:1, which is exactly the member's "jade is the worst, then
  orchid". `readableOn` tries white and then the DARK body ink (a BRIGHT tone in dark
  mode), so a dark-mode hero has to be deep enough for white on its own.
- `paperStatCardColor` blends the hero toward `#FFF6EB`, and the light named hero was a
  0.40-lightness slab beside the app's own pastel rose banner.

## 3. What was built (v421)

- **Voice note looks** — `PersonalVoiceStyle` (HAND / BARS / BUBBLE / MINIMAL) in
  `PersonalVoice.kt`, each pairing a wave drawing with a play treatment. Stored per
  recording in `PersonalBlock.audioStyle` (codec key `ast`, omitted at the default), so a
  read-only view draws what the editor drew. The picker is the Tune mark on the note in
  the editor, opening `PersonalVoiceStyleSheet` with live previews per look.
- **The clipping fix and the drawn control** — the pulse and the alt drawings now measure
  from a band inset by half a stroke (plus the depth pass's drop), so nothing can run off
  any edge; the play mark and the clock are drawn/ set in the wave's own ink (HAND and
  MINIMAL), while the disc looks keep a real disc.
- **`LiveVoiceWave`** — the recording capsule rolls the same history through the same
  drawing, wears a 22dp card instead of a pill, and sets its clock in Fraunces.
- **The gauge drag** — ONE `awaitEachGesture` keyed `Unit`, reading the callback through
  `rememberUpdatedState`, seeking from the first MOVEMENT (a press that never moves is
  still a tap, and seeks on release).
- **"Your shelf"** — one `headlineMedium` line, centred in a box exactly as tall as the
  rolled two-line slot (resolved in dp so a large font scale grows it).
- **The named themes** — light hero `tone(hue, 0.46, 0.66)` (airy, the theme's own deep
  ink); dark page `tone(hue, 0.36, 0.11)`, dark ladder `0.30` hold 0.15→0.27, dark hero
  `tone(hue, 0.50, 0.33)` with the pale ink, dark accent ink `tone(hue, 0.46, 0.78)`.
- **The dark Curio rose** — `HomeRosewoodDark` `#7D2C3B` → `#713842`, and the
  pastel-dark rose branch in all three hero resolvers takes a ~0.37 hold.
- **The silver page** — `CurioSilverLightScheme` (white page, silver-grey ladder) for
  Curio rose and Azure; the Appearance Paper row now exists only for light + Adaptive
  Hero.

## 4. Verification

- Brace/paren balance 0/0 across every touched Kotlin file (`PersonalVoice.kt` 216/216,
  `PersonalCanvas.kt` 646/646, `BookDetailScreen.kt` 370/370, `JournalListScreen.kt`
  80/80).
- Every `PersonalBlock(...)` construction checked for positional arguments before the new
  field was added — all named, so no call site broke.
- No Gradle in this environment — CI validates the compile.

## 5. Open notes

- The dark accent ink and the `hue`-derived tones were measured by hand for the five hues
  in the enum; if a sixth theme or a re-tune lands, re-measure with `contrast` (it is
  `internal` in `ui/theme`).
- `settingsAccentInk()` was deliberately left alone: it is shared with Curio rose, azure
  and Adaptive Hero, and its dark lift is derived from the hero, which the named themes
  now supply at a deeper value.
- The voice note's scrubber maps the finger across the WHOLE strip while the wave is drawn
  inside a half-stroke inset — under ~2% of the strip, no audible difference.

## 6. The request that followed (this session, drawer)

> "no in the drawer the pattern is good, now remove its background just keep the
> pattern, increase the size, make the dots of it solid filled. and tapping them
> the highlight is bad fix that too, and fix its colors in both white and dark
> mode just the drawer, and before pushing check the ci if its green then push or
> else just commit"

Confirmed with the member via `ask_user`:

- "increase the size" is **the whole pattern** (not just the dots), with the dots
  solid at their current size.
- The picked star's highlight should be **the star lighting up, no ring**.

## 7. What was built

- **The map's plate is gone** — `DrawerLaneStarMap` lost its
  `surfaceContainerHigh` fill, its 18dp clip and its corner radius, so the
  pattern is drawn straight on the drawer. Every tone (dust, hairlines, each
  star's three steps) now mixes from `page = MaterialTheme.colorScheme.surface`
  — the surface it actually sits on — which is also the colour fix in both modes:
  mixing toward a plate that is no longer there is what made the halos look like
  plate-tinted blobs.
- **The sky is taller** — `DrawerStarMapHeight` 254 → 320dp, because `starPoint`
  scales a polar radius by half the box's SHORTER side, so on a phone drawer the
  height is the only dial that can enlarge the pattern. `STAR_DUST_COUNT` 46 → 56
  with it.
- **Solid points** — an untouched lane is a solid dim dot; the hollow `Stroke`
  circle is gone (and with it the file's only use of `Stroke`).
- **The pick comes on** — the orbit ring is deleted. A picked lane's core grows
  1.5× and its halo steps brighten toward the lane's own accent; an unexplored
  lane lights too (at the smallest lit size), so a tap on a lane you have not
  started still answers.

## 8. Verification

- Brace/paren balance on `HomeScreen.kt` 411/411 and 1335/1335; no `panel`
  reference left outside comments; `Stroke` import removed (no longer used).
- No Gradle in this environment — CI validates the compile. The push for this
  change was gated on the previous run being green (member's instruction: "check
  the ci if its green then push or else just commit").

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — both requests are logged above as §1–§8)
