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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
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
import com.curio.app.ui.theme.CurioIcon
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.curio.app.ui.theme.CurioIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    // v268 — MANUAL PICK: auto-detecting the system wallpaper fails on many
    // devices (permission-gated), so the lab has an explicit "Set wallpaper"
    // button using the photo picker — no permission needed, and the picked
    // image loads instantly over the gradient fallback.
    var wallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    // v271 — a one-line status after auto-detect attempts (success/failure).
    var detectStatus by remember { mutableStateOf<String?>(null) }
    // v272 — auto-detect decodes off the main thread; this scope hosts it.
    val autoDetectScope = rememberCoroutineScope()
    // v273 - when the user leaves to grant All-files access, re-run the
    // detection ladder automatically on return.
    var pendingDetectAfterGrant by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                pendingDetectAfterGrant
            ) {
                pendingDetectAfterGrant = false
                autoDetect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun decodeUri(uri: android.net.Uri): ImageBitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input)?.asImageBitmap()
        }
    }.getOrNull()

    // v271 — WallpaperExport-style AUTO-DETECT: try every strategy the OS
    // allows, most-permissive first. On Android 13+ getDrawable is
    // permission-gated (MANAGE_EXTERNAL_STORAGE per the WallpaperExport
    // project), so failure is EXPECTED there — the manual picker stays the
    // reliable path and the status line says so instead of failing silently.
    // Same deliberate suppression as the entry load below:
    // READ_WALLPAPER_INTERNAL is signature-only, so lint can't be
    // satisfied — failures are caught and reported in the status.
    @android.annotation.SuppressLint("MissingPermission")
    fun autoDetect() {
        wallpaper = null
        detectStatus = "Detecting device wallpaper…"
        val scope = autoDetectScope
        scope.launch(Dispatchers.IO) {
            val found = runCatching {
                val wm = WallpaperManager.getInstance(context)
                // 1. Direct file descriptor (works on many OEM builds).
                runCatching {
                    wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                        android.graphics.BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                            ?.asImageBitmap()
                    }
                }.getOrNull()
                    // 2/3. Classic drawables.
                    ?: runCatching { wm.getDrawable() as? BitmapDrawable }.getOrNull()?.bitmap?.asImageBitmap()
                    ?: runCatching { wm.peekDrawable() as? BitmapDrawable }.getOrNull()?.bitmap?.asImageBitmap()
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (found != null) {
                    wallpaper = found
                    AppPreferences.setGlassLabWallpaperUri(context, "auto")
                    detectStatus = "Device wallpaper detected."
                } else {
                    detectStatus = "Auto-detect blocked \u2014 tap Grant access to allow wallpaper reading." — pick an image below."
                }
            }
        }
    }

    // Entry: reload the persisted pick (or re-run auto-detect if it was the
    // last chosen source).
    LaunchedEffect(Unit) {
        val saved = AppPreferences.getGlassLabWallpaperUri(context)
        when {
            saved.startsWith("content:") -> {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        android.net.Uri.parse(saved),
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                decodeUri(android.net.Uri.parse(saved))?.let {
                    wallpaper = it
                    return@LaunchedEffect
                }
                autoDetect()
            }
            saved == "auto" -> autoDetect()
        }
    }
    val pickWallpaper = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            AppPreferences.setGlassLabWallpaperUri(context, uri.toString())
            decodeUri(uri)?.let { wallpaper = it }
            detectStatus = "Wallpaper set from picked image."
        }
    }

    // READ_WALLPAPER_INTERNAL is signature-only and MANAGE_EXTERNAL_STORAGE
    // is far too broad to request — but getDrawable() works for the caller's
    // own preview on real devices (and everything here is wrapped in
    // runCatching with a gradient fallback), so lint's hard requirement is
    // suppressed deliberately.
    @android.annotation.SuppressLint("MissingPermission")
    LaunchedEffect(Unit) {
        wallpaper = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = WallpaperManager.getInstance(context).drawable
                (drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
            }.getOrNull()
        }
    }

    // v267 — the lab runs the RAW Kyant recipe (drawBackdrop + vibrancy +
    // blur + lens) DIRECTLY, decoupled from the global Liquid-glass toggle —
    // this is a test bed, so real refraction is unconditional on Android 12+
    // regardless of any Appearance setting. (The old build gated everything
    // behind the toggle AND routed through liquidGlassCapsule, whose fallback
    // paths could serve a non-refracting pane — that's why it looked flat.)
    val glassOn = Build.VERSION.SDK_INT >= 31

    // Fallback wallpaper: a rich multi-stop gradient so the lab still works
    // when no bitmap wallpaper is set (live wallpapers return nothing).
    val fallbackBrush = Brush.linearGradient(
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
                        "The lab needs Android 12+ to render real refracting glass over your wallpaper.",
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
            // v274 - the One UI FROST comparison tile: exactly what the real
            // home-screen widget looks like (launcher-side wallpaper blur +
            // baked pane) - no in-app refraction, by design.
            var frostPos by remember { mutableStateOf(IntOffset(40, 590)) }

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

            // v274 - One UI FROST tile (the shipped widget's look): a baked
            // gradient pane + rim over the launcher-blurred wallpaper. Drag it
            // next to the liquid-glass shapes to compare what each can do.
            Box(
                modifier = Modifier
                    .offset { frostPos }
                    .size(196.dp, 58.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0x59FFFFFF),
                                    Color(0x2E3A3A44)
                                )
                            ),
                            cornerRadius = CornerRadius(28.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xA6FFFFFF),
                            style = Stroke(width = 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(28.dp.toPx())
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            frostPos = IntOffset(
                                (frostPos.x + amount.x).roundToInt().coerceAtLeast(8),
                                (frostPos.y + amount.y).roundToInt().coerceAtLeast(60)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Curio \u00b7 5-day streak",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0x66000000),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 3f
                        )
                    )
                )
            }
        }

        // ── Wallpaper source pills — top-end row ─────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
                .align(Alignment.TopEnd)
        ) {
            Surface(
                onClick = { detectStatus = null; autoDetect() },
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Text(
                    "Auto-detect",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Surface(
                onClick = {
                    pickWallpaper.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Text(
                    "Set wallpaper image",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // v273 - Android 13+ gates wallpaper reading behind All-files
            // access. Open the exact Settings page; auto-detect re-runs when
            // the user comes back.
            Surface(
                onClick = {
                    pendingDetectAfterGrant = true
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:" + context.packageName)
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }.onFailure {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Text(
                    "Grant access",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        detectStatus?.let { msg ->
            Text(
                msg,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
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
                    // v267 — raw recipe here too (see LabGlassShape).
                    if (glassOn) Modifier.drawBackdrop(
                        backdrop = wallLayer,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(6.dp.toPx())
                            lens(18.dp.toPx(), 18.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
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

/**
 * One draggable liquid-glass widget shape sampling [backdrop]. v267 — uses
 * the RAW drawBackdrop recipe (vibrancy + blur + lens), the exact nav-bar
 * effects, with NO toggle gating and NO clip in front of it — guaranteed
 * real refraction of the wallpaper on Android 12+.
 */
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
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(24.dp.toPx(), 24.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
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
