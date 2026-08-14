package com.curio.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Base64
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
    const val FORMAT_VERSION = 6

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

    /** Gson type for the typed-preferences map (export writes, restore reads). */
    private val PREFERENCES_TYPE: Type =
        object : TypeToken<Map<String, Map<String, PrefEntry>>>() {}.type

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
        // v6 — read the pending (unsaved) write handoff ONCE: a background
        // append (device-screenshot watcher) could otherwise shift it between
        // the shot bundle and the pending-write payload below.
        val pwTarget = ExploreSessionStore.pendingWriteTarget()

        // v6 — the pending (unsaved) write handoff payload: category, topic,
        // elapsed time, the shared note and its screenshots.
        val pendingWrite = pwTarget?.let { (cat, topic) ->
            PendingWriteBackup(
                categoryId = cat.name,
                topicName = topic,
                elapsedMillis = ExploreSessionStore.peekWriteSessionMillis(cat, topic),
                note = ExploreSessionStore.peekWriteSessionNote(cat, topic),
                screenshotPaths = ExploreSessionStore.peekWriteSessionScreenshots(cat, topic)
            )
        }
        // A stale/corrupt imported catalog must not make an otherwise complete
        // Curio backup fail. The catalog is supplementary data; captures and
        // preferences remain the backup's required payload.
        val speciesCatalogJson = runCatching {
            FieldMindLegacyImport.speciesCatalogJson(context)
        }.getOrNull()

        // v44 — STREAMING export. The old path read EVERY audio recording,
        // image attachment and session screenshot into memory, base64-copied
        // the whole payload into one giant JSON String, then copied that into
        // a byte[] again — a backup with many (or large) media files blew the
        // heap (OutOfMemoryError on a mid-range device). The JSON is now
        // written incrementally to the chosen location and each media file is
        // read + base64-encoded ONE AT A TIME as its value is written, so
        // peak memory is one file's bytes (plus its base64), never the whole
        // archive. The output shape is byte-for-byte the same Gson payload
        // (same field names, same base64 encoding), so restore is unchanged.
        val exportedAt = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open the chosen location for writing")
            stream.use { out ->
                val writer = JsonWriter(OutputStreamWriter(out, Charsets.UTF_8))
                val gson = Gson()
                val b64 = Base64.getEncoder()
                writer.beginObject()
                writer.name("format").value(FORMAT_NAME)
                writer.name("version").value(FORMAT_VERSION)
                writer.name("exportedAtMillis").value(exportedAt)

                // Captures — one small row at a time.
                writer.name("captures")
                writer.beginArray()
                captures.forEach { gson.toJson(it, CaptureEntity::class.java, writer) }
                writer.endArray()

                // Preferences — the small typed prefs map.
                writer.name("preferences")
                gson.toJson(
                    prefs,
                    PREFERENCES_TYPE,
                    writer
                )

                // Audio (v2) — stream each recording the moment it is
                // written, keyed by capture id. Missing/unreadable files are
                // skipped exactly like the old map builder (restore treats a
                // missing key as "no recording").
                writer.name("audioFiles")
                writer.beginObject()
                captures.forEach { capture ->
                    val path = runCatching {
                        CaptureConverters.deserializeCaptureData(capture.formatDataJson)
                    }.getOrNull()?.audioPathOrNull()
                    if (!path.isNullOrBlank()) {
                        val bytes = runCatching {
                            val file = File(path)
                            if (file.isFile && file.length() > 0L) file.readBytes() else null
                        }.getOrNull()
                        if (bytes != null) {
                            writer.name(capture.id)
                            writer.value(b64.encodeToString(bytes))
                        }
                    }
                }
                writer.endObject()

                // Images (v3) — streamed + deduped by URI (the same photo
                // attached to several entries is stored once).
                writer.name("imageFiles")
                writer.beginObject()
                val writtenUris = HashSet<String>()
                captures.forEach { capture ->
                    val uris = runCatching {
                        CaptureConverters.deserializeCaptureData(capture.formatDataJson)
                    }.getOrNull()?.imageUrisAll().orEmpty()
                    uris.forEach { uri ->
                        if (writtenUris.add(uri)) {
                            val bytes = runCatching {
                                context.contentResolver.openInputStream(Uri.parse(uri))
                                    ?.use { input -> input.readBytes() }
                            }.getOrNull()
                            if (bytes != null) {
                                writer.name(uri)
                                writer.value(b64.encodeToString(bytes))
                            }
                        }
                    }
                }
                writer.endObject()

                // Session screenshots (v5) — streamed + deduped by their
                // ORIGINAL absolute path (every entry from one session shares
                // the same shots, so each unique file is stored once). The
                // pending write's shots ride along (v6).
                writer.name("sessionShots")
                writer.beginObject()
                val writtenPaths = HashSet<String>()
                val writeShot: (String) -> Unit = { path ->
                    if (writtenPaths.add(path)) {
                        val bytes = runCatching {
                            val file = File(path)
                            if (file.isFile && file.length() > 0L) file.readBytes() else null
                        }.getOrNull()
                        if (bytes != null) {
                            writer.name(path)
                            writer.value(b64.encodeToString(bytes))
                        }
                    }
                }
                captures.forEach { capture ->
                    deserializeStringList(capture.sessionScreenshotsJson).forEach(writeShot)
                }
                pwTarget?.let { (cat, topic) ->
                    ExploreSessionStore.peekWriteSessionScreenshots(cat, topic).forEach(writeShot)
                }
                writer.endObject()

                writer.name("pendingWrite")
                if (pendingWrite != null) {
                    gson.toJson(pendingWrite, PendingWriteBackup::class.java, writer)
                } else {
                    writer.nullValue()
                }
                writer.name("speciesCatalogJson")
                if (speciesCatalogJson != null) {
                    writer.value(speciesCatalogJson)
                } else {
                    writer.nullValue()
                }
                writer.endObject()
                writer.flush()
            }
        }

        // Remember the last successful backup so Settings can show it.
        context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_AT, exportedAt)
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
        val gson = Gson()
        // ── v47 — STREAMING restore. The old path read the whole backup into
        //    a String, built a JSONObject tree, then Gson-parsed the ENTIRE
        //    payload (every media byte[] decoded and resident at once) —
        //    restoring a large file could OOM exactly like the old export.
        //    Restore now reads the file TWICE with a streaming JsonReader:
        //    pass 1 validates the envelope, every capture and the preferences
        //    keys (nothing written, nothing held — the same full pre-flight,
        //    so a truncated/crafted file never touches live data); pass 2
        //    walks the sections, decoding + writing each media file ONE AT A
        //    TIME as it is reached, recording only tiny path maps, and only
        //    then wipes + rewrites the database. Peak memory is one media
        //    file's bytes instead of the whole archive.
        val captureCount = withContext(Dispatchers.IO) {
            validateBackupStream(gson, context, uri)
        }

        // Path maps the media pass records (strings only — tiny) and the
        // capture rewrite below reads instead of holding byte arrays.
        val audioPaths = mutableMapOf<String, String>()
        val imagePathsByCapture = mutableMapOf<String, MutableMap<String, String>>()
        val shotPaths = mutableMapOf<String, String>()
        val shotIndexByPath = mutableMapOf<String, Int>()
        val captures = mutableListOf<CaptureEntity>()
        val captureIds = mutableSetOf<String>()
        // uri -> the (captureId, per-entry index) destinations it restores to
        // (one write per capture per unique URI index, matching the old code).
        val imageDests = mutableMapOf<String, LinkedHashSet<Pair<String, Int>>>()
        var payloadPreferences: Map<String, Map<String, PrefEntry>> = emptyMap()
        var pendingWrite: PendingWriteBackup? = null
        var speciesCatalogJson: String? = null

        withContext(Dispatchers.IO) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open the backup file")
                input.use { raw ->
                    val reader = JsonReader(InputStreamReader(raw, Charsets.UTF_8))
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "format", "version", "exportedAtMillis" -> reader.skipValue()
                            "captures" -> {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    val capture: CaptureEntity = gson.fromJson(reader, CaptureEntity::class.java)
                                    captures.add(capture)
                                    captureIds.add(capture.id.orEmpty())
                                    // Pre-index session shots in capture order
                                    // (the same getOrPut order the old restore
                                    // used) and pre-compute each capture's
                                    // image destinations so the media pass
                                    // knows where every URI goes.
                                    deserializeStringList(capture.sessionScreenshotsJson).forEach { path ->
                                        if (path !in shotIndexByPath) shotIndexByPath[path] = shotIndexByPath.size
                                    }
                                    runCatching {
                                        val data = CaptureConverters.deserializeCaptureData(capture.formatDataJson.orEmpty())
                                        val uris = data.imageUrisAll()
                                        if (uris.isNotEmpty()) {
                                            val idxByUri = mutableMapOf<String, Int>()
                                            uris.forEach { uri -> if (uri !in idxByUri) idxByUri[uri] = idxByUri.size }
                                            idxByUri.forEach { (uri, idx) ->
                                                imageDests.getOrPut(uri) { linkedSetOf() }.add(capture.id.orEmpty() to idx)
                                            }
                                        }
                                    }
                                }
                                reader.endArray()
                            }
                            "preferences" -> payloadPreferences = gson.fromJson(reader, PREFERENCES_TYPE)
                            "audioFiles" -> {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val id = reader.nextName()
                                    val bytes = Base64.getDecoder().decode(reader.nextString())
                                    // Only captures present in the payload get
                                    // their recording (orphan keys are skipped,
                                    // exactly like the old per-capture lookup).
                                    if (id in captureIds) {
                                        runCatching { AudioStorageManager.restoreAudio(context, id, bytes) }
                                            .getOrNull()?.let { audioPaths[id] = it }
                                    }
                                }
                                reader.endObject()
                            }
                            "imageFiles" -> {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val uri = reader.nextName()
                                    val bytes = Base64.getDecoder().decode(reader.nextString())
                                    imageDests[uri]?.forEach { (captureId, idx) ->
                                        runCatching { ImageStorageManager.restoreImage(context, captureId, idx, bytes) }
                                            .getOrNull()?.let { newPath ->
                                                imagePathsByCapture.getOrPut(captureId) { mutableMapOf() }[uri] = newPath
                                            }
                                    }
                                }
                                reader.endObject()
                            }
                            "sessionShots" -> {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val path = reader.nextName()
                                    val bytes = Base64.getDecoder().decode(reader.nextString())
                                    val idx = shotIndexByPath.getOrPut(path) { shotIndexByPath.size }
                                    runCatching { SessionShots.restore(context, "shot-$idx", bytes) }
                                        .getOrNull()?.let { shotPaths[path] = it }
                                }
                                reader.endObject()
                            }
                            "pendingWrite" -> {
                                pendingWrite = if (reader.peek() == JsonToken.NULL) {
                                    reader.nextNull()
                                    null
                                } else {
                                    gson.fromJson(reader, PendingWriteBackup::class.java)
                                }
                            }
                            "speciesCatalogJson" -> {
                                speciesCatalogJson = if (reader.peek() == JsonToken.NULL) {
                                    reader.nextNull()
                                    null
                                } else {
                                    reader.nextString()
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
            } catch (e: JsonIOException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            } catch (e: JsonSyntaxException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            } catch (e: IllegalStateException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            } catch (e: IOException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            }
        }

        // Rebuild every capture's storage references from the recorded path
        // maps (media files were already written above, one at a time).
        val restoredCaptures = captures.map { capture ->
            // Normalize Gson-bypassed nulls exactly like the old pre-flight
            // (pre-v6 backups have no sessionScreenshotsJson, pre-tags ones
            // have no tagsJson — the NOT NULL columns must never see null).
            var updated = capture.copy(
                id = capture.id.orEmpty(),
                topicId = capture.topicId.orEmpty(),
                categoryId = capture.categoryId.orEmpty(),
                topicName = capture.topicName.orEmpty(),
                topicSubtype = capture.topicSubtype.orEmpty(),
                topicTeaser = capture.topicTeaser.orEmpty(),
                format = capture.format.orEmpty(),
                formatDataJson = capture.formatDataJson.orEmpty(),
                tagsJson = capture.tagsJson.orEmpty().ifBlank { "[]" },
                sessionScreenshotsJson = capture.sessionScreenshotsJson.orEmpty().ifBlank { "[]" }
            )
            // Backward compatibility for v3: backups created before the
            // explicit provenance column have no isLegacy field. Preserve
            // their already-imported rows once, while new FieldMind restores
            // set the flag directly and never infer it here.
            if (!updated.isLegacy && updated.topicSubtype == FieldMindLegacyImport.LEGACY_SUBTYPE) {
                updated = updated.copy(isLegacy = true)
            }
            // Audio (v2): never preserve an audio path from the source device
            // (a backup is untrusted). Clear every stored path first; only a
            // bundled recording can establish a new app-private destination.
            val safeData = CaptureConverters.deserializeCaptureData(updated.formatDataJson)
                .withoutAudioPaths()
            updated = updated.copy(formatDataJson = Gson().toJson(safeData))
            audioPaths[capture.id]?.let { newPath ->
                runCatching {
                    CaptureConverters.deserializeCaptureData(updated.formatDataJson)
                        .withAudioPath(newPath)
                }.getOrNull()?.let { updated = updated.copy(formatDataJson = Gson().toJson(it)) }
            }
            // Images (v3): point every image URI (flat lists + mood-board
            // tile layouts) at its restored file. Same URI twice in one
            // entry reuses one stored file.
            val imageMap = imagePathsByCapture[capture.id]
            if (imageMap != null && imageMap.isNotEmpty()) {
                runCatching {
                    val data = CaptureConverters.deserializeCaptureData(updated.formatDataJson)
                    if (data.imageUrisAll().isNotEmpty()) {
                        val remapped = data.withImageUris { uri -> imageMap[uri] ?: uri }
                        updated = updated.copy(formatDataJson = Gson().toJson(remapped))
                    }
                }
            }
            // Session screenshots (v5): point every path at its restored
            // file. The shared index maps one original path to one restored
            // file across EVERY entry, preserving the shared-shots
            // relationship.
            runCatching {
                val paths = deserializeStringList(updated.sessionScreenshotsJson)
                if (paths.isNotEmpty()) {
                    val remapped = paths.map { path -> shotPaths[path] ?: path }
                    updated = updated.copy(sessionScreenshotsJson = Gson().toJson(remapped))
                }
            }
            updated
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

        speciesCatalogJson?.let { speciesJson ->
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
        // v6 — restore the pending (unsaved) write handoff: the shared note
        // and screenshots survive a mid-write backup. Screenshot paths are
        // remapped through the SAME shared index as saved entries, so one
        // original path still maps to one restored file even when an entry
        // and the pending write share a shot.
        if (pendingWrite != null) {
            val cat = CategoryId.values().firstOrNull { it.name == pendingWrite.categoryId }
            if (cat != null && pendingWrite.topicName.isNotBlank()) {
                val restoredPaths = pendingWrite.screenshotPaths.mapNotNull { path -> shotPaths[path] }
                ExploreSessionStore.handoffWriteSession(
                    context,
                    cat,
                    pendingWrite.topicName,
                    pendingWrite.elapsedMillis.coerceAtLeast(0L),
                    pendingWrite.note,
                    restoredPaths
                )
            }
        } else {
            // Pre-v6 backups never bundled the pending write's screenshots —
            // the prefs-restored package would point at dead paths. Drop it
            // so the write page never shows dangling attachments.
            ExploreSessionStore.clearPendingWrite(context)
        }
        return RestoreResult(captures.size, payloadPreferences.size)
    }

    /**
     * v47 — streaming validation pass for [restore]: reads the backup with a
     * JsonReader (no full-file String, no JSONObject tree) and enforces the
     * same contract the old JSONObject/Gson validation did — envelope
     * format/version, a captures array with unique, safe, well-formed
     * records, and preferences restricted to the known files. Throws before
     * any destructive step; returns the capture count.
     */
    private fun validateBackupStream(gson: Gson, context: Context, uri: Uri): Int {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open the backup file")
        input.use { raw ->
            val reader = JsonReader(InputStreamReader(raw, Charsets.UTF_8))
            reader.beginObject()
            var format: String? = null
            var version = -1
            var capturesSeen = false
            var prefsSeen = false
            var captureCount = 0
            val seenIds = mutableSetOf<String>()
            val prefFiles = mutableSetOf<String>()
            try {
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "format" -> format = reader.nextString()
                        "version" -> version = reader.nextInt()
                        "exportedAtMillis" -> reader.skipValue()
                        "captures" -> {
                            capturesSeen = true
                            reader.beginArray()
                            while (reader.hasNext()) {
                                captureCount++
                                val cap: CaptureEntity = gson.fromJson(reader, CaptureEntity::class.java)
                                val id = cap.id.orEmpty()
                                val formatStr = cap.format.orEmpty()
                                val formatDataJson = cap.formatDataJson.orEmpty()
                                require(id.isNotBlank()) { "Backup capture $captureCount has no id" }
                                require(seenIds.add(id)) { "Backup contains duplicate capture id" }
                                require(formatStr.isNotBlank()) { "Backup capture $captureCount has no format" }
                                require(formatDataJson.isNotBlank()) { "Backup capture $captureCount has no capture data" }
                                require(isSafeStorageSegment(id)) { "Backup capture $captureCount has an unsafe id" }
                                require(runCatching { CaptureFormat.valueOf(formatStr) }.isSuccess) {
                                    "Backup capture $captureCount has an unknown format"
                                }
                                runCatching { CaptureConverters.deserializeCaptureData(formatDataJson) }
                                    .getOrElse {
                                        throw IllegalArgumentException("Backup capture $captureCount has invalid capture data")
                                    }
                            }
                            reader.endArray()
                        }
                        "preferences" -> {
                            prefsSeen = true
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val name = reader.nextName()
                                prefFiles.add(name)
                                reader.skipValue()
                            }
                            reader.endObject()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            } catch (e: JsonIOException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            } catch (e: JsonSyntaxException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            } catch (e: IllegalStateException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            } catch (e: IOException) {
                throw IllegalArgumentException("That file is not a readable Curio backup.")
            }
            require(capturesSeen) { "This backup is missing its captures data" }
            require(prefsSeen) { "This backup is missing its settings data" }
            require(format == FORMAT_NAME) { "That file isn't a Curio backup" }
            require(version >= 0) { "This backup has no valid format version" }
            require(version <= FORMAT_VERSION) { "This backup was made by a newer version of Curio" }
            require(prefFiles.all { it in USER_PREF_FILES }) {
                "This backup contains an unknown preferences file"
            }
            return captureCount
        }
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
/**
 * v6 — an in-flight write handoff: the shared note + screenshots a session
 * finished with but the user hasn't saved as an entry yet. Included so a
 * backup taken mid-write (finish -> save) doesn't lose it.
 */
data class PendingWriteBackup(
    val categoryId: String,
    val topicName: String,
    val elapsedMillis: Long,
    val note: String,
    val screenshotPaths: List<String> = emptyList()
)

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
    /**
     * Session-screenshot bytes keyed by their original app-private file
     * path (v5). Shared across every entry saved from one session, so each
     * unique path is stored once.
     */
    val sessionShots: Map<String, ByteArray> = emptyMap(),
    /** v6 — the pending (unsaved) write handoff (note + screenshots). */
    val pendingWrite: PendingWriteBackup? = null,
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
