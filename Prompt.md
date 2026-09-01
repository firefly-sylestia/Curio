# Prompt.md — current request log

## Request (DONE): Signature share cards — full redesign for 7 categories (normal + Deepen)

User: the classic share styles (Paper, Clean, Collage, Editorial, Minimal)
are loved — keep them. The SIGNATURE cards for **Animals, Animated Films,
Anime, Artists, Artworks, Astronomy, Authors** are bad ("some are just so
bad") — scrap the current ones and fully redesign them with NEW fonts,
creative style and a beautiful minimal look, researched properly. Redesign
BOTH the normal variant AND the Deepen option. Keep the classic ones.

### What shipped (this turn)

- **TopicShareCard.kt — `signatureDesign` (normal) redesigned for the 7
  categories** (classic `signatureDesignClassic` + base styles untouched):
  - **Artists** — concert poster: single warm amber spotlight cone on a
    dark stage, hairline floor + glow pools, **Bebas Neue** 42sp title,
    POSTER layout.
  - **Animals** — naturalist field note: sage botanical sprig + seed dots
    on warm cream, **Lora** serif title, CENTERED.
  - **Animated Films** — storybook pastel: thin five-band rainbow arc +
    placed sparkles on lavender wash, **Corben** rounded display, CENTERED.
  - **Anime** — rising-sun poster: vermilion sun + ground line + brush
    stroke on paper white, **Maven Pro** title, CENTERED.
  - **Artworks** — quiet gallery: one thin framed abstract + floor hairline
    on warm gallery white, **Cormorant Garamond** title, STANDARD.
  - **Authors** — literary manuscript: faint ruled lines + red margin +
    flourish on cream paper, **Playfair Display** title, STANDARD.
  - **Astronomy** — star chart: constellation + thin ringed planet +
    coordinate ticks on deep navy, **Space Mono** title, BOTTOM.
- **`signatureDesignDetailed` (Deepen) redesigned for the same 7** — richer
  scenes in the SAME design language: Artists (light rig + crowd + sound
  arcs), Animals (forest clearing: sprig cluster, paw trail, fireflies),
  Animated Films (full rainbow + film-frame cel + confetti), Anime (sun rays
  + torii gate + petals), Artworks (two spotlight cones + two pieces),
  Authors (inkwell + quill under lamp glow), Astronomy (nebula + shooting
  star). Layouts match their normal counterparts.
- **Fix:** the detailed Animated-Films branch now matches
  `"ANIMATED FILMS" || "ANIMATED MOVIES"` (was keyed only on the legacy
  name → fell through to the fallback).
- Design research: 2025 minimal-poster trends (bold typography as the
  design, few elements, heavy emphasis) informed the per-category font/palette
  choices; all fonts drawn from the existing bundled library (no new assets).
- DOX: app/AGENTS.md v3xx3 bullet + fastlane changelog updated.

### Progress
- [x] Research minimal design directions.
- [x] Redesign `signatureDesign` branches (7 categories, normal).
- [x] Redesign `signatureDesignDetailed` branches (7 categories, Deepen).
- [x] Verify classic/base styles untouched; syntax sanity (brace balance ok,
      paren delta pre-existing).
- [x] DOX pass (AGENTS.md + changelog).
- [ ] Commit & push (pending this turn).
