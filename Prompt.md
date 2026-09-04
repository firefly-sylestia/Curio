# Prompt — Logcat triage: saved-note detail lags on very long notes (no liquid glass)

## Request
User pasted a logcat taken with liquid glass OFF and asked:
1. What issues does the log show?
2. In the detail view of saved notes, when the note text is very long, how
   much lag is visible in the logs?

## Analysis (no code changes yet — diagnosis only)
The pasted capture (2026-09-04 09:12:19 → 09:13:15, MainActivity) shows:

- **GC storm / allocation churn** — the dominant signal. Background
  concurrent mark-compact GC fires roughly every ~0.4–0.6 s, each cycle
  freeing 33–76 MB; ~120 MB/s of garbage in the densest stretch
  (09:12:23.2–24.2 freed 122 MB in ~1 s). Heap saw-tooths up to
  111 MB live / 159 MB total (09:12:34.781). GC pauses reach ~2–5 ms.
- **Large-object (LOS) churn during interaction** — single GCs free
  12–44 MB of LOS objects (big arrays/bitmaps) while the user scrolls
  (09:12:34.7–35.5: 5 GCs in 0.7 s, ~168 MB of LOS total). This is the
  "note too long" signature: giant text/spans rebuilt per recomposition +
  heavy paper-card canvas work.
- **Continuous redraw bursts with zero input** — ~60 fps dispatchDraw /
  recreateChildDisplayList bursts (09:12:20.3–24.3, 29.8–31.9) with no
  pointer events at all; the UI never idles on the page in view.
- **No Choreographer / "Skipped N frames" lines** in the capture, so exact
  dropped-frame counts can't be read; jank must be inferred from GC cadence
  + frame-log gaps. Recommend capturing `Choreographer`/gfxinfo to quantify.

### Code suspects for the long-note detail lag (EntryDetailScreen.kt /
PaperCard.kt, verified by reading)
1. Whole detail body = one NON-LAZY `Column(verticalScroll)` (hero + meta +
   body). A giant note makes one huge always-composed subtree; every
   invalidation above it re-records everything.
2. Note text rendered as a single `Text(buildRichAnnotated(note, spans…))`
   built in composition — a fresh full AnnotatedString on every
   recomposition of that node (LOS-size arrays for long notes).
3. `PaperCard`/`NotePaperCard` canvas work scales with card height
   (ruled-line while-loop + `drawPaperTexture` specks/gradients per draw);
   an unbounded-height note card redraws all of it on each invalid frame.
4. Quote-card `onTextLayout` truncation loop mutates text state during
   layout → measure/layout churn on cards with long quotes.
5. Note: liquid glass OFF still shows the churn — glass is not the cause.

## Status
Diagnosis delivered; no code changed. Open follow-ups: capture with
Choreographer for frame counts; then fix (lazy body / remember AnnotatedString /
texture budget / layout-safe truncation).
