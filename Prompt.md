# Prompt.md — current request log

## Request: v332 — share-card editor content toggle + Customise gating, app-wide copy polish, CI baseline-profile fix

User's follow-up on the share-card editor (commit 9469b675/58e7efe4 batch):
"i didn't ask you to place the content quick fact / custom fact options at that
place, i asked you to place the toggle and let it open where it was opening
before; the Customise button now just stays like that; there are a lot of em
dashes around the app (not the JSON) — in the app hints etc — rephrase them in
proper premium English, fix lowercase hint starts." An ask_user clarified:
toggle pill in the bottom bar that opens the content panel where it used to
open, whole-app UI copy pass with elegant "old English" register.

Also during the turn, CI failed on the v331 baseline profile
(`expandReleaseArtProfileWildcards`: "Class rules don't support flags, but
'HSP' were specified" — `HSPLcom/curio/app/MainActivity;`): class rules in HRF
take NO flags (flags H/S/P are method-rule-only; "L" is the DEX descriptor
prefix). Profile rewritten to valid shape.

Completed:

1. **Content toggle pill** (`TopicShareCard.kt`, `TopicShareSheet`): while
   editing, the bottom bar (where Save/Share/Text sit) shows ONE pill labelled
   with the card's current content (`activeSource.label`) + arrow icon. Tapping
   it opens/closes the "source" tool panel — the SAME panel the toolbar's
   Content tool opens — which again holds the content source pills (Quick fact
   / No fact / saved sources / + Custom fact) restored to where they were
   before v330. Tapping Done still returns Save/Share/Text.
2. **Customise pill gated**: the floating Customise button now renders only
   when `!editMode`; mid-edit the bottom bar owns Reset/Done/content toggle.
3. **Copy pass** (~159 literal replacements, 30 files): em dashes rephrased
   app-wide in user-facing strings —
   - card meta rows / `metaSeparator` join with " · " (was " — "),
   - quote attributions lose the leading dash ("— $quoteAuthor" → author
     alone), footers unify to "· Stay curious" / "· via Curio",
     "$sharerName — Curio" → "· Curio", stray "~ Stay Curious" removed,
   - tool headings "$selName font" / "$selName format", "Tone · level
     unlocks", adjust hint split into two sentences,
   - full-sentence hints/empty states (pet designer, stats, settings,
     experiments, picker, updates, reveal dialog) rephrased into period- or
     semicolon-joined prose; short status pairs use the middot.
   Deliberately NOT touched: JSON/topic content, chapter/page ranges
   ("Books IV–VIII", "pp. 12–14"), name-qualifier parsing (" — " match
   delimiters), the exported pet-design format headers, crash/log text, and
   the waveform time range. (Missing-at-first, then added: UpdateChecker
   notification copy, GlassWidgetLab auto-detect copy.)
4. **Baseline profile HRF fix** (`app/src/main/baseline-prof.txt`): rewritten
   to valid rules — class-only line `Lcom/curio/app/MainActivity;` (no flags),
   single classes AOT'd via `HSPLcom/.../Class;->**(**)**:`, package-wide via
   `<pkg>/**->**(**)**`, hot libs via their packages. Header documents the
   syntax lesson. This fixes the release-build CI failure.

## Verification

- All modified files re-lexed (string literals balanced, no unterminated
  strings); `git diff --check` clean.
- Audit of remaining dashes in code literals shows only intentional ones
  (ranges, parsers, log text, format headers).
- CI validates the profile parse + compile — Gradle not run locally per
  project rules.

## Notes / follow-ups

- The v330 changelog bullet describing content pills directly in the bottom
  bar is superseded by the new toggle bullet (both remain in the store
  changelog per its per-commit append rule).
