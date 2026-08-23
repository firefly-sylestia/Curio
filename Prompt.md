# Prompt.md — current request log

## Request (complete): clear-glass blobs + light-mode active ink (v235) — committed & pushed

User reported, with Clear glass ON: (1) a small circular blob inside the shaded
active indicator and another at the centre of the whole bar capsule; (2) active
indicator category colors/label still unreadable in light mode.

### Blob root causes (two distinct artifacts)
1. The hidden accent-tinted tab-row copy (recorded into `tabsBackdrop` so the pill
   refracts COLORED icons) ran its OWN full glass rendering — vibrancy + blur +
   lens + surface wash — before recording. The draggable pill therefore refracted
   a duplicated GLASS RENDER of the whole bar; with frost gone (blur 8→2dp) the
   render's refraction rings became visible as circular blobs inside the indicator
   and mid-capsule. Fix: the hidden row now records PLAIN accent-tinted content;
   the pill applies its own optics.
2. At rest the pill's drawBackdrop had NO effects at all (lens/highlight/shadow are
   all press-scaled), so it drew the combined backdrop nearly RAW — the tinted icon
   copy beneath showed sharply as a "blob". Fix: always-on soft blur on the pill's
   sample (8dp normal / 4dp clear).
Also: lens refraction bands shrink in clear mode (24→14/18dp) on the main bar and
`liquidGlassCapsule`, so short capsules don't fold the top+bottom refraction bands
over each other mid-pill.

### Light-mode active ink
`curioActivePillInk` pairs with the classic SOLID accent fill; the glass indicator
only wears a translucent wash so pastel accents vanished. Glass tabs now compute a
deep saturated hue twin in light mode:
`fromHsl(hsl.h, hsl.s.coerceAtLeast(0.45f), (hsl.l * 0.55f).coerceAtMost(0.30f))`.
Dark mode keeps the classic ink.

Files: CurioLiquidGlassTabBar.kt, LiquidGlassPills.kt, CurioBottomNav.kt +
changelog 20260920.txt + app/AGENTS.md (v235). Balance checks pass ×3.
CI validates compilation on push.
