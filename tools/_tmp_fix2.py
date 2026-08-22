#!/usr/bin/env python3
"""CI fixes: asPaddingValues import, composable-in-lambda glass bug; + update-notification deep link."""
def apply(path, pairs):
    src = open(path, encoding="utf-8").read()
    for old, new in pairs:
        n = src.count(old)
        assert n == 1, f"{path}: matched {n}:\n{old[:140]}"
        src = src.replace(old, new)
    open(path, "w", encoding="utf-8").write(src)
    print(f"OK {path} ({len(pairs)})")

# ── 1. CabinetScreen: missing import ────────────────
apply("app/src/main/java/com/curio/app/features/cabinet/CabinetScreen.kt", [
    (
        "import androidx.compose.foundation.layout.navigationBars\n",
        "import androidx.compose.foundation.layout.asPaddingValues\n"
        "import androidx.compose.foundation.layout.navigationBars\n",
    ),
])

# ── 2. LiquidGlassPills: composable call inside a plain lambda ──
GLASS = "app/src/main/java/com/curio/app/ui/components/LiquidGlassPills.kt"
apply(GLASS, [
    (
        """@Composable
fun Modifier.liquidGlassCapsule(container: Color): Modifier {
    if (!isLiquidGlassPillsActive()) return this
    val backdrop = CurioGlassPills.backdrop ?: return this
    return this.then(
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(8.dp.toPx())
                lens(24.dp.toPx(), 24.dp.toPx())
            },
            highlight = { Highlight.Default },
            shadow = {
                Shadow.Default.copy(
                    color = Color.Black.copy(alpha = if (isCurioDarkTheme()) 0.20f else 0.10f)
                )
            },""",
        """@Composable
fun Modifier.liquidGlassCapsule(container: Color): Modifier {
    if (!isLiquidGlassPillsActive()) return this
    val backdrop = CurioGlassPills.backdrop ?: return this
    // Hoisted: the drawBackdrop lambdas are PLAIN functions (not
    // @Composable), so the theme read must happen here in composition.
    val shadowAlpha = if (isCurioDarkTheme()) 0.20f else 0.10f
    return this.then(
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(8.dp.toPx())
                lens(24.dp.toPx(), 24.dp.toPx())
            },
            highlight = { Highlight.Default },
            shadow = {
                Shadow.Default.copy(color = Color.Black.copy(alpha = shadowAlpha))
            },""",
    ),
])

# ── 3a. PendingUpdatesOpen handoff object ───────────
apply("app/src/main/java/com/curio/app/navigation/CurioRoutes.kt", [
    (
        """object CurioRoutes {
""",
        """/**
 * Out-of-band handoff for the update-notification tap.
 *
 * The update-available notification carries a boolean extra so tapping it
 * opens the app ON the Updates page (the screen that offers check /
 * download / install) instead of plain Home — same contract as
 * [PendingSpinOpen]: stashed here because MainActivity may be cold-started
 * (onCreate) or already running (onNewIntent), consumed once by the NavHost
 * when it reaches a stable root route.
 */
object PendingUpdatesOpen {
    const val EXTRA_OPEN_UPDATES = "com.curio.app.extra.OPEN_UPDATES"

    private var pending = false
    // Compose-observable bump: capture() may run from MainActivity (outside
    // composition), so the NavHost must recompose when it fires — a plain
    // Boolean would never invalidate the LaunchedEffect key.
    private val counter = mutableIntStateOf(0)

    /** Stashes the updates-open request carried by [intent], if present. */
    fun capture(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATES, false) == true) {
            pending = true
            counter.intValue++
        }
    }

    /** Monotonic bump — the NavHost keys its open-effect on this. */
    val trigger: Int get() = counter.intValue

    /** Consumes and returns whether the Updates page should be opened. */
    fun take(): Boolean {
        val p = pending
        pending = false
        return p
    }
}

object CurioRoutes {
""",
    ),
])

# ── 3b. MainActivity captures the extra on both entry paths ──
MAIN = "app/src/main/java/com/curio/app/MainActivity.kt"
src = open(MAIN, encoding="utf-8").read()
old = """            PendingEntryOpen.capture(intent)
            PendingSpinOpen.capture(intent)"""
assert src.count(old) == 1
src = src.replace(old, old.replace(
    "PendingSpinOpen.capture(intent)",
    "PendingSpinOpen.capture(intent)\n            PendingUpdatesOpen.capture(intent)"
))
old2 = """        PendingEntryOpen.capture(intent)
        PendingSpinOpen.capture(intent)"""
assert src.count(old2) == 1
src = src.replace(old2, old2.replace(
    "PendingSpinOpen.capture(intent)",
    "PendingSpinOpen.capture(intent)\n        PendingUpdatesOpen.capture(intent)"
))
imp = "import com.curio.app.navigation.PendingSpinOpen\n"
assert src.count(imp) == 1
src = src.replace(imp, imp + "import com.curio.app.navigation.PendingUpdatesOpen\n")
open(MAIN, "w", encoding="utf-8").write(src)
print(f"OK {MAIN} (captures + import)")

# ── 3c. UpdateChecker: put the extra on the content intent ──
UPD = "app/src/main/java/com/curio/app/data/UpdateChecker.kt"
apply(UPD, [
    (
        """        val openApp = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(Intent.ACTION_MAIN)""",
        """        val openApp = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(Intent.ACTION_MAIN)
        // v227b — deep link: tapping the notification lands ON the Updates
        // page (check / release notes / install), not plain Home. The extra
        // is stashed by PendingUpdatesOpen and consumed by the NavHost once
        // a stable root route is up.
        openApp.putExtra(PendingUpdatesOpen.EXTRA_OPEN_UPDATES, true)""",
    ),
])
