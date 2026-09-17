package com.curio.app.features.personal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.PersonalAudioBars
import com.curio.app.features.capture.AudioRecorder
import com.curio.app.ui.components.LiveWaveform
import com.curio.app.ui.components.WaveformExtractor
import com.curio.app.ui.components.formatRecordingTime
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * v389 — VOICE NOTES IN THE JOURNAL.
 *
 * A page is not only written: some thoughts arrive out loud. The journal page
 * carries a floating mic of its own, the recording takes over the page's bottom
 * while it runs (the tool dock steps aside — a voice note is not a moment for
 * bold), and what it leaves behind is a BLOCK in the page: the waveform, the
 * time, and a tap or a drag anywhere on the bars to jump to that moment. The
 * writing carries on underneath it, so a page can say what happened and then be
 * annotated in words (user request: "below we can still add notes").
 *
 * THE RECORDING OUTLIVES THE SCREEN. It is held by [PersonalVoiceRecording], not
 * by the page, because leaving a page mid-thought must not decide anything on
 * its own: the member is ASKED (keep recording / keep the note / discard), and
 * while a recording is kept alive a small pill rides the app (see
 * [PersonalVoicePill]) saying so. Everything else about a page — the document,
 * the autosave, the flush — stays where it was ([PersonalWritingPage]).
 *
 * Storage: the file lands in the app's own audio directory through
 * [AudioStorageManager] (the captures' own store, so a voice note is a first-class
 * recording and not a cache file with a path in it), and the waveform is
 * EXTRACTED ONCE and stored with the block ([PersonalAudioBars]) — decoding
 * audio through MediaCodec while a page scrolls would be a stutter.
 */

/** One recording, saved and ready to become a block. */
internal data class RecordedVoice(
    /** Absolute path under `filesDir/audio/`. */
    val path: String,
    val seconds: Int,
    /** The waveform, already downsampled — see [PersonalAudioBars]. */
    val bars: String
)

/**
 * THE LIVE RECORDING: the recorder, the clock and the mic level the meter
 * dances to. Created and owned by [PersonalVoiceRecording]; a screen only reads
 * it, so closing the screen cannot end the recording by accident.
 */
internal class PersonalVoiceSession(
    /** The note this recording is being made ON. */
    val noteId: String,
    /** Where a tap on the kept-alive pill should take the member back to. */
    val returnRoute: String,
    private val recorder: AudioRecorder
) {
    /** Mic level 0..1 (refreshed by [tick]) — the live meter's input. */
    var level by mutableFloatStateOf(0f)
        private set

    /** Whole seconds recorded so far. */
    var seconds by mutableIntStateOf(0)
        private set

    var paused by mutableStateOf(false)
        private set

    val elapsed: String get() = formatRecordingTime(seconds)

    fun pause() {
        recorder.pause()
        paused = true
    }

    fun resume() {
        recorder.resume()
        paused = false
    }

    internal fun tick() {
        seconds = recorder.elapsedSeconds
        level = (recorder.maxAmplitude / 32767f).coerceIn(0f, 1f)
    }

    /**
     * Stops the recorder and returns the cache file's path — the file is LEFT
     * ON DISK on purpose: it is the source [AudioStorageManager] copies into the
     * app's own audio store, and [AudioRecorder.release] would delete it first.
     * The caller deletes the cache copy once the file is safely stored.
     */
    internal fun stop(): String = recorder.stop()

    internal fun discard() {
        recorder.discard()
    }
}

/**
 * v389 — THE APP'S ONE LIVE RECORDING.
 *
 * A singleton on purpose: the page that started it can be left (that is what the
 * leave dialog is about), the pill at the app's root has to be able to say
 * "still recording", and coming back to the page has to find the same recording
 * rather than a second one. It knows which note is currently ON SCREEN
 * ([onScreenNoteId]) so the pill and the page's own capsule never both show.
 */
internal object PersonalVoiceRecording {

    /** The live recording, or null. */
    var session by mutableStateOf<PersonalVoiceSession?>(null)
        private set

    /** The note whose page is composed right now (the capsule lives there). */
    var onScreenNoteId by mutableStateOf<String?>(null)
        private set

    private var ticker: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    fun isRecordingHere(noteId: String): Boolean = session?.noteId == noteId

    /**
     * Starts recording for [noteId]. Returns null when the device refuses to
     * start (no microphone available, or it is already in use) — the caller
     * says so instead of leaving a dead button.
     */
    fun start(context: Context, noteId: String, returnRoute: String): PersonalVoiceSession? {
        session?.let { live -> if (live.noteId == noteId) return live }
        val recorder = AudioRecorder(context.applicationContext)
        val started = runCatching { recorder.start() }.isSuccess
        if (!started) {
            runCatching { recorder.release() }
            return null
        }
        val created = PersonalVoiceSession(noteId, returnRoute, recorder)
        session = created
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                created.tick()
                delay(80)
            }
        }
        return created
    }

    /**
     * Keeps the recording: the file is copied into the app's own audio store,
     * the waveform is extracted from the finished file, and the block's three
     * facts come back. Returns null when there was nothing live.
     */
    suspend fun keep(context: Context, noteId: String): RecordedVoice? {
        val live = session ?: return null
        stopTicking()
        session = null
        val cachePath = live.stop()
        val secondsNow = live.seconds
        return withContext(Dispatchers.Default) {
            val bars = WaveformExtractor.extract(cachePath, PersonalAudioBars.BAR_COUNT)
                ?: FloatArray(PersonalAudioBars.BAR_COUNT) { 0.08f }
            val persisted = runCatching {
                AudioStorageManager.persistAudio(
                    context.applicationContext,
                    cachePath,
                    "note_${noteId}_${System.currentTimeMillis()}"
                )
            }.getOrNull()
            val stored = persisted != null
            if (stored) runCatching { File(cachePath).delete() }
            RecordedVoice(
                // A failed copy keeps the cache file (it plays until the OS
                // clears the cache) rather than throwing the note away.
                path = persisted?.persistentPath ?: cachePath,
                seconds = secondsNow,
                bars = PersonalAudioBars.encode(bars)
            )
        }
    }

    fun discard() {
        stopTicking()
        session?.discard()
        session = null
    }

    /** The page says "I am the note on screen" (and unsays it on the way out). */
    fun setOnScreen(noteId: String?) {
        onScreenNoteId = noteId
    }

    private fun stopTicking() {
        ticker?.cancel()
        ticker = null
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Recording UI
// ────────────────────────────────────────────────────────────────────────────

/**
 * THE MIC'S PERMISSION DOOR.
 *
 * The returned call is what a tap on the mic runs: it hands the question to
 * Android's own launcher and NOTHING else. `RequestPermission` answers
 * immediately when the permission is already held, so one path covers "already
 * allowed", "never asked" and "asked before" — and recording starts from the
 * GRANTED callback, never from the tap itself.
 *
 * v389 — this is the bug the member hit ("tapping it doesnt do anything"). The
 * old version read the permission and returned a Boolean, and the caller was
 * `if (!ask()) start()`: on the very FIRST tap the read said "not granted"
 * (which launched the system dialog) AND `start()` still ran, so a
 * MediaRecorder was built with no permission, threw, and put "Could not start
 * recording" on the page while the dialog was still up. A tap that is refused
 * for good now says so instead of failing silently — [onDenied] is the door to
 * Android's app settings, which is the only way back from "Don't allow".
 */
@Composable
internal fun rememberRecordPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
): () -> Unit {
    val granted = rememberUpdatedState(onGranted)
    val denied = rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { allowed ->
        if (allowed) granted.value() else denied.value()
    }
    return { launcher.launch(Manifest.permission.RECORD_AUDIO) }
}

/**
 * Android's own page for THIS app, where a permission that was refused for good
 * can be given back. Returns null when the device has no such screen, so the
 * caller can stay quiet rather than send the member nowhere.
 */
internal fun appPermissionSettingsIntent(context: Context): Intent? =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).takeIf { it.resolveActivity(context.packageManager) != null }

/**
 * THE FLOATING MIC. It floats over the writing ABOVE the tool dock, so "say it
 * instead" is one tap away without ever sitting on the tools themselves.
 *
 * v389 — it wears the accent's DEEP shade as its fill rather than the airy one:
 * a pale accent disc on a pale page reads as a disabled control, and this is
 * the one button on the page whose whole job is to be pressed.
 */
@Composable
internal fun PersonalVoiceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = personalAccentInk(),
        shadowElevation = 8.dp,
        modifier = modifier.size(48.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(
                CurioIcons.Mic,
                "Record a voice note",
                tint = MaterialTheme.colorScheme.surface,
                size = 22.dp
            )
        }
    }
}

/**
 * THE RECORDING CAPSULE — what the page's bottom becomes while a voice note is
 * being made: a pulsing dot, the mic's real level running past it, the clock,
 * pause, the way out (✗ discards) and the way to keep it (✓). The tool dock is
 * hidden while this is up: a recording is not a moment for bold.
 */
@Composable
internal fun PersonalVoiceRecorderCapsule(
    session: PersonalVoiceSession,
    onKeep: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    val accentInk = personalAccentInk()
    val transition = rememberInfiniteTransition(label = "voice-recording")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "voice-recording-pulse"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(color = MaterialTheme.colorScheme.error.copy(alpha = pulse), shape = CircleShape)
            )
            Text(
                session.elapsed,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink
            )
            LiveWaveform(
                color = accentInk,
                active = !session.paused,
                barCount = 22,
                level = session.level,
                modifier = Modifier.width(74.dp).height(26.dp)
            )
            if (session.paused) {
                Text(
                    "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
            }
            Spacer(Modifier.weight(1f, fill = false))
            VoiceControl(
                glyph = if (session.paused) CurioIcons.PlayArrow else CurioIcons.Pause,
                label = if (session.paused) "Resume recording" else "Pause recording",
                tint = ink,
                onClick = { if (session.paused) session.resume() else session.pause() }
            )
            VoiceControl(
                glyph = CurioIcons.Close,
                label = "Discard the recording",
                tint = ink.copy(alpha = 0.7f),
                onClick = onDiscard
            )
            Surface(
                onClick = onKeep,
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(38.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.Check, "Keep the voice note", tint = personalOnAccent(), size = 19.dp)
                }
            }
        }
    }
}

/** One round control inside the recording capsule. */
@Composable
private fun VoiceControl(
    glyph: String,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(34.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(glyph, label, tint = tint, size = 19.dp)
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// The stored voice note
// ────────────────────────────────────────────────────────────────────────────

/**
 * A VOICE NOTE AS IT SITS IN A PAGE: a round play/pause and the waveform, with
 * the played part filled in [accent]. Tap or drag ANYWHERE along the bars to
 * jump to that moment (the whole strip is the scrubber — a timestamp should
 * never need a handle to be findable).
 */
@Composable
internal fun PersonalVoiceBar(
    path: String,
    seconds: Int,
    bars: String,
    ink: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    /** Non-null in the EDITOR: the ✕ that throws the recording away with the
     *  block. The read-only views pass nothing. */
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val samples = remember(bars) { PersonalAudioBars.decode(bars) }
    val carried = LocalPersonalBlockCarried.current
    var isPlaying by rememberSaveable(path) { mutableStateOf(false) }
    var position by rememberSaveable(path) { mutableLongStateOf(0L) }
    var duration by rememberSaveable(path) { mutableLongStateOf(seconds * 1000L) }

    // A stored audio path is an absolute file path — wrap it, or ExoPlayer's
    // data source parses it as a schemeless URI and plays nothing.
    val audioUri = remember(path) {
        val parsed = Uri.parse(path)
        if (parsed.scheme != null) parsed else Uri.fromFile(File(path))
    }
    val player = remember(audioUri) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(audioUri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> duration = player.duration.coerceAtLeast(0L)
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        position = 0L
                        player.seekTo(0)
                    }
                    Player.STATE_IDLE -> isPlaying = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // The clock only runs while it is playing.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            position = player.currentPosition.coerceAtLeast(0L)
            delay(200)
        }
    }

    val totalMs = if (duration > 0L) duration else seconds * 1000L
    val progress = if (totalMs > 0L) (position.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    fun seekTo(fraction: Float) {
        if (totalMs <= 0L) return
        val target = (fraction.coerceIn(0f, 1f) * totalMs).toLong()
        position = target
        player.seekTo(target)
    }

    fun toggle() {
        if (isPlaying) {
            player.pause()
        } else {
            // A finished clip replays from the start instead of dead-ending.
            if (position >= totalMs - 60L) {
                position = 0L
                player.seekTo(0)
            }
            player.play()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = { toggle() },
            shape = CircleShape,
            color = accent,
            modifier = Modifier.size(38.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(
                    if (isPlaying) CurioIcons.Pause else CurioIcons.PlayArrow,
                    if (isPlaying) "Pause the voice note" else "Play the voice note",
                    tint = personalOnAccent(),
                    size = 20.dp
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .pointerInput(path) {
                    detectTapGestures { offset ->
                        seekTo(offset.x / size.width)
                        if (!isPlaying) toggle()
                    }
                }
                // v389 — AND THE STRIP STANDS DOWN WHILE THE BLOCK IS CARRIED:
                // picking the note up (see PersonalMovableBlock) and scrubbing
                // it are both drags, and one finger cannot mean two things.
                .pointerInput(path, carried) {
                    if (carried) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset -> seekTo(offset.x / size.width) },
                        onDrag = { change, _ ->
                            change.consume()
                            seekTo(change.position.x / size.width)
                        }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (samples.isEmpty()) return@Canvas
                val slot = size.width / samples.size
                val barWidth = (slot * 0.62f).coerceAtLeast(1.6f)
                val playedUpTo = size.width * progress
                samples.forEachIndexed { index, level ->
                    // A floor of 8% keeps a quiet passage reading as a voice
                    // note rather than a gap in the strip.
                    val amplitude = (level.coerceIn(0f, 1f) * 0.92f + 0.08f)
                    val barHeight = (size.height * amplitude).coerceAtLeast(3f)
                    val left = index * slot + (slot - barWidth) / 2f
                    drawRoundRect(
                        color = if (left + barWidth / 2f <= playedUpTo) accent
                        else ink.copy(alpha = 0.26f),
                        topLeft = Offset(left, (size.height - barHeight) / 2f),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )
                }
                // The playhead, so a scrub lands where the eye expects.
                if (progress > 0f) {
                    drawLine(
                        color = accent,
                        start = Offset(playedUpTo, 0f),
                        end = Offset(playedUpTo, size.height),
                        strokeWidth = 2f.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Text(
            if (isPlaying || position > 0L) {
                "${formatRecordingTime((position / 1000L).toInt())} / ${formatRecordingTime(seconds)}"
            } else {
                formatRecordingTime(seconds)
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ink.copy(alpha = 0.7f)
        )
        if (onRemove != null) {
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(30.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Close,
                        "Remove the voice note",
                        tint = ink.copy(alpha = 0.5f),
                        size = 17.dp
                    )
                }
            }
        }
    }
}

/**
 * THE VOICE BLOCK as the EDITOR wears it: the bar, plus the ✕ that removes it.
 * Removing it takes its RECORDING with it — a file nothing points at is a leak,
 * and the member threw the note away on purpose.
 */
@Composable
internal fun PersonalVoicePageBlock(
    path: String,
    seconds: Int,
    bars: String,
    ink: Color,
    accent: Color,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    PersonalVoiceBar(
        path = path,
        seconds = seconds,
        bars = bars,
        ink = ink,
        accent = accent,
        modifier = modifier,
        onRemove = if (!enabled) null else (
            {
                AudioStorageManager.deleteAudio(context, path)
                onRemove()
            }
            )
    )
}

// ────────────────────────────────────────────────────────────────────────────
// The keep-recording pill (app root) and the leave-page dialog
// ────────────────────────────────────────────────────────────────────────────

/**
 * v389 — A PAGE'S MIC, FOR A PAGE THAT IS NOT [PersonalWritingPage].
 *
 * The journal, a topic note and a to-do list get their mic from the writing
 * core; the BOOK REVIEW and the CHAPTER REVIEW are their own layouts (a book's
 * page is not a day), so they had none — which is what "also add in book review
 * chapter review too" asked for. What is shared here is the whole door: the
 * floating button, the ONE permission request ([rememberRecordPermission]), the
 * two ways it can fail (refused, or the recorder busy) and the registration that
 * stops the app's keep-recording pill covering the page it belongs to. What a
 * page does with the RESULT stays with the page: it reads
 * `PersonalVoiceRecording.session` for its own `noteId` to swap its dock for the
 * capsule, and inserts what was kept at its caret ([PersonalEditorState.insertVoice]).
 *
 * [entryId] is any stable tag for the page — it names the session and is what
 * the pill comes back to, so it does not have to be a stored row id.
 */
@Composable
internal fun PersonalVoiceMic(
    entryId: String,
    route: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var recordFailed by remember { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }

    fun startVoice() {
        val started = PersonalVoiceRecording.start(context, entryId, route)
        if (started == null) recordFailed = true
    }

    val askToRecord = rememberRecordPermission(
        onGranted = { startVoice() },
        onDenied = { micDenied = true }
    )

    // Only the page whose page is OPEN hides the pill at the app's root.
    DisposableEffect(entryId) {
        PersonalVoiceRecording.setOnScreen(entryId)
        onDispose { PersonalVoiceRecording.setOnScreen(null) }
    }

    PersonalVoiceButton(onClick = askToRecord, modifier = modifier)

    if (recordFailed) {
        AlertDialog(
            onDismissRequest = { recordFailed = false },
            title = { Text("Could not start recording") },
            text = { Text("The microphone is busy or unavailable. Try again in a moment.") },
            confirmButton = { TextButton(onClick = { recordFailed = false }) { Text("OK") } }
        )
    }

    // Android's own page for THIS app, when the device has one.
    val micSettings = remember(context) { appPermissionSettingsIntent(context) }
    if (micDenied) {
        AlertDialog(
            onDismissRequest = { micDenied = false },
            title = { Text("Curio needs the microphone") },
            text = {
                Text(
                    "Recording a voice note on this page needs microphone access. " +
                        "If Android will not ask again, its own page for Curio is " +
                        "where it is turned back on."
                )
            },
            confirmButton = {
                if (micSettings != null) {
                    TextButton(onClick = {
                        micDenied = false
                        runCatching { context.startActivity(micSettings) }
                    }) { Text("Open settings") }
                } else {
                    TextButton(onClick = { micDenied = false }) { Text("OK") }
                }
            },
            dismissButton = if (micSettings != null) {
                { TextButton(onClick = { micDenied = false }) { Text("Not now") } }
            } else {
                null
            }
        )
    }
}

/**
 * THE KEEP-RECORDING PILL.
 *
 * Rides the app's root while a voice note is still being recorded but its page
 * is not the one on screen — the member who chose "keep recording" and walked
 * away is never left wondering whether it is still going. Tapping it goes back
 * to the page it belongs to.
 */
@Composable
internal fun PersonalVoicePill(navController: NavController) {
    val live = PersonalVoiceRecording.session ?: return
    if (PersonalVoiceRecording.onScreenNoteId == live.noteId) return

    val ink = MaterialTheme.colorScheme.onSurface
    val transition = rememberInfiniteTransition(label = "voice-pill")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "voice-pill-pulse"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 108.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Surface(
            onClick = { navController.navigate(live.returnRoute) { launchSingleTop = true } },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = pulse),
                            shape = CircleShape
                        )
                )
                Text(
                    "Recording ${live.elapsed}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = ink
                )
                CurioIcon(
                    CurioIcons.ChevronRight,
                    null,
                    tint = ink.copy(alpha = 0.45f),
                    size = 17.dp
                )
            }
        }
    }
}

/**
 * THE WAY OUT OF A PAGE THAT IS RECORDING: keep recording (it carries on, and
 * the pill above says so), keep the note (stop and leave it in the page) or
 * throw it away. Nothing is decided silently — a voice note is somebody's
 * thought, and losing one to a back gesture is unforgivable.
 */
@Composable
internal fun PersonalVoiceLeaveDialog(
    elapsed: String,
    onKeepRecording: () -> Unit,
    onKeepNote: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepRecording,
        title = {
            Text(
                "Still recording",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = { Text("This voice note is $elapsed long.") },
        confirmButton = {
            TextButton(onClick = onKeepNote) {
                Text("Keep the note", color = personalAccentInk(), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDiscard) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onKeepRecording) { Text("Keep recording") }
            }
        }
    )
}
