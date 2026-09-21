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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.curio.app.ui.components.WaveformExtractor
import com.curio.app.ui.components.formatRecordingTime
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.curioTintOn
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
            // The moment the recording was MADE, captured once: it is both the
            // file's name in the device folder and the note's date, so the two
            // can never disagree about when it happened.
            val madeAt = System.currentTimeMillis()
            val persisted = runCatching {
                AudioStorageManager.persistAudio(
                    context.applicationContext,
                    cachePath,
                    "note_${noteId}_$madeAt"
                )
            }.getOrNull()

            // ── AND THE RECORDING LEAVES THE APP (v405) ──────────────────
            //
            // A copy goes into the device's own music library, under
            // `Your Journal Voices`, named by the day it was made — so an
            // accidental uninstall cannot take the member's recordings with it,
            // and the folder reads as a diary in date order (member's ask:
            // "those voice recording saves in the device too … so it doesnt
            // delete even after deleing the app by mistake… name the voice note
            // based on date"). The app's own copy is then removed, because two
            // copies of one recording is one copy too many; it is kept only when
            // the publish was refused, so the note never depends on it.
            val published = persisted?.let { result ->
                runCatching {
                    AudioStorageManager.publishVoice(
                        context.applicationContext,
                        result.persistentPath,
                        madeAt
                    )
                }.getOrNull()
            }
            val stored = persisted != null
            if (stored) runCatching { File(cachePath).delete() }
            if (published != null && persisted != null) {
                runCatching {
                    AudioStorageManager.deleteAudio(context.applicationContext, persisted.persistentPath)
                }
            }
            RecordedVoice(
                // A failed copy keeps the cache file (it plays until the OS
                // clears the cache) rather than throwing the note away.
                path = published ?: persisted?.persistentPath ?: cachePath,
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
    // ── v421 — THE CAPSULE SPEAKS THE NOTE'S LANGUAGE ───────────────────
    //
    // It used to be a 50%-radius pill running the bar-chart meter, which is the
    // one drawing the member had already retired on the note itself ("make its
    // graph the aduio graph pulse wave hand drawn style"). A recording under way
    // is a PREVIEW of the note it is about to become, so it now wears the same
    // three things the note does: the drawn pulse as a live meter, the clock in
    // the page's serif face, and a soft card instead of a pill (a 22dp radius — 
    // a capsule read as a system control, and this is part of the page). The
    // fill is an OPAQUE tint of the floating surface rather than an alpha wash,
    // so its shadow stays clean (see the shadow rule in the root AGENTS.md).
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = curioTintOn(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            accent,
            0.10f
        ),
        shadowElevation = 6.dp,
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
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FrauncesFontFamily
                ),
                color = ink.copy(alpha = 0.9f)
            )
            LiveVoiceWave(
                level = session.level,
                active = !session.paused,
                ink = ink.copy(alpha = 0.62f),
                accent = accentInk,
                modifier = Modifier.width(84.dp).height(28.dp)
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
                tint = ink.copy(alpha = 0.85f),
                onClick = { if (session.paused) session.resume() else session.pause() }
            )
            VoiceControl(
                glyph = CurioIcons.Close,
                label = "Discard the recording",
                tint = ink.copy(alpha = 0.6f),
                onClick = onDiscard
            )
            Surface(
                onClick = onKeep,
                shape = CircleShape,
                color = accentInk,
                modifier = Modifier.size(38.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(CurioIcons.Check, "Keep the voice note", tint = personalOnAccent(), size = 19.dp)
                }
            }
        }
    }
}

/**
 * The floor the live mic's reference can fall to (see [LiveVoiceWave]).
 *
 * About the level of a quiet room, so a silent strip is drawn as silence instead
 * of being amplified into a picture of speech.
 */
private const val MIN_MIC_REF = 0.05f

/**
 * How much of the live reference survives each 70ms tick (see [LiveVoiceWave]).
 *
 * Slow enough that a pause mid-sentence does not shrink the strip under the
 * words that follow it, fast enough that a member who drops to a whisper is met
 * at their new loudness within a couple of seconds.
 */
private const val MIC_REF_DECAY = 0.995f

/**
 * v421 — THE LIVE METER, DRAWN LIKE THE NOTE IT IS MAKING.
 *
 * The same rolling history the old bar meter kept (one entry per 70ms, eased
 * toward the recorder's real level), drawn as the note's own [drawVoicePulse].
 * The played part is the whole strip while it is live — a recording in progress
 * has been "heard" all the way to its own front.
 */
@Composable
internal fun LiveVoiceWave(
    level: Float,
    active: Boolean,
    ink: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 40
) {
    val levelState by rememberUpdatedState(level)
    val history = remember(barCount) { FloatArray(barCount) { 0.06f } }
    // ── v439 — THE METER IS READ AGAINST THE SPEAKER, NOT FULL SCALE ───────
    //
    // The mic's own level is `maxAmplitude / 32767`, and 32767 is the loudest a
    // 16-bit sample can be — a number a voice in a room never approaches. Rough
    // speech peaks land near a tenth of it, so the meter sat at the bottom of its
    // travel and read as a nearly flat line while the member was talking (the
    // same fault, and the same fix, as the stored waveform's — see
    // [WaveformExtractor]): the level is now read against a REFERENCE that
    // follows the loudest thing the member has said so far and decays slowly, so
    // the strip fills when they speak at their own loudness and still shows the
    // shape of how they said it.
    //
    // The reference has a floor ([MIN_MIC_REF]) so a recording of a quiet room is
    // not amplified into a picture of speech — an honest meter shows an honest
    // room.
    val reference = remember { floatArrayOf(MIN_MIC_REF) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(active, barCount) {
        while (true) {
            val raw = if (active) levelState.coerceIn(0f, 1f) else 0f
            // The loudest recent moment wins, then lets go a little each tick: a
            // speaker who leans in fills the strip, and one who drifts off does
            // not keep it pinned.
            reference[0] = maxOf(raw, reference[0] * MIC_REF_DECAY)
                .coerceAtLeast(MIN_MIC_REF)
            val target =
                if (active) (raw / reference[0]).coerceIn(0f, 1f) else 0.06f
            if (barCount > 0) {
                for (i in 0 until barCount - 1) history[i] = history[i + 1]
                val front = history[barCount - 1]
                history[barCount - 1] = (front + (target - front) * 0.65f).coerceIn(0.06f, 1f)
            }
            tick++
            delay(70)
        }
    }
    val drawn = tick
    Canvas(modifier) {
        if (drawn < 0) return@Canvas
        drawVoicePulse(history, 1f, ink, accent)
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
 * A VOICE NOTE AS IT SITS IN A PAGE: a play control and the waveform, with the
 * part that has been HEARD filled in [accent]. Tap or drag ANYWHERE along the
 * bars to jump to that moment (the whole strip is the scrubber — a timestamp
 * should never need a handle to be findable).
 *
 * v421 — THE NOTE HAS LOOKS ([PersonalVoiceStyle]), picked from a control on
 * the note itself while the page is being EDITED and remembered per recording.
 * Every look pairs a WAVE DRAWING with a PLAY TREATMENT, because those are the
 * two things the member pointed at when they asked for styles ("the voice note
 * looks adds differnt waves and button styles"). The default — the drawn pulse
 * with a drawn play mark — is the one the member described in full: the control
 * stops being a Material disc parked beside a hand-drawn wave and becomes part
 * of the drawing.
 */
@Composable
internal fun PersonalVoiceBar(
    path: String,
    seconds: Int,
    bars: String,
    ink: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    /** The note's look. Absent on an older note: the drawn wave. */
    style: PersonalVoiceStyle = PersonalVoiceStyle.HAND,
    /** Non-null in the EDITOR: the ✕ that throws the recording away with the
     *  block. The read-only views pass nothing. */
    onRemove: (() -> Unit)? = null,
    /** Non-null in the EDITOR: the door to the look picker. A read-only view
     *  renders the stored look and offers no way to change it. */
    onStyle: ((PersonalVoiceStyle) -> Unit)? = null
) {
    val context = LocalContext.current
    val samples = remember(bars) { PersonalAudioBars.decode(bars) }
    val carried = LocalPersonalBlockCarried.current
    var isPlaying by rememberSaveable(path) { mutableStateOf(false) }
    var position by rememberSaveable(path) { mutableLongStateOf(0L) }
    var duration by rememberSaveable(path) { mutableLongStateOf(seconds * 1000L) }
    /** The ✕ holds here until the member says yes (see the dialog below). */
    var confirmRemove by remember(path) { mutableStateOf(false) }
    /** The look picker (editor only — see [onStyle]). */
    var pickingStyle by remember(path) { mutableStateOf(false) }

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

    // NO CONTAINER (v389): the note used to sit in a rounded surfaceContainerLow
    // box, which made a recording look like a card parked in the writing rather
    // than part of it (user request: "it shows on the page as a box, but i want
    // it with the graph only the play and cross button no backgroud"). What is
    // left is the three things the note actually is — a play button, the
    // waveform, and the clock — sitting on the page itself, starting where the
    // paragraph starts. The LIFT while it is being carried still comes from
    // PersonalMovableBlock, which is the only state that had any business
    // drawing a surface here.
    //
    // v404 — AND NO SHADOW EITHER. v403 lifted the strip with a soft shadow and
    // the member called it back: a recording is part of the writing, so it
    // casts nothing (user request: "for voice note in journal dont give it the
    // shadow keep it how it was ith no backgroud"). No box and no halo — the
    // play button, the drawn pulse and the clock, sitting on the page itself.
    // The BUBBLE look wears its chrome as a fill BEHIND the strip rather than as
    // a second layout: a voice message is the same three things in one bubble,
    // and one row can say both. Its fill is an OPAQUE tint of the card it sits
    // on (never an alpha wash), so nothing shows through it.
    val bubble = style == PersonalVoiceStyle.BUBBLE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (bubble) {
                    Modifier
                        // v423 — AND IT IS A MESSAGE, NOT A CARD: the bubble's
                        // own corner is cut at the foot, the way a sent message
                        // reads as coming from a mouth and not from a box.
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomEnd = 18.dp,
                                bottomStart = 5.dp
                            )
                        )
                        .background(
                            curioTintOn(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                accent,
                                0.14f
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── v421 — THE PLAY CONTROL, IN THE NOTE'S OWN LANGUAGE ──────────
        //
        // v404 made it a dark, solid, filled disc, and the member took it back
        // with the wave beside it in view: a Material disc next to a hand-drawn
        // line is two drawings in one strip, and the disc is the odd one
        // (user request: "the play button doesnt matches the wave look as its a
        // pill"). The two bare looks DROP THE DISC and draw the mark in the
        // wave's own ink and weight, so the whole note is one drawing; the two
        // that keep a filled control keep a REAL one (a round disc is the voice
        // message's own mark, and the bubble is that language).
        when (style) {
            PersonalVoiceStyle.HAND -> VoiceDrawnControl(
                playing = isPlaying,
                tint = ink.copy(alpha = 0.86f),
                ring = null,
                label = if (isPlaying) "Pause the voice note" else "Play the voice note",
                onClick = { toggle() }
            )
            PersonalVoiceStyle.MINIMAL -> VoiceDrawnControl(
                playing = isPlaying,
                tint = ink.copy(alpha = 0.80f),
                ring = ink.copy(alpha = 0.26f),
                label = if (isPlaying) "Pause the voice note" else "Play the voice note",
                onClick = { toggle() }
            )
            // v424 — AND THE PILL, which is the same pulse with a wider mark.
            PersonalVoiceStyle.PILL -> VoicePillControl(
                playing = isPlaying,
                label = if (isPlaying) "Pause the voice note" else "Play the voice note",
                onClick = { toggle() }
            )
            else -> Surface(
                onClick = { toggle() },
                shape = CircleShape,
                color = personalAccentInk(),
                modifier = Modifier.size(38.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        if (isPlaying) CurioIcons.Pause else CurioIcons.PlayArrow,
                        if (isPlaying) "Pause the voice note" else "Play the voice note",
                        tint = MaterialTheme.colorScheme.surface,
                        size = 22.dp
                    )
                }
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
            // ── THE HAND-DRAWN PULSE (v404, REDRAWN AS ONE WAVE IN v411) ─
            //
            // v389 drew the note as a bar chart: rounded columns on a centre
            // line, evenly spaced, which is what an audio widget looks like and
            // not what a page looks like (user request: "make its graph the aduio
            // graph pulse wave hand drawn style").
            //
            // v404 replaced that with the voice's own envelope as ink — but it
            // drew the envelope TWICE, a mirrored pair of strokes above and
            // below the centre line, so the strip read as two waves folded
            // together (member: "the voice note wave in journal its 2 wave and
            // looks weird fix it please and use 1 wave style and proper depth").
            //
            // It is ONE LINE now. The path crosses the centre on every step, so
            // each peak of the voice is one rise and one fall of a single
            // stroke — the pulse a spoken sentence makes, not a shape mirrored
            // under itself. Two details keep it legible at 72 stored samples:
            //  · the samples are BUCKETED (about 18 steps across the strip, the
            //    loudest sample of each bucket wins), because a rise and a fall
            //    every 3dp is a fuzzy band rather than a wave;
            //  · a stable hand wobble (a hash of the sample's own index, never a
            //    random number, so the ink never crawls while the note plays).
            //
            // DEPTH: the line is drawn a hair lower first, soft, so the ink sits
            // ON the paper instead of floating over it — the strip itself stays
            // backgroundless and shadowless (§v404), because a recording is part
            // of the writing and not a card in it.
            Canvas(Modifier.fillMaxSize()) {
                if (samples.isEmpty()) return@Canvas
                // v423 — EVERY LOOK BUT THE HAND-DRAWN ONE IS A SHAPE (see
                // [drawVoiceWave]); HAND is the pulse. v424 — and PILL draws the
                // same pulse with a different control (see [drawsPulse]).
                if (!style.drawsPulse) {
                    drawVoiceWave(samples, progress, ink, accent, style)
                    return@Canvas
                }
                drawVoicePulse(samples, progress, ink, accent)
            }
        }
        // v390 — ONE FIGURE, NEVER TWO.
        //
        // The clock used to swap between the note's LENGTH and "position /
        // length", so the figure changed width the moment playback started and
        // the strip beside it moved with it (user request: "fix the total time
        // shifting during play and dont show like 0:00/0/xx just show x:xx during
        // play so less shifting"). It is one figure now: the position while the
        // note is sounding, the note's own length while it stands still. Tabular
        // figures make every digit the same width, so counting up does not
        // twitch either — a recording of a minute or less reads as `m:ss` in
        // both states, which is the same width digit for digit.
        // ── v421 — THE CLOCK IS PART OF THE DRAWING TOO ─────────────────
        //
        // The member named the number in the same breath as the play button —
        // "the play button doesnt matches the wave look as its a pill. also the
        // number" — and they are right: beside a hand-drawn line a plain UI
        // label reads as a caption borrowed from another app. In the two BARE
        // looks the clock takes the page's editorial face (the Fraunces serif
        // the app already sets its heads in) at the wave's own ink, so the strip
        // is one hand. The disc looks keep the plain label, which is what a
        // voice message's own clock is.
        val drawnLook = style == PersonalVoiceStyle.HAND ||
            style == PersonalVoiceStyle.MINIMAL
        val clock = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
        )
        Text(
            if (isPlaying || position > 0L) {
                formatRecordingTime((position / 1000L).toInt())
            } else {
                formatRecordingTime(seconds)
            },
            style = if (drawnLook) clock.copy(fontFamily = FrauncesFontFamily) else clock,
            color = ink.copy(alpha = if (drawnLook) 0.82f else 0.7f),
            maxLines = 1
        )
        // ── v421 — THE LOOK PICKER'S DOOR (editor only) ────────────────
        //
        // A quiet mark beside the ✕ rather than a pill of its own: the note is
        // part of the writing and the strip is not a toolbar, so the door to its
        // looks is drawn at the same weight as the rest of the furniture — its
        // shape is previewed in the sheet, which is where a look is chosen.
        if (onStyle != null) {
            Surface(
                onClick = { pickingStyle = true },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(30.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Tune,
                        "Change the voice note style",
                        tint = ink.copy(alpha = 0.45f),
                        size = 16.dp
                    )
                }
            }
        }
        if (onRemove != null) {
            Surface(
                // v389 — ASKS FIRST. The ✕ sits beside the play button, on the
                // page, where a mis-tap during a scrub is easy — and what it
                // destroys is a recording that cannot be made again (user
                // request: "the cross button should ask for confimation before
                // deleting it"). The dialog names the cost; the audio file is
                // only unlinked on the confirm.
                onClick = { confirmRemove = true },
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
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove this voice note?") },
            text = {
                Text("The recording goes with it, and a recording cannot be made again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        onRemove?.invoke()
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Keep it") }
            }
        )
    }
    if (pickingStyle && onStyle != null) {
        PersonalVoiceStyleSheet(
            current = style,
            ink = ink,
            accent = accent,
            samples = samples,
            onPick = {
                onStyle(it)
                pickingStyle = false
            },
            onDismiss = { pickingStyle = false }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// The note's looks
// ────────────────────────────────────────────────────────────────────────────

/**
 * v421 — HOW A VOICE NOTE IS DRAWN.
 *
 * Each entry is a WAVE DRAWING *and* a play treatment, because those are the
 * two halves of what a note looks like and pairing them is what keeps a look
 * coherent: a hand-drawn pulse beside a filled Material disc was exactly the
 * mismatch the member reported.
 *
 *  · [HAND] — the drawn pulse in ink, a drawn play mark, the clock in the
 *    page's serif face. The default, and the one the member described.
 *  · [BARS] — rounded columns on a centre line (the v389 drawing) with the
 *    filled disc: the plainest, most utilitarian reading of a voice.
 *  · [BUBBLE] — the pulse inside a soft bubble with a disc, the voice-MESSAGE
 *    language: the note as something that was SENT rather than drawn.
 *  · [MINIMAL] — one flat line with the played run and a dot, and an outlined
 *    mark: the note reduced to the single fact that it is playing.
 *
 * The key is what the document stores, so these strings are a contract: never
 * rename one — a new look is a new key, and an unknown key reads as [HAND].
 */
internal enum class PersonalVoiceStyle(val key: String, val label: String, val hint: String) {
    HAND("hand", "Hand-drawn", "A drawn pulse in ink, with a drawn play mark"),
    BARS("bars", "Bars", "Rounded columns on a centre line, with a filled disc"),
    BUBBLE("bubble", "Bubble", "A voice message — mirrored bars in a tinted bubble"),
    MINIMAL("minimal", "Minimal", "One thin line and a dot, with an outlined mark"),
    RIBBON("ribbon", "Ribbon", "A single drawn ribbon, out along the voice and back"),
    BEADS("beads", "Beads", "A bead per moment, strung evenly on the centre line"),
    PILL("pill", "Pill", "The drawn pulse, with the play mark in a pill");

    /**
     * v424 — WHICH LOOKS WEAR THE DRAWN PULSE.
     *
     * [PILL] is a CONTROL's look, not a wave's: it pairs the pulse with a pill
     * play mark, so the two looks that draw the pulse say so here instead of the
     * drawing having to know about a button (see [drawVoicePulse]).
     */
    val drawsPulse: Boolean get() = this == HAND || this == PILL

    companion object {
        /** The stored key, or [HAND] — the look every earlier note already had. */
        fun fromKey(key: String?): PersonalVoiceStyle =
            entries.firstOrNull { it.key == key } ?: HAND
    }
}

/**
 * HOW FAR THE INK REACHES FOR A LEVEL — and the whole difference between a
 * whisper and a shout.
 *
 * ── v424 PUT THE SQUARE HERE, AND v439 TOOK IT BACK OUT ─────────────────────
 *
 * v424's square was right for the data it had. It was compensating for a real
 * fault one layer down: the stored levels were normalized against 16-bit FULL
 * SCALE, so normal speech sat at 0.05–0.35 and the squaring collapsed it to
 * nearly nothing. With the floor on top, a quiet passage and a loud one did look
 * the same — and the square was the only thing that separated them again.
 *
 * **That fault is fixed at the source now** (see [WaveformExtractor]: every bar
 * is relative to the recording's own loudest moment, and it is a peak+RMS
 * envelope rather than a bare peak), so the curve does not have to compress
 * twice. The reach is the LEVEL itself, which is the honest reading of a
 * waveform: **the height IS the amplitude.** A shout takes the whole band, a
 * normal sentence takes half of it, a whisper a tenth. [VOICE_FLOOR] keeps a
 * silence reading as a line rather than as a gap in the drawing.
 *
 * If a member ever says the wave is too subtle again, reach for the extractor's
 * normalization — not for another exponent here. A curve on top of un-normalized
 * data is what produced two rounds of "not clear differnt".
 */
private const val VOICE_FLOOR = 0.08f

/**
 * v440 — THE SHORTEST CANVAS A VOICE WAVE IS DRAWN ON, in raw pixels (a dp)
 * independent value on purpose: this is a guard against a canvas with NO room, not
 * a design measurement. Below it the wave's band cannot hold a stroke and its own
 * ink would be off the edges (see [drawVoicePulse]).
 */
private const val MIN_WAVE_HEIGHT_PX = 12f

private fun voiceReach(level: Float, bandHalf: Float): Float {
    val loud = level.coerceIn(0f, 1f)
    return bandHalf * (VOICE_FLOOR + (1f - VOICE_FLOOR) * loud)
}

/**
 * THE PULSE — the note as a single hand-drawn line.
 *
 * Everything is measured from ONE BAND, and the band is what makes the drawing
 * safe: it is inset by half a stroke on every side, so the loudest possible
 * sample still leaves the ink inside the canvas. `bandBottom` reserves the depth
 * pass's own drop as well, because the depth line is drawn a hair lower than the
 * main one and used to be the first thing to run off the bottom on a loud note.
 *
 * The stroke is ONE line crossing the centre on every step: each peak of the
 * voice is one rise and one fall, never a shape mirrored under itself. Two
 * details keep it legible at 72 stored samples — the samples are BUCKETED (18
 * steps: the loudest sample of each bucket wins, because a rise and a fall every
 * 3dp is a fuzzy band rather than a wave) and a stable hand wobble (a hash of the
 * sample's own index, never a random number, so the ink never crawls while the
 * note plays).
 *
 * v427 — it is `internal`, so A SHEET OF PAPER CAN DRAW THE NOTE THE PAGE
 * DRAWS: the PDF used to draw its own lookalike of this (a filled pill and a row
 * of rounded bars), a second drawing of the same note that drifted from the
 * first — the member's "the waves are also not visible as it is in journal eye
 * view". This stroke is now drawn on the sheet's own canvas too (see
 * `drawExportVoice`), so the two cannot disagree again.
 */
internal fun DrawScope.drawVoicePulse(
    samples: FloatArray,
    progress: Float,
    ink: Color,
    accent: Color
) {
    val count = samples.size
    if (count == 0) return
    // ── v440 — A WAVE WITH NO ROOM IS NOT DRAWN, AND THAT IS THE FIX FOR A CRASH ──
    //
    // The member's crash report: *"Cannot coerce value to an empty range: maximum
    // -0.9 is less than minimum 0.9"*, thrown DURING DRAW (`dispatchDraw` in the
    // stack). Those two numbers are this function's own: the stroke is floored at
    // 1.8dp so `bandTop` is 0.9, and `bandBottom` is `size.height - halfStroke -
    // depthDrop` — so on a canvas whose height is nil or a pixel, the band CLOSES
    // and inverts, and `.coerceIn(bandTop, bandBottom)` throws on the empty range.
    //
    // A row that has not been measured yet reports exactly that size for a frame, so
    // this is a real state and not a fanciful one. Two guards, both cheap: a canvas
    // with no room is left alone, and the band can never close even if a caller
    // hands in something tiny.
    if (size.height < MIN_WAVE_HEIGHT_PX || size.width < 8f) return
    val strokeWidth = (size.height * 0.11f).coerceAtLeast(1.8f)
    val halfStroke = strokeWidth / 2f
    val depthDrop = size.height * 0.06f
    val bandLeft = halfStroke
    val bandRight = size.width - halfStroke
    val bandTop = halfStroke
    val bandBottom = (size.height - halfStroke - depthDrop).coerceAtLeast(bandTop + 1f)
    val bandMid = (bandTop + bandBottom) / 2f
    val bandHalf = ((bandBottom - bandTop) / 2f).coerceAtLeast(1f)
    val playedUpTo = bandLeft + (bandRight - bandLeft) * progress

    // A stable little wobble, which is what makes the stroke read as drawn by
    // hand instead of plotted.
    fun wobble(seed: Int): Float {
        val hash = seed * 374761393 + 668265263
        val mixed = (hash xor (hash shr 13)) * 1274126177
        return ((mixed % 1000).toFloat() / 1000f - 0.5f) * bandHalf * 0.10f
    }

    // How far the voice reaches from the centre for a level. The floor keeps a
    // quiet passage reading as a voice rather than as a break in the line, and
    // the lift is what the band's own half-width can hold.
    // v424 — and the reach is the voice's own scale (see [voiceReach]).
    fun reach(level: Float): Float = voiceReach(level, bandHalf)

    val buckets = 18
    val perBucket = (count + buckets - 1) / buckets
    val stepCount = (count + perBucket - 1) / perBucket
    val span = (stepCount - 1).coerceAtLeast(1).toFloat()

    // THE POINTS FIRST, THEN ONE SMOOTH STROKE THROUGH THEM. A hard `lineTo` at
    // every step is a sawtooth — every peak a corner, every corner a spike — so
    // each rise and fall is a CUBIC segment whose control points sit at the
    // midpoint between the steps: the wave keeps its peaks but curves into them.
    val points = ArrayList<Offset>(stepCount)
    for (step in 0 until stepCount) {
        val from = step * perBucket
        val to = (from + perBucket).coerceAtMost(count)
        var loudest = 0f
        for (i in from until to) {
            val level = samples[i]
            if (level > loudest) loudest = level
        }
        val x = bandLeft + (bandRight - bandLeft) * step / span
        val side = if (step % 2 == 0) -1f else 1f
        val y = (bandMid + side * reach(loudest) + wobble(from))
            .coerceIn(bandTop, bandBottom)
        points.add(Offset(x, y))
    }
    val wave = Path()
    points.firstOrNull()?.let { first -> wave.moveTo(first.x, first.y) }
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val point = points[i]
        val midX = (prev.x + point.x) / 2f
        wave.cubicTo(midX, prev.y, midX, point.y, point.x, point.y)
    }

    val stroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    // DEPTH — the same line, a hair lower, drawn soft, so the ink sits ON the
    // paper instead of floating over it.
    translate(top = depthDrop) {
        drawPath(wave, ink.copy(alpha = 0.22f), style = stroke)
    }
    // THE WHOLE NOTE in the page's ink — and the part that has been HEARD in the
    // note's own colour, cut at the playhead.
    drawPath(wave, ink.copy(alpha = 0.72f), style = stroke)
    if (progress > 0f) {
        clipRect(right = playedUpTo) {
            drawPath(wave, accent, style = stroke)
        }
        // ── v423 — THE PROGRESS RIDES THE WAVE ────────────────────────
        //
        // The head of the heard run was a straight bar across the band, which is
        // not part of a hand-drawn line and read as a knob bolted onto it
        // (member: "the progress straight knob in the wave"). It is a BEAD now —
        // a dot of the note's own ink sitting ON the line, at the step the head
        // has actually reached — and it is drawn ONLY while there is somewhere
        // left to go: a note played to its end, and a recording in progress
        // (whose strip has been "heard" all the way to its own front), wear no
        // knob at all, which is what made the live meter look inaccurate
        // (member: "the floating recorder waves feels inaccurate with that
        // straight knob").
        //
        // v424 — AND IT SITS ON THE INK, NOT ON A VERTEX. The head used to be
        // snapped to the nearest point of the drawing, so between two peaks it
        // drifted off the curve it belonged to (member: "the progress small dot
        // doesnt properly follow the waves"); [pulsePointAt] evaluates the
        // segment instead (see [pulsePointAt]).
        if (progress < 0.995f) {
            val bead = pulsePointAt(points, playedUpTo)
            if (bead != null) {
                val radius = (strokeWidth * 0.62f).coerceAtLeast(2f)
                drawCircle(
                    color = ink.copy(alpha = 0.22f),
                    radius = radius,
                    center = Offset(bead.x, bead.y + depthDrop)
                )
                drawCircle(color = accent, radius = radius, center = bead)
            }
        }
    }
}

/**
 * THE LOOKS THAT ARE NOT THE PULSE: [PersonalVoiceStyle.BARS] and
 * [PersonalVoiceStyle.MINIMAL].
 *
 *  · **BARS** — rounded columns on a centre line, the v389 drawing: the plainest
 *    way to show a voice, and what a member who wants a meter picks.
 *  · **MINIMAL** — one flat line with the played run in the accent and a dot at
 *    the playhead: the note reduced to the fact that it is playing.
 *
 * Both measure from the same idea the pulse does — a band, inset so no peak can
 * reach the canvas edge — for the same reason.
 *
 * v423 — AND THE LOOKS THAT ARE SHAPES. [RIBBON] draws the envelope and its own
 * mirror as ONE closed shape, [BEADS] sets a dot per moment on the centre line
 * (each as big as the sound it stands for), and [BUBBLE] draws the mirrored bars
 * a voice message is drawn with — the three looks the member asked for when they
 * said the bubble was not good enough and that the note wanted more of them. The
 * heard run is the SAME drawing in the accent, cut at the play head, in every one
 * of them: a bar that fills up is not part of a drawing, and neither is a knob.
 */
internal fun DrawScope.drawVoiceWave(
    samples: FloatArray,
    progress: Float,
    ink: Color,
    accent: Color,
    style: PersonalVoiceStyle
) {
    val bandTop = size.height * 0.12f
    val bandBottom = size.height * 0.88f
    val bandMid = size.height / 2f
    val bandHalf = ((bandBottom - bandTop) / 2f).coerceAtLeast(1f)
    val played = (size.width * progress).coerceIn(0f, size.width)
    when (style) {
        PersonalVoiceStyle.BARS -> {
            val columns = 42
            val steps = bucketLevels(samples, columns)
            val slot = size.width / columns
            val gap = (slot * 0.30f).coerceAtLeast(0.6f)
            val barW = (slot - gap).coerceAtLeast(1.2f)
            steps.forEachIndexed { index, level ->
                // v424 — the voice's own scale (see [voiceReach]).
                val reach = voiceReach(level, bandHalf)
                val h = (reach * 2f).coerceAtLeast(2.4f)
                val left = index * slot + gap / 2f
                drawRoundRect(
                    color = if (left + barW / 2f <= played) accent else ink.copy(alpha = 0.55f),
                    topLeft = Offset(left, bandMid - h / 2f),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2f)
                )
            }
        }
        PersonalVoiceStyle.MINIMAL -> {
            val lineW = (size.height * 0.055f).coerceAtLeast(1.2f)
            drawLine(
                color = ink.copy(alpha = 0.30f),
                start = Offset(0f, bandMid),
                end = Offset(size.width, bandMid),
                strokeWidth = lineW,
                cap = StrokeCap.Round
            )
            if (progress > 0f) {
                drawLine(
                    color = accent,
                    start = Offset(0f, bandMid),
                    end = Offset(played, bandMid),
                    strokeWidth = lineW,
                    cap = StrokeCap.Round
                )
                val r = (size.height * 0.15f).coerceAtLeast(2.4f)
                drawCircle(
                    color = accent,
                    radius = r,
                    center = Offset(played.coerceIn(r, size.width - r), bandMid)
                )
            }
        }
        // ── v424 — THE RIBBON IS A STROKE, NOT A FILLED MIRROR ──────────
        //
        // It used to be a CLOSED SHAPE — the voice's envelope, its own mirror and
        // a solid fill between them — which reads as a blob with a wave on top
        // rather than as a ribbon, and the member took it back ("the ribbon style
        // isnt greaat"). It is now ONE round-capped stroke that runs out along the
        // top of the voice and back down its own mirror, so the note reads as
        // DRAWN; the two halves are the same vertices, so the ribbon can never be
        // lopsided, and the heard run is the same stroke cut at the head.
        PersonalVoiceStyle.RIBBON -> {
            val steps = bucketLevels(samples, 30)
            val span = (steps.size - 1).coerceAtLeast(1).toFloat()
            val strokes = (size.height * 0.075f).coerceAtLeast(1.4f)
            val inset = strokes / 2f
            val half = (bandHalf - inset).coerceAtLeast(1f)
            val outline = ArrayList<Offset>(steps.size * 2)
            for (index in steps.indices) {
                val x = inset + (size.width - inset * 2f) * index / span
                outline.add(Offset(x, bandMid - voiceReach(steps[index], half)))
            }
            for (index in steps.indices.reversed()) {
                val x = inset + (size.width - inset * 2f) * index / span
                outline.add(Offset(x, bandMid + voiceReach(steps[index], half)))
            }
            val ribbon = Path()
            outline.firstOrNull()?.let { first -> ribbon.moveTo(first.x, first.y) }
            for (index in 1 until outline.size) {
                val previous = outline[index - 1]
                val point = outline[index]
                val midX = (previous.x + point.x) / 2f
                ribbon.cubicTo(midX, previous.y, midX, point.y, point.x, point.y)
            }
            ribbon.close()
            val ribbonStroke = Stroke(
                width = strokes,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            drawPath(ribbon, ink.copy(alpha = 0.30f), style = ribbonStroke)
            if (progress > 0f) {
                clipRect(right = played) {
                    drawPath(ribbon, accent.copy(alpha = 0.95f), style = ribbonStroke)
                }
            }
        }
        // ── v424 — AND THE BEADS ARE EVEN ───────────────────────────────
        //
        // A bead used to carry its own radius out of the step, so a loud passage
        // pushed its neighbours along and the strip's spacing said as much as its
        // size did — and the heard run was cut at a plain fraction of the width
        // while the beads were laid out inside a radius-shaped inset, which is
        // exactly why the fill and the drawing disagreed (member: "beats design is
        // bad too" · "for waves the progress small dot doesnt properly follow the
        // waves"). Every bead now takes the SAME slot, measured from its own
        // centre, so nothing moves when a passage gets loud — a bead's size alone
        // carries the sound, on one clear scale — and the cut falls where the
        // fraction says. The wire they are strung on is drawn, so a quiet bead
        // still reads as a bead.
        PersonalVoiceStyle.BEADS -> {
            val count = 26
            val steps = bucketLevels(samples, count)
            val slot = size.width / count
            val room = (bandHalf * 0.92f).coerceAtLeast(1.4f)
            val minRadius = (room * 0.16f).coerceAtLeast(0.9f)
            drawLine(
                color = ink.copy(alpha = 0.18f),
                start = Offset(0f, bandMid),
                end = Offset(size.width, bandMid),
                strokeWidth = 1.dp.toPx()
            )
            steps.forEachIndexed { index, level ->
                // v439 — LINEAR, like every other look: the bead's size IS the
                // sound. The square here was v424's compensation for levels that
                // had been normalized against full scale (see [voiceReach] and
                // [WaveformExtractor]) — with that fault fixed, squaring again
                // would draw an ordinary sentence as a row of minRadius dots,
                // which is the opposite of the accurate depiction the member
                // asked for and would make the beads disagree with the bars.
                val loud = level.coerceIn(0f, 1f)
                val radius = minRadius + (room - minRadius) * loud
                val x = slot * (index + 0.5f)
                drawCircle(
                    color = if (progress > 0f && x <= played) accent else ink.copy(alpha = 0.42f),
                    radius = radius,
                    center = Offset(x, bandMid)
                )
            }
        }
        PersonalVoiceStyle.BUBBLE -> {
            val columns = 34
            val steps = bucketLevels(samples, columns)
            val slot = size.width / columns
            val barW = (slot * 0.52f).coerceAtLeast(1.6f)
            steps.forEachIndexed { index, level ->
                // v424 — the voice's own scale (see [voiceReach]).
                val reach = voiceReach(level, bandHalf)
                val h = (reach * 2f).coerceAtLeast(3f)
                val left = index * slot + (slot - barW) / 2f
                drawRoundRect(
                    color = if (progress > 0f && left + barW / 2f <= played) accent
                    else ink.copy(alpha = 0.40f),
                    topLeft = Offset(left, bandMid - h / 2f),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2f)
                )
            }
        }
        else -> Unit
    }
}

/**
 * v424 — THE POINT OF THE DRAWN PULSE AT [x].
 *
 * The pulse is a chain of CUBIC segments whose control points sit at the midpoint
 * between two vertices, and that one fact gives both halves of the answer: the x
 * along a segment is LINEAR in its parameter (so the parameter for an x is just
 * how far across that segment the x is), and its y is the two vertices blended by
 * `(1-t)²(1+2t)` and `t²(3-2t)`.
 *
 * Evaluating that is what puts the head bead ON the ink. The old head was snapped
 * to the nearest VERTEX, so between two peaks the dot sat on the straight line
 * between them while the ink curved away — the member's "the progress small dot
 * doesnt properly follow the waves".
 */
private fun pulsePointAt(points: List<Offset>, x: Float): Offset? {
    if (points.isEmpty()) return null
    if (points.size == 1) return points.first()
    if (x <= points.first().x) return points.first()
    if (x >= points.last().x) return points.last()
    for (index in 1 until points.size) {
        val from = points[index - 1]
        val to = points[index]
        if (x > to.x) continue
        val span = to.x - from.x
        if (span <= 0f) return to
        val t = ((x - from.x) / span).coerceIn(0f, 1f)
        val blendFrom = (1f - t) * (1f - t) * (1f + 2f * t)
        val blendTo = t * t * (3f - 2f * t)
        return Offset(x, from.y * blendFrom + to.y * blendTo)
    }
    return points.last()
}

/**
 * THE LOUDEST SAMPLE OF EACH BUCKET, [buckets] of them.
 *
 * A waveform is stored at 72 samples so a page can draw it without decoding the
 * audio, but drawing all 72 across a phone-width strip puts a rise and a fall
 * every few dp — a fuzzy band, not a wave. Taking the loudest of each bucket
 * keeps the PEAKS of a fast passage (a quieter mean would flatten exactly the
 * thing the drawing is about) while giving the stroke room to read.
 *
 * ── v439 — AND THE BUCKETS COVER THE WHOLE RECORDING, EXACTLY ONCE ────────
 *
 * They did not. The old split was a CEILING division — `per = ceil(size /
 * buckets)` — and every bucket past the end simply repeated the one before it.
 * The counts almost never divide: the bars look draws 42 columns from 72 stored
 * samples (per = 2, so 36 columns carried data and SIX repeated the previous
 * column), the ribbon draws 30 (24 real, six repeated) and the bubble draws 34
 * (24 real, TEN repeated). The visible result is a wave whose last tenth is a
 * flat stale strip and whose time axis is compressed into the left of the
 * drawing — a depiction that is not only inaccurate at the end, it is wrong
 * about WHEN everything happened. **That is part of what the member means by
 * "more accurate depiction".**
 *
 * Each bucket now takes its own proportional span of the samples — bucket `b`
 * covers `[b·size/buckets, (b+1)·size/buckets)` — so every column carries its
 * own moment, the last column carries the last moment, and the whole recording is
 * drawn exactly once at whatever resolution the strip can show. **Never go back
 * to a per-bucket ceiling count here.**
 */
private fun bucketLevels(samples: FloatArray, buckets: Int): FloatArray {
    if (samples.isEmpty() || buckets <= 0) return FloatArray(buckets) { 0.08f }
    val out = FloatArray(buckets)
    for (bucket in 0 until buckets) {
        val from = (bucket.toLong() * samples.size / buckets).toInt()
        val to = ((bucket + 1).toLong() * samples.size / buckets).toInt()
        val start = from.coerceIn(0, samples.size - 1)
        // At least one sample per bucket: a strip with more columns than stored
        // bars repeats a moment rather than leaving a hole, and the repeats are
        // spread where their moment is rather than piled at the end.
        val end = to.coerceAtLeast(start + 1).coerceAtMost(samples.size)
        var loudest = 0f
        for (i in start until end) {
            if (samples[i] > loudest) loudest = samples[i]
        }
        out[bucket] = loudest
    }
    return out
}

/**
 * THE PLAY CONTROL, DRAWN.
 *
 * [ring] is null for the bare drawn look and a hairline colour for the outlined
 * one. Either way the mark is a path in the WAVE'S OWN INK at the wave's own
 * weight, which is the whole point: a note drawn by hand should not have a
 * Material button parked in the middle of it.
 *
 * v424 — AND THE MARK IS MEASURED RATHER THAN GUESSED AT. The old one put a
 * wedge between 28% and 72% of the box and 34% in from each side — TALLER than
 * it was wide, and sized for the whole box rather than for the room the ring
 * leaves — so a play mark inside a ring read as a small off-balance triangle
 * (member: "the minimal play button isnt accurate in journal voice note"). The
 * mark is now a real one: its height is the room it has, its width follows that
 * (a play mark is never taller than it is wide), it carries a hair of optical
 * lift to the right because a triangle's mass sits left of its own box, and the
 * stroke is taken OUT of its size rather than added around it, so the mark can
 * never look smaller than the circle drawn with it.
 */
@Composable
private fun VoiceDrawnControl(
    playing: Boolean,
    tint: Color,
    ring: Color?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The control's own box. The previews draw the same mark smaller. */
    controlSize: Dp = 38.dp
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .size(controlSize)
            .semantics { contentDescription = label }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = (size.minDimension * 0.085f).coerceAtLeast(1.7f)
            val room = size.minDimension - stroke * 2f
            if (ring != null) {
                drawCircle(
                    color = ring,
                    radius = size.minDimension / 2f - stroke / 2f,
                    style = Stroke(stroke)
                )
            }
            // A play mark is WIDER than it is tall, and inside a ring it is the
            // ring's own inner room that decides it.
            val markHeight = room * if (ring != null) 0.52f else 0.60f
            val markWidth = markHeight * 0.92f
            val centreX = size.width / 2f + markWidth * 0.06f
            val centreY = size.height / 2f
            if (playing) {
                // Two bars with the same footprint as the mark they replace, so
                // the control does not jump the moment it starts to sound.
                val offset = markWidth * 0.24f
                val top = centreY - markHeight / 2f
                val bottom = centreY + markHeight / 2f
                drawLine(
                    tint,
                    Offset(centreX - offset, top),
                    Offset(centreX - offset, bottom),
                    stroke,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(centreX + offset, top),
                    Offset(centreX + offset, bottom),
                    stroke,
                    StrokeCap.Round
                )
            } else {
                val mark = Path().apply {
                    moveTo(centreX - markWidth / 2f, centreY - markHeight / 2f)
                    lineTo(centreX + markWidth / 2f, centreY)
                    lineTo(centreX - markWidth / 2f, centreY + markHeight / 2f)
                    close()
                }
                drawPath(
                    path = mark,
                    color = tint,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * v424 — THE PLAY MARK IN A PILL.
 *
 * The member asked for the control as a pill as well as a disc ("for the waves
 * add the play button as pill option too"), and the honest way to do that is a
 * LOOK of its own rather than a shape every look has to be rewritten for: a look
 * pairs a wave with a play treatment (see [PersonalVoiceStyle]), and this one
 * pairs the drawn pulse with the pill — a wide, soft, filled control that reads
 * as a button in the writing rather than as a small disc beside it.
 */
@Composable
private fun VoicePillControl(
    playing: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = personalAccentInk(),
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .semantics { contentDescription = label }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(
                if (playing) CurioIcons.Pause else CurioIcons.PlayArrow,
                null,
                tint = MaterialTheme.colorScheme.surface,
                size = 20.dp
            )
        }
    }
}

/**
 * THE LOOK PICKER.
 *
 * One row per look, each PREVIEWED in its own drawing — the wave it will draw and
 * the control it will wear, at the note's own inks — so choosing between them is
 * looking, not reading. The current one wears the accent wash and the tick, the
 * app's own sheet language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalVoiceStyleSheet(
    current: PersonalVoiceStyle,
    ink: Color,
    accent: Color,
    samples: FloatArray,
    onPick: (PersonalVoiceStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Voice note style",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FrauncesFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "How this recording is drawn on the page. Each note keeps the look you give it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            PersonalVoiceStyle.entries.forEach { option ->
                val live = option == current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (live) {
                                curioTintOn(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    accent,
                                    0.14f
                                )
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onPick(option) }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    VoiceStylePreview(option, ink = ink, accent = accent, samples = samples)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            option.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (live) {
                        CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                    }
                }
            }
        }
    }
}

/**
 * ONE LOOK, DRAWN SMALL — the row's own proof of what it does: its wave form, its
 * play treatment, and (for the bubble) its chrome.
 */
@Composable
private fun VoiceStylePreview(
    style: PersonalVoiceStyle,
    ink: Color,
    accent: Color,
    samples: FloatArray
) {
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 34.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (style == PersonalVoiceStyle.BUBBLE) {
                    Modifier.background(
                        curioTintOn(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            accent,
                            0.14f
                        )
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (style) {
                PersonalVoiceStyle.PILL -> Box(
                    modifier = Modifier
                        .size(width = 26.dp, height = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(personalAccentInk())
                )
                PersonalVoiceStyle.HAND, PersonalVoiceStyle.MINIMAL -> VoiceDrawnControl(
                    playing = false,
                    tint = ink.copy(alpha = 0.80f),
                    ring = if (style == PersonalVoiceStyle.MINIMAL) {
                        ink.copy(alpha = 0.26f)
                    } else {
                        null
                    },
                    label = "",
                    onClick = {},
                    controlSize = 20.dp
                )
                else -> Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(personalAccentInk())
                )
            }
            Canvas(Modifier.weight(1f).height(18.dp)) {
                if (samples.isEmpty()) return@Canvas
                if (style.drawsPulse) {
                    drawVoicePulse(samples, 0.42f, ink, accent)
                } else {
                    drawVoiceWave(samples, 0.42f, ink, accent, style)
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
    modifier: Modifier = Modifier,
    /** The look the note wears (v421). */
    style: PersonalVoiceStyle = PersonalVoiceStyle.HAND,
    /** The look picker's door. Only the EDITOR passes it — a read-only view
     *  renders the stored look and offers no way to change it. */
    onStyle: ((PersonalVoiceStyle) -> Unit)? = null
) {
    val context = LocalContext.current
    PersonalVoiceBar(
        path = path,
        seconds = seconds,
        bars = bars,
        ink = ink,
        accent = accent,
        modifier = modifier,
        style = style,
        onStyle = if (enabled) onStyle else null,
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
