package com.curio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
private val Coral = Color(0xFFFF8FA3)           // CoralBlush — brand primary tint
private val Butter = Color(0xFFFFD97D)          // ButterYellow — brand secondary tint

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

// v27t — persisted keys (DesktopPreferences JSON store).
private const val PREF_LANE = "lane"
private const val PREF_TOPIC = "topic"
private const val PREF_DARK = "dark"
private const val PREF_WIN_W = "windowW"
private const val PREF_WIN_H = "windowH"
private const val PREF_WIN_X = "windowX"
private const val PREF_WIN_Y = "windowY"

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
    // Persist shell settings as they change (lane, landed topic, theme).
    LaunchedEffect(shell.selectedSlug) {
        DesktopPreferences.set(PREF_LANE, shell.selectedSlug)
    }
    LaunchedEffect(shell.currentTopic?.id) {
        DesktopPreferences.set(PREF_TOPIC, shell.currentTopic?.id ?: "")
    }
    LaunchedEffect(shell.darkMode) {
        DesktopPreferences.setBoolean(PREF_DARK, shell.darkMode)
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
            Row(Modifier.fillMaxSize()) {
                CategorySidebar()
                MainPane()
            }
        }
    }
}

// ── Shared app state (kept at the top level of the shell) ───────────────────
private class CurioShellState {
    var selectedSlug by mutableStateOf(DesktopPreferences.get(PREF_LANE, "artists"))
    var currentTopic by mutableStateOf<DesktopTopic?>(null)
    var browseMode by mutableStateOf(false)
    var darkMode by mutableStateOf(DesktopPreferences.getBoolean(PREF_DARK, false))
}

private val shell = CurioShellState()

private fun pickRandomTopic(pool: List<DesktopTopic>): DesktopTopic {
    val current = shell.currentTopic
    val candidates = if (current != null && pool.size > 1) {
        pool.filter { it.id != current.id }
    } else {
        pool
    }
    return candidates[Random.nextInt(candidates.size)]
}

// ── Sidebar: brand + full category list ─────────────────────────────────────
@Composable
private fun CategorySidebar() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(272.dp)
            .fillMaxHeight()
    ) {
        Column(Modifier.padding(vertical = 28.dp)) {
            Text(
                "Curio",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                "Your curiosity, one spin at a time.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "LANES",
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(DesktopCatalog.categories) { cat ->
                    val selected = shell.selectedSlug == cat.slug
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clickable {
                                shell.selectedSlug = cat.slug
                                shell.currentTopic = null
                            }
                            .background(
                                if (selected) Coral.copy(alpha = 0.28f) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            cat.displayName,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${DesktopCatalog.load(cat.slug).size}",
                            fontSize = 12.sp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Main pane: mode toggle + spin deck / browse list ────────────────────────
@Composable
private fun MainPane() {
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Text(
                DesktopCatalog.displayName(shell.selectedSlug),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            ModePill("Spin", !shell.browseMode) { shell.browseMode = false }
            Spacer(Modifier.width(8.dp))
            ModePill("Browse", shell.browseMode) { shell.browseMode = true }
            Spacer(Modifier.width(8.dp))
            // v27t — theme toggle (persisted via the preferences store).
            ModePill(if (shell.darkMode) "Light" else "Dark", shell.darkMode) {
                shell.darkMode = !shell.darkMode
            }
        }
        if (shell.browseMode) {
            BrowseList()
        } else {
            SpinPane()
        }
    }
}

@Composable
private fun ModePill(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

// ── Spin pane: deck (front ticket + 2 peeks) + reveal ───────────────────────
@Composable
private fun SpinPane() {
    val pool = remember(shell.selectedSlug) { DesktopCatalog.load(shell.selectedSlug) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 8.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        DeckStack(pool)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { shell.currentTopic = pickRandomTopic(pool) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(52.dp)
        ) {
            Text(
                "SPIN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 36.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
        RevealCard()
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DeckStack(pool: List<DesktopTopic>) {
    val topic = shell.currentTopic
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Two peek cards behind the front ticket.
        repeat(2) { i ->
            val backTopic = pool.getOrNull(i + 1) ?: pool.getOrNull(0)
            DeckCard(
                title = backTopic?.name ?: "?",
                subtitle = backTopic?.subtype ?: "",
                dim = 0.45f + i * 0.18f,
                modifier = Modifier
                    .width(300.dp)
                    .height(180.dp)
                    .offset(x = (22 + i * 14).dp, y = (18 + i * 10).dp)
            )
        }
        // Front ticket.
        if (topic != null) {
            DeckCard(
                title = topic.name,
                subtitle = topic.subtype,
                dim = 0f,
                modifier = Modifier
                    .width(340.dp)
                    .height(210.dp)
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(340.dp)
                    .height(210.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Pick a lane, then spin.",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckCard(
    title: String,
    subtitle: String,
    dim: Float,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (dim <= 0f) Coral.copy(alpha = 0.10f) else Color.Transparent,
                    RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    subtitle.ifBlank { "Curio" }.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (dim <= 0f) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    fontSize = if (dim <= 0f) 21.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (dim <= 0f) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RevealCard() {
    val topic = shell.currentTopic ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    topic.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(10.dp))
                if (topic.safeByline.isNotBlank()) {
                    Surface(
                        color = Coral.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            topic.safeByline,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                topic.teaser,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${topic.exploreAction.verb} ${topic.exploreAction.targetName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (topic.exploreAction.durationMinutes > 0) {
                        Text(
                            "~${topic.exploreAction.durationMinutes} min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        topic.exploreAction.instruction,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Browse mode: topic list for the selected lane ───────────────────────────
@Composable
private fun BrowseList() {
    val pool = remember(shell.selectedSlug) { DesktopCatalog.load(shell.selectedSlug) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
    ) {
        items(pool, key = { it.id }) { topic ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        shell.currentTopic = topic
                        shell.browseMode = false
                    }
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            topic.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (topic.safeByline.isNotBlank()) {
                            Text(
                                topic.safeByline,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        topic.teaser,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
