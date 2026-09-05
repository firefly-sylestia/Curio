# Request Log — book synopsis quality pass (connected prose)

## Status: complete — committing & pushing (CI will validate)

## The request (user)
"analyse the books properly i feel some synopsis doesnt feel connected
like i ont know, proper synopsis"

## Audit findings
Re-read all 60 rewritten synopses. The problem: the NON-FICTION ones
were listy, sentences of the form "he shows X; he explores Y; he
examines Z" with colon-catalogues (Predictably Irrational, Nudge, Ego
Is the Enemy, Stillness Is the Key, The Tipping Point, Algorithms to
Live By, The 48 Laws of Power, So Good They Can't Ignore You). A scan
of the remaining 736 catalog entries found the same disease in the
shortest ones (320-494 chars): thin one-paragraph blurbs with
disconnected sentences.

## Fix (tools/enrich_book_synopses_quality1.py) — 32 books
Rewrote as flowing, connected prose. The recipe applied to every one:
open with a human hook (Ariely's bandage story, Duckworth's classroom
question, Tolle's night of despair, the nurses, the lobsters), develop
the argument through natural transitions and woven-in examples (no
mid-sentence colon-catalogues), and close by tying the whole together.
- 8 of my own listy rewrites: Predictably Irrational, Nudge, Ego Is
  the Enemy, Stillness Is the Key, The Tipping Point, Algorithms to
  Live By, The 48 Laws of Power, So Good They Can't Ignore You.
- 24 worst short ones: The Dispossessed, The Princess Bride, The
  Jungle, Sister Carrie, Uncle Tom's Cabin, Love in the Time of
  Cholera, Gideon the Ninth, The Souls of Black Folk, Ball Lightning,
  The Tale of Genji, The Four Agreements, 12 Rules for Life, The Dark
  Forest, Solaris, The Lion the Witch and the Wardrobe, The Last
  Unicorn, The Power of Now, The Prophet, Homo Deus, The Art of War,
  Outliers, Grit, The Midnight Library, Quiet.
Validation: 32/32 entries changed, 988-1283 chars, no em/en dashes,
curly quotes, double spaces or paren mismatches.

## Status
92 books now carry quality synopses. Going forward the quality bar is
connected prose, never topic catalogues. Remaining: ~700 books still on
the old longer-format synopses (mostly fine; the shortest ones are
already done in batches 1, 2 and this pass).

## Docs
- app/AGENTS.md: v367 entry (quality pass).
- fastlane changelog 20260921.txt: FIX bullet at the top.
- Prompt.md: this log.