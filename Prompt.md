# Prompt.md — current request log

## Request: Fix the 4-part XP-economy critique; re-read the DOX chain (previous commit had a Codebuff footer)

User supplied a 4-row critique table:

| Problem | Fix |
|---|---|
| XP economy is shallow | Named levels + rewards — unlock new pet outfits, new share-card palettes, custom lane order (give XP a reason) |
| Daily quests generic | Align quests with the real loop: "Reveal 2 new topics", "Save 1 discovery", "Capture with voice once" — show quest progress on Home |
| Pet games hidden in profile | Pet reacts on Home (nudges when quests complete) + quick "Play" bubble |
| No pet customisation payoff | Outfit shop funded by streak/quests (pure cosmetics, no IAP pressure) |

Plus: "read dox chain again ur previous commit had codebuff mention" — the
last several commits carried a `Generated with Codebuff` footer, which the
root AGENTS.md forbids. Committed clean this time (no footer); noted the
policy violation to the user.

## Decisions (ask_user)

- **Lane order:** GATE the Manage Categories drag-reorder behind the Level-5
  reward (hiding lanes stays open). Chosen over "keep open, list as milestone".
- **Outfit shop entry:** Quests + Profile (chosen over Pet-designer-only or
  Quests-only).

## What landed (all in `app/`)

1. **Level rewards catalog** — `data/LevelRewards.kt`: Reward(level, id,
   title, kind, glyph) with kinds OUTFIT / PALETTE / LANE_ORDER. Premium
   share-card tones (Midnight L2, Forest L8, Lavender L15, Ember L30),
   outfits (Scarf L3, Coat L10, Crown L20, Galaxy L40), lane order L5.
   Level card + pet hero footer show "next unlock"; level-up banner lists
   newly-unlocked rewards.
2. **Lane-order gate** — ManageCategoriesScreen: drag-reorder + steppers
   locked until Level 5 with a "Custom order locked" card; hide toggle
   unaffected.
3. **Real-loop dailies** — CurioQuests: DailyKind.REVEAL + DailyKind.VOICE;
   pool entries "Reveal 2 new topics", "Save a discovery", "Capture with
   voice once" (+ bonus Reveal-4 / Voice-twice); `noteReveal` hook wired in
   TopicRevealScreen next to CurioPassport.noteReveal; `onSave` bumps VOICE
   for SoundBite captures.
4. **Home quest strip** — HomeDailyStrip: the day's 3 core dailies with
   live progress bars under the quest block; tap → Quests.
5. **Pet reacts on Home** — CurioPet.pendingQuestNudge (set in
   noteQuestComplete, consumed once by the Home flower bed) → one-shot
   "Quest done! +sparkles ✨" bubble; quick PLAY bubble (awake + at home)
   → notePlay (feeds PLAY daily + persona).
6. **Outfit shop** — `data/PetOutfits.kt` (4 outfits, 16×16 accessory
   art), `features/outfits/OutfitShopScreen.kt` (wallet card, outfit cards
   with sprite previews, Buy/Equip/Locked, next-unlock hint), route
   `OUTFIT_SHOP` (NavHost + center-pop list). Sparkles wallet in
   AppPreferences (KEY_SPARKLES + get/add/spend) funded by daily claims
   (+2), weekly claims (+5), streak milestones (+5). Equipped outfit
   overlays the sprite's `accessories` detail layer at render time
   (CurioPetSprite.activeDesign merge; never mutates saved art).
7. **Entries** — Quests: OutfitShopEntryCard under the hero; Profile:
   ProfilePetShopRow after the gamification card.
8. **Share palettes** — 4 premium tones in TopicShareCard; `paletteFor`
   cycles the player's AVAILABLE tones (base 4 + level unlocks) via
   `unlockedToneCount(level)`.

## Verification

- All 16 touched/new files brace/paren-balanced vs HEAD (apostrophes in
  comments confound naive scanners; compared against HEAD deltas).
- Icon glyphs verified against the bundled font subset (only existing
  constants used — `checkroom`/`forest`/`swap_vert`/`lock`/`local_florist`
  were tofu; replaced with pets/drag_handle/dark_mode).
- Imports audited per file (LevelRewards/PetOutfits where used); no unused
  imports added (removed a dead `mood`/`stage` val and an unused
  `paletteUnlockLevel` helper).
- Compile-safety rules followed: no state writes during composition
  (consumeQuestNudge happens in a LaunchedEffect), `size` never shadows
  DrawScope, imports match referenced types.
- CI compiles on push; local Gradle is forbidden in this environment.

## Docs

- `fastlane/metadata/android/en-US/changelogs/20260921.txt` — new bullets
  on top (levels unlock, outfit shop, real-loop dailies, Home pet).
- `app/AGENTS.md` — v321 bullet (rewards catalog, lane gate, real-loop
  dailies, Home strip + pet nudge/Play, sparkles + outfit shop).
- Commit WITHOUT the Codebuff footer this time (root AGENTS.md policy).