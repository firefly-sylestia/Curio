# Prompt.md — current request log

## Request (complete): v231 batch — nav squish, quote card, parallax toggle, constellation centering

User (multi-part): fix the Home nav pills being squished with text/icons cut
("keep it expanded for all"); do the previous requests (constellation
centering + moodboard quote card too wide / "expanding fully to the bottom
from the top"); add the parallax-on-tilt effect as a NEW experiment toggle;
push everything together.

### Changes

1. **Nav-bar squish** (`CurioLiquidGlassTabBar.kt`): the glass bar used
   `width(IntrinsicSize.Min)` on its container + `weight(1f)` tabs. Intrinsic
   MIN width of Text is tiny (soft wrap), so every tab collapsed to a sliver
   and icons/labels clipped. Now: natural content sizing + `widthIn(min =
   64.dp)` floor — always expanded.
2. **Constellation centering** (`CurioConstellation.kt`): two real bugs —
   (a) targets used `(star.nx - 0.5) * wPx` but stars live in a LETTERBOXED
   1400-viewBox (`px(x)=ox+x*s`, s=min(w,h)/1400), wrong whenever w≠h;
   (b) pixel targets were multiplied by density a SECOND time inside
   graphicsLayer → overshoot by the density factor (2.8× on the reporter's
   A35). Fixed both.
3. **Moodboard quote slips** (`MoodBoardZoom.kt`): default slot cap 240→180px,
   resize ceiling 60%→42% of board, and a hard 1.6× cap on textScale — a
   degenerate baseW could explode the font and stretch a slip over the full
   board height ("expanding fully to the bottom from the top").
4. **Glass parallax tilt** (new Experiments toggle, default OFF):
   `GlassParallax.kt` gravity listener (TYPE_GRAVITY, low-pass 0.18,
   dead-zone 0.12) exposes normalized tilt; `liquidGlassCapsule` sways the
   capsule against it via a graphicsLayer block (snapshot reads only — no
   recomposition per frame). Listener lifecycle managed in CurioNavHost
   LaunchedEffect + the Settings toggle.

Verification: delimiter balance OK on all 8 touched files; CI compiles on push.
Docs: changelog ADD bullets + app/AGENTS.md v231 entry. Committed AND pushed
(user asked for everything together).
