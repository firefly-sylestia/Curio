#!/usr/bin/env python3
"""v224 - Drawer constellation: Material ink, light-mode visibility, centering."""

def apply(path, pairs):
    src = open(path, encoding="utf-8").read()
    for old, new in pairs:
        n = src.count(old)
        assert n == 1, f"{path}: pattern matched {n} times:\n{old[:160]}"
        src = src.replace(old, new)
    open(path, "w", encoding="utf-8").write(src)
    print(f"OK {path} ({len(pairs)} edits)")

C = "app/src/main/java/com/curio/app/ui/components/CurioConstellation.kt"

NEW_PARAM = (
    "    modifier: Modifier = Modifier,\n"
    "    plainBackground: Boolean = false,\n"
    "    // v224 - MATERIAL ink mode (the drawer's curiosity map): lines and\n"
    "    // stars wear THEME ROLES (onSurfaceVariant lines, primary explored\n"
    "    // stars, dim unexplored) instead of the fixed pale cosmic palette,\n"
    "    // whose near-white stars were invisible on the cream drawer surface\n"
    "    // in light mode. The Stats deep-space page stays on the SVG palette.\n"
    "    materialInk: Boolean = false,\n"
    "    popoverContent: (@Composable (CategoryId) -> Unit)? = null\n"
)

apply(C, [
    (
        "    modifier: Modifier = Modifier,\n"
        "    plainBackground: Boolean = false,\n"
        "    popoverContent: (@Composable (CategoryId) -> Unit)? = null\n",
        NEW_PARAM,
    ),
    (
        "    val isDark = isCurioDarkTheme()\n"
        "    val pageBg = MaterialTheme.colorScheme.background\n",
        "    val isDark = isCurioDarkTheme()\n"
        "    val pageBg = MaterialTheme.colorScheme.background\n"
        "    // v224 - theme-role inks for material mode.\n"
        "    val matLine = MaterialTheme.colorScheme.onSurfaceVariant\n"
        "    val matPrimary = MaterialTheme.colorScheme.primary\n",
    ),
])

# Remove the old hardcoded-guess auto-zoom effect (relocated + fixed below).
src = open(C, encoding="utf-8").read()
start = src.find("    // Auto-zoom to star on tap (when 3D zoom enabled) + star-based tilt\n")
assert start != -1, "old effect anchor missing"
end_marker = "    BoxWithConstraints(modifier = modifier) {"
end = src.find(end_marker)
assert end > start, "BoxWithConstraints anchor order wrong"
src = src[:start] + src[end:]
open(C, "w", encoding="utf-8").write(src)
print("OK removed old auto-zoom effect")

apply(C, [
    (
        "        val hPx = with(density) { maxHeight.toPx() }\n",
        "        val hPx = with(density) { maxHeight.toPx() }\n"
        "\n"
        "        // v224 - CENTERING FIX: the old effect lived OUTSIDE the layout\n"
        "        // scope and guessed the distance with a hardcoded 80px constant,\n"
        "        // so the tapped star never landed at the center (and ignored the\n"
        "        // zoom factor entirely). Now it reads the REAL canvas size and\n"
        "        // cancels the scale-out about the layer's center pivot exactly:\n"
        "        // rendered(p) = z*(p-c) + c + t  =>  t = -z*(p-c) puts the tapped\n"
        "        // star at the center at zoom 2.\n"
        "        LaunchedEffect(selected, zoom3d, wPx, hPx) {\n"
        "            if (selected != null && zoom3d) {\n"
        "                val star = stars.getOrNull(explored.indexOf(selected)) ?: return@LaunchedEffect\n"
        "                val dx = (star.nx - 0.5f) * wPx\n"
        "                val dy = (star.ny - 0.5f) * hPx\n"
        "                tiltY = (star.nx - 0.5f) * 10f\n"
        "                tiltX = -(star.ny - 0.5f) * 6f\n"
        "                coroutineScope {\n"
        "                    launch { zoom.animateTo(2f, spring(dampingRatio = 0.7f, stiffness = 200f)) }\n"
        "                    launch { offsetX.animateTo(-dx * 2f, spring(dampingRatio = 0.7f, stiffness = 200f)) }\n"
        "                    launch { offsetY.animateTo(-dy * 2f, spring(dampingRatio = 0.7f, stiffness = 200f)) }\n"
        "                }\n"
        "            } else {\n"
        "                coroutineScope {\n"
        "                    launch { zoom.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 200f)) }\n"
        "                    launch { offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 200f)) }\n"
        "                    launch { offsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 200f)) }\n"
        "                }\n"
        "                tiltX = 0f; tiltY = 0f\n"
        "            }\n"
        "        }\n",
    ),
    (
        "            label = \"nebulaPulse\"\n"
        "    )\n",
        "            label = \"nebulaPulse\"\n"
        "    )\n"
        "    // v224 - star twinkle phase (drives EVERY named star's gentle sine\n"
        "    // alpha in material mode - the map feels alive, not printed) and an\n"
        "    // expanding pulse ring on the SELECTED star.\n"
        "    val twinklePhase by infiniteTransition.animateFloat(\n"
        "        initialValue = 0f,\n"
        "        targetValue = (2f * PI).toFloat(),\n"
        "        animationSpec = infiniteRepeatable(\n"
        "            animation = tween(durationMillis = 7000, easing = androidx.compose.animation.core.LinearEasing),\n"
        "            repeatMode = RepeatMode.Restart\n"
        "        ),\n"
        "        label = \"twinklePhase\"\n"
        "    )\n"
        "    val selPulse by infiniteTransition.animateFloat(\n"
        "        initialValue = 0f,\n"
        "        targetValue = 1f,\n"
        "        animationSpec = infiniteRepeatable(\n"
        "            animation = tween(durationMillis = 1500, easing = androidx.compose.animation.core.LinearEasing),\n"
        "            repeatMode = RepeatMode.Restart\n"
        "        ),\n"
        "        label = \"selPulse\"\n"
        "    )\n",
    ),
])
