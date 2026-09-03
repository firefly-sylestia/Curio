# Prompt.md — current request log

## Request: v330 — share-card editor layout/editing UX + picker hold-action rewrite

User direction (paraphrased):

> Refine the share card editor: (1) position changes should be per-card —
> each design keeps its OWN layout, not one shared move applied to all
> styles; (2) during Customise, tapping OUTSIDE the selected box (empty
> card space, not another box) should auto-deselect; (3) be able to SWIPE
> between cards while editing; (4) the move grip still hovers over the
> text while typing the quick fact — fix it; (5) the floating Done/Reset
> over the card is in the way — put Done/Reset where Save/Share sit, move
> the content selector (share text / + custom / quick fact / no fact etc.)
> there too, and when Done is tapped the Save/Share/Share-as-text buttons
> come back. (6) The category picker tap-and-hold morph action is "totally
> bad and wrong" — still ~5× huge, collapses before 3 seconds — rewrite
> the logic fully with a new animation and new button style.

## Completed

1. **Per-design (per-style) layout edits.** The editor's single shared
   `move` became `movesByStyle: Map<ShareCardStyle, ShareCardMove>`
   (`TopicShareSheet`): a derived `move` for the current style + an
   `updateMove()` helper, and every pager page renders ITS OWN saved move
   (`pageMove = movesByStyle[styles[page]]`), so dragging the title on
   Paper no longer shoves the title on Vinyl. Persistence nests a per-style
   `moves` object under the topic (new `AppPreferences.saveShareCardEdits
   (context, topic, edit: JSONObject)`; the sheet builds the JSON);
   `loadShareCardEdits` parses it, with legacy flat saves (pre-per-style)
   falling back to one move applied to every style. Reset clears the whole
   map ("Reset all edits").
2. **Tap-outside auto-deselect.** `ArrangeableCard`'s edit overlay now
   lays a full-size, no-indication clickable Box FIRST (behind the element
   boxes): tapping empty card space clears focus + deselects
   (`selectedResizeTarget = NONE`); taps on title/fact/meta/badge still
   select that element; horizontal drags still reach the style pager
   (clickable children don't block the pager's requireUnconsumed=false
   scroll).
3. **Swipe between designs while editing.** Pager `userScrollEnabled` is
   now `!editMode || selectedResizeTarget == NONE` — swiping works while
   editing as long as nothing is selected (a selected element means the
   drag belongs to the move grip / typing field, so it locks the pager).
   Hint updated: "Tap a thing to select · swipe for another design".
4. **Grip no longer covers typed text.** The FACT `MoveHandle` hides while
   `factEditMode` is on (typing) and reappears when text editing ends.
5. **Bottom-bar editing.** The floating Edit-text/Reset/Done cluster over
   the card is GONE. While editing, the bottom action row (where
   Save/Share/Text live) becomes: content pills (Quick fact / No fact /
   saved sources / + Custom fact with an Add glyph) in a scrollable row,
   a Reset circle, and a primary **Done** pill. Done → editMode off →
   Save/Share/Share-as-text reappear. The Edit-text tool moved back into
   the toolbar row (shown only when the quick fact is selected); the
   Content tool panel kept only its extras (custom text field, chapter
   picker, photo, song) since the content pills now live in the bottom bar.
6. **Category picker hold menu rewritten** (`RadialHoldMenu.kt`). The
   gooey drag-to-pick radial ring (giant discs, blur goo, instant collapse
   on lift) is fully replaced by a compact icon+label pill menu:
   springs in (bouncy scale + fade) ABOVE the finger, clamped inside the
   sheet; STAYS OPEN after release (no more <1s collapse — the user's "3
   seconds" complaint) so the user taps an option to run it; tap the
   full-size scrim to dismiss; ~6s idle auto-dismiss as a safety net.
   `HoldSession` / `radialHoldMenu` / `HoldAction` / call-sites unchanged;
   the dead goo blob / ripple / ring helpers were removed.

## Verification

- `git diff --check` clean; brace/paren delta across the TopicShareCard.kt
  diff is 0 (whole-file string-strip regex is unreliable on this file due
  to string contents); RadialHoldMenu.kt balanced.
- Grep-verified all `move =` writes became `updateMove(...)` /
  `movesByStyle` writes (remaining matches are named args), the pager +
  single-style branches pass per-style moves, and no other callers of
  `saveShareCardEdits`/`loadShareCardEdits` exist besides the sheet.
- No Gradle commands run (project DOX forbids them here) — CI validates.

## Follow-up (same v330 batch) — mix colors + picker preselection + log
analysis

User: "fix the colors in your mixes its too saturated in light mode for red
ones, and also use wide variety of premium colors" + "in old picker page 1
when i tap multiple ones to start a mix why not use those selection and
when i tap the + icon it preselects them for new mix creation" + "analyse
this log fully for heating issues and app optimisation and backend and
potential crash on other device".

1. **Mix identity palette widened + de-saturated.** `MIX_IDENTITY_TONES`
   10 → 16 muted "premium" deep tones (oxblood, clay, cocoa, olive,
   forest, teal, petrol, navy, slate blue, slate, charcoal, bronze,
   plum, wine, mauve, ink). The old list's loud saturated reds (deep
   magenta `880E4F`, deep rust `BF360C`) read as harsh red blobs on the
   cream mix cards in light mode; every replacement keeps the deep
   tone + white glyph contrast (light) and lifts softly in dark
   (existing `namedMixIdentity` lerp).
2. **Classic-page selection flows into the + editor.** `ClassicPickerPage`
   now reports its pending multi-select lane IDs via `onMixStatus(count,
   ids, apply)`; `NewCategoryPickerSheet` hoists `page0MixSelection` and
   `MixEditorSheet` gained `initialSelection: Set<CategoryId>` — tapping
   + (Create a mix) opens the editor with those lanes pre-ticked.
   Browse-screen editor calls unchanged (default empty).
3. **Log analysis (logcat_2026-09-03_15-50-54.txt, ~2m16s session).**
   No crash / ANR / OOM / network errors in-session. Findings: (a) 548×
   `setRequestedFrameRate` from the obfuscated glass/backdrop render
   view (`df.a`/`p7.dispatchDraw` — kyant0 `backdrop` + Haze) — the
   glass layer redraws continuously even between touches → display
   never idles → heat/battery on every device; (b) 28 background GCs
   freeing 44–58MB each (heap peaked ~102MB) — heavy per-frame
   allocation tied to the constant redraws; (c) `Suspending all threads
   took 13.152ms` — a GC pause near the 16.6ms frame budget → jank on
   slower devices; (d) JIT compiling giant Compose methods (up to
   7.7MB per method) — baseline-profile opportunity (ProfileInstaller
   present but skipped); (e) benign noise: `vendor.perf.ems.egg` prop
   denials (Samsung), `Missing inline cache`, RemoteInputConnectionImpl
   IME warnings. Legacy glass snapshotter (the old 10MB/tick
   full-screen readback) is API 26–30 only and was NOT the cause here
   (device is API 36). Backend: session was fully offline — zero
   network activity; the only network (book-cover fetch) is opt-in and
   disk-cached. Recommendations: gate/throttle the glass backdrop
   refresh (don't re-blur when nothing changes), consider a baseline
   profile to kill the JIT compile spikes, keep the existing liquid-
   glass auto-disable for budget devices; the 13ms suspension + 100MB
   heap are the risk vectors for low-RAM devices.

## NEXT

Push this follow-up for CI. Then return to the signature-card campaign
(one category at a time, no SVG without permission) — awaiting the user's
pick + design for the next category.