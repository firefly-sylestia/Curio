package com.curio.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.random.Random

// ── Curio brand palette — mirrors CurioColors ───────────────────────────────
// Brand tint accents (used as translucent tints on BOTH themes).
internal val Coral = Color(0xFFFF8FA3)           // CoralBlush — brand primary tint
internal val Butter = Color(0xFFFFD97D)          // ButterYellow — brand secondary tint

// Light theme (warm paper).
private val PaperCream = Color(0xFFF7F0E4)      // SoftCream — background
private val SoftSand = Color(0xFFF6EFE4)        // surface container
private val CardWhite = Color(0xFFFFFBF5)       // CreamWhite — cards
private val Ink = Color(0xFF3A2B20)             // warm brown ink
private val InkSoft = Color(0xFF8A7660)         // muted ink
private val CoralInk = Color(0xFFE2556B)        // deep rose — readable on light
private val GoldInk = Color(0xFFB8860B)         // deep gold — readable on light

// Dark theme (warm near-black).
private val DarkPaper = Color(0xFF1F1813)
private val DarkSand = Color(0xFF2C241D)
private val DarkCard = Color(0xFF2A211B)
private val DarkInk = Color(0xFFF0E6D7)
private val DarkInkSoft = Color(0xFFB3A18A)
private val DarkCoral = Color(0xFFFF9DB0)       // light coral — readable on dark

// Rose hero banner (Home) — theme-aware fill + ink.
internal val RoseHeroLight = Color(0xFFFF8FA3)
internal val RoseHeroInkLight = Color(0xFF6E1B2E)
internal val RoseHeroDark = Color(0xFF4A141F)
internal val RoseHeroInkDark = Color(0xFFFFD9DE)

// ── Persisted keys (DesktopPreferences JSON store) ──────────────────────────
private const val PREF_LANE = "lane"
private const val PREF_TOPIC = "topic"
private const val PREF_DARK = "dark"
private const val PREF_SCREEN = "screen"
private const val PREF_WIN_W = "windowW"
private const val PREF_WIN_H = "windowH"
private const val PREF_WIN_X = "windowX"
private const val PREF_WIN_Y = "windowY"

/** The desktop app's top-level screens (Android bottom-nav parity). */
internal enum class DesktopScreen(val label: String) {
    HOME("Home"),
    SPIN("Spin"),
    CABINET("Cabinet"),
    SETTINGS("Settings")
}

// ── Shared app state (top level of the shell) ───────────────────────────────
internal class CurioShellState {
    var screen by mutableStateOf(
        runCatching { DesktopScreen.valueOf(DesktopPreferences.get(PREF_SCREEN, "HOME")) }
            .getOrDefault(DesktopScreen.HOME)
    )
    var selectedSlug by mutableStateOf(DesktopPreferences.get(PREF_LANE, "artists"))
    var currentTopic by mutableStateOf<DesktopTopic?>(null)
    var browseMode by mutableStateOf(false)
    var darkMode by mutableStateOf(DesktopPreferences.getBoolean(PREF_DARK, false))
}

internal val shell = CurioShellState()

fun main() = application {
    // Restore the last window size (defaults to a tablet-ish starting size).
    val state = rememberWindowState(
        width = DesktopPreferences.getInt(PREF_WIN_W, 1120).dp,
        height = DesktopPreferences.getInt(PREF_WIN_H, 760).dp
    )
    // Restore the last window position — only when the saved coords are
    // non-negative (an off-screen value would open the window out of reach).
    val savedX = DesktopPreferences.getInt(PREF_WIN_X, -1)
    val savedY = DesktopPreferences.getInt(PREF_WIN_Y, -1)
    if (savedX >= 0 && savedY >= 0) {
        state.position = WindowPosition(savedX, savedY)
    }
    Window(
        onCloseRequest = {
            saveWindowGeometry(state)
            exitApplication()
        },
        title = "Curio",
        state = state
    ) {
        CurioDesktopApp()
    }
}

/** Persists the window size + position on close so the next launch resumes. */
private fun saveWindowGeometry(state: WindowState) {
    val size = state.size
    DesktopPreferences.setInt(PREF_WIN_W, size.width.value.toInt())
    DesktopPreferences.setInt(PREF_WIN_H, size.height.value.toInt())
    val pos = state.position
    if (pos.isSpecified) {
        DesktopPreferences.setInt(PREF_WIN_X, pos.x)
        DesktopPreferences.setInt(PREF_WIN_Y, pos.y)
    }
}

@Composable
fun CurioDesktopApp() {
    // Persist shell settings as they change (lane, landed topic, theme, tab).
    LaunchedEffect(shell.selectedSlug) {
        DesktopPreferences.set(PREF_LANE, shell.selectedSlug)
    }
    LaunchedEffect(shell.currentTopic?.id) {
        DesktopPreferences.set(PREF_TOPIC, shell.currentTopic?.id ?: "")
    }
    LaunchedEffect(shell.darkMode) {
        DesktopPreferences.setBoolean(PREF_DARK, shell.darkMode)
    }
    LaunchedEffect(shell.screen) {
        DesktopPreferences.set(PREF_SCREEN, shell.screen.name)
    }
    // Restore the last landed topic for the current lane on cold start.
    LaunchedEffect(Unit) {
        if (shell.currentTopic == null) {
            val id = DesktopPreferences.get(PREF_TOPIC, "")
            if (id.isNotBlank()) {
                shell.currentTopic =
                    DesktopCatalog.load(shell.selectedSlug).firstOrNull { it.id == id }
            }
        }
    }
    MaterialTheme(
        colorScheme = if (shell.darkMode) darkColorScheme(
            primary = DarkCoral,
            onPrimary = Color(0xFF5C0E1E),
            secondary = Butter,
            onSecondary = Color(0xFF4A3500),
            background = DarkPaper,
            onBackground = DarkInk,
            surface = DarkCard,
            onSurface = DarkInk,
            surfaceVariant = DarkSand,
            onSurfaceVariant = DarkInkSoft
        ) else lightColorScheme(
            primary = CoralInk,
            onPrimary = Color.White,
            secondary = Butter,
            onSecondary = GoldInk,
            background = PaperCream,
            onBackground = Ink,
            surface = CardWhite,
            onSurface = Ink,
            surfaceVariant = SoftSand,
            onSurfaceVariant = InkSoft
        )
    ) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (shell.screen) {
                        DesktopScreen.HOME -> DesktopHome()
                        DesktopScreen.SPIN -> DesktopSpin()
                        DesktopScreen.CABINET -> DesktopCabinet()
                        DesktopScreen.SETTINGS -> DesktopSettings()
                    }
                }
                BottomNav()
            }
        }
    }
}

/** Bottom navigation bar — Home · Spin · Cabinet · Settings (Android parity). */
@Composable
private fun BottomNav() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            DesktopScreen.entries.forEach { screen ->
                val active = shell.screen == screen
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { shell.screen = screen }
                ) {
                    Text(
                        screen.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp)
                    )
                }
            }
        }
    }
}

/** Picks a random topic from the pool, avoiding the one already shown. */
internal fun pickRandomTopic(pool: List<DesktopTopic>): DesktopTopic {
    val current = shell.currentTopic
    val candidates = if (current != null && pool.size > 1) {
        pool.filter { it.id != current.id }
    } else {
        pool
    }
    return candidates[Random.nextInt(candidates.size)]
}
