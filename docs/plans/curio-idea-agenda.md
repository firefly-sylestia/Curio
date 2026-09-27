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

### 5.1 The riffle — BUILT (v490) ★

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

### 5.2 Tear it off the page — the reveal's save (OWED)

**What.** Saving stops being a button. The reveal is a page with a perforation
across it; dragging the strip TEARS it along the finger, ragged edge and all, and
the piece that comes away is the card that lands in the Cabinet.

**Why.** The app already owns torn-paper shape language (`SoftTornBottomShape`
and the hero's tear) — this makes that language the *mechanic* instead of the
decoration, and it is the most literal "visually interactive" of the four.

**Interaction.** Finger drag along the perforation; the tear line follows with a
ragged jitter, a per-notch tick of haptics as it advances, and the sheet falling
away with momentum when it lets go. Full physical (the member's choice:
momentum, springs, overshoot).

**Where it hooks in.** `TopicRevealScreen`'s save action; `PaperCard`/
`SoftTornSheetShape` for the paper; the Cabinet entry is already the far end.

**⚠️ Removal warning.** This REPLACES the save control on the reveal, and the
standing rule is that a removal is asked for — the member has named it, but the
control it retires must be confirmed before it goes, and a visible fallback must
survive (a gesture may never be the only way to do something).

### 5.3 Fold the card — the Cabinet's save/open (OWED)

**What.** A card folds in half along a crease you drag; folded means saved, and
the crease stays visible in the Cabinet afterwards. Unfold to read it again.

**Why.** No button, no toast — the paper states the state.

**⚠️ Open question.** 5.2 and 5.3 are BOTH a save verb. They cannot both own the
same act on the same surface: the tear belongs to the reveal (the act of taking
something out of the page) and the fold to the Cabinet card (the state of having
kept it) — **confirm which surface each owns before building either.**

### 5.4 Press the stamp — the Passport (OWED)

**What.** The Passport's stamps stop appearing: a rubber stamp head hangs over the
page, you bring your thumb down, it compresses and lands with a *thunk* — haptic
plus a squashed ink silhouette that comes out slightly imperfect, like a real one.

**Why.** The Passport's stamps are the app's most collector-ish moment and today
they are drawn, not pressed. `CurioPassport` already writes the stamp.

**Interaction.** Press-and-hold with compression, the imprint landing on release;
the imperfection is deterministic per lane (seeded), never random per frame.

**Where it hooks in.** `features/quests`'s stamp rendering + `CurioPassport`.

### 5.5 Still on the list, not started

- **The reader pencil margin** — drag in from the right edge, scrawl in the
  margin with the `SignatureSketchbook` ink, anchored to that page. The member
  said keep it, and it folds naturally into 5.6's surface.
- **Time-capsule notes** — seal a note (wax seal / envelope) and Curio returns it
  in three months. Agenda §3.5; `PersonalNotes` + a "sealed until" field.
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
   which is already underway: **5.1 the riffle is built (v490)**; **5.4 the
   stamp** and **5.5's two kept items** are next, and **5.2/5.3 (tear / fold)**
   need their surface question answered first — they are both a save verb and
   cannot both own it.

## Sources

- Gamification in language apps: what helps, what hurts —
  <https://lingoat.app/en/blog/gamification-language-learning-apps/> (summarises
  the meta-analyses, the Duolingo disclosures and the metric/attention/competition
  drift literature, with its own citations).
- Mobile App Design Trends 2026: UI Patterns —
  <https://muz.li/blog/whats-changing-in-mobile-app-design-ui-patterns-that-matter-in-2026/>
  (haptic confirmation, contextual gesture discovery, thumb-first surfaces,
  adaptive layouts).
