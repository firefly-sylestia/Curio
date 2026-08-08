# Request — v8.28: fix washed-out colors in light & pastel mode (done)

## What the user asked

Colors on the Quests category passport texts were not visible properly in
pastel mode, and the saved-bookmark ("pin") icon was unreadable too. Make
the text/icons a little darker in light and pastel mode.

## Root cause

`PassportStamp` (Quests page) tinted its lane glyph and status labels
("Peeked" / "New · spin!") with `cat.themedAccent()` — in pastel mode that
is an airy pastel (lightness 0.80), so the text washed out. The
saved-quote bookmark in `EntryDetailScreen` did the same
(`category.themedAccent()` as the icon tint). `categoryInk()` fixed the
pastel case but only darkens PALE accents when pastel mode is ON — the
wildcard coral accent (pale by nature) still washed out in plain light mode.

## Fix (pushed)

- NEW public helper `CurioCategory.readableAccentInk()` in
  `ui/theme/CategoryInk.kt`: deep accent in light mode, deep hue twin for
  pale accents (wildcard) in EVERY light theme (not just pastel), light
  twin in dark mode.
- `PassportStamp` (QuestsScreen): glyph + PEEKED/UNSEEN label tints →
  `readableAccentInk()`; UNSEEN stamp border bumped to the ink at 0.45 so
  the "New · spin!" outline reads. Fills keep the pastel accent.
- `EntryDetailScreen` saved-quote bookmark icon tint →
  `readableAccentInk()`.
- Store changelog `20260817.txt`.

## Validation

Brace balance + `git diff --check` pass on all 3 files. CI on push is the
compile gate.

## Parked v8.28 hooks spec (user picks, build later)

1. Topic of the day (Home) — gold must-see card, deterministic rotation.
2. Come-back teaser (Home) — rotating mix: pet missed you + what's waiting
   + streak warning.
3. Spin streak combo — XP multiplier up to 2x + "Spin Storm" meter.
4. Rare card moments — ~1 in 20, pet sniffs out / telegraphs.
5. Mystery card slot + smooth scrollable viewed-cards stack behind the
   landed topic (UX-first).
6. Streak freeze (7-day milestones) + revival (XP scaled by streak).
7. Weekly rotating themed chain (e.g. "Explorer Week").
8. User's own Pet/Cabinet/Profile ideas, to share later.

Always-on unless the user asks for a toggle.
