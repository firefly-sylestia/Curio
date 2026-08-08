package com.curio.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Curio's in-app backup & restore.
 *
 * Exports the two things that make up a user's data — the Room `captures`
 * table and the SharedPreferences files that hold real user state — into a
 * single portable JSON file the user saves anywhere they like (Downloads,
 * Drive, a USB drive…). Restore reads that file back and replaces the
 * current data atomically.
 *
 * **What is backed up:**
 *  - all [CaptureEntity] rows (topic, format, notes, timestamps)
 *  - SoundBite audio recordings, embedded base64 in the JSON keyed by
 *    capture id (v2). Restore writes them back to `filesDir/audio/{id}.m4a`
 *    and rewrites each capture's `audioFilePath` to the restored location.
 *  - image attachments (Reel Notes / Marginalia / Field Notes photos and
 *    the whole Gallery Wall mood board), embedded base64 in the JSON keyed
 *    by their URI string (v3). Restore writes them to
 *    `filesDir/images/{id}/{n}.img` and rewrites every image URI in the
 *    capture to the restored file path — provider URIs from a document
 *    picker would otherwise be dead on a new device.
 *  - the user-facing prefs: [AppPreferences], [AudioQualitySettings],
 *    [StreakTracker], the quests/levels state ([CurioQuests]), the
 *    explore-session state (done topics / mark-as-done, active + queued
 *    sessions, recently explored / unexplored), autosaved capture drafts
 *    ([CaptureDraftStore]) and the onboarding-completed flag
 *
 * **What is not backed up:** crash-log prefs (device noise). Cloud Auto
 * Backup still excludes audio via `data_extraction_rules.xml` to protect
 * the 25 MB quota — the in-app file backup is the complete archive.
 *
 * Pref values are stored as typed [PrefEntry]s (boolean/int/long/float/
 * string/stringset) because SharedPreferences is type-strict: a value
 * written with `putLong` must be read with `getLong` or AOSP throws
 * ClassCastException. JSON only knows numbers, so without the recorded type
 * a Long like the streak epoch-day would round-trip as an Int and crash the
 * next streak read.
 *
 * The file starts with a versioned envelope so a future app version can
 * keep reading old backups; restore refuses files from a NEWER app version.
 */
object CurioBackupManager {

    /** Bump when the payload shape changes. Restore accepts version <= this. */
    const val FORMAT_VERSION = 4

    /** MIME type used by the file pickers. */
    const val MIME_TYPE = "application/json"

    private const val FORMAT_NAME = "curio-backup"
    private const val META_PREFS = "curio_backup_meta"
    private const val KEY_LAST_BACKUP_AT = "last_backup_at"
    private const val KEY_LAST_BACKUP_COUNT = "last_backup_count"

    private const val TYPE_BOOLEAN = "boolean"
    private const val TYPE_INT = "int"
    private const val TYPE_LONG = "long"
    private const val TYPE_FLOAT = "float"
    private const val TYPE_STRING = "string"
    private const val TYPE_STRING_SET = "stringset"

    /**
     * The SharedPreferences files holding genuine user data. Crash logs
     * (`curio_crash_logs`) are deliberately excluded — they're device noise,
     * not user state.
     */
    private val USER_PREF_FILES = listOf(
        "curio_app_prefs",        // AppPreferences — name, theme, reminder
        "curio_audio_quality",    // AudioQualitySettings
        "curio_streak",           // StreakTracker
        "curio_quests",           // CurioQuests — XP, journey, daily, badges
        "curio_prefs",            // ExploreSessionStore — done topics (mark-as-done),
                                  //   active/queued sessions, explored/unexplored recents;
                                  //   CaptureDraftStore — autosaved capture drafts
        "curio_pet",              // CurioPet — mood timestamps (v8.5 pet companion)
        "curio_passport",         // CurioPassport — category stamps + counters (v8.5)
        "curio_onboarding"        // onboarding-completed flag
    )

    /** Result of a successful export. */
    data class ExportResult(val captureCount: Int, val uri: Uri)

    /** Result of a successful restore. */
    data class RestoreResult(val captureCount: Int, val preferenceFiles: Int)

    /** Suggested file name for the export picker, e.g. curio-backup-20260731-1430.json. */
    fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "curio-backup-$stamp.json"
    }

    /**
     * Write the current captures + user prefs to [uri] as a versioned JSON
     * file. Runs on the caller's coroutine (I/O is off the main thread).
     */
    suspend fun export(context: Context, uri: Uri): ExportResult {
        val dao = CurioDatabase.getInstance(context).captureDao()
        val captures = dao.getAll()
        val prefs = USER_PREF_FILES.associateWith { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                .all
                .mapValues { (_, v) -> v.toTypedEntry() }
        }
        // Bundle SoundBite audio (v2): read each capture's bytes, keyed by
        // capture id. Missing/unreadable files are skipped — the capture
        // still backs up, just without its recording. File reads can be
        // multi-MB, so this runs off the main thread (v2.1).
        val audioFiles = withContext(Dispatchers.IO) {
            val files = mutableMapOf<String, ByteArray>()
            captures.forEach { capture ->
                val path = runCatching {
                    CaptureConverters.deserializeCaptureData(capture.formatDataJson)
                }.getOrNull()?.audioPathOrNull()
                if (!path.isNullOrBlank()) {
                    runCatching {
                        val file = File(path)
                        if (file.isFile && file.length() > 0L) file.readBytes() else null
                    }.getOrNull()?.let { files[capture.id] = it }
                }
            }
            files
        }
        // Bundle image attachments (v3): read each capture's image bytes,
        // keyed by the URI string (deduped — the same photo attached to
        // several entries is stored once). Missing/unreadable sources are
        // skipped — the capture still backs up, just without that photo.
        val imageFiles = withContext(Dispatchers.IO) {
            val files = mutableMapOf<String, ByteArray>()
            captures.forEach { capture ->
                val uris = runCatching {
                    CaptureConverters.deserializeCaptureData(capture.formatDataJson)
                }.getOrNull()?.imageUrisAll().orEmpty()
                uris.forEach { uri ->
                    if (!files.containsKey(uri)) {
                        runCatching {
                            context.contentResolver.openInputStream(Uri.parse(uri))
                                ?.use { input -> input.readBytes() }
                        }.getOrNull()?.let { files[uri] = it }
                    }
                }
            }
            files
        }

        val payload = BackupPayload(
            format = FORMAT_NAME,
            version = FORMAT_VERSION,
            exportedAtMillis = System.currentTimeMillis(),
            captures = captures,
            preferences = prefs,
            audioFiles = audioFiles,
            imageFiles = imageFiles,
            // A stale/corrupt imported catalog must not make an otherwise
            // complete Curio backup fail. The catalog is supplementary data;
            // captures and preferences remain the backup's required payload.
            speciesCatalogJson = runCatching {
                FieldMindLegacyImport.speciesCatalogJson(context)
            }.getOrNull()
        )
        // With audio bundled, the base64 JSON can be tens of MB — serialize
        // and write off the main thread (v2.1).
        withContext(Dispatchers.IO) {
            val json = Gson().toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open the chosen location for writing")
        }

        // Remember the last successful backup so Settings can show it.
        context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_AT, payload.exportedAtMillis)
            .putInt(KEY_LAST_BACKUP_COUNT, captures.size)
            .apply()
        return ExportResult(captures.size, uri)
    }

    /**
     * Replace the current data with the contents of a Curio backup file.
     *
     * The captures table is wiped and re-inserted inside a single Room
     * transaction (either the whole restore lands or none of it). Prefs are
     * cleared per file then re-written using each entry's recorded type —
     * Gson decodes every JSON number as Double, so the recorded type is what
     * maps it back to the exact Int/Long/Float the app's getters expect.
     */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS") // legacy-blob nulls bypass the non-null types
    suspend fun restore(context: Context, uri: Uri): RestoreResult {
        val json = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: throw IllegalStateException("Could not open the backup file")
        }

        val root = runCatching { JSONObject(json) }
            .getOrNull() ?: throw IllegalArgumentException("That file is not a readable Curio backup.")
        val rawCaptures = root.optJSONArray("captures")
            ?: throw IllegalArgumentException("This backup is missing its captures data")
        require(root.optJSONObject("preferences") != null) {
            "This backup is missing its settings data"
        }
        // Validate the records before deleting any current media or database
        // rows. A truncated JSON file must never be treated as an empty
        // backup and erase the user's existing Curio data. IDs must also be
        // unique: Room's REPLACE policy would otherwise silently discard an
        // earlier attacker-supplied record.
        val seenCaptureIds = mutableSetOf<String>()
        for (index in 0 until rawCaptures.length()) {
            val capture = rawCaptures.optJSONObject(index)
                ?: throw IllegalArgumentException("Backup capture ${index + 1} is invalid")
            val rawId = capture.optString("id")
            require(rawId.isNotBlank()) {
                "Backup capture ${index + 1} has no id"
            }
            require(seenCaptureIds.add(rawId)) {
                "Backup contains duplicate capture id"
            }
            require(capture.optString("format").isNotBlank()) {
                "Backup capture ${index + 1} has no format"
            }
            require(capture.has("formatDataJson") && !capture.isNull("formatDataJson")) {
                "Backup capture ${index + 1} has no capture data"
            }
        }
        require(root.optString("format") == FORMAT_NAME) {
            "That file isn't a Curio backup"
        }
        require(root.optInt("version", -1) >= 0) {
            "This backup has no valid format version"
        }
        val payload = runCatching {
            Gson().fromJson(json, BackupPayload::class.java)
        }.getOrNull() ?: throw IllegalArgumentException("That file is not a readable Curio backup.")
        require(payload.format == FORMAT_NAME) { "That file isn't a Curio backup" }
        require(payload.version <= FORMAT_VERSION) {
            "This backup was made by a newer version of Curio"
        }

        // Gson allocates older payloads without Kotlin constructor defaults.
        // Treat omitted collections as empty so a v1–v3 backup cannot fail
        // before the actual capture migration begins.
        val payloadCaptures = payload.captures.orEmpty()
        require(payloadCaptures.size == rawCaptures.length()) {
            "This backup contains an invalid captures list"
        }
        // Fully validate and normalize every capture before touching current
        // media or Room. Gson can bypass Kotlin constructor defaults, so this
        // also protects older backups whose required strings were omitted.
        val validatedCaptures = payloadCaptures.mapIndexed { index, capture ->
            val id = capture.id.orEmpty()
            val format = capture.format.orEmpty()
            val formatDataJson = capture.formatDataJson.orEmpty()
            require(isSafeStorageSegment(id)) {
                "Backup capture ${index + 1} has an unsafe id"
            }
            require(format.isNotBlank()) { "Backup capture ${index + 1} has no format" }
            require(formatDataJson.isNotBlank()) { "Backup capture ${index + 1} has no capture data" }
            require(runCatching { CaptureFormat.valueOf(format) }.isSuccess) {
                "Backup capture ${index + 1} has an unknown format"
            }
            runCatching { CaptureConverters.deserializeCaptureData(formatDataJson) }
                .getOrElse {
                    throw IllegalArgumentException("Backup capture ${index + 1} has invalid capture data")
                }
            capture.copy(
                id = id,
                topicId = capture.topicId.orEmpty(),
                categoryId = capture.categoryId.orEmpty(),
                topicName = capture.topicName.orEmpty(),
                topicSubtype = capture.topicSubtype.orEmpty(),
                topicTeaser = capture.topicTeaser.orEmpty(),
                format = format,
                formatDataJson = formatDataJson,
                tagsJson = capture.tagsJson.orEmpty().ifBlank { "[]" }
            )
        }
        val payloadPreferences = payload.preferences.orEmpty()
        require(payloadPreferences.keys.all { it in USER_PREF_FILES }) {
            "This backup contains an unknown preferences file"
        }

        // Restore audio (v2): wipe current recordings, then write each
        // bundled one to filesDir/audio/{id}.m4a and rewrite the capture's
        // audioFilePath so the restored file resolves on this device.
        // Audio writes can be multi-MB, so this runs off the main thread
        // (v2.1). A single failed write only drops that one recording — the
        // capture itself still restores (missing audio degrades gracefully
        // in EntryDetail).
        val audioFiles = payload.audioFiles.orEmpty()
        val imageFiles = payload.imageFiles.orEmpty()
        val restoredCaptures = withContext(Dispatchers.IO) {
            // Media is staged/overwritten only after the payload has been
            // fully validated above. Do not wipe current media here: if a
            // later Room insert fails, the existing files must remain usable.
            validatedCaptures.map { capture ->
                // Tags (v7.17): backups predating the tags column deserialize
                // tagsJson to null (Gson Unsafe allocation skips constructor
                // defaults) — normalize to the empty array so the NOT NULL
                // column insert can't fail.
                var updated = if (capture.tagsJson == null) capture.copy(tagsJson = "[]") else capture
                // Backward compatibility for v3: backups created before the
                // explicit provenance column have no isLegacy field. Preserve
                // their already-imported rows once, while new FieldMind
                // restores set the flag directly and never infer it here.
                if (!updated.isLegacy && updated.topicSubtype == FieldMindLegacyImport.LEGACY_SUBTYPE) {
                    updated = updated.copy(isLegacy = true)
                }
                // Audio (v2): never preserve an audio path from the source
                // device. A backup is untrusted and its original absolute
                // path may point anywhere on this device. Clear every stored
                // path first; only a bundled recording below can establish a
                // new, app-private destination.
                val safeData = CaptureConverters.deserializeCaptureData(updated.formatDataJson)
                    .withoutAudioPaths()
                updated = updated.copy(formatDataJson = Gson().toJson(safeData))
                audioFiles[capture.id]?.let { bytes ->
                    val newPath = runCatching {
                        AudioStorageManager.restoreAudio(context, capture.id, bytes)
                    }.getOrNull()
                    if (newPath != null) {
                        runCatching {
                            CaptureConverters.deserializeCaptureData(updated.formatDataJson)
                                .withAudioPath(newPath)
                        }.getOrNull()?.let { updated = updated.copy(formatDataJson = Gson().toJson(it)) }
                    }
                }
                // Images (v3): write each bundled photo and rewrite every
                // image URI in the capture (flat lists + mood-board tile
                // layouts) to the restored file path. Same URI twice in one
                // entry reuses one stored file.
                runCatching {
                    val data = CaptureConverters.deserializeCaptureData(updated.formatDataJson)
                    if (data.imageUrisAll().isNotEmpty()) {
                        val indexByUri = mutableMapOf<String, Int>()
                        var remappedAny = false
                        val remapped = data.withImageUris { uri ->
                            val bytes = imageFiles[uri]
                            if (bytes != null) {
                                remappedAny = true
                                val idx = indexByUri.getOrPut(uri) { indexByUri.size }
                                Uri.fromFile(
                                    File(ImageStorageManager.restoreImage(context, capture.id, idx, bytes))
                                ).toString()
                            } else {
                                uri
                            }
                        }
                        // Only rewrite the JSON when a photo actually moved
                        // (skips pointless round-trips on legacy backups).
                        if (remappedAny) {
                            updated = updated.copy(formatDataJson = Gson().toJson(remapped))
                        }
                    }
                }
                updated
            }
        }

        val db = CurioDatabase.getInstance(context)
        val dao = db.captureDao()
        db.withTransaction {
            dao.clearAll()
            restoredCaptures.forEach { dao.insert(it) }
        }
        // Restored audio/image paths are deterministic per capture and are
        // overwritten as each bundled file is written. Leave unrelated files
        // in place: deleting the live storage tree here would also delete the
        // just-restored files. Orphan cleanup can be performed independently
        // once a future storage index exists.

        payload.speciesCatalogJson?.let { speciesJson ->
            FieldMindLegacyImport.restoreSpeciesCatalog(context, speciesJson)
        }

        payloadPreferences.forEach { (name, entries) ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = prefs.edit().clear()
            entries.orEmpty().forEach { (key, entry) ->
                val safeEntry = entry ?: return@forEach
                val v = safeEntry.value
                when (safeEntry.type) {
                    TYPE_BOOLEAN -> (v as? Boolean)?.let { editor.putBoolean(key, it) }
                    TYPE_INT -> (v as? Number)?.let { editor.putInt(key, it.toInt()) }
                    TYPE_LONG -> (v as? Number)?.let { editor.putLong(key, it.toLong()) }
                    TYPE_FLOAT -> (v as? Number)?.let { editor.putFloat(key, it.toFloat()) }
                    TYPE_STRING -> (v as? String)?.let { editor.putString(key, it) }
                    TYPE_STRING_SET -> {
                        val list = v as? List<*>
                        if (list != null) {
                            val strings = list.mapNotNull { it as? String }
                            if (strings.isNotEmpty()) {
                                editor.putStringSet(key, strings.toSet())
                            }
                        }
                    }
                }
            }
            editor.apply()
        }
        // Restore the in-memory preference state too; otherwise the UI keeps
        // showing the pre-restore theme/reminder values until process restart.
        AppPreferences.initThemeMode(context)
        if (AppPreferences.isReminderEnabled(context)) {
            DailyReminderScheduler.schedule(context, AppPreferences.getReminderHour(context))
        } else {
            DailyReminderScheduler.cancel(context)
        }
        // curio_prefs rides in the generic prefs map — re-seed the reactive
        // explore-session state (done topics, recents, active/queued
        // sessions) so the UI reflects the restored data immediately instead
        // of after a process restart. CaptureDraftStore reads prefs fresh on
        // every access, so drafts need no re-seed.
        ExploreSessionStore.seed(context)
        return RestoreResult(payloadCaptures.size, payloadPreferences.size)
    }

    /** Milliseconds of the last successful export, or 0 if never backed up. */
    fun lastBackupAtMillis(context: Context): Long =
        context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_AT, 0L)

    /** Capture count captured in the last successful export. */
    fun lastBackupCount(context: Context): Int =
        context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_BACKUP_COUNT, 0)

    /**
     * Tag a live SharedPreferences value with its concrete type so restore
     * can write it back with the exact putX call the app's getter expects.
     */
    private fun Any?.toTypedEntry(): PrefEntry = when (this) {
        is Boolean -> PrefEntry(TYPE_BOOLEAN, this)
        is Int -> PrefEntry(TYPE_INT, this)
        is Long -> PrefEntry(TYPE_LONG, this)
        is Float -> PrefEntry(TYPE_FLOAT, this)
        is String -> PrefEntry(TYPE_STRING, this)
        is Set<*> -> PrefEntry(TYPE_STRING_SET, this.map { it.toString() })
        else -> PrefEntry(TYPE_STRING, this?.toString() ?: "")
    }
}

/**
 * A SharedPreferences value tagged with its storage type — preserves exact
 * putInt/putLong/putFloat semantics through the JSON round-trip.
 */
data class PrefEntry(val type: String, val value: Any?)

/** Versioned backup envelope — serialized with Gson. */
data class BackupPayload(
    val format: String,
    val version: Int,
    val exportedAtMillis: Long,
    val captures: List<CaptureEntity> = emptyList(),
    val preferences: Map<String, Map<String, PrefEntry>> = emptyMap(),
    /** SoundBite audio bytes keyed by capture id (v2). Gson encodes ByteArray as base64. */
    val audioFiles: Map<String, ByteArray> = emptyMap(),
    /** Image-attachment bytes keyed by their original URI string (v3). */
    val imageFiles: Map<String, ByteArray> = emptyMap(),
    /** Imported FieldMind species catalog, preserved by Curio backup/restore. */
    val speciesCatalogJson: String? = null
)

/**
 * Returns the SoundBite audio file path carried by [this] capture data,
 * recursing through OpenNotebook wrappers. Null for non-audio formats.
 */
private fun CaptureData.audioPathOrNull(): String? = when (this) {
    is CaptureData.SoundBite -> audioFilePath
    is CaptureData.OpenNotebook -> subData.audioPathOrNull()
    is CaptureData.Portfolio -> sections.firstNotNullOfOrNull { it.data.audioPathOrNull() }
    else -> null
}

/**
 * Returns a copy of [this] capture data with every SoundBite audio path
 * pointed at [newPath] (used after restore re-homes the file).
 */
private fun CaptureData.withAudioPath(newPath: String): CaptureData = when (this) {
    is CaptureData.SoundBite -> copy(audioFilePath = newPath)
    is CaptureData.Marginalia -> copy(audioFilePath = newPath)
    is CaptureData.OpenNotebook -> copy(subData = subData.withAudioPath(newPath))
    is CaptureData.Portfolio -> copy(sections = sections.map { it.copy(data = it.data.withAudioPath(newPath)) })
    else -> this
}

/** Removes all source-device audio paths from untrusted backup data. */
private fun CaptureData.withoutAudioPaths(): CaptureData = when (this) {
    is CaptureData.SoundBite -> copy(audioFilePath = null)
    is CaptureData.Marginalia -> copy(audioFilePath = null)
    is CaptureData.OpenNotebook -> copy(subData = subData.withoutAudioPaths())
    is CaptureData.Portfolio -> copy(sections = sections.map { it.copy(data = it.data.withoutAudioPaths()) })
    else -> this
}
