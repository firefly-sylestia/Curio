# Curio app audit — findings, fixes, and what is still unverified

**Owner:** `app/` (Android). Companion contracts: `app/AGENTS.md`, root `AGENTS.md`.
**Scope of this document:** the defects and perf costs a read-through of the app
turned up, what was done about each, and — deliberately — what was *not*
checked. Anything marked **UNVERIFIED** is a lead, not a finding: it was seen
while reading, not reproduced on a device.

Written across v453 (the read-through) and v454 (the Lite-mode pass). Numbers
are re-checkable with the exact command noted beside them.

---

## 1 · Fixed

### Ink measured, never assumed — the chat bubble (v453)

**Where:** `features/community/DirectMessageScreen.kt`.
**What:** v412 established the rule "a fill's ink must be measured, never
assumed". It had been applied to *your* bubble only: a **received** message
took plain `onSurface` whatever its fill was, and its quote, timestamp and
"edited" mark went further down the alpha ramp. On a theme whose container
ladder carries colour, that is exactly how text lands close enough to its
background to blend.
**Fix:** one `bubbleFill` and one measured `bubbleInk` that every piece of text
in the bubble reads, both sides; dark mode's received fill nudged a hair toward
the ink with an opaque blend.
**Found while fixing it:** the **"edited" label was a hardcoded `Color.White`**
— the precise bug v412 existed to delete, surviving at the one call site nobody
re-read.

### `EditQuietAction` — a translucent fill cannot take a clean shadow (v453)

**Where:** `features/profile/ProfileEditScreen.kt`.
**What:** the edit-profile photo actions (Change photo / Use a photo) were
translucent fills with a shadow. A translucent fill lets the blur read *through*
it, so the "elevation" arrived as a smudge rather than a lift — the same class
of bug as root `AGENTS.md` COMPILE-SAFETY rule 11 (opaque fills only for
`shadowElevation`).
**Fix:** opaque fill, 2dp lift, true capsules, each taking an equal half of the
row; the camera disc on the avatar corner lifted to match.

### Journals — controls that had to be discovered by tapping (v453)

**Where:** `features/personal/JournalListScreen.kt`.
**What:** three unlabelled horizontally-scrolling filter rows; a colour filter
the member asked to remove.
**Fix:** the colour filter and its state are gone; the two remaining rows are
labelled **Felt** and **Length**.

### The dictionary's Offline door answered nothing (v452)

**Where:** `features/personal/BookReaderScreen.kt`, `ReaderOfflineDictionary.kt`.
**What:** the badge you reached for was the *abridged* Webster's alone, so
standing on it could answer nothing while Modern and Full 1913 answered.
**Fix:** the door owns every volume on the phone and searches best-first
(WordNet → complete 1913). Each volume keeps its own download, progress and
Remove. Three rules the merge forced: a badge dims only when **none** of its
volumes is present; removing one volume keeps you on Offline while any other
remains; and *no volume here* (`null`) stays distinct from *no such word*
(`emptyList`, the miss path with spelling suggestions).
**v457 update:** the abridged Webster's door was later REMOVED (the same
public-domain 1913 text as the full edition in a lighter conversion, so the two
rows read as one dictionary twice) — the door is `MODERN, FULL` now and
`purgeRetired` deletes a phone's copy of the retired file.

### The "wide" look that rotated back (v448)

**What:** not a layout bug — a race. The rotation rebuilds the reader, the
rebuild re-read the store, and the look's save was debounced 400ms, so the store
still held the value just replaced.
**Fix:** the look store loads once per process and an orientation choice is
written the moment it is applied.

### `HomeScreen.kt:3035 Unresolved reference 'open'` (v448)

A parameter named `open` collides with the `open` **modifier keyword**: Kotlin
read the *value* as a modifier. The flag is `drawerOpen` everywhere now.

### Always-running clocks that woke a frame for nothing (v454)

**Where:** `ui/components/CurioScrollIndicator.kt`.
**What:** the knob's frame-paced drain loop (`withFrameNanos`) ran for as long
as the indicator existed, waking every frame to read the scroll state and find
`pendingDelta == 0` — 60 wake-ups a second on an **idle** knob, on every screen
with a scroll rail.
**Fix:** the loop parks on `snapshotFlow { pendingDelta }.first { it != 0f }`
and resumes only when a drag actually put something there. One frame per
gesture, not one per frame forever. (It also drops a stale accumulation when the
list is not scrollable, instead of applying it later as a jump.)

---

## 2 · Lite mode (v454)

One opt-in switch in **Appearance → Lite mode** (`AppPreferences.liteModeState`,
key `lite_mode_v1`, default **OFF**). It is a *performance* profile, not a
style: layouts, colours and state are untouched, and every preference it holds
down (the glass switch included) comes back exactly as it was when it goes off.

**The rule for gating, which is the whole design of it:**

- **Gate** motion that only exists to be looked at — ambient clocks (shimmer,
  twinkle, breathe, idle wobble) and refraction/blur passes.
- **Never gate** motion that carries meaning — a screen transition, a sheet
  opening, a press, a state change, a progress bar answering a real action, a
  page turn. Freezing those does not make the app cheaper to use, it makes it
  feel broken, which is what Lite mode must not do.

**How it is wired:** one file, `ui/theme/CurioLiteMode.kt`, exposes
`isLiteMode`, `isAmbientMotionOn` and `rememberAmbientTransition(label)`
(the ambient clock that returns `null` when parked).

- **Glass** — gated in **one** place, `isLiquidGlassRequested()` in
  `ui/components/LiquidGlassPills.kt`: the single question all ~33 glass sites
  already ask. Only the *pass* is skipped, never the pill: each site's
  non-glass branch is the solid elevated fill it had before glass existed, so
  nothing vanishes and no layout moves.
- **Ambient clocks** — the shared helpers (`rememberBreathingScale`,
  `rememberShimmerBrush`, `rememberRotatingReveal`, `rememberPulseScale`,
  `CurioSkeleton`'s shimmer sweep) hold still; so do the screens' own: the
  splash logo, the drawer's star-map twinkle, the Incursion mark, Spin's idle
  die, and the pet sprite's *flourishes* (breath, glance, ear flick — the body
  bob and blink stay, so the pet still reads as alive).
- **Left running on purpose:** Spin's orbit (it reports a shuffle in progress),
  its shuffle glyph and opening pulse, every transition, the reader's page turn
  and the recording pulses — all state, not ambience.

---

## 3 · UNVERIFIED — leads, not findings

Each of these was seen while reading and could not be confirmed here (no
device, no build in this environment). Check before acting.

1. **Touch targets under 48dp.** Not counted. Method:
   `grep -rn "size(\(3[0-9]\|4[0-7]\)\.dp)" --include=*.kt .` then confirm each
   hit is an icon *inside* a ≥48dp clickable rather than the clickable itself.
2. **Icon-only buttons with no description.** `CurioIcon(` with a `null`
   description appears **134** times
   (`grep -rn "CurioIcon(" --include=*.kt . | grep -c "null"`). Most are
   decorative inside a labelled row, which is correct; the ones that matter are
   icon-only *actions*. Not separated yet.
3. **`Surface(onClick)` with no pressed state.** 12 sites
   (`grep -rn "Surface(" --include=*.kt . | grep -c "onClick"`). Not checked
   for a press indication.
4. **Hardcoded `Color.White` in themed surfaces.** **452** occurrences
   (`grep -rn "Color.White" --include=*.kt . | wc -l`). A sample read says the
   bulk are the share-card and waveform renderers where white-on-photo is
   deliberate, and photo overlays generally — so this is **not** a mass
   problem, but the class exists, and after v453/454 the remaining instances
   inside a themed surface are the ones nobody has re-read one at a time.
5. **The reader keeps its glass when Appearance → Liquid glass is OFF.**
   Reported by the member; left as-is on their word ("no no the liquid glass is
   fine"). Worth deciding once: is the reader's glass part of the reader's own
   identity, or should it honour the app-wide switch?
6. **`while (true)` polling loops.** 66 matches. Read at the time of the pass:
   they are scoped to visible composables (`LaunchedEffect` cancels on leave)
   and most tick at 1s or slower. Not exhaustively timed.

---

## 4 · Not audited at all

Said plainly so nobody assumes coverage: accessibility (screen reader) passes,
RTL behaviour, every screen's empty/error states, the Community realtime
under flaky networks, battery while the reader is foregrounded for an hour, and
the desktop/web ports (out of scope, see root `AGENTS.md`).
