package com.curio.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * ── v465c — THE CORE EDITION'S VOICE PACKS, WHICH IS DELIBERATELY NOTHING ────
 *
 * The twin of `app/src/full/java/com/curio/app/data/NeuralVoices.kt`. Same
 * package, same names, same signatures, no behaviour — and every member has to
 * stay in step with the full edition's file, because `main` is compiled against
 * whichever edition is being built.
 *
 * **Why this is a file and not an `if`.** The full implementation imports
 * `org.apache.commons.compress.*` and (through its sibling `NeuralSpeaker`) the
 * vendored sherpa-onnx AAR. `main` is compiled into BOTH editions, so a
 * commons-compress or sherpa type may never appear there and neither dependency
 * may be an `implementation` one — they are `fullImplementation` in
 * app/build.gradle.kts. A flavor source set is the only place a type can exist
 * for one edition and not the other.
 *
 * **Why the empty [NeuralVoicePacks.CATALOG] is the safety, not the flag.** A
 * member of this catalog is the only way a pack reaches the settings row: an
 * empty catalog draws no rows at all. `BuildConfig.EDITION_NEURAL_VOICES` is the
 * second, belt-and-braces door for anything that might otherwise offer the
 * feature by name.
 *
 * **Adding a member to one file means adding it to the other in the same change,
 * or the core edition stops compiling** — CI catches it, but only after a push.
 */
object NeuralVoicePacks {

    enum class Kind { PIPER, KOKORO }

    enum class Tier(val label: String) {
        RECOMMENDED("Recommended"),
        BEST("Best quality"),
    }

    data class Pack(
        val id: String,
        val displayName: String,
        val voiceLabel: String,
        val sizeLabel: String,
        val sizeBytes: Long,
        val url: String,
        val kind: Kind,
        val tier: Tier,
        val archiveRoot: String,
        /** See the full edition's twin: speaker-id order, or empty for one voice. */
        val speakers: List<String>,
    )

    /** Empty, and that emptiness is what closes the rows (see the header). */
    val CATALOG: List<Pack> = emptyList()

    fun byId(id: String?): Pack? = null

    fun packsDir(context: Context): File = File(context.filesDir, "voice-packs")

    fun packDir(context: Context, id: String): File = File(packsDir(context), id)

    fun modelDir(context: Context, pack: Pack): File = packDir(context, pack.id)

    fun isDownloaded(context: Context, id: String?): Boolean = false

    fun sizeOnDisk(context: Context, id: String?): Long = 0L

    fun delete(context: Context, id: String) = Unit

    fun pruneRemoved(context: Context) = Unit

    fun findModel(context: Context, pack: Pack): File? = null

    fun lexicons(context: Context, pack: Pack): String = ""

    fun espeakDataDir(context: Context, pack: Pack): File = modelDir(context, pack)

    fun voicesFile(context: Context, pack: Pack): File = modelDir(context, pack)

    fun tokensFile(context: Context, pack: Pack): File = modelDir(context, pack)

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> String.format(java.util.Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> String.format(java.util.Locale.US, "%.0f MB", bytes / 1_048_576.0)
        bytes <= 0L -> "0 MB"
        else -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
    }

    internal const val DONE_MARKER = ".curio-pack-complete"
    internal const val PARTIAL_SUFFIX = ".part"
}

object NeuralVoiceDownloads {

    enum class Status { Idle, Downloading, Extracting, Failed }

    data class State(
        val status: Status = Status.Idle,
        val progress: Float = 0f,
        val error: String? = null,
        /**
         * v465i — the same three fields the full edition's twin carries (see its
         * own note): nothing here ever downloads, so they are always zero, and
         * they exist because `main` is compiled against this signature.
         */
        val bytesRead: Long = 0L,
        val totalBytes: Long = 0L,
        val bytesPerSecond: Long = 0L,
    )

    private val _states = MutableStateFlow<Map<String, State>>(emptyMap())
    val states: StateFlow<Map<String, State>> = _states

    /** Nothing to download, so nothing happens — and no dependency is touched. */
    fun start(context: Context, pack: NeuralVoicePacks.Pack) = Unit

    fun cancel(context: Context, id: String) = Unit
}
