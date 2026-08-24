package com.curio.app.features.settings

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.ui.components.isLiquidGlassRequested
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * v264 — GLASS WIDGET LAB. A test bed for future home-screen WIDGET designs:
 * the user's CURRENT WALLPAPER is loaded and shown full-screen, and real
 * liquid-glass widget shapes (a round clock tile, a stadium timer pill, a
 * small circular glyph tile) can be dragged freely over it — each one a REAL
 * refracting capsule (vibrancy + blur + lens, the exact nav-bar recipe)
 * sampling the wallpaper through its own capture layer.
 *
 * WHY A LAB: an actual RemoteViews home-screen widget cannot sample the
 * wallpaper per-pixel (it renders in the launcher's process with no backdrop
 * API), so true live refraction is impossible to ship in a real widget today.
 * This screen is where designs get chosen — whatever wins here ships as the
 * closest possible static widget treatment later.
 *
 * Requires the Liquid glass toggle (the recipe needs Android 12+); without
 * it the lab shows a hint instead of glass shapes.
 */
@Composable
fun GlassWidgetLabScreen(navController: NavController) {
    val context = LocalContext.current

    // ── The user's current wallpaper ──────────────────────────────────
    var wallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        wallpaper = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = WallpaperManager.getInstance(context).drawable
                (drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
            }.getOrNull()
        }
    }

    val glassOn = isLiquidGlassRequested() && Build.VERSION.SDK_INT >= 31

    // Fallback wallpaper: a rich multi-stop gradient so the lab still works
    // when no bitmap wallpaper is set (live wallpapers return nothing).
    val fallbackBrush = androidx.compose.foundation.Brush.linearGradient(
        listOf(
            Color(0xFF7E57C2),
            Color(0xFFEF9A9A),
            Color(0xFF80DEEA),
            Color(0xFFFFD54F)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141416))
    ) {
        // ── Wallpaper layer + its capture ─────────────────────────────
        val wallLayer = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (glassOn) Modifier.layerBackdrop(wallLayer) else Modifier)
        ) {
            if (wallpaper != null) {
                Image(
                    bitmap = wallpaper!!,
                    contentDescription = "Current wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(fallbackBrush))
            }
        }

        if (!glassOn) {
            // ── Hint card — the recipe needs Android 12 + the toggle ──
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Glass widget lab",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Turn on Liquid glass in Appearance (Android 12+) to drag real refracting widget shapes over your wallpaper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // ── The draggable glass widget shapes ─────────────────────
            // Positions are free (top-start anchored); every capsule is a
            // sibling of the captured wallpaper Box, so none of them sample
            // themselves.
            var clockPos by remember { mutableStateOf(IntOffset(40, 340)) }
            var timerPos by remember { mutableStateOf(IntOffset(40, 470)) }
            var glyphPos by remember { mutableStateOf(IntOffset(190, 350)) }

            LabGlassShape(
                position = clockPos,
                onDrag = { dx, dy ->
                    clockPos = IntOffset(
                        (clockPos.x + dx).roundToInt().coerceAtLeast(8),
                        (clockPos.y + dy).roundToInt().coerceAtLeast(60)
                    )
                },
                backdrop = wallLayer,
                modifier = Modifier.size(112.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("12:34", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Wed 12", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.85f))
                }
            }

            LabGlassShape(
                position = timerPos,
                onDrag = { dx, dy ->
                    timerPos = IntOffset(
                        (timerPos.x + dx).roundToInt().coerceAtLeast(8),
                        (timerPos.y + dy).roundToInt().coerceAtLeast(60)
                    )
                },
                backdrop = wallLayer,
                modifier = Modifier.width(196.dp).height(58.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)
                ) {
                    CurioIcon(name = CurioIcons.PlayArrow, contentDescription = null, size = 22.dp, tint = Color.White)
                    Text("Exploring · 12m", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }

            LabGlassShape(
                position = glyphPos,
                onDrag = { dx, dy ->
                    glyphPos = IntOffset(
                        (glyphPos.x + dx).roundToInt().coerceAtLeast(130),
                        (glyphPos.y + dy).roundToInt().coerceAtLeast(200)
                    )
                },
                backdrop = wallLayer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(name = CurioIcons.Check, contentDescription = null, size = 24.dp, tint = Color.White)
                }
            }
        }

        // ── Back pill — floats over everything, wears the glass too ────
        Surface(
            onClick = { navController.popBackStack() },
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 0.dp,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .then(
                    if (glassOn) Modifier.liquidGlassCapsule(
                        Color.White,
                        washAlpha = 0.25f,
                        backdrop = wallLayer,
                        alwaysClear = true,
                        shape = CircleShape
                    ) else Modifier
                )
        ) {
            CurioIcon(
                name = CurioIcons.ChevronLeft,
                contentDescription = "Back",
                tint = Color.White,
                size = 24.dp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

/** One draggable liquid-glass widget shape sampling [backdrop]. */
@Composable
private fun LabGlassShape(
    position: IntOffset,
    onDrag: (dx: Float, dy: Float) -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .offset { position }
            .clip(RoundedCornerShape(50))
            .liquidGlassCapsule(
                Color.White,
                washAlpha = 0.25f,
                backdrop = backdrop,
                alwaysClear = true,
                shape = RoundedCornerShape(50)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    onDrag(amount.x, amount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
