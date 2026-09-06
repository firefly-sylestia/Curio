# Prompt Log — current request

## Request (2026-09-06)

"finish the previous request that you paused; book chapter add note is bad —
add ability to share that note as chapter review in the share card (auto
selects custom fact / chapter), book/album/series covers in share cards with
fetch, condensed + book-page + editorial fact formats, writing box below the
tools with enlarge, editorial drop-cap options, album fav box height/width,
and when moving the quick fact box the title should only move if the fact
touches it; also: book cover fetch sometimes fails with different providers —
remove Google Books provider (keep ratings), album color extraction is
inaccurate, expanded chapter color is bad, note box too small with expand."

## Completed (commit `…` pending)

1. **Fact formats** — `ShareCardFactFormat` CONDENSED + BOOK_PAGE and
   `ShareCardFactDropCap` (NONE / FIRST_LETTER / FIRST_WORD) render through a
   new `FactBody` used by every style's fact + the chapter-fact sites
   (17 call sites); `BookPageText` splits into two justified columns,
   `EditorialDropCapBlock` draws the big initial. Formats + drop caps apply
   to quick AND custom facts.
2. **Writing box below the tools** (quick/custom/chapter-review) with an
   Enlarge button → full white writing sheet.
3. **Album/series cover fetch in the share editor** — `isAlbumTopic` /
   `isSeriesTopic` wired at all three TopicShareSheet call sites (reveal,
   Share Hub, entry detail); Fetch/Refetch/Remove row resolves
   AlbumArtFetch (iTunes→MusicBrainz) / SeriesPosterFetch (TVMaze→iTunes).
   Cover renders LEFT of title (COVER_SIDE_SHIFT: title shifts right,
   wraps ~26% narrower, ~0.9× shrink).
4. **Chapter note → Share as review** — BookNotesSheet chapter note field
   gained EXPAND (full white dialog, same AppPreferences slot) + SHARE
   buttons; share opens TopicShareSheet seeded as chapter_review
   (seedReviewText/Chapter win over restored edits).
5. **Collision-push fact drag** — title/info row only travel with the fact
   when the fact actually touches them (4dp gap).
6. **Album fav box** — Whole-box slider added alongside strip width/rows.
7. **Cover colours** — `extractCoverSwatches` pixel-vote HSL histogram
   replaces androidx Palette; `CoverSwatches.dominant` = true majority;
   notesSheetPalette wash keys off dominant; album/series sheets feed the
   RESOLVED artwork URL (they previously used the empty authored imageUrl);
   expanded chapter/episode rows are soft accent tints + borders.
8. **Google Books removed as cover source** (hub picker, reveal fallback,
   share cascade) — books fall back iTunes → Open Library; ratings/ISBN
   lookups keep Google Books keyless.

## Follow-up: "does the auto-adjuster expand the box / shrink text?" (fixed)

Checked: it DID expand (line budget) but only past ~130 chars, and the fact
font never shrank via auto-fit (each style's built-in length curve only
kicks in at 180–350+ chars). Fixed in `TopicShareCard.kt`: the Balanced
growth curve now starts at 90 chars (1.15×) and a new `factScale`
(`autoFactScale`, per-style floors 0.86–0.90, starts ~150 chars) shrinks
the FACT font on top of the built-in curves, applied via
`effectiveBodyScale = bodyScale * autoFit.factScale * move.factScale` so
preview/export/typing-field stay in sync. `factScale` is seeded into
`ShareCardMove` (persisted, counts as touched) so the manual-grab handoff
doesn't pop the text back or clip it; Reset restores full size.

## Notes for the next request

- Changelog (fastlane `20260921.txt`) + app/AGENTS.md v371 bullet updated.
- CI will compile-check (no Gradle in this environment). One risk spot:
  the palette extractor's `while` loop + `best()` calls (pure Kotlin, no
  API surprises expected), and the new `Dialog` imports in TopicRevealScreen.