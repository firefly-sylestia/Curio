# Prompt — Implement the long-note detail lag fixes (logcat follow-up)

## Request
Implement the three fixes agreed after the earlier logcat triage of the
saved-note detail page lagging on very long notes (glass OFF): cache the
AnnotatedString, gate layerBackdrop off when glass is off, budget the
paper-card canvas.

## Root causes (from the logcat triage)
1. Saved-note bodies rebuilt a full `AnnotatedString` via
   `buildRichAnnotated(...)` on every recomposition of their subtree.
2. The detail page applied kyant's `.layerBackdrop(detailGlassBackdrop)`
   UNCONDITIONALLY — with glass off there is no consumer, yet the whole
   (potentially giant) page kept being recorded to an offscreen layer,
   which matched the capture's idle ~60fps redraw bursts + texture-sized
   LOS churn even with glass OFF.
3. The paper-card decor (texture clouds + speck field, coffee stains)
   was built per-DRAW: each redraw seeded `kotlin.random.Random`,
   allocated 4+ radial-gradient `Brush`es, organic `Path`s and the speck
   scatter. The canvas re-records while typing / when the page invalidates,
   so those per-draw allocations were the large-object churn driver.

## Changes
1. `RichTextEditor.kt` — new `@Composable rememberRichAnnotated(text,
   spans, highlightColor)` remembering the built AnnotatedString keyed on
   its inputs. `EntryDetailScreen.kt`'s seven read-only note render sites
   (note, review, journal, quote card, observed / surprised / learn-next
   field notes) now use it, so parent recompositions reuse the same
   instance and the giant `Text` can skip re-layout. The live editor keeps
   calling `buildRichAnnotated` directly (spans change per keystroke).
2. `EntryDetailScreen.kt` — the `layerBackdrop` capture is gated on
   `isInScreenGlassActive()` (the same predicate the sticky pills use):
   `.then(if (detailGlassCaptureOn) Modifier.layerBackdrop(...) else
   Modifier)`. Holder + nullable backdrop plumbing unchanged; pills only
   consume the capture when glass is active.
3. `PaperCard.kt` — the decor canvas on BOTH `PaperCard` and
   `TornPaperCard` became a `Spacer` + `Modifier.drawWithCache`: the
   seeded geometry is BUILT once per (size, seed, palette) and only
   REPLAYED per draw. New private `PaperTextureSpec` / `CoffeeStainsSpec`
   holders mirror the deleted per-draw helpers with identical Random
   sequences (byte-identical rendering per size); rules / red margin /
   sheen replay inline from cached scalars. `key(...)` on the palette +
   flag inputs forces a fresh cache when only those change at the same
   size (editor color toggles). Sheen hoisted via `remember` on PaperCard.
   Note: `TopicShareCard.kt` has its own unrelated private
   `drawPaperTexture(palette)` — untouched.

## Verification
- No Gradle in this environment (forbidden; CI validates on push).
- Brace/paren balance: all three files report ZERO imbalance vs a clean
  HEAD baseline (PaperCard +45 parens / +15 braces, symmetric).
- Read-backs of every edited region; call-site greps confirm no stale
  references and no other callers of the removed helpers.
- Changelog (20260921.txt) updated with a FIX: Performance bullet.

## Status
Complete. Committed and pushed. Residual (from the same triage, NOT in
this request's scope): the detail body is still one non-lazy
Column(verticalScroll) — a lazy/capped note renderer is the follow-up if
the remaining hot spots persist.
