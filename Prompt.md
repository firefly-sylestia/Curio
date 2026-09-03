# Prompt.md — current request log

## Request: Level unlocks invisible, more unlocks + pet-shop games, quest fixes, picker hold-action redesign

User report (condensed):

> (1) The level-unlocked card gradients/tones I can't see anywhere — where
> are they? Add more level unlocks, and add more things to the pet shop for
> the small pet — maybe some games, ball games etc. (2) The quests on the
> Home screen are bad — remove it. (3) In the Quests screen it says "quest
> screen unlocked" even when completed — fix quest issues. (4) The "Try a
> new lane" daily keeps making you explore a new lane and never completes
> with the lane it said to explore — fix it.
>
> Also: rebuild the category picker's tap-and-hold action as a fluid,
> water-like radial menu (the user pasted a React `RadialHoldMenu` example:
> gooey blobs welling out of the press point, morphing into a ring of glass
> discs around the thumb, drag-to-switch with nearest-highlight, release to
> pick, NO dark overlay, buttons open around the press point).

## Decisions (asked the user)

- **Tones visible:** tone picker in the share-card editor (swatches of every
  unlocked tone + Auto) — unlocks become visible/pickable.
- **More unlocks:** 6 new share-card tones + pet-game unlocks (RewardKind.GAME).
- **Pet shop:** more accessories + Ball/fetch + other mini-games.
- **Home quests:** remove the "Today's quests" strip.

## Root causes found

1. **Tones invisible** — the 4 premium tones (Midnight/Forest/Lavender/
   Ember) were auto-applied to share cards by category rotation; there was
   no UI to see or choose them, so unlocking one changed nothing visible.
2. **"Try a new lane" never completes** — the discovery daily resolved its
   target lane LIVE via `CurioPassport.leastEngaged()` at both render and
   completion; exploring the displayed lane changed its stamp, so by
   completion time `leastEngaged()` pointed at a different lane.
3. **"Bonus quests unlocked!" lingered** — the line stayed visible once the
   bonus quests themselves were claimed.
4. Home quest strip duplication — quests already live on the Quests screen.

## Completed

- **Home** (`HomeScreen.kt`): `HomeDailyStrip` + its "Today's quests" strip
  removed (quests live on the Quests screen only).
- **Quests** (`QuestsScreen.kt`): the gold "Bonus quests unlocked!" line now
  hides once BOTH bonus quests are claimed (`bonus.any { it.id !in awarded }`).
- **Try-a-new-lane** (`CurioQuests.kt`): the discovery daily PINNS its lane
  at the 4 AM rollover — new `KEY_DAILY_LANE` / `dailyLaneState`
  (stored as `CategoryId.name`) + `dailyDiscoveryLane()` (defensive
  runCatching resolution); both the daily pool and the completion hooks use
  the pinned lane, so the lane shown is always the lane that completes it.
- **Tone picker + tones** (`TopicShareCard.kt`, `LevelRewards.kt`):
  `ShareCardPalette.name`, six new curated tones (Ocean 12, Rose Gold 18,
  Moss 25, Storm 35, Pearl 45, Sunburst 50) with matching LevelRewards
  entries; new **Tone** tool pill in the editor opens a scrollable swatch
  row of every UNLOCKED tone (+ Auto rotation); `TopicShareCard.toneIndex`
  threads the pick into the live preview AND the Save/Share exports.
- **Pet shop** (`PetOutfits.kt`, `AppPreferences.kt`, `OutfitShopScreen.kt`):
  4 new outfits (Polka Bowtie, Sun Hat, Curio Glasses, Tail Puff); new
  `PetOutfits.Games` catalog (Ball fetch, Star catch, Bubble storm — level-
  + reward-gated, sparkle-priced) with `ownedGamesState` /
  `getOwnedGames` / `buyGame`; the shop gained a "Toys & games" section
  (Locked → Buy → Play, Play fires `CurioPet.notePlay` + a confirm haptic).
  Game glyphs verified present in the Material Symbols subset.
- **Radial hold menu** (`features/picker/RadialHoldMenu.kt` NEW +
  `NewCategoryPicker.kt` + `NewCategoryPickerBrowse.kt`): the picker's
  tap-and-hold actions are now a gooey radial menu — blobs well out of the
  press point (blur+alpha-contrast RenderEffect goo on API 31+), settle
  into a ring of crisp glass discs around the thumb, live drag-to-switch
  (nearest-disc highlight with hit slop), release over a disc to pick,
  release over nothing to cancel, no dark scrim. Gesture =
  `Modifier.radialHoldMenu(HoldSession(onOpen/onMove/onEnd/onTap))` on
  `NewPickerTile` / `NewPinnedPill` / `NewMixCard` / Continue-exploring
  tiles / Browse tabs + mix rows; visuals = `RadialHoldMenuOverlay` at the
  held anchor, fed by shared `holdCursor`/`holdEnd` sheet state.
  `CategoryOptionPill`/`MixOptionPill` became `internal` (shared with
  Browse) and render the overlay. `NewPickerTile` kept a plain `onLongClick`
  for the classic page's hold-to-multi-select. The old `HoldActionsPill` is
  dead code (no references). All new glyphs (palette, play_circle, star,
  bubble_chart…) verified in the icons font.
- Docs: changelog (6 new bullets), `app/AGENTS.md` (v323 bullet), this file.

## Verification

- `git diff --check` passes; all edited regions re-read for compile-safety
  (imports present, signatures match, nullability resolved — e.g.
  `CurioCategory.id.name` not `.name`, `byId` wrapped in runCatching).
- No Gradle compile/build/lint/test command was run because the project DOX
  explicitly forbids Android Gradle validation in this environment; CI is
  the verification path.
- `ANALYSIS.md` remains untracked/untouched; `share-card-improvements.txt`
  (stale first-request scratch notes) was restored after an accidental
  uncommitted deletion.