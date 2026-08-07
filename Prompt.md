# Prompt.md — Current Request Log

## Request (2026-08-07, 20th): Revert the text-out-of-morph restructure (72449c9) — full-card morph is back — DONE (pushed)

**User request:** "https://github.com/firefly-sylestia/Curio/commit/72449c98a968720e609ef9bcbf0f4809dfba3067 revert this commit"

### Analysis
- `72449c9` ("main card text glitch on back") restructured the shared-element morph: instead of the WHOLE card morphing (gradient + glyph + text), the shared element became only the card face gradient, with all text hoisted out and blooming in after the morph. The user's follow-ups ("why did u remove the morph animation of the main card", "i still dont see any morph open animation of main card") made it clear the full-card morph was the desired behavior — the revert restores it.

### Fix — revert 72449c9, restore the original whole-card morph
- **`features/spin/SpinScreen.kt`** — restored from `72449c9^`: `sharedElement` sits back on the OUTER ticket Box (whole card — face gradient, glyph, byline pill, title/tags/teaser all inside it), so the entire card expands out of the deck into the topic page and reverses on back.
- **`features/reveal/TopicRevealScreen.kt`** — restored from `72449c9^`: `sharedElement` sits back on the hero `Surface` (whole hero — gradient, glyph, action badge, byline, subtype pills all inside it). The `HeroPillEntrance` bloom wrapper is gone (it was added by the reverted commit).
- **`ui/adaptive/RevealSharedScopes.kt`** — removed `RevealGlyphSharedElementKey` (added by ae28095 on top of the reverted structure): the glyph now lives back inside the whole-card shared element, so the separate glyph element is dead code.
- Kept from later commits (NOT part of 72449c9): the detail route's bottom-bar reserve removal (ae28095, NavHost) and the reserve fix (db393bd). Changelog top bullets rewritten to describe the full-card morph.

### Validation
No Gradle in this env (per AGENTS.md) — restored files byte-identical to `72449c9^` (verified via `git diff 72449c9^` empty), braces balanced (SpinScreen 394/394, TopicReveal 196/196, RevealSharedScopes 2/2), exactly one `sharedElement`/`rememberSharedContentState` per file (whole-card + whole-hero), no `RevealGlyphSharedElementKey` references remain, `git diff --check` clean. Code review ran. CI on the pushed HEAD is the compile gate.

### Follow-ups
- The original text-squash glitch on back is expected to return (that is what 72449c9 fixed) — the user chose the full-card morph over the glitch-free version.

---

## Request (2026-08-07, 21st): Move topic reveal actions into the themed bottom area — DONE

### Implementation
- Approved plan: move Start exploring and Already watched/listened/read/explored out of the scrolling content into a bottom action dock.
- Updated `features/reveal/TopicRevealScreen.kt` with a full-height parent layout, scrolling content region, responsive stacked/horizontal dock, category-surface tint, and navigation-bar inset handling.
- Preserved browse-mode, done-state, dialogs, session, and engagement behavior.

### Validation
- Gradle commands are prohibited in this environment; static validation and git diff review remain to be run before commit/push.
