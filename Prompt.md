# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): Shuffle fairness Q&A + deck round-trip fix + browse-mode Explore + two passed experiments

**Date:** 2026-08-12

**What was asked:** (1) How does the shuffle randomizer work — does every topic have a fair chance? (2) Why does the deck topic change after opening a topic and tapping back — it shouldn't. (3) Why doesn't the Explore button in the Topic Browser → reveal trigger the explore session? (4) Make the Enhanced main gradient always-on (it passed) and remove its Experiments option. (5) Same for Pastel crown depth.

**Findings (answered, no code for #1):**
- **Shuffle fairness:** NOT equal odds — deliberately weighted. The pool = active categories' topics minus DONE topics (explored/captured/"already"); falls back to full pool when everything is done. `pickFrom` excludes the last ~20 dealt topics (`recentTopicIds`), then weight-picks: tier 1 = 100 pts, tier 2 = 60, tier 3 = 20 (else 30); liked topics 2× / disliked 0.25×; category affinity up to 2.5× / down to 0.25×; Films recency 1.6× (2020s) → 0.55× (pre-1970). Everything except done/recent CAN appear — nothing is blocked, but curated tiers and your likes shape the odds.
- **Topic changes on back (BUG, fixed):** the NavHost disposes Spin's composition while Reveal is open; on return the hand re-deals from `buildDeckHand(deckPool, landedTopic)`. When `landedTopicName` is null (idle deck — never landed, or after a swipe), the front card was a FRESH RANDOM topic. Fix: `onDeckCardTap` now pins `landedTopicName = resolved.name` (v25), so the tapped card survives the round-trip; the pin clears on the next swipe/spin.
- **Browse-mode Explore (v8.12 silent design, changed):** the Topic Database reveal's Explore button ran `openSilentExplore` (out-of-app search only, no session). User wants the real flow. Fix: browse-mode Explore now runs the real explore (dialog → `startExploreSession` → timer/recents/done-mark). Removed `latestOnSilentExplore`, the `onSilentExplore` param of `RevealActionRow`, and the `openSilentExplore` import; the function stays defined in ExploreSession.kt (now unused by UI).

**Done:**
- SpinScreen: pinned tapped topic in `onDeckCardTap`; hardcoded `heroGradientOn = true` (v25 comment); pastel `topCrown` always the 5% black deepen (no toggle).
- TopicRevealScreen: browse-mode Explore → real session (dialog/`onExplore` in both branches, label "Explore" kept in browse mode); `heroGradientOn = true`.
- ExperimentsScreen: removed the Enhanced main gradient toggle and the whole Deck & controls card (Pastel crown depth toggle); dropped the now-empty divider.
- CurioColors: pastel card crown always uses the deepen (`pastelColorsState` alone gates it).
- Changelog (20260919.txt) + app/AGENTS.md v25 bullets added.

**Validation:** braces + `git diff --check` clean; grep confirms no `onSilentExplore`/`latestOnSilentExplore` refs left in UI, toggles gone from Experiments, passed-pref reads only dormant in AppPreferences + one dead branch in the rejected heroBlendGradient (unreachable, left as-is).

**Next:** none pending.
