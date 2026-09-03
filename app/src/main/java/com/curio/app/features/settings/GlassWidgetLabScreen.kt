package com.curio.app.features.settings

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
// NonObservableLocale: the clock re-formats every second via its own
    // ticker, so a locale change applies on the next tick by design.
    @android.annotation.SuppressLint("NonObservableLocale")
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
                    detectStatus = "Auto-detect is blocked. Tap Grant access to allow wallpaper reading."
                }
            }
        }
    }
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
            // ── v279: per-widget state — selection, size, blur, text color ──
            val selectedId = remember { mutableStateOf<String?>(null) }
            var widgetsVisible by remember { mutableStateOf(true) }

            // 1-second ticker: real clock + live session elapsed.
            var now by remember { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    now = System.currentTimeMillis()
                }
            }
            // Locale read is deliberately non-observable: the ticker re-
            // formats every second anyway, and a mid-session locale change
            // just updates on the next tick.
            @android.annotation.SuppressLint("NonObservableLocale")
            fun fmt(pattern: String): String =
                java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                    .format(java.util.Date(now))
            val clock = fmt("HH:mm")
            val dateLine = fmt("EEE · MMM d")
            // Live explore session: "Exploring · Xm" while running, else Explored.
            val activeSession = remember { com.curio.app.data.ExploreSessionStore.getActiveSession(context) }
            val timerText = if (activeSession != null) {
                val mins = (activeSession.elapsedMillis(now) / 60000L).coerceAtLeast(0)
                "Exploring · ${mins}m"
            } else {
                "Explored"
            }
            val streak = remember { com.curio.app.data.StreakTracker.getStreak(context) }
            val batteryPct = remember {
                val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE)
                    as? android.os.BatteryManager
                bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it > 0 } ?: 80
            }

            val clockState = remember { LabShapeState(IntOffset(40, 340)).apply { id = "clock" } }
            val analogState = remember { LabShapeState(IntOffset(210, 210)).apply { id = "analog" } }
            val timerState = remember { LabShapeState(IntOffset(40, 470)).apply { id = "timer" } }
            val streakState = remember { LabShapeState(IntOffset(200, 480)).apply { id = "streak" } }
            val batteryState = remember { LabShapeState(IntOffset(210, 340)).apply { id = "battery" } }
            val dateState = remember { LabShapeState(IntOffset(60, 620)).apply { id = "date" } }
            // v274 - the One UI FROST comparison tile (baked pane, no refraction).
            val frostState = remember { LabShapeState(IntOffset(40, 720)).apply { id = "frost" } }

            fun dragTo(state: LabShapeState, dx: Float, dy: Float) {
                state.pos = IntOffset(
                    (state.pos.x + dx).roundToInt().coerceAtLeast(8),
                    (state.pos.y + dy).roundToInt().coerceAtLeast(60)
                )
            }

            if (widgetsVisible) {
                // ── Clock — REAL time ──
                if (clockState.visible) {
                    LiquidLabShape(clockState, selectedId, wallLayer, modifier = Modifier.size(112.dp)) {
                        Text(
                            clock,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = clockState.textColor
                        )
                    }
                }
                // ── Analog clock — real ticking hands ──
                if (analogState.visible) {
                    LiquidLabShape(analogState, selectedId, wallLayer, modifier = Modifier.size(112.dp)) {
                        Canvas(modifier = Modifier.size(96.dp)) {
                            val r = size.minDimension / 2f
                            val c = center
                            val cal = java.util.Calendar.getInstance()
                            fun hand(angleDeg: Float, lenFrac: Float, widthPx: Float) {
                                val rad = Math.toRadians((angleDeg - 90).toDouble())
                                drawLine(
                                    color = analogState.textColor,
                                    start = c,
                                    end = Offset(
                                        c.x + (lenFrac * r * cos(rad)).toFloat(),
                                        c.y + (lenFrac * r * sin(rad)).toFloat()
                                    ),
                                    strokeWidth = widthPx,
                                    cap = StrokeCap.Round
                                )
                            }
                            drawCircle(
                                color = analogState.textColor.copy(alpha = 0.35f),
                                radius = r,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            val hourAngle =
                                ((cal.get(java.util.Calendar.HOUR_OF_DAY) % 12) + cal.get(java.util.Calendar.MINUTE) / 60f) * 30f
                            val minuteAngle =
                                cal.get(java.util.Calendar.MINUTE) * 6f
                            hand(hourAngle, 0.52f, 4.dp.toPx())
                            hand(minuteAngle, 0.78f, 2.5.dp.toPx())
                            drawCircle(color = Color(0xFFFF8A3C), radius = 3.5.dp.toPx())
                        }
                    }
                }
                // ── Session pill — LIVE elapsed / Explored ──
                if (timerState.visible) {
                    LiquidLabShape(timerState, selectedId, wallLayer, modifier = Modifier.width(196.dp).height(58.dp)) {
                        Text(
                            timerText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = timerState.textColor
                        )
                    }
                }
                // ── Streak ring — fire icon + count ──
                if (streakState.visible) {
                    LiquidLabShape(streakState, selectedId, wallLayer, modifier = Modifier.size(92.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CurioIcon(
                                name = CurioIcons.LocalFire,
                                contentDescription = null,
                                tint = streakState.textColor,
                                size = 22.dp
                            )
                            Text(
                                "$streak",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = streakState.textColor
                            )
                        }
                    }
                }
                // ── Battery — real level ──
                if (batteryState.visible) {
                    LiquidLabShape(batteryState, selectedId, wallLayer, modifier = Modifier.size(92.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CurioIcon(
                                name = "battery_full",
                                contentDescription = null,
                                tint = batteryState.textColor,
                                size = 22.dp
                            )
                            Text(
                                "$batteryPct%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = batteryState.textColor
                            )
                        }
                    }
                }
                // ── Date pill ──
                if (dateState.visible) {
                    LiquidLabShape(dateState, selectedId, wallLayer, modifier = Modifier.width(170.dp).height(58.dp)) {
                        Text(
                            dateLine,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = dateState.textColor
                        )
                    }
                }
                // ── One UI frost tile (baked, non-refracting comparison) ──
                if (frostState.visible) Box(
                    modifier = Modifier
                        .offset { frostState.pos }
                        .clickable { selectedId.value = "frost" }
                        .border(
                            width = if (selectedId.value == "frost") 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .size((196 * frostState.scale).dp, (58 * frostState.scale).dp)
                        .clip(RoundedCornerShape(28.dp))
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x2E3A3A44))),
                                cornerRadius = CornerRadius(28.dp.toPx())
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                dragTo(frostState, amount.x, amount.y)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Curio · $streak-day streak",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = frostState.textColor
                    )
                }
            } else {
                Text(
                    "Widgets hidden",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 120.dp)
                )
            }

            // ── Show/hide all floating widgets ──
            Surface(
                onClick = {
                    widgetsVisible = !widgetsVisible
                    listOf(clockState, analogState, timerState, streakState, batteryState, dateState, frostState)
                        .forEach { it.visible = widgetsVisible }
                },
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 56.dp)
            ) {
                Text(
                    if (widgetsVisible) "Hide all" else "Show all",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // ── Per-widget editor: size · blur · text color ──
            selectedId.value?.let { selId ->
                val st: LabShapeState? = when (selId) {
                    "clock" -> clockState; "analog" -> analogState; "timer" -> timerState
                    "streak" -> streakState; "battery" -> batteryState
                    "date" -> dateState; else -> frostState
                }
                st?.let { sel ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selId.replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                // v282 - PER-WIDGET hide: removes just this one;
                                // "Show all" brings everything back.
                                Text("Hide",
                                    color = Color(0xFFFF9B9B),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        sel.visible = false
                                        selectedId.value = null
                                    }
                                )
                                Spacer(Modifier.width(16.dp))
                                Text("Done", color = Color(0xFF9FBFFF), fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { selectedId.value = null })
                            }
                            Text("Size · ${(sel.scale * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                            Slider(value = sel.scale, onValueChange = { sel.scale = it }, valueRange = 0.6f..1.8f)
                            if (selId != "frost") {
                                Text("Liquid blur · ${sel.blurDp.toInt()}dp", color = Color.White, fontSize = 11.sp)
                                Slider(value = sel.blurDp, onValueChange = { sel.blurDp = it }, valueRange = 2f..20f)
                            }
                            Text("Text hue", color = Color.White, fontSize = 11.sp)
                            val hsv = remember(selId) {
                                FloatArray(3).also { android.graphics.Color.colorToHSV(sel.textColor.toArgb() or 0xFF000000.toInt(), it) }
                            }
                            var textHue by remember(selId) { mutableFloatStateOf(hsv[0]) }
                            Slider(
                                value = textHue, onValueChange = {
                                    textHue = it
                                    val a = sel.textColor.alpha
                                    sel.textColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(it, 0.25f, 1f))).copy(alpha = a)
                                },
                                valueRange = 0f..360f
                            )
                        }
                    }
                }
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
private fun LiquidLabShape(
    state: LabShapeState,
    selectedId: androidx.compose.runtime.MutableState<String?>,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val selected = selectedId.value == state.id
    Box(
        modifier = modifier
            .offset { state.pos }
            .graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    vibrancy()
                    blur(state.blurDp.dp.toPx())
                    lens(24.dp.toPx(), 24.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            )
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    state.pos = IntOffset(
                        (state.pos.x + amount.x).roundToInt().coerceAtLeast(8),
                        (state.pos.y + amount.y).roundToInt().coerceAtLeast(60)
                    )
                }
            }
            .clickable { selectedId.value = state.id }
            // Selection = soft white ring ONLY while selected; resting shapes
            // carry no border at all.
            .drawBehind {
                if (selected) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = 2.5.dp.toPx()),
                        cornerRadius = CornerRadius(size.minDimension / 2f)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** v279 - per-shape lab state (position, size, blur strength, text color). */
private class LabShapeState(initialPos: IntOffset) {
    var id: String = ""
    var pos by mutableStateOf(initialPos)
    var visible by mutableStateOf(true)
    var scale by mutableFloatStateOf(1f)
    var blurDp by mutableFloatStateOf(8f)
    var textColor by mutableStateOf(Color.White)
}
