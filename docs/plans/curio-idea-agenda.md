# Curio — the idea agenda

A durable backlog of feature ideas for the Android app, each written down with
*why it exists*, *how it should feel*, and *where it hooks into the code*, so a
later session can build one without re-deriving the reasoning.

It is a plan, not a contract. `app/AGENTS.md` remains the binding contract for
anything it describes once built.

---

## 1. The evidence this agenda is built on

Researched 2026-09-27 before any idea was written (see the sources at the foot).

1. **Gamification only helps learning when it drives retrieval, not activity.**
   Pooled effects across education are moderate-to-large (Hedges' g ≈ 0.82) but
   with wide variance, and the reviews agree the benefit is *motivation-mediated*:
   game elements pay off when the practice they push people into is evidence-based
   — retrieval practice, spacing, cumulative recall — and not when they optimise
   XP or minutes. Duolingo's own published analytics show the mechanism is
   persistence (a 7-day streak ⇒ 2.4× more likely to use the app next day, 3.6×
   more likely to complete a course), and Duolingo itself has moved its success
   metric toward "Time Spent Learning Well" precisely because time spent is a poor
   proxy for progress.
2. **The three failure modes are all misalignment, and they are named:**
   *metric drift* (rewarding what is easy to measure), *attention drift* (fixation
   on points/badges pulls attention off the goal), *competition drift*
   (leaderboards amplify variance — they motivate some and demoralise others).
   **Practical consequence for Curio: keep progress private and self-referential;
   never add a leaderboard, and never let XP be earned by the cheapest action.**
3. **The 2026 interaction patterns that actually shipped:** compound gestures
   paired with **haptic feedback** ("a gesture without haptics is a guess");
   **contextual gesture discovery** — teach the gesture at the moment it is
   needed, then fade the hint (Superhuman's inline-shortcut pattern); feedback
   that *replaces* screens (audible / haptic / temporal); bottom-sheet-first,
   thumb-first surfaces; adaptive layouts that surface the most common action
   first.

## 2. The gap that gap exposes in Curio

- `CurioQuests` awards XP for **actions** — spin (+2), explore (+5), save (+10),
  pin/quote (+3). **No XP is tied to recall.** That is metric drift by the
  literature's own definition: the app rewards having done things and never asks
  whether you still remember them.
- `CurioPassport` is a real model (per-lane `Stamp` = UNSEEN → PEEKED →
  EXPLORED → MASTERED, plus `leastEngaged()`) whose only surface is badges inside
  Quests.
- `BrainStats.brainProfile()` is a genuinely science-mapped six-dimension model
  (knowledge / memory / expression / focus / …) with **no interaction** — it is
  six meters and one tip.
- The drawer sky hides three link styles behind a **hold nobody is told about**.
- The app has **no sound layer at all** (only TTS and media playback).

## 3. The ideas

### 3.1 The Return — spaced resurfacing of completed topics ★ recommended first

**What.** A topic you Completed comes back on a widening schedule (1d → 3d →
7d → 21d → 60d) as **one** card: the old capture fades in behind it, and a single
prompt — *"You explored Bioluminescence three weeks ago. What do you still
remember?"* A line you type is appended to that entry as a recall block and the
card is scheduled further out.

**Why.** This is the retrieval-practice + spacing mechanism the research says
gamification must be tied to. It also converts the streak from "days you spun"
(the metric-drift reading) into "things you still know".

**The interaction.** It arrives like a letter and opens with the app's existing
paper-tear language; one soft haptic knock; the old entry's photo fades up behind
the writing line. Answering is one line, never a form.

**Where it hooks in.** `ExploreSession`/`CaptureRepository` already know a topic
was completed; `CurioQuests` owns XP; `TopicHistory`'s Completed list is the
record; the paper editor already exists (`PersonalCanvas`/`RichTextEditor`).
Needs: a small persisted due-date table (Room migration or its own prefs file
like `CurioPassport`), and one new card surface.

**Open.** Whether a recall earns XP (my view: yes, and more than a spin), and
whether a recall can be skipped without penalty (my view: yes, "not now" pushes
it a day, no streak loss).

### 3.2 Gesture language + haptic words + gesture discovery

**What.** One `CurioHaptics` vocabulary — tick / bloom / knock / thud / confirm —
so every gesture has a distinct signature, and **gesture discovery**: the first
time a hidden gesture is available, a faint inline hint appears; once used, the
hint never returns.

**Why.** The researched pattern (haptics as confirmation; progressive disclosure
of gestures). It also fixes a live defect: the drawer's hold-to-cycle-line-style
is undiscoverable.

**Where it hooks in.** Haptics today are ad hoc per screen
(`HapticFeedbackType.TextHandleMove` etc.); a vocabulary belongs beside
`CurioMotion`'s clock in the design system. Discovery is a small
persisted "gesture seen" set + one hint composable.

### 3.3 The Passport as a real object ★ best "fun" one

**What.** A page you flip through. A lane gets a **hand-drawn stamp** the day it
is MASTERED, the page keeps the date and the first line you wrote there, and a
**boarding pass** tears off for a random barely-touched lane.

**Why.** The Passport's own header says its job is to push into barely-touched
categories; today that is a badge row. Collector energy with no leaderboard.

**Where it hooks in.** `CurioPassport` (stamps already exist), `QuestsScreen`'s
stamp rendering, `StatsRange`/`laneGridItems`. New page + route.

### 3.4 The reveal as one continuous paper object

**What.** Today spin and reveal are two screens. The deck card **slides out and
unfolds** into the topic page — one sheet of paper, one gesture.

**Why.** Cinematic continuity; it removes a screen transition the member has
never been asked about.

**Where it hooks in.** `SpinScreen`/`TopicRevealScreen`'s existing shared-element
work. Highest motion risk of the set — do it with the animation tokens, never a
literal duration.

### 3.5 Time-capsule notes

**What.** Write a note on a topic; Curio seals it (wax seal, envelope) and
returns it in three months.

**Why.** Spaced recall wearing a different costume, and it gives journaling a
payoff without a notification.

**Where it hooks in.** `PersonalNotes` + the debounced save already exist; needs
a "sealed until" field and one delivery surface.

### 3.6 A small optional sound layer

**What.** Off by default: a paper rustle on a reader page turn, a stamp press, a
chime when an entry is filed.

**Why.** Researched pattern — feedback that replaces screens. Also the app's
only audio today is speech and media playback.

**Where it hooks in.** A new `CurioSounds` object; the app owns no audio assets
beyond TTS, so this is asset work as much as code.

### 3.7 The pet as a recall partner

**What.** `CurioPetBrain` already learns traits and coins catchphrases; let it be
the one that asks about your topics — "you saved the frog one — what was it?"

**Why.** The same retrieval mechanism delivered by a character; the pet's brain
is already grounded in real local stats.

**Where it hooks in.** `CurioPetBrain.say()` + the `### 3.1` due table.

### 3.8 "Your Curiosity" — the same science, made interactive ★ asked for

**What.** The Stats page's `YOUR BRAIN` card keeps `BrainStats`'s six
science-mapped dimensions **and every fact it states**; what changes is that the
page stops being six meters and one tip.

**Why.** The member's own note: *"the your curiosity page, we can change that
keeping the science knowledge facts"*.

**Shape (to be designed with the member).** Options on the table: each dimension
as something you can touch and that answers (a dimension you tap shows the
evidence behind it — which real actions fed it); the six drawn as a radar/constellation
that animates as it is earned; the weakest dimension becoming a *door* into the
action that raises it (one tap → the deck filtered to that kind of practice).
Nothing here removes a dimension or a tip.

**Where it hooks in.** `features/stats/StatsScreen.kt` (`YourBrainCard`),
`data/BrainStats.kt` (`brainProfile`, `LaneKnowledge`), `StatsRange`.

## 4. Decisions taken (2026-09-27, from the member)

*(See also §5 for the second round's decisions — the four manipulation ideas, the
deck-LOOK-untouched rule, and "full physical".)*

- Build **all** of the ideas above; **save them first**, then take them in order.
- **The Return lives in Home's quest hero.** Home's existing "Today's quest /
  Shuffle the deck" card **cycles** between the quest and a due recall — both
  kept, alternating (and on the first day of a recall, the recall leads).
- **New measures ship behind a Settings toggle, ON by default** (the project's
  standing rule for a new measure), with the toggle removed once the behaviour
  has settled.
- **No leaderboards, no public comparison** — per the research.

## 5. The second round — ideas you MANIPULATE (2026-09-27)

The first round of "new ideas" offered after the agenda (a broadsheet of your
week, a launcher widget, an audio stream, a gyro depth pass, a thread between
entries, a reader pencil margin) was **rejected**: *"bad ones not good the ideas
are bad, i said interactive thing visually interactive beautiful"*. The reading
of that, and it is the rule for everything below: **every one of those was a
screen you READ.** An idea earns its place by being an object with **mass,
travel and a reaction under the thumb** — it moves because the member moved it,
and you can see how far it has to go.

Six were offered on that rule; **four were picked, in this order.** All four ship
behind a Settings toggle, ON by default, per the standing rule for a new measure.
The deck's LOOK is untouched by the first one.

### 5.1 The riffle (BUILT, v490) ★

Spin's fan is thumbed like a real deck: drag it sideways and the whole fan slides
and leans in the hand, one card passes per 48dp of thumb travel (the OLD swipe
threshold, so a short swipe still deals exactly one), and the release's own
velocity flings it on before the spring settles it back with a small overshoot.
`AppPreferences.riffleDeckState`, **Deck** section in both experiments screens.
Full description and the traps are in `app/AGENTS.md`'s v490 section.

**The Spin button's riffle (v490, same switch).** The member's follow-up — *"the
Spin button should riffle the deck visibly when pressed"* — is built: the fan
pumps once per card the wheel deals, the pump grows as the wheel slows, and it
comes home with an overshoot. The reel turns `cycleIndex`, which `Carousel`
already receives, so the pump needed **no new plumbing**; the return is a tween
matched to the reel's interval (a spring would be interrupted on every tick) and
the overshoot is saved for the reel's end. Off = the Spin button exactly as it
was.

### 5.2 Tear it off the page — the reveal's save (BUILT, v494)

**Built as a SECOND way to do the Completed star's write, not a replacement.** The
removal warning below was held to: the star pill stays exactly what it was, the
strip's tear makes the star's exact two calls, and "Torn off and kept — tap to
put it back" is the way back. A perforated strip hangs below the reveal's actions:
pulling it past the threshold tears it free, the piece falls away with momentum,
and the ragged edge is drawn on the page in the category accent, seeded (never
random per frame). Behind **Tear it off** in Settings → Experiments → Memory, on
by default. Full description and the traps are in `app/AGENTS.md`'s v494 section.

**⚠️ Removal warning (held, for any future pass).** Retiring the reveal's save
control is a REMOVAL and must be confirmed before it goes; a visible fallback must
survive (a gesture may never be the only way to do something).

### 5.3 Fold the card — the Cabinet's save/open (BUILT, v495)

**Built exactly on the member's ruling** — *"folding closes a Cabinet card"* — as
the Cabinet's KEPT state: drag a saved entry's card down and it folds shut along a
crease you pull into it (tick per notch, the fold committing at half travel with
the app's heaviest confirm); the crease stays in the shelf and the card reads
"Folded — tap to open" until it is tapped open again. Folds live in prefs keyed by
entry id (no Room migration), so they travel in a backup and agree across the
cabinet grid, list view and collections. Behind **Fold the cards** in
Settings → Experiments → Cabinet, on by default. Full description and the traps
are in `app/AGENTS.md`'s v495 section.

**What.** A card folds in half along a crease you drag; folded means saved, and
the crease stays visible in the Cabinet afterwards. Unfold to read it again.

**Why.** No button, no toast — the paper states the state.

**✅ SETTLED (member, 2026-09-27): "Tear saves on the reveal; folding closes a
Cabinet card."** That is exactly the split this entry proposed — the tear is the
act of taking something out of the page (the reveal's save) and the fold is the
state of having kept it (a Cabinet card closing) — so **each owns a different
surface** and neither is a rival save verb on the other's page.

**⚠️ Removal warning (applies to 5.2).** The tear REPLACES the save control on the
reveal, and the standing rule is that a removal is asked for: **the control it
retires must be confirmed before it goes, and a visible fallback must survive** (a
gesture may never be the only way to do something). Building the tear as a second
way to save — with the existing control left in place — needs no permission; retiring
the control does.

### 5.4 Press the stamp — the Passport (BUILT, v491)

**Built as designed, plus the one thing the design needed to not break a
passport:** masteries earned *before* this version are **grandfathered in as
already pressed**, so an upgrade never un-inks a page somebody already filled —
only a lane mastered from here on waits for its press. The imprint's
imperfection (±2.4° tilt, ±1.2dp nudge) is derived from the lane's own id, so it
is fixed for that lane for life and a fresh install draws the same passport.
Behind **Press the stamp** in Settings → Experiments → Passport, on by default.
Full description and the traps are in `app/AGENTS.md`'s v491 section. The rest of
this entry is the design it was built from, kept so the intent is not lost.

**What (as designed).** The Passport's stamps stop appearing: a rubber stamp head hangs over the
page, you bring your thumb down, it compresses and lands with a *thunk* — haptic
plus a squashed ink silhouette that comes out slightly imperfect, like a real one.

**Why.** The Passport's stamps are the app's most collector-ish moment and today
they are drawn, not pressed. `CurioPassport` already writes the stamp.

**Interaction.** Press-and-hold with compression, the imprint landing on release;
the imperfection is deterministic per lane (seeded), never random per frame.

**Where it hooks in.** `features/quests`'s stamp rendering + `CurioPassport`.

### 5.5 The kept items

- **The reader pencil margin — BUILT (v492).** A strip over the page's edge you
  scrawl in with a finger, kept per page of the book, opened from a new **Margin**
  door in the reader's ⋯ menu. Two corrections to the idea as written here:
  (1) it was offered as *"drag in from the right edge"*, but **the reader's right
  edge is already a tap zone** (page-forward, with the motion lock freezing
  gestures over the page), so a hidden drag there would be two gestures fighting
  for one thumb — the door is visible instead, as the research requires; (2) the
  ink is **not** `SignatureSketchbook`'s — that component draws a book's
  PROCEDURAL doodle, it does not take freehand input, so the margin has its own
  two-pass pencil renderer (`ReaderMargin.kt`). Full description and the traps
  are in `app/AGENTS.md`'s v492 section.
- **Time-capsule notes — BUILT (v493).** The member gave it its own shape when
  the round was offered: *"a new time capsule option with its page … a really
  beautiful time machine style ui to write the message"*, and, asked where the
  return should land, **"it covers the whole screen on the day it returns"**.
  So it is not the note-with-a-date the original idea described: it is a **writing
  page of its own** (a brass dial for how far ahead, the message on the member's
  paper, a wax seal that is pressed) behind Home's **+** door, and the return is a
  **full-screen sealed envelope** over Home that waits rather than expiring. Five
  fixed spans (1 / 3 / 6 / 12 / 60 months) instead of a date picker. Its own prefs
  file `curio_time_capsules`, in the backup list. Behind **Time capsule** in
  Settings → Experiments → Memory, on by default. Full description and the traps
  are in `app/AGENTS.md`'s v493 section.
- **5.6 Turn the page for real** was offered but NOT picked; if the pencil margin
  is built, that surface is where its page-turn would live.

## 6. Proposed build order

1. **The Return** (3.1) + the Home hero cycling — the one that changes what the
   app is for, and it reuses entries, paper, streaks and haptics.
2. **Gesture vocabulary + discovery** (3.2) — cheap, and it fixes a real defect
   on the way.
3. **Your Curiosity** (3.8) — revisit the science page with the member.
4. **The Passport** (3.3), then **time capsules** (3.5).
5. **The reveal as one object** (3.4), then the **sound layer** (3.6) and the
   **pet** (3.7).
6. **The second round** (§5), which the member took as its own workstream and
   which is **BUILT IN FULL**: **5.1 the riffle (v490)**, **5.4 the press-stamp
   (v491)**, **5.5's pencil margin (v492)**, **5.5's time capsule (v493)**,
   **5.2 the tear (v494)** and **5.3 the fold (v495)** — the tear/fold surface
   question settled by the member's ruling (**"Tear saves on the reveal; folding
   closes a Cabinet card"**) and held to exactly.

## Sources

- Gamification in language apps: what helps, what hurts —
  <https://lingoat.app/en/blog/gamification-language-learning-apps/> (summarises
  the meta-analyses, the Duolingo disclosures and the metric/attention/competition
  drift literature, with its own citations).
- Mobile App Design Trends 2026: UI Patterns —
  <https://muz.li/blog/whats-changing-in-mobile-app-design-ui-patterns-that-matter-in-2026/>
  (haptic confirmation, contextual gesture discovery, thumb-first surfaces,
  adaptive layouts).
