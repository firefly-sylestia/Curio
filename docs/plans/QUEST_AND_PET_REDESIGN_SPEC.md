# Quest & Curio Pet Redesign Spec

## 1. Product goal

Redesign Quests from a dense achievement ledger into a friendly daily discovery hub that tells the user exactly what to do next, makes daily quests immediately visible, pushes users across different categories, and rewards progress with a cute pixelated Curio pet that grows from XP.

The new system should feel like a cozy game layer over the app: quick to understand, satisfying to complete, and motivating without becoming noisy or manipulative.

## 2. Problems to solve

1. **Daily quests are buried.** The daily tasks should be the first thing the user sees, not a lower section after level cards and chains.
2. **The page feels overwhelming.** Too many chains, badges, counters, locked items, and progress bars compete at once.
3. **Category discovery is weak.** Current quests can reward generic spins/explores, but they do not strongly guide the user into different lanes.
4. **The first tutorial is incomplete.** The tour moves between major pages, but it does not fully guide the user through landing on a topic, opening it, reading/writing through the pages, exploring, and saving.
5. **Reward moments need more delight.** Completing quests should create satisfying feedback: animation, sound/haptics hooks, pet reactions, clear XP gain, and visible progress.
6. **The quest system needs clearer logic.** Daily, journey, category, tutorial, and pet growth loops should each have a defined purpose.

## 3. Design principles

- **Daily first.** The top of Quests should answer: “What can I do today in the next 2 minutes?”
- **One primary next step.** Always surface one recommended quest card; hide complexity behind expandable sections.
- **Discovery over grinding.** Reward exploring new categories, formats, and topics more than repeating the same easy action.
- **Show, then explain.** The tutorial should navigate the user through real screens and real actions instead of only describing them.
- **Pet as emotional progress.** The Curio pet should be a companion, not a blocking assistant. It reacts, celebrates, nudges, and grows.
- **Cozy, tactile feedback.** Use small bursts, bounce, sparkles, progress ticks, and pet expressions to make completion feel good.
- **Respect the user.** Avoid shame, streak anxiety, dark patterns, or constant interruption.

## 4. New Quests screen information architecture

### 4.1 Top-to-bottom layout

1. **Hero header: “Today’s Curiosity”**
   - Contains the pet, user level, XP ring, and the primary daily quest.
   - Daily quests are visible immediately above the fold.
   - Header copy should be short: “3 little discoveries today.”

2. **Daily quest stack**
   - Three compact cards.
   - Each card shows category, action, reward, progress, and one CTA.
   - Completed cards collapse into a satisfying claimed state after XP is collected.

3. **Recommended next quest**
   - One bigger card generated from the user’s least-explored category, unfinished tutorial step, or active chain.
   - The CTA says exactly what will happen: “Spin Music,” “Open the landed topic,” “Write your first note,” etc.

4. **Category passport**
   - A horizontal/2-row category map with stamps for explored categories.
   - Missing categories are visually enticing and tappable.
   - Each category has a mini objective like “Find 1 album,” “Save 1 book note,” or “Try a science topic.”

5. **Quest paths**
   - Collapsed sections for longer chains.
   - Default collapsed to avoid overwhelming new users.
   - Examples: First Journey, Category Explorer, Cabinet Keeper, Streak Flame, Collector.

6. **Badge shelf**
   - Moved to the bottom or separate tab/sheet.
   - Shows earned badges first, locked badges as silhouettes.
   - Does not dominate the main page.

### 4.2 Visual structure

Use a friendly “adventure journal + pixel companion” style:

- Soft cream background with current category wash when a category quest is selected.
- Cards should feel like rounded stickers or paper slips.
- Daily quest cards use strong hierarchy: icon/glyph, title, progress, CTA.
- Quest chains use timeline beads instead of dense lists.
- Completed cards animate into stamps.
- Locked chain steps are partially hidden until the user expands a path.

### 4.3 Screen wireframe

```text
┌────────────────────────────────────┐
│ Today’s Curiosity              ⚙︎  │
│ ┌───────────────┬────────────────┐ │
│ │ pixel pet     │ Level 7         │ │
│ │ happy/idle    │ 120 / 205 XP    │ │
│ │ speech bubble │ “Let’s try Art” │ │
│ └───────────────┴────────────────┘ │
│                                    │
│ DAILY QUESTS                       │
│ ┌────────────────────────────────┐ │
│ │ 🌅 Daily Pick                  │ │
│ │ Explore 1 fresh topic          │ │
│ │ [0/1] +15 XP        [Start]    │ │
│ └────────────────────────────────┘ │
│ ┌────────────────────────────────┐ │
│ │ 🎨 New Lane                    │ │
│ │ Try a category you rarely open │ │
│ │ [0/1] +20 XP        [Choose]   │ │
│ └────────────────────────────────┘ │
│ ┌────────────────────────────────┐ │
│ │ ✍ Save a Spark                 │ │
│ │ Save any capture today         │ │
│ │ [0/1] +25 XP        [Go]       │ │
│ └────────────────────────────────┘ │
│                                    │
│ RECOMMENDED NEXT                   │
│ ┌────────────────────────────────┐ │
│ │ Passport missing: Books        │ │
│ │ Spin Books and open the topic  │ │
│ │ +20 XP              [Spin]     │ │
│ └────────────────────────────────┘ │
│                                    │
│ CATEGORY PASSPORT                  │
│ [Music ✓] [Movies ·] [Books ?]     │
│ [Art ?] [Science ·] [Wildcard ✦]   │
│                                    │
│ QUEST PATHS                        │
│ ▸ First Journey        2/7         │
│ ▸ Category Explorer    1/10        │
│ ▸ Cabinet Keeper       0/5         │
│                                    │
│ BADGES                             │
│ [View shelf]                         │
└────────────────────────────────────┘
```

## 5. Daily quest redesign

### 5.1 Daily quest rules

Every day should generate three quests with distinct roles:

1. **Warm-up quest** — easy completion in one action.
   - Examples: Spin once, open today’s topic, visit the Cabinet.
   - Reward: 10–15 XP.

2. **Discovery quest** — pushes category variety.
   - Examples: Try a category with low exposure, explore a category not used this week, use Wildcard.
   - Reward: 15–25 XP.

3. **Creation quest** — asks the user to save or reflect.
   - Examples: Save a note, add an image, record a thought, pin a topic.
   - Reward: 20–35 XP.

> **v8.27 — five quests a day.** The day now ships **three CORE quests**
> (the warm-up / discovery / creation trio above) plus **two BONUS quests**
> that unlock once all three core quests are claimed. Bonus rewards are
> higher (25–40 XP) and the core rewards were raised ~50%, so a full day
> pays roughly 120–140 XP instead of ~45. On the Quests page, claimed core
> quests hide away (animated out) and the bonus pair pops in with a gold
> "Bonus quests unlocked!" line once the trio is done.

### 5.2 Daily quest ordering

Daily quests must appear at the top of the Quests page, directly under the pet/level hero. If screen height is limited, show the first two daily quests and a “Show third” affordance rather than pushing all daily content below chains.

### 5.3 Daily quest card states

- **Fresh:** shows action, reward, and category accent.
- **In progress:** progress bar/checkpoints fill.
- **Ready to claim:** card glows softly, CTA becomes “Claim +XP.”
- **Claimed:** card shrinks slightly into a stamped state and shows the pet reaction.
- **Expired:** only shown in history/debug; never shame the user for missing it.

### 5.4 Daily quest examples

| Quest | Logic | CTA | Reward |
| --- | --- | --- | --- |
| Morning Spin | Complete any spin today | Spin now | +10 XP |
| New Lane | Explore a category not used in 7 days | Pick lane | +20 XP |
| Cabinet Spark | Save any capture | Save a thought | +25 XP |
| Wildcard Wander | Spin Wildcard and open the landed topic | Wildcard | +20 XP |
| Deep Dive | Read/open the topic detail and start explore | Open topic | +15 XP |
| Taste Test | Explore two different categories today | Start first | +30 XP |

## 6. Category discovery system

### 6.1 Category passport

Add a “Category Passport” module that tracks the user’s relationship with every visible category.

Per category, track:

- Spins.
- Topic reveals opened.
- Explores started.
- Captures saved.
- Last explored date.
- Favorite/avoid signals if available.

Each category has a stamp state:

- **Unseen:** user has never opened a topic in this category.
- **Peeked:** user revealed/opened a topic but did not explore.
- **Explored:** user started exploration.
- **Captured:** user saved a result.
- **Mastered:** user completed the category’s first mini-chain.

### 6.2 Discovery quest selection

When generating category-discovery quests, prioritize:

1. Categories never opened.
2. Categories opened but never explored.
3. Categories explored but never saved.
4. Categories not touched in the last 7–14 days.
5. Wildcard if the user repeats the same category too often.

Avoid assigning quests for unavailable/coming-soon categories.

### 6.3 Category CTA behavior

Tapping a category quest should route directly into the correct flow:

- If the quest says “Spin Books,” open the Spin screen with Books preselected.
- If the quest says “Open the landed topic,” navigate to the current reveal if one exists.
- If no landed topic exists, spin first, then guide the user to open the resulting topic.
- If the quest says “Save a thought,” route to capture only after a topic has been explored; otherwise route to reveal/explore first.

## 7. First tutorial redesign

### 7.1 Goal

The first tutorial should walk the user through the entire core loop:

**Home → Quests → Spin → landed topic → topic pages/details → Explore → Capture/write → Save → Cabinet → Pet/XP reward.**

The user should finish the tutorial understanding what Curio is for, where content lives, and how quests connect to XP and pet growth.

### 7.2 Tutorial structure

Use a guided “First Journey” quest chain with real actions:

1. **Welcome Home**
   - Route: Home.
   - Teach: Home shows today’s starting point.
   - CTA: “See today’s quest.”

2. **Daily Quest Hub**
   - Route: Quests.
   - Teach: Daily quests are the fastest way to grow.
   - CTA: “Start First Spin.”

3. **Pick a lane**
   - Route: Spin/category picker.
   - Teach: Categories are lanes of curiosity.
   - CTA: Pick a suggested starter category or Wildcard.

4. **Spin and land**
   - Route: Spin.
   - Waits for: actual spin action.
   - Teach: The app chooses a topic.

5. **Open the landed topic**
   - Route: Topic reveal.
   - Waits for: user opens the landed topic/reveal page.
   - Teach: Topic reveal gives the teaser and action.

6. **Move through topic pages**
   - Route: Topic detail/writing pages if present.
   - Waits for: user navigates through the topic’s available sections/pages.
   - Teach: Read the prompt, context, and instructions before exploring.
   - Requirement: if the current app has multiple writing/detail pages, the tutorial must explicitly highlight each page and how to move next/back.

7. **Start exploring**
   - Route: Topic reveal/detail.
   - Waits for: user taps Explore/Start exploring.
   - Teach: Curio expects an outside-app action.

8. **Capture what you found**
   - Route: Capture.
   - Waits for: text/audio/photo capture input or explicit skip if supported.
   - Teach: Save a note to make the discovery yours.

9. **Save to Cabinet**
   - Route: Cabinet after save.
   - Waits for: save completion.
   - Teach: Saved discoveries live in the Cabinet.

10. **Reward and pet growth**
    - Route: Quests or overlay.
    - Teach: XP grows levels and feeds the Curio pet.
    - Reward: Complete First Journey, grant XP, show pet animation.

### 7.3 Tutorial overlay behavior

- Use a floating coach bubble, not a blocking full-screen modal except for the initial welcome.
- Position bubble near the target control without covering it.
- Every step has one primary CTA.
- Steps that require action should disable “Next” and show “Do this to continue.”
- Allow “Skip tour” from every step.
- If skipped, leave the First Journey quest chain available so the user can resume.
- Persist the exact step so the tour survives process death.

### 7.4 Tutorial copy tone

- Short, warm, direct.
- Avoid explaining too much at once.
- Example: “Nice — you landed on a topic. Open it and I’ll show you how to turn it into a discovery.”

## 8. Quest logic redesign

### 8.1 Quest types

1. **Daily quests** — short-term, refresh daily.
2. **Journey quests** — onboarding and long-term app mastery.
3. **Category quests** — encourage breadth across categories.
4. **Creation quests** — encourage saves, notes, photos, audio, and quotes.
5. **Pet care quests** — lightweight interactions with the pet.
6. **Challenge quests** — optional weekly/monthly goals after the basics are stable.

### 8.2 Recommended quest engine

The “Recommended next” card should select one quest using this priority:

1. Resume incomplete tutorial step.
2. Ready-to-claim daily quest.
3. Unfinished daily quest with fewest remaining actions.
4. Category passport gap.
5. Active journey chain stage.
6. Weekly challenge, if enabled.

### 8.3 XP and balancing

- Keep XP rewards small and frequent early.
- Reward new behavior more than repeated behavior.
- Add diminishing returns for repeated same-category spins in a single day.
- Give bonus XP for completing all three dailies, but keep it modest.
- Pet growth should use earned XP, not separate food currency at first.

Suggested rewards:

| Action | XP |
| --- | ---: |
| Spin | 2 |
| Open landed topic | 3 |
| Start explore | 5 |
| Save capture | 10 |
| First category explore | 15 bonus |
| Daily completion | 10–35 |
| First Journey completion | 50 |
| Category passport stamp | 20 |

## 9. Satisfying feedback system

### 9.1 Completion moment

When a quest completes:

1. Card checks off with a spring scale.
2. XP chip flies toward the level/pet area.
3. Pet reacts with a happy animation.
4. Progress bar ticks forward.
5. If a level-up happens, show a larger but still skippable celebration.

### 9.2 Microinteractions

- Daily card press: subtle squish.
- Progress increase: animated fill, not instant jump.
- Claim button: sparkle burst in category accent.
- Category passport stamp: stamp lands with a tiny rotation and dust particles.
- Pet idle: blink, sway, inspect card, fall asleep if idle.
- Pet excited: hop, sparkle eyes, tiny speech bubble.

### 9.3 Haptics/sound hooks

Design hooks only; implementation should respect device settings:

- Light tick for progress.
- Soft pop for quest claim.
- Stronger but brief haptic for level-up.
- No mandatory sound; if sound is added later, provide a Settings toggle.

## 10. Curio pet concept

### 10.1 Role

The Curio pet is a cute pixelated companion that lives over the app, reacts to exploration, and grows with XP from quests. It should make progress feel emotional and memorable without getting in the way.

### 10.2 Visual direction

- Pixel-art creature with a tiny explorer/librarian personality.
- Silhouette: small round body, big expressive eyes, tiny antenna/ears, little satchel or scarf.
- Animations: idle blink, hop, sleep, celebrate, curious sniff, reading, carrying a spark.
- Keep it readable at 48–96 dp.
- Prefer vector/pixel-style Compose drawing or bundled sprite sheets. If generated bitmap art is used later, store source prompts and export sizes.

### 10.3 App presence

Pet placement rules:

- Quests: lives in the top hero as the main companion.
- Home: small optional corner presence near the daily quest summary.
- Spin: watches the wheel; reacts when the topic lands.
- Topic reveal: points at the Explore action or reads the topic card.
- Capture: quietly encourages saving, never covers input fields.
- Cabinet: celebrates new saved entries.

The pet should not always float above content. Use a docked/anchored placement per screen. A draggable floating pet can be explored later as an opt-in experiment.

### 10.4 Growth stages

| Stage | Name | Unlock | Behavior |
| --- | --- | --- | --- |
| 1 | Hatchling Spark | First Journey starts | shy blink, small hops |
| 2 | Curious Sprout | Level 3 or 100 XP | points at daily quests |
| 3 | Trail Buddy | first 3 categories explored | carries small satchel |
| 4 | Archive Pal | 10 saves | reads tiny book |
| 5 | Lane Guardian | all ready categories explored | category-color aura |
| 6 | Curio Sage | high-level milestone | calm sparkle idle |

### 10.5 Pet moods

Moods should be derived from user/app state:

- **Happy:** quest completed recently.
- **Curious:** recommended category available.
- **Sleepy:** user has been idle or no quest interaction today.
- **Proud:** level-up or passport stamp earned.
- **Focused:** user is writing/saving.
- **Excited:** spin landed on a new category.

Avoid negative guilt moods. No sad pet for missed days.

### 10.6 Pet AI behavior

The pet can have lightweight AI-style interactions without requiring server AI at first:

1. **Rule-based v1**
   - Uses templates based on screen, category, quest state, and pet mood.
   - Example: “We haven’t tried Books yet — want to earn a new stamp?”

2. **Personalized v2**
   - Uses local stats to suggest underexplored categories and formats.
   - No sensitive inference; just app activity.

3. **Optional AI v3**
   - If an actual model is added later, it must be optional, privacy-reviewed, and clearly explained.
   - Pet dialogue should never invent facts about topic content unless grounded in local topic data.
   - Provide graceful offline fallback to rule-based dialogue.

### 10.7 Pet dialogue rules

- One sentence maximum for passive bubbles.
- Two sentences maximum for tutorial bubbles.
- Never nag.
- Never interrupt typing.
- No bubble more than once per screen visit unless user taps the pet.
- Tapping the pet can show: current mood, next quest suggestion, XP to next growth stage.

## 11. Data model guidance for future implementation

Do not implement in this planning task, but future code should likely add persistent state for:

- Quest definitions and generated daily instances.
- Daily quest date, progress, claim state, and assigned category.
- Category passport counters/stamps.
- Tutorial step, status, and resume route.
- Pet stage, selected skin/accessory, mood cooldowns, and last reaction.
- Event log or counters for spin/open/explore/save/category actions.

Quest progress should update from centralized event hooks, not scattered UI-only state. Events should include enough metadata to support category quests, for example `categoryId`, `topicId`, `captureFormat`, and timestamp.

## 12. Navigation requirements

Future implementation should ensure every quest CTA can route precisely:

- Tab routes should preserve tab behavior.
- Push routes should use single-top navigation.
- A tutorial step that needs a landed topic should store or derive the active topic.
- If a topic is unavailable after process death, the tutorial should recover by routing back to Spin with a clear message.
- Quest CTAs should not dead-end on screens where the user cannot complete the quest.

## 13. Settings and rollout guidance

Because this redesign contains new systems, future implementation should follow project rules:

- Ask the user whether each new feature ships always-on or behind a user-facing Settings toggle before implementing.
- Experimental versions of the pet overlay, AI dialogue, weekly challenges, sound, or draggable floating behavior should be opt-in Settings toggles.
- Once an experiment is decided, remove the toggle and hardcode the winning behavior.

Recommended rollout:

1. **Phase A — Quests IA only:** move daily quests to top, simplify sections, add recommended quest card.
2. **Phase B — Tutorial core loop:** replace current tour with real-action First Journey.
3. **Phase C — Category passport:** add category stamps and discovery quest generation.
4. **Phase D — Reward polish:** add claim animations, XP flight, stamp effects, level-up moment.
5. **Phase E — Pet v1:** add rule-based pixel pet in Quests/Home with growth stages.
6. **Phase F — Pet expansion:** add per-screen reactions and optional AI/personality work.

## 14. Accessibility and comfort

- Support reduced motion by shortening or disabling non-essential animations.
- Pet speech bubbles must be readable by screen readers.
- Quest cards need clear labels and progress text, not color-only status.
- Touch targets should remain at least 48 dp.
- Celebrations should be skippable and not block repeated app use.
- Avoid flashing effects.

## 15. Success metrics

Track these manually or via future analytics if allowed:

- New users completing First Journey.
- Percentage of users who complete at least one daily quest.
- Category diversity: number of categories explored per user.
- Spin → topic open → explore → save conversion.
- Return rate after first quest completion.
- Pet taps/reactions, if pet is implemented.
- Qualitative feedback: “less overwhelming,” “I knew what to do,” “pet feels cute not annoying.”

## 16. Acceptance criteria for implementation

A future implementation should be considered successful when:

- Daily quests are visible at the top of the Quests page without scrolling on a normal phone.
- New users can complete the full tutorial loop from Home to Cabinet.
- Tutorial steps wait for real user actions when needed.
- Quest CTAs route to the exact screen needed to progress.
- Category-discovery quests prioritize categories the user has not meaningfully explored.
- The page shows one recommended next action and does not overwhelm with every chain expanded.
- Quest completion has a clear, satisfying feedback moment.
- The Curio pet grows from XP and reacts to quest/category progress.
- The pet never blocks core UI or interrupts capture input.

## 17. Non-goals for the first implementation

- No server-side AI requirement.
- No social leaderboard.
- No punishment for missed days.
- No monetization hooks.
- No always-floating pet unless explicitly approved as a toggleable experiment.
- No replacing the core Spin → Explore → Save loop; quests should amplify it, not take over.
