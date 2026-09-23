package com.curio.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * v465 — THE CORE EDITION'S OFFLINE TRANSCRIPTION, WHICH IS TO SAY: NONE.
 *
 * The member split Curio into two editions — *"one with advance feature focising on
 * online and all, one smaller with the core curio features"* — and the two things the
 * CORE edition drops are the two that are large BINARY rather than large FEATURE: the
 * neural read-aloud voice packs, and this, the on-device speech-to-text stack (Vosk
 * bundles roughly 19 MB of arm `.so` into a release APK for a feature a member uses
 * only when they transcribe a recorded voice note).
 *
 * ── WHY THIS FILE EXISTS AT ALL ──────────────────────────────────────────
 *
 * Vosk's classes arrive through `fullImplementation(libs.com.alphacephei.vosk.android)`,
 * so any file importing `org.vosk.*` cannot be in `main` — `main` is compiled for BOTH
 * editions. The real implementation therefore lives in `src/full/java`, and this is its
 * TWIN in `src/core/java`: the same package, the same top-level names, the same
 * signatures, so `main` compiles against whichever the edition provides and not one
 * call site needs to know which edition it is in.
 *
 * **The twin must keep the real one's API EXACTLY, and that is the whole contract.**
 * If `OfflineTranscriber`, `VoskModels` or `VoskModelDownloads` gains a member in
 * `src/full`, it gains one here in the same change, or the core edition stops
 * compiling — which is a failure CI will catch, but only after a push. Read the full
 * edition's file beside this one whenever either is touched.
 *
 * ── AND IT IS AN HONEST EMPTY, NOT A FAKE ────────────────────────────────
 *
 * [VoskModels.CATALOG] is empty, nothing is ever downloaded, nothing is ever
 * transcribed, and [OfflineTranscriber.transcribe] answers `null` — the same answer
 * the real implementation gives when it has no model. Because the catalog is empty,
 * EVERY door built on it closes by itself: `VoskModels.byId` finds nothing,
 * `isDownloaded` is false, and the entry detail page's Transcribe affordance — which
 * is drawn only when a model IS downloaded — never appears. The ONE door that would
 * otherwise still open is Settings → Recording → "Offline model", an empty picker with
 * no models to offer, so that row is hidden by the edition flag instead
 * (`BuildConfig.EDITION_OFFLINE_TRANSCRIPTION`). Do not let a second such door through.
 */

/**
 * The full edition's downloadable model catalog (see `src/full/.../OfflineTranscriber.kt`),
 * empty here: this edition ships no offline transcription, so it offers no models and
 * carries no downloader.
 */
object VoskModels {

    /** Kept for API parity — a tier badge nothing in this edition can ever draw. */
    enum class Tier(val label: String, val hint: String) {
        SMALL("Small", "fast & light"),
        LARGE("Large", "more accurate")
    }

    /** One downloadable offline model. Same shape as the full edition's. */
    data class Info(
        val id: String,
        val displayName: String,
        val langLabel: String,
        val sizeLabel: String,
        val sizeBytes: Long,
        val url: String,
        val tier: Tier
    )

    /** EMPTY, and that emptiness is what closes every door in this edition. */
    val CATALOG: List<Info> = emptyList()

    fun byId(id: String?): Info? = CATALOG.firstOrNull { it.id == id }

    /** The root the full edition installs models into. Present only so the twin matches. */
    fun modelsDir(context: Context): File = File(context.filesDir, "vosk-models")

    /**
     * Always null in this edition — no model is ever installed, so no directory is
     * ever a model. The full edition's own check (`am/` present) is preserved in shape
     * so a call site that reads this cannot behave differently between editions.
     */
    fun modelDir(context: Context, id: String?): File? {
        if (id.isNullOrBlank()) return null
        val dir = File(modelsDir(context), id)
        return if (dir.isDirectory && File(dir, "am").isDirectory) dir else null
    }

    fun isDownloaded(context: Context, id: String?): Boolean = modelDir(context, id) != null

    fun modelSizeBytes(context: Context, id: String?): Long {
        val dir = modelDir(context, id) ?: return 0L
        return dir.walkBottomUp().sumOf { it.length() }
    }

    /** Free bytes in the app's file storage — the full edition's own arithmetic. */
    fun availableStorageBytes(context: Context): Long =
        runCatching {
            android.os.StatFs(context.filesDir.absolutePath).availableBytes
        }.getOrDefault(0L)

    /** B / KB / MB / GB — the same formatter the full edition uses, so labels never differ. */
    fun formatModelSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        else -> "%.2f GB".format(bytes.toDouble() / (1024L * 1024 * 1024))
    }

    /**
     * Nothing to delete: this edition never installs a model, and the only writer of
     * `vosk-models/` is the full edition's downloader, in the FULL edition's own
     * storage (the two editions are separate packages with separate `filesDir`).
     */
    fun deleteModel(context: Context, id: String) = Unit

    /** No catalog to prune and no selection to clear — see [CATALOG]. */
    fun pruneRemovedModels(context: Context) = Unit
}

/**
 * The full edition's app-scoped download manager (it outlives the picker sheet, so a
 * transfer keeps running after the dialog closes). Here it is the same API reporting
 * only [Status.Idle]: nothing can be started, because [VoskModels.CATALOG] is empty
 * and no row offers one. Kept as a faithful twin so `main`'s picker compiles unchanged
 * in both editions.
 */
object VoskModelDownloads {

    enum class Status { Idle, Downloading, Paused, Failed }

    data class State(
        val status: Status = Status.Idle,
        val progress: Float = 0f,
        val error: String? = null
    )

    private val _states = MutableStateFlow<Map<String, State>>(emptyMap())

    /** Per-model download state — empty for ever in this edition. */
    val states: StateFlow<Map<String, State>> = _states

    fun start(context: Context, info: VoskModels.Info) = Unit

    fun pause(id: String) = Unit

    fun resume(id: String) = Unit

    fun cancel(context: Context, id: String) = Unit
}

/**
 * The core edition of `OfflineTranscriber`. [transcribe] answers `null`, which every call
 * site already handles — it is the same answer the real implementation gives for a
 * missing model or audio file, and it is why no caller needed a second code path.
 *
 * `onProgress` is accepted and never called: there is no work to report on.
 */
object OfflineTranscriber {

    suspend fun transcribe(
        context: Context,
        audioPath: String?,
        modelId: String?,
        onProgress: (Float) -> Unit = {}
    ): String? = null
}
