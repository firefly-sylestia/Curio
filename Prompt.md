# Current Request — Hole-ring coil redrawn from the revised SVG (truncated arch) + peeks out the card's left edge

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim)
"now i added a better ring this time can u use that instead of the previous one, and also the ring should be come out from the left of it like peek out from the left not entirely inside the stat card." + a revised SVG (`svgviewer-output (12).svg` — same 150×420, three coils, but each coil's path is now truncated: `M38 62 C38 39 54 24 76 24 C98 24 111 37 111 52` — the bottom curl `C111 66 102 75 90 75 …` is GONE; the bounding box is 73×38 instead of 73×51, and the dark depth pass uses the SAME truncated path).

## What changed (v197)
`ui/components/PaperStatCard.kt` → `drawCoilRing` (the default "coil" ring style):
- **New shape**: `CoilOutlineNorm` re-normalized to the revised 73×38 box — start (0,1.0), cubics (0,0.395 / 0.219,0 / 0.521,0) and (0.822,0 / 1,0.342 / 1,0.737). The wire is now a clean arch: up the left, over the top, down the right — no curl-in at the bottom. `coilH` aspect 51/73 → 38/73.
- **New specular**: `CoilSpecularNorm` re-normalized to the same 73×38 box (the SVG's highlight line is unchanged in SVG space: 0.068,0.868 / 0.096,0.395 / 0.274,0.132 / 0.521,0.132 / 0.740,0.132 / 0.890,0.342 / 0.932,0.632).
- **PEEK-OUT from the left**: the coil is pushed LEFT past the card edge — `leftPeek ≈ 9dp`, so its left arc + leg protrude ~6.5dp past the card's left edge like a real spiral binding sticking out of the paper, instead of sitting entirely inside the card. The hole stays centered vertically under the arch; the wire's right leg dives through it. (Works because the fill's `drawWithCache` drawing isn't clipped to the card shape — the Surface doesn't clip its content here, see Home's v74 note.)
- Depth/metal/specular passes unchanged (dark depth + metal + white specular), same wire widths.

## Docs
- `app/AGENTS.md` — v197 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 1 FIX bullet.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). Normalized coordinates verified point-by-point against the revised SVG (box x 38→111 = 73 wide, y 24→62 = 38 tall). "split" / "oblique" styles untouched.
