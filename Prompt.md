# Request — v8.28 (PLANNED): addictive hooks, per user's picks. CI fix done.

## What the user asked

1. Brainstorm addictive features screen by screen; they answered a picker.
2. Fix a CI compile error, commit + push.
3. Ask follow-up clarifications on their picks (done below).

## CI fix (done, pushed `3013ac0`)

`QuestsScreen.kt:368` — `ConfettiBurst(trigger = levelUpBanner)` failed:
"Smart cast to 'Any' is impossible, because 'levelUpBanner' is a delegated
property." Fixed by passing the value: `trigger = levelUpBanner ?: 0`.

## Picks (round 1) + clarifications (round 2) — FEATURES ARE PARKED, build later

### 1. Topic of the day (Home)
- Gold "Today's must-see" card on Home.
- Selection: **deterministic rotation through the whole catalog**, no repeats
  until the cycle is done.

### 2. Come-back teaser (Home)
- When returning after a gap, show a **short rotating mix** of all three:
  pet missed you (emotional greeting animation), what's waiting (ready bonus
  quests / gift / today's quests), and a streak warning ("one more day and
  your streak breaks!").

### 3. Spin streak combo (Spin)
- Consecutive spins in one session stack an **XP multiplier up to 2x**.
- Simultaneously fill a **"Spin Storm" meter** that pays out a bonus when
  full (meter climbs as the multiplier climbs).

### 4. Rare card moments (Spin)
- ~**1 in 20** spins the deck lands a rare topic (sparkle + bonus XP).
- The **pet occasionally sniffs out / telegraphs** a rare card before you
  spin.

### 5. Mystery card slot + viewed-cards stack (Spin / Reveal)
- A **third face-down deck slot** that flips on landing with suspense.
- PLUS: a **stack of previously viewed cards behind the landed topic** the
  user can smoothly scroll / peek through and choose to explore instead.
  UX must be smooth and polished (this is the priority detail).

### 6. Streak freeze & revival
- Freezes earned at **7-day milestones**.
- Revival costs **XP scaled by streak length**.

### 7. Weekly rotating special chain (Quests)
- One **special themed chain per week that rotates** (e.g. "Explorer Week")
  with its own rewards and badge.

### 8. Parked separately
- User has their own Pet / Cabinet / Profile ideas to share later.

## Build order suggestion (when user says go)
Topic of the day → Come-back teaser → Spin combo + Storm meter → Rare cards
+ pet sniff → Mystery slot + viewed-cards stack → freeze/revival → weekly
chain. Always-on (no toggles) unless the user asks otherwise.
