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

- Build **all** of the ideas above; **save them first**, then take them in order.
- **The Return lives in Home's quest hero.** Home's existing "Today's quest /
  Shuffle the deck" card **cycles** between the quest and a due recall — both
  kept, alternating (and on the first day of a recall, the recall leads).
- **New measures ship behind a Settings toggle, ON by default** (the project's
  standing rule for a new measure), with the toggle removed once the behaviour
  has settled.
- **No leaderboards, no public comparison** — per the research.

## 5. Proposed build order

1. **The Return** (3.1) + the Home hero cycling — the one that changes what the
   app is for, and it reuses entries, paper, streaks and haptics.
2. **Gesture vocabulary + discovery** (3.2) — cheap, and it fixes a real defect
   on the way.
3. **Your Curiosity** (3.8) — revisit the science page with the member.
4. **The Passport** (3.3), then **time capsules** (3.5).
5. **The reveal as one object** (3.4), then the **sound layer** (3.6) and the
   **pet** (3.7).

## Sources

- Gamification in language apps: what helps, what hurts —
  <https://lingoat.app/en/blog/gamification-language-learning-apps/> (summarises
  the meta-analyses, the Duolingo disclosures and the metric/attention/competition
  drift literature, with its own citations).
- Mobile App Design Trends 2026: UI Patterns —
  <https://muz.li/blog/whats-changing-in-mobile-app-design-ui-patterns-that-matter-in-2026/>
  (haptic confirmation, contextual gesture discovery, thumb-first surfaces,
  adaptive layouts).
