package com.curio.app.features.capture

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.NotePaperColor
import com.curio.app.data.CaptureConverters
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureDraftStore
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CaptureRepository
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.JournalMood
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.CurioTopic
import com.curio.app.data.StreakTracker
import com.curio.app.data.TopicCatalog
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.formatSessionShort
import com.curio.app.data.shortName
import android.util.Log
import com.curio.app.features.capture.formats.FieldNotesFormat
import com.curio.app.features.capture.formats.GalleryWallFormat
import com.curio.app.features.capture.formats.MarginaliaFormat
import com.curio.app.features.capture.formats.MoodChipsRow
import com.curio.app.features.capture.formats.OpenNotebookFormat
import com.curio.app.features.capture.formats.ReelNotesFormat
import com.curio.app.features.capture.formats.SoundBiteFormat
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.ConfettiBurst
import com.curio.app.ui.components.EmberBurst
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.formatGlyph
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.glyph
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.notePaperBorder
import com.curio.app.ui.theme.notePaperInk
import com.curio.app.ui.theme.notePaperRule
import com.curio.app.ui.theme.notePaperSurface
import com.curio.app.ui.theme.paperControlAccent
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Save / Capture — see Curio capture contract.
 *
 * Premium redesign with:
 *  - Structured data collection via [CaptureData] callbacks from each format
 *  - Room database persistence via [CaptureRepository]
 *  - Dual confetti + ember burst on save success
 *  - Format body renders instantly (no entrance delay)
 *  - Proper back-navigation with discard confirmation
 *
 * When [editEntryId] is set (edit mode — a single mood board or a whole
 * multi-section Portfolio), the screen preloads the saved entry's data into
 * the format body and saves changes back in place (same id → Room REPLACE),
 * then returns to the live-updating detail screen.
 */
@Composable
fun SaveCaptureScreen(
    categorySlug: String,
    topicName: String,
    navController: NavController,
    editEntryId: String? = null
) {
    val context = LocalContext.current

    // Edit mode: load the saved entry so its data can prefill the format.
    // Falls back to a sample entry (not in the DB) so preview boards can be
    // edited too — saving then persists a real copy of the board.
    val editingEntry by produceState<CurioEntry?>(initialValue = null, editEntryId) {
        value = editEntryId?.let { id ->
            runCatching { CurioRepositoryHolder.repo.getById(id) }.getOrNull()
                ?: TopicCatalog.sampleEntries().find { it.id == id }
        }
    }

    // Always call remember (stable slot) — the entry-driven category only
    // applies in edit mode.
    val fallbackCat = remember(categorySlug) {
        CurioCategories.byRouteSlug(categorySlug)
            ?: CurioCategories.byId(CategoryId.WILDCARD)
    }
    val cat = editingEntry?.let { CurioCategories.byId(it.topic.categoryId) } ?: fallbackCat

    val topic by produceState<CurioTopic?>(initialValue = null, topicName, cat.id, editingEntry) {
        val existing = editingEntry
        if (existing != null) {
            value = existing.topic
            return@produceState
        }
        val cached = TopicCatalog.findByName(topicName)
        if (cached != null) {
            value = cached
            return@produceState
        }
        val pool = TopicJsonLoader.load(cat.id)
        // Graceful fallback: an unknown topic stays null so the save CTA
        // stays disabled instead of silently capturing the wrong topic.
        value = pool.firstOrNull { it.name == topicName }
    }

    var canSave by remember { mutableStateOf(false) }
    // True when ANY take holds drafted content — text, quotes, a rating,
    // images, an audio note, tiles, or a live recording — even if that
    // content alone wouldn't satisfy the format's canSave rule (e.g. only
    // optional fields filled). This (not canSave) gates the leave dialog:
    // canSave's blind spot let back/exit silently drop optional-only drafts.
    var hasAnyDraft by remember { mutableStateOf(false) }
    var currentCaptureData by remember { mutableStateOf<CaptureData?>(null) }
    var saveInProgress by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var emberTrigger by remember { mutableIntStateOf(0) }
    var savedEntryId by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // ── Custom tags (v7.17) — free-form labels added on the save page and
    // persisted on the entry (Room tagsJson column). Shown as chips on the
    // detail page and matched by Cabinet search. Edit mode prefills from the
    // saved entry so re-saving keeps existing tags.
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }
    LaunchedEffect(editingEntry) {
        if (editEntryId != null) tags = editingEntry?.tags.orEmpty()
    }

    // ── Session note (v27) — the explore session's SHARED note, attached
    // to this entry on save. On a fresh save it comes from the pending
    // write package (handed off when the session ended); in edit mode the
    // entry already carries it. (v60 — the session-SCREENSHOT attachment
    // was removed: no more auto-attached shots, no photo permission.)
    var sessionNote by remember { mutableStateOf("") }
    LaunchedEffect(editEntryId, editingEntry, topic?.name) {
        sessionNote = if (editEntryId != null) {
            editingEntry?.sessionNote.orEmpty()
        } else {
            ExploreSessionStore.peekWriteSessionNote(cat.id, topic?.name.orEmpty())
        }
    }
    // The shared-note button expands into a small editor card.
    var showNoteEditor by remember { mutableStateOf(false) }
    // The floating note button shows when a pending write handoff exists
    // for THIS exact topic (fresh save) or the entry already carries a note
    // (edit mode), plus whenever a note has been typed. topic is a delegated
    // property, so the name is read via safe-call rather than smart-cast.
    val hasSessionAttachments = editEntryId != null ||
        (topic?.let { ExploreSessionStore.hasPendingWriteFor(cat.id, it.name) } == true) ||
        sessionNote.isNotBlank()

    // ── Draft autosave (v7.17) ──────────────────────────────────────────
    // While on this page, the current capture data is debounce-snapshotted
    // to [CaptureDraftStore] so a stray back / app kill / rotation never
    // silently loses drafted content. Reopening the SAME topic's save page
    // offers to resume the stored draft (see the resume dialog below); a
    // successful save clears it.
    var resumedDraftData by remember { mutableStateOf<CaptureData?>(null) }
    var pendingDraft by remember { mutableStateOf<CaptureData?>(null) }
    var showResumeDraftDialog by remember { mutableStateOf(false) }
    // Best-effort snapshot emitted by the format body — non-null for partial
    // drafts too (vs [currentCaptureData], which is only set when every
    // section is ready). Drives the debounced autosave.
    var draftData by remember { mutableStateOf<CaptureData?>(null) }

    // Load the stored draft ONCE per screen open (keyed on category+topic;
    // edit mode skips — the saved entry is the source of truth there). The
    // dialog only offers to RESUME; dismissing it keeps the stored draft for
    // the next visit without seeding this one.
    LaunchedEffect(categorySlug, topicName) {
        if (editEntryId != null) return@LaunchedEffect
        val draft = CaptureDraftStore.get(context, categorySlug, topicName)
            ?: return@LaunchedEffect
        val data = runCatching {
            CaptureConverters.deserializeCaptureData(draft.dataJson)
        }.getOrNull() ?: return@LaunchedEffect
        pendingDraft = data
        showResumeDraftDialog = true
    }

    // Debounced autosave — 700ms of quiet after the last change snapshots
    // the take to the draft store. Skipped while nothing is drafted.
    // saveInProgress keys + guards the write: when a save lands, the draft is
    // cleared, and a debounced write that was already in flight must NOT
    // re-save it (that would resurrect a "Resume draft?" prompt for a topic
    // that was just saved).
    LaunchedEffect(draftData, hasAnyDraft, topic?.name, categorySlug, saveInProgress) {
        if (editEntryId != null) return@LaunchedEffect
        if (saveInProgress) return@LaunchedEffect
        if (!hasAnyDraft) return@LaunchedEffect
        val data = draftData ?: return@LaunchedEffect
        val resolvedTopic = topic ?: return@LaunchedEffect
        delay(700)
        // Re-check after the debounce window — the user may have saved.
        if (saveInProgress) return@LaunchedEffect
        val stillCurrent = draftData ?: return@LaunchedEffect
        CaptureDraftStore.save(
            context, categorySlug, resolvedTopic.name, Gson().toJson(stillCurrent)
        )
    }

    // v7.17 — back ALWAYS asks before leaving this capture page (both the
    // system back and the top-bar back button): leaving drops you out of
    // the capture flow, so a stray back should never silently discard
    // drafted content. The dialog is context-aware — the full save dialog
    // (save / keep editing / discard) when anything is drafted, a simple
    // "Leave this capture?" confirm when the page is empty. While a save is
    // in flight, back is ignored entirely (the save finishes and navigates
    // on its own).
    BackHandler(enabled = !saveInProgress) {
        showDiscardDialog = true
    }

    val scope = rememberCoroutineScope()

    // ── Handle save ─────────────────────────────────────────────────────
    val performSave: () -> Unit = {
        val data = currentCaptureData
        if (data != null) {
            saveInProgress = true
            saveError = null
            scope.launch {
                try {
                    val entryId = editEntryId ?: CaptureRepository.createId()

                    // Persist audio file from cache to internal storage before
                    // saving — recurses through Portfolio/OpenNotebook so every
                    // SoundBite section gets a stable path, not just top-level.
                    val persistedData = persistAudioDeep(context, data, entryId)
                    val resolvedTopic = topic ?: run {
                        saveError = "The topic is still loading. Please try again."
                        return@launch
                    }

                    // Local capture: editingEntry is a delegated property (produceState),
                    // so the compiler can't smart-cast it — grab a stable local first.
                    val existingEntry = editingEntry
                    // Edit mode must NEVER write a fresh entry: Room REPLACEs by id,
                    // so a fresh entry here would overwrite the original with blank
                    // data. If the source entry is somehow missing, abort instead.
                    if (editEntryId != null && existingEntry == null) {
                        saveError = "This entry is no longer available. Please go back and try again."
                        return@launch
                    }
                    // v17 — how long this topic's explore session ran
                    // (pause-aware). The "write about it" flows hand the
                    // elapsed time over BEFORE clearing the session, so the
                    // handoff is the primary source; a still-running session
                    // (e.g. opened via Recents) is read live as a fallback.
                    // Only attributed when the session matches this topic,
                    // and only on a fresh save (edit re-saves keep their
                    // original session time).
                    val sessionMillis = if (existingEntry == null) {
                        // Peek (don't consume): the handoff is only cleared
                        // once the save succeeds, so a retry after a failed
                        // save keeps the session label.
                        ExploreSessionStore.peekWriteSessionMillis(
                            resolvedTopic.categoryId, resolvedTopic.name
                        ).takeIf { it > 0L } ?: ExploreSessionStore.activeSessionState
                            ?.takeIf {
                                it.categoryId == resolvedTopic.categoryId &&
                                    it.topicName == resolvedTopic.name
                            }
                            ?.elapsedMillis()
                            ?.coerceAtLeast(0L) ?: 0L
                    } else 0L
                    // v27 — the session's SHARED note + screenshots ride onto
                    // this entry. The note editor and screenshots section keep
                    // them in local state; push the note back to the pending
                    // package so the entry + any later save agree.
                    if (existingEntry == null && sessionNote.isNotBlank()) {
                        ExploreSessionStore.setPendingNote(
                            context, resolvedTopic.categoryId, resolvedTopic.name, sessionNote
                        )
                    }
                    val entry = if (existingEntry != null) {
                        // Edit mode: keep id/topic/title/timestamp, swap the data.
                        existingEntry.copy(
                            format = formatOf(persistedData),
                            captureData = persistedData,
                            tags = tags,
                            sessionNote = sessionNote.takeIf { it.isNotBlank() }
                                ?: existingEntry.sessionNote
                        )
                    } else {
                        CurioEntry(
                            id = entryId,
                            topic = resolvedTopic,
                            // A single section stores its own format; a Portfolio
                            // entry uses its first section's format so Cabinet glyph
                            // and detail dispatch stay correct.
                            format = formatOf(persistedData),
                            captureData = persistedData,
                            tags = tags,
                            sessionTimeMillis = sessionMillis,
                            sessionNote = sessionNote.takeIf { it.isNotBlank() }
                        )
                    }
                    runCatching { CurioRepositoryHolder.repo.save(entry) }
                        .onSuccess {
                            savedEntryId = entry.id
                            saveError = null
                            // v17 — the handed-off session duration is now
                            // banked on the entry; drop the pending handoff so
                            // a later save of the same topic can't inherit a
                            // stale duration.
                            ExploreSessionStore.clearWriteSessionHandoff(
                                context, resolvedTopic.categoryId, resolvedTopic.name
                            )
                            StreakTracker.recordActivity(context)
                            // Feed the quests system — NEW saves drive journey +
                            // daily + badges (the format feeds Every Format).
                            // Edit re-saves never re-count: they update an
                            // existing keepsake, not a new discovery.
                            if (editEntryId == null) {
                                CurioQuests.onSave(context, entry.format)
                                // Feed the category passport — a saved capture
                                // masters the lane's stamp (spec §6.1).
                                CurioPassport.noteSave(context, entry.topic.categoryId)
                                // v16 — the pet remembers this keepsake + lane
                                // for its memory lines ("you saved 3 songs this
                                // week").
                                CurioPet.noteSavedLane(
                                    context,
                                    CurioCategories.all
                                        .firstOrNull { it.id == entry.topic.categoryId }
                                        ?.displayName ?: entry.topic.categoryId.name.lowercase(),
                                    resolvedTopic.name
                                )
                                // The pet celebrates a new keepsake (spec §10.6).
                                CurioPet.reactTo(CurioPet.Event.SAVE)
                            }
                            // Saved — the autosaved draft is now redundant. Null the
                            // snapshot too, so a debounced write that re-fires when
                            // saveInProgress flips back can't resurrect it.
                            draftData = null
                            CaptureDraftStore.clear(context, categorySlug, resolvedTopic.name)
                            delay(400)
                            confettiTrigger++
                            emberTrigger++
                        }
                        .onFailure { error ->
                            Log.e("SaveCaptureScreen", "Failed to save capture ${entry.id}", error)
                            saveError = "Couldn't save this entry. Your recording is still here. Try again."
                        }
                } catch (error: Exception) {
                    Log.e("SaveCaptureScreen", "Failed to prepare capture", error)
                    saveError = "Couldn't save this entry. Your recording is still here. Try again."
                } finally {
                    saveInProgress = false
                }
            }
        }
    }

    // ── Navigate after save celebration ─────────────────────────────────
    LaunchedEffect(confettiTrigger) {
        if (confettiTrigger > 0) {
            delay(800)
            savedEntryId?.let { id ->
                if (editEntryId != null) {
                    // Edit mode: return to the detail screen — it observes the
                    // repository flow, so the updated board renders live.
                    navController.popBackStack()
                } else {
                    navController.navigate(CurioRoutes.entryDetail(id)) {
                        popUpTo(CurioRoutes.HOME)
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Category tint wash — the capture screen wears a faint wash of
            // the active category over the theme background, matching the
            // Spin page so saving stays in the same color story. Theme-aware:
            // deep accent over cream in light, pastel twin glow over midnight
            // in dark (deep accents look muddy on dark).
            .background(cat.categoryBackgroundWash())
    ) {
        // ── Premium top bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CurioBackButton(
                onClick = {
                    // Always confirm before leaving (see BackHandler above) —
                    // ignored only while a save is in flight.
                    if (!saveInProgress) showDiscardDialog = true
                }
            )
            Text(
                text = if (editEntryId != null) "Edit entry" else "Save your take",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(40.dp)) // balance the back button
        }

        // ── Topic reminder strip with gradient ───────────────────────────
        // Wears the category tint with the tint setting on; with it off it
        // falls back to a plain theme surface so the whole flow goes neutral.
        // v23 — how long this topic was explored, shown ALONGSIDE the topic
        // in the strip: the saved entry's session in edit mode, or the
        // pending write-session handoff (live session as fallback) on a
        // fresh save — the same sources the save itself uses.
        // `topic` is a delegated property, so grab a stable local first (the
        // compiler can't smart-cast a delegated getter past a null check).
        val localTopic = topic
        val displaySessionMillis = when {
            editEntryId != null -> editingEntry?.sessionTimeMillis ?: 0L
            localTopic != null -> ExploreSessionStore.peekWriteSessionMillis(cat.id, localTopic.name)
                .takeIf { it > 0L }
                ?: ExploreSessionStore.activeSessionState
                    ?.takeIf { it.categoryId == cat.id && it.topicName == localTopic.name }
                    ?.elapsedMillis()
                    ?.coerceAtLeast(0L)
                ?: 0L
            else -> 0L
        }
        val tintWash = AppPreferences.tintWashEffective()
        // v29 — the strip wears the SAME category-tinted card surface as the
        // rest of the app ([categorySurface] is fully OPAQUE — the old lerp
        // of the accent at 20% over surfaceContainerHigh was also opaque but
        // its tint didn't match the cards in dark mode, where it read as a
        // muddy near-grey instead of the category's mid-tone). The ink rides
        // the category's readable ink ([categoryInk]: deep accent in light,
        // light twin in dark, deep twin in pastel) so the topic text stays
        // readable on the strip in every theme.
        val stripColor = if (tintWash) {
            cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val stripInk = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurface

        // ── Universal multi-take editor state (v58) ──────────────────────
        // Hoisted out of the format body so the take tabs + format chips pin
        // in a compact row UNDER the topic strip (floating with the topic)
        // and the mood pill can live INSIDE the strip — universal across
        // every take. Section state (format + live data per take) is held
        // here so switching takes never loses in-progress content; the old
        // body-level rules apply unchanged.
        val defaultFormat = if (cat.defaultFormat == CaptureFormat.OpenNotebook)
            CaptureFormat.SoundBite else cat.defaultFormat
        val sectionEntryFormat = editingEntry?.format
        val sectionInitData = editingEntry?.captureData ?: resumedDraftData
        val sections = remember(sectionEntryFormat, sectionInitData) {
            mutableStateListOf<CaptureSectionState>().apply {
                when {
                    sectionInitData is CaptureData.Portfolio && sectionInitData.sections.isNotEmpty() ->
                        sectionInitData.sections.forEachIndexed { i, s ->
                            add(CaptureSectionState(i, s.format).apply {
                                seed = s.data
                                data = s.data
                                mood = s.data.moodOf()
                                canSave = true
                            })
                        }
                    sectionInitData is CaptureData.OpenNotebook ->
                        add(CaptureSectionState(0, sectionInitData.subFormat).apply {
                            seed = sectionInitData.subData
                            data = sectionInitData.subData
                            mood = sectionInitData.subData.moodOf()
                            canSave = true
                        })
                    sectionInitData != null ->
                        // v113 — the resumed section wears the DRAFT's OWN
                        // format, not the page default: the old code seeded
                        // the draft's data into a `defaultFormat` section, so
                        // resuming a draft written on a non-default take
                        // (e.g. a Journal draft on a SoundBite category)
                        // opened the default take's body with the wrong data
                        // and the draft never "restored". Edit mode keeps
                        // [sectionEntryFormat] (the saved entry's format);
                        // new-capture resumes fall back to `formatOf(data)`.
                        add(CaptureSectionState(0, sectionEntryFormat ?: formatOf(sectionInitData)).apply {
                            seed = sectionInitData
                            data = sectionInitData
                            mood = sectionInitData.moodOf()
                            canSave = true
                        })
                    else -> add(CaptureSectionState(0, defaultFormat))
                }
            }
        }
        // Mood-board edits reopen on their board section; everything else
        // starts on the first take.
        var activeIndex by remember(sectionEntryFormat, sectionInitData) {
            mutableIntStateOf(
                (sectionInitData as? CaptureData.Portfolio)
                    ?.sections?.indexOfFirst { it.format == CaptureFormat.GalleryWall }
                    ?.coerceAtLeast(0) ?: 0
            )
        }
        var nextId by remember(sectionEntryFormat, sectionInitData) {
            mutableIntStateOf(sections.maxOfOrNull { it.id }?.plus(1) ?: 0)
        }

        // Snapshot the outgoing section's data so switching back restores it.
        fun snapshotActive() {
            sections.getOrNull(activeIndex)?.let { it.seed = it.data }
        }

        // Removes a take and re-anchors the active index — shared by the X
        // button's direct-remove path and the remove-confirmation dialog so
        // the two can never drift apart.
        fun removeSection(i: Int) {
            if (i < activeIndex) activeIndex--
            sections.removeAt(i)
            if (activeIndex >= sections.size) activeIndex = sections.size - 1
        }

        // Switching a FILLED section's format clears its content — confirm
        // first so a fat-finger on the format chips never silently wipes a
        // take.
        var pendingFormatSwitch by remember { mutableStateOf<CaptureFormat?>(null) }
        // Removing a take that holds drafted content (or a live recording)
        // also confirms first — in edit mode every take arrives prefilled,
        // so the X must never silently throw away drafted changes.
        var pendingRemoveIndex by remember { mutableStateOf<Int?>(null) }
        fun applyFormat(section: CaptureSectionState, fmt: CaptureFormat) {
            section.format = fmt
            section.canSave = false
            section.data = null
            section.seed = null
            section.busy = false
        }

        // ── Aggregate: all sections must be filled to save ──────────────
        val allReady = sections.isNotEmpty() && sections.all { it.canSave && it.data != null }
        val combinedData: CaptureData? = when {
            !allReady -> null
            sections.size == 1 -> sections[0].data
            else -> CaptureData.Portfolio(
                sections.map { CaptureData.CaptureSection(it.format, it.data!!) }
            )
        }
        // ANY take holding drafted content (or a live recording) — the leave
        // / switch / remove guards key on this.
        val anyTakeDraft = sections.any { it.data != null || it.busy }
        // BEST-EFFORT draft snapshot for autosave — non-null even before
        // every section is complete, so a partial multi-section draft still
        // autosaves the filled content instead of nothing.
        val sectionDraftData: CaptureData? = when {
            sections.isEmpty() -> null
            sections.size == 1 -> sections[0].data
            else -> {
                val filled = sections.mapNotNull { s ->
                    s.data?.let { CaptureData.CaptureSection(s.format, it) }
                }
                if (filled.isNotEmpty()) CaptureData.Portfolio(filled) else null
            }
        }
        LaunchedEffect(allReady, combinedData, anyTakeDraft, sections.toList(), topic) {
            canSave = allReady && topic != null && (editEntryId == null || editingEntry != null)
            hasAnyDraft = anyTakeDraft
            currentCaptureData = combinedData
            draftData = sectionDraftData
        }
        // The strip's mood pill toggles the shared mood selector pinned
        // under the strip (see the capture header below).
        var moodSelectorOpen by remember { mutableStateOf(false) }
        Surface(
            color = stripColor,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 3.dp,
            // v29 — dark mode elevation visibility (glow).
            modifier = Modifier
                .curioDarkGlow(3.dp, RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    // v27u — opaque icon plate (was the accent at 15% alpha —
                    // no transparency anywhere on the strip).
                    color = if (tintWash) {
                        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, cat.themedAccent(), 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    CurioIcon(
                        name = cat.iconGlyph,
                        contentDescription = null,
                        tint = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 22.dp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    // v27 — the session duration sits ALONGSIDE the topic in
                    // the strip (long topics ellipsize so the pill never wraps).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = topic?.name ?: "Loading…",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = stripInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (displaySessionMillis > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Timer,
                                    contentDescription = null,
                                    tint = stripInk.copy(alpha = 0.7f),
                                    size = 13.dp
                                )
                                Text(
                                    text = formatSessionShort(displaySessionMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = stripInk.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    Text(
                        text = cat.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = stripInk.copy(alpha = 0.7f)
                    )
                }

                // ── Mood pill (v58) — the active take's mood lives in the
                // topic strip now instead of the scrolling body: shows the
                // take's mood (or a "Mood +" affordance) and toggles the
                // shared selector pinned under the strip.
                val activeMood = sections.getOrNull(activeIndex)?.mood
                Surface(
                    onClick = { moodSelectorOpen = !moodSelectorOpen },
                    shape = RoundedCornerShape(50),
                    color = if (tintWash) lerp(stripColor, cat.themedAccent(), 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.curioDarkGlow(2.dp, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        CurioIcon(
                            name = activeMood?.glyph ?: CurioIcons.MoodHappy,
                            contentDescription = "Current mood",
                            tint = if (tintWash) cat.categoryInk() else stripInk,
                            size = 16.dp
                        )
                        Text(
                            text = activeMood?.label ?: "Mood",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (tintWash) cat.categoryInk() else stripInk
                        )
                        if (activeMood == null) {
                            CurioIcon(
                                name = CurioIcons.Add,
                                contentDescription = "Add mood",
                                tint = if (tintWash) cat.categoryInk() else stripInk,
                                size = 14.dp
                            )
                        }
                    }
                }
            }
        }

        // ── Pinned capture header (v58) — the format chips + take tabs
        // float ATTACHED to the topic strip (fixed under it, never scrolling
        // away with the body): one compact horizontally-scrollable row so it
        // never eats the screen. The strip's mood pill expands the shared
        // mood selector here.
        val activeSection = sections.getOrNull(activeIndex)
        // Edit mode: hold the header until the saved entry loads (the section
        // state re-initializes from it) so it never flashes empty chips.
        val editingLoaded = editEntryId == null || editingEntry != null
        if (activeSection != null && editingLoaded) {
            if (moodSelectorOpen) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    MoodChipsRow(
                        mood = activeSection.mood,
                        accent = cat.themedAccent(),
                        onMoodChange = { m ->
                            activeSection.mood = m
                            // Stamp into the take's live data so the saved
                            // entry + meta card see it even before the
                            // editor re-emits.
                            activeSection.data = activeSection.data?.withMood(m)
                            // v69 — picking a mood collapses the selector
                            // right away (one tap to set, no extra tap to
                            // close) — the strip pill shows the result.
                            moodSelectorOpen = false
                        },
                        header = null
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Compact format chips — control the ACTIVE section ────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val onSwitch = { target: CaptureFormat ->
                        // Confirm when this take holds ANY content — text,
                        // quotes, a rating, images, a voice note, tiles or a
                        // live recording. An empty take switches freely.
                        if (activeSection.data != null || activeSection.busy) {
                            pendingFormatSwitch = target
                        } else {
                            applyFormat(activeSection, target)
                        }
                    }
                    CAPTURE_FORMATS.forEach { fmt ->
                        FormatChip(fmt = fmt, active = activeSection, category = cat, onSwitch = onSwitch)
                    }
                }
                // ── Take tabs + add another take ─────────────────────────
                CaptureTakeTabs(
                    category = cat,
                    sections = sections,
                    activeIndex = activeIndex,
                    onSelect = { i -> snapshotActive(); activeIndex = i },
                    onRequestRemove = { i ->
                        val section = sections.getOrNull(i)
                        if (section != null && (section.data != null || section.busy)) {
                            pendingRemoveIndex = i
                        } else {
                            removeSection(i)
                        }
                    },
                    onAddTake = {
                        snapshotActive()
                        sections.add(CaptureSectionState(nextId++, defaultFormat))
                        activeIndex = sections.lastIndex
                    }
                )
            }
        }

        // ── Scrollable format body ───────────────────────────────────────
        // v27k — wrapped in a Box so the shared session-note pill can float
        // over the scrolling content (pinned above the save CTA, reachable
        // no matter how far the body is scrolled).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // v7.98 — the format body fills the page: 16dp side margins
            // (the app-standard edge) instead of the old 24dp, so the paper
            // fields and tool docks breathe edge-to-edge.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    if (editEntryId != null && editingEntry == null) {
                        // Edit mode: hold until the saved entry loads. Rendering
                        // the format body now would fall back to the Wildcard
                        // category's body and could emit blank data.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = cat.themedAccent())
                        }
                    } else {
                        FormatBodyForCategory(
                            category = cat,
                            sections = sections,
                            activeIndex = activeIndex,
                            // Reuse the saved entry's id-derived seed so the editor's
                            // watermark pattern matches the saved view exactly.
                            boardSeed = editEntryId?.hashCode()
                        )
                    }

                    // ── Custom tags (v7.17) — free-form labels to find this
                    // capture later. Rendered under the format body so the
                    // tags ride along with the take content in the scroll.
                    TagEditorRow(
                        tags = tags,
                        tagInput = tagInput,
                        onTagInputChange = { tagInput = it },
                        onAddTag = { raw ->
                            val clean = raw.trim().trimStart('#').trim()
                            if (clean.isNotBlank() && clean.length <= 24 && tags.size < 12) {
                                tags = (tags + clean).distinct()
                            }
                            tagInput = ""
                        },
                        onRemoveTag = { tag -> tags = tags.filterNot { it == tag } },
                        accent = cat.themedAccent(),
                        tint = cat.tint,
                        ink = cat.categoryInk(),
                        onAccentContent = cat.onAccent()
                    )

            }
            }

            // ── Floating session-note pill (v27k) — pinned inside the
            // scroll area (a Box sibling of the body) so the shared note
            // stays reachable no matter how far the format body is scrolled.
            // Shows the note text once typed; tapping opens the compact paper
            // editor popup right above, which rides the IME while typing.
            if (hasSessionAttachments) {
                // align() is a BoxScope modifier, so the pill itself stays
                // alignment-free and the Box wrapper pins it bottom-end.
                Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                    SessionNoteFloatingPill(
                        cat = cat,
                        note = sessionNote,
                        expanded = showNoteEditor,
                        onToggle = { showNoteEditor = !showNoteEditor },
                        onNoteChange = {
                            sessionNote = it
                            if (editEntryId == null) {
                                ExploreSessionStore.setPendingNote(
                                    context, cat.id, topic?.name.orEmpty(), it
                                )
                            }
                        }
                    )
                }
            }
        }

        // ── Sticky bottom Save CTA with gradient edge ────────────────────
        // Wears the same category wash as the page (and the Spin bottom bar)
        // so the CTA tray blends into the tinted screen instead of sitting on
        // a plain patch of theme background.
        Surface(
            color = cat.categoryBackgroundWash(),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box {
                // Top gradient edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = if (tintWash) listOf(
                                    cat.tint,
                                    cat.themedAccent().copy(alpha = 0.3f),
                                    cat.tint
                                ) else listOf(
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        )
                )
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    saveError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    // The save button wears the category TINT with ink content
                    // when the tint setting is on; with it off it reverts to
                    // the plain accent fill + white content as before.
                    // v8.18 — the Save CTA is a FUN pet landmark: the pet
                    // sometimes dashes over and boops it while you write.
                    PetLandmark(
                        id = "save",
                        kind = PetLandmarks.Kind.FUN,
                        screen = "capture"
                    ) { m ->
                        Button(
                            onClick = performSave,
                            enabled = canSave && !saveInProgress,
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tintWash) cat.tint else cat.themedAccent(),
                                contentColor = if (tintWash) cat.categoryInk() else cat.onAccent(),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(vertical = 18.dp),
                            modifier = m
                                .fillMaxWidth()
                                .scale(if (saveInProgress) 0.97f else 1f)
                        ) {
                            if (saveInProgress) {
                                CircularProgressIndicator(
                                    color = if (tintWash) cat.categoryInk() else cat.onAccent(),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Saving…",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            } else {
                                CurioIcon(
                                    name = CurioIcons.Check,
                                    contentDescription = null,
                                    tint = if (tintWash) cat.categoryInk() else cat.onAccent(),
                                    size = 20.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (editEntryId != null) "Save changes" else "Save entry",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
        // ── Confirm before removing a take with drafted content ─────────
        pendingRemoveIndex?.let { removeIdx ->
            AlertDialog(
                containerColor = curioDialogContainerColor(),
                shape = CurioDialogShape,
                onDismissRequest = { pendingRemoveIndex = null },
                title = { Text("Remove this take?") },
                text = { Text("This will delete the content you've drafted in this take (including any live recording).") },
                confirmButton = {
                    TextButton(onClick = {
                        removeSection(removeIdx)
                        pendingRemoveIndex = null
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingRemoveIndex = null },
                        colors = curioDialogActionButtonColors()
                    ) {
                        Text("Keep editing")
                    }
                }
            )
        }

        // ── Confirm before switching a filled take's format ──────────────
        // Offers THREE paths: keep the current content as its own take and
        // switch this one (Save and switch), clear this take and switch
        // (Switch), or stay put (Keep editing).
        pendingFormatSwitch?.let { fmt ->
            AlertDialog(
                containerColor = curioDialogContainerColor(),
                shape = CurioDialogShape,
                onDismissRequest = { pendingFormatSwitch = null },
                title = { Text("Switch format?") },
                text = { Text("Switch to ${fmt.shortName}? You can keep what you've added here as its own take first, or switch and clear it.") },
                dismissButton = {
                    TextButton(
                        onClick = { pendingFormatSwitch = null },
                        colors = curioDialogActionButtonColors()
                    ) {
                        Text("Keep editing")
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            val section = sections.getOrNull(activeIndex)
                            if (section != null) applyFormat(section, fmt)
                            pendingFormatSwitch = null
                        }) {
                            Text("Switch and clear", color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                val section = sections.getOrNull(activeIndex)
                                if (section != null) {
                                    // Snapshot the drafted content into a NEW take
                                    // at this position, then switch this take's
                                    // format — nothing is lost, and the drafts live
                                    // on as their own tabs.
                                    val saved = CaptureSectionState(nextId++, section.format).apply {
                                        seed = section.data
                                        data = section.data
                                        mood = section.mood
                                        canSave = section.canSave
                                    }
                                    sections.add(activeIndex, saved)
                                    // activeIndex still points at the ORIGINAL take
                                    // (the new one was inserted BEFORE it) — switch
                                    // that one to the new format.
                                    applyFormat(section, fmt)
                                }
                                pendingFormatSwitch = null
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = curioDialogActionColor(),
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Save and switch")
                        }
                    }
                }
            )
        }
    }

    // ── Three-way leave dialog (save and switch / keep editing / discard) ─
    //    Shown when leaving with unsaved edits. Discard sits on the LEFT;
    //    the primary "Save and switch" action is the rightmost button.
    if (showDiscardDialog) {
        if (hasAnyDraft) {
            // Drafted content → the full three-way leave dialog: save and
            // switch / keep editing / discard (discard pops the page).
            AlertDialog(
                containerColor = curioDialogContainerColor(),
                shape = CurioDialogShape,
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Unsaved changes") },
                text = { Text("You have unsaved edits. Save them and switch away, or leave without saving.") },
                dismissButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        navController.popBackStack()
                    }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Keep editing")
                        }
                        Button(
                            onClick = {
                                showDiscardDialog = false
                                performSave()
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = curioDialogActionColor(),
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Save and switch")
                        }
                    }
                }
            )
        } else {
            // Nothing newly drafted → a light confirm so a stray back
            // doesn't silently drop the user out of the capture flow. The
            // message is context-aware: edit mode preloads the saved entry
            // (the page is full of content even with no NEW edits), while a
            // fresh capture is genuinely empty.
            AlertDialog(
                containerColor = curioDialogContainerColor(),
                shape = CurioDialogShape,
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(if (editEntryId != null) "Discard your edits?" else "Leave this capture?") },
                text = {
                    Text(
                        if (editEntryId != null)
                            "Your changes to this entry won't be saved. Leave without saving?"
                        else
                            "You haven't added anything yet. Leave the capture page?"
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            navController.popBackStack()
                        },
                        colors = curioDialogActionButtonColors()
                    ) {
                        Text("Leave")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showDiscardDialog = false },
                        colors = curioDialogActionButtonColors()
                    ) {
                        Text("Keep editing")
                    }
                }
            )
        }
    }

    // ── Resume-draft dialog (v7.17) ────────────────────────────────────
    // Shown when an autosaved draft exists for this category+topic. Resume
    // seeds the format body with the stored snapshot; Discard permanently
    // clears it; tapping outside keeps it stored for the next visit without
    // loading it now.
    if (showResumeDraftDialog) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = {
                pendingDraft = null
                showResumeDraftDialog = false
            },
            title = { Text("Resume draft?") },
            text = { Text("You have an autosaved draft for this topic. Pick up where you left off, or start fresh.") },
            dismissButton = {
                TextButton(onClick = {
                    CaptureDraftStore.clear(context, categorySlug, topicName)
                    pendingDraft = null
                    showResumeDraftDialog = false
                }) {
                    Text("Discard draft", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resumedDraftData = pendingDraft
                        pendingDraft = null
                        showResumeDraftDialog = false
                    },
                    colors = curioDialogActionButtonColors()
                ) {
                    Text("Resume")
                }
            }
        )
    }

    // ── Confetti + ember celebration ────────────────────────────────────
    if (confettiTrigger > 0) {
        ConfettiBurst(
            colors = listOf(cat.themedAccent(), if (AppPreferences.tintWashEffective()) cat.tint else cat.themedAccent(), CurioColors.ButterYellow),
            trigger = confettiTrigger,
            particleCount = CurioMotion.ConfettiParticleCountLarge,
            modifier = Modifier.fillMaxSize(),
            onComplete = {}
        )
    }
    if (emberTrigger > 0) {
        EmberBurst(
            colors = listOf(cat.themedAccent(), CurioColors.ButterYellow),
            trigger = emberTrigger,
            particleCount = 12,
            modifier = Modifier.fillMaxSize(),
            onComplete = {}
        )
    }
}

/**
 * Custom-tags editor (v7.17) — a compact label row on the save page.
 * Entered tags render as removable #chips (FlowRow wrap); the input field
 * adds on the add-button or the IME Done action. Kept minimal: 12 tags max,
 * 24 chars each, deduped, trimmed of leading '#'s.
 */
@Composable
private fun TagEditorRow(
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    accent: Color,
    tint: Color,
    ink: Color,
    onAccentContent: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        // v27n — opaque tinted chip (was the accent tint at
                        // 14% alpha, which let the elevation shadow bleed
                        // through).
                        color = if (AppPreferences.tintWashEffective()) {
                            lerp(MaterialTheme.colorScheme.surface, tint.copy(alpha = 1f), 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (AppPreferences.tintWashEffective()) ink else accent
                            )
                            Surface(
                                onClick = { onRemoveTag(tag) },
                                shape = CircleShape,
                                color = Color.Transparent
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Close,
                                    contentDescription = "Remove tag $tag",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 14.dp,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { onTagInputChange(it.take(24)) },
                placeholder = { Text("Add a tag…") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddTag(tagInput) }),
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = { onAddTag(tagInput) },
                shape = RoundedCornerShape(50),
                color = if (AppPreferences.tintWashEffective()) tint else accent
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = "Add tag",
                    tint = if (AppPreferences.tintWashEffective()) ink else onAccentContent,
                    size = 20.dp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

/**
 * v27k — the shared session note's FLOATING button on the save page.
 * Pinned inside the scroll area (bottom-end, above the save CTA) so it stays
 * on screen no matter how far the format body is scrolled. Shows the note
 * text once anything is typed; tapping toggles a compact paper editor popup
 * right above, which rides the IME so typing never hides it.
 */
@Composable
private fun SessionNoteFloatingPill(
    cat: CurioCategory,
    note: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onNoteChange: (String) -> Unit
) {
    val accent = cat.themedAccent()
    Column(
        modifier = Modifier
            .imePadding()
            .padding(end = 16.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Compact note editor popup — paper look, same copy as before ──
        if (expanded) {
            // v166 — dark mode: the note-paper palette is deliberately
            // theme-agnostic for SAVED notes, but this floating popup is a
            // UI CONTROL — the bright cream sheet glared on the pitch-black
            // page, so dark mode swaps to a dark elevated sheet (light mode
            // keeps the cream paper). The text field gets explicit paper-
            // paired colors so typed text / placeholder / borders always
            // read on the sheet (M3 defaults painted light-on-dark text
            // over the bright cream — the invisible-text bug).
            val dark = isCurioDarkTheme()
            val paperInkColor = if (dark) MaterialTheme.colorScheme.onSurface
                                else notePaperInk(NotePaperColor.CREAM)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh
                        else notePaperSurface(NotePaperColor.CREAM),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(0.94f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Shared session note",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = paperInkColor.copy(alpha = 0.85f)
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { onNoteChange(it.take(240)) },
                        placeholder = { Text("What stayed with you? (one note per session)") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = paperInkColor,
                            unfocusedTextColor = paperInkColor,
                            cursorColor = paperControlAccent(),
                            focusedBorderColor = if (dark) MaterialTheme.colorScheme.outline
                                                else notePaperBorder(NotePaperColor.CREAM),
                            unfocusedBorderColor = if (dark) MaterialTheme.colorScheme.outlineVariant
                                                   else notePaperRule(NotePaperColor.CREAM),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedPlaceholderColor = paperInkColor.copy(alpha = 0.45f),
                            unfocusedPlaceholderColor = paperInkColor.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Shown on every entry saved from this session.",
                        style = MaterialTheme.typography.labelSmall,
                        color = paperInkColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
        // ── The floating button itself — accent pill, shows the note ──
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(50),
            color = accent,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Note,
                    contentDescription = null,
                    tint = cat.onAccent(),
                    size = 16.dp
                )
                Text(
                    text = if (note.isNotBlank()) note.take(24) else "Session note",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = cat.onAccent(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (expanded) {
                    CurioIcon(
                        name = CurioIcons.KeyboardArrowDown,
                        contentDescription = null,
                        tint = cat.onAccent(),
                        size = 16.dp
                    )
                }
            }
        }
    }
}

/**
 * Universal capture body — renders the ACTIVE take's format editor. The
 * section state (takes, active index, format switching, aggregation) is
 * hoisted to the screen so the format chips + take tabs pin in a compact
 * row under the topic strip and the mood pill lives inside the strip; this
 * function only composes the active editor, keyed by section id so
 * switching takes never bleeds editor state. The editor emits its data
 * continuously and the screen aggregates + re-emits canSave / draft state.
 */
@Composable
private fun FormatBodyForCategory(
    category: CurioCategory,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    boardSeed: Int? = null
) {
    val current = sections.getOrNull(activeIndex)
    if (current != null) {
        key(current.id) {
            when (current.format) {
                    CaptureFormat.SoundBite -> SoundBiteFormat(
                        category.themedAccent(), category.tint,
                        { current.canSave = it },
                        // Stamp the universal mood into whatever the editor
                        // emits — the row lives above the options, not in the
                        // format body, so the take's mood is applied here.
                        { current.data = it?.withMood(current.mood) },
                        onBusyChange = { current.busy = it },
                        initialData = current.seed as? CaptureData.SoundBite
                    )
                    CaptureFormat.ReelNotes -> ReelNotesFormat(
                        category.themedAccent(), category.tint,
                        { current.canSave = it },
                        { current.data = it?.withMood(current.mood) },
                        initialData = current.seed as? CaptureData.ReelNotes
                    )
                    CaptureFormat.Marginalia -> MarginaliaFormat(
                        category.themedAccent(), category.tint,
                        { current.canSave = it },
                        { current.data = it?.withMood(current.mood) },
                        initialData = current.seed as? CaptureData.Marginalia
                    )
                    CaptureFormat.GalleryWall -> GalleryWallFormat(
                        category.themedAccent(), category.tint,
                        { current.canSave = it },
                        { current.data = it?.withMood(current.mood) },
                        initialData = current.seed as? CaptureData.GalleryWall,
                        boardSeed = boardSeed
                    )
                    CaptureFormat.FieldNotes -> FieldNotesFormat(
                        category.themedAccent(), category.tint,
                        { current.canSave = it },
                        { current.data = it?.withMood(current.mood) },
                        initialData = current.seed as? CaptureData.FieldNotes
                    )
                    CaptureFormat.OpenNotebook -> OpenNotebookFormat(
                        category.themedAccent(), category.tint,
                        { current.canSave = it },
                        { current.data = it?.withMood(current.mood) },
                        initialData = current.seed as? CaptureData.OpenNotebook,
                        boardSeed = boardSeed
                    )
                }
            }
        }
}

/**
 * The pinned take tabs + "Add take" row — sits with the format chips under
 * the topic strip so switching takes never scrolls away. [onRequestRemove]
 * is invoked when the user taps a take's X; the screen decides whether to
 * confirm (drafted content) or remove directly.
 */
@Composable
private fun CaptureTakeTabs(
    category: CurioCategory,
    sections: SnapshotStateList<CaptureSectionState>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onRequestRemove: (Int) -> Unit,
    onAddTake: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEachIndexed { i, s ->
            Surface(
                onClick = { onSelect(i) },
                shape = RoundedCornerShape(50),
                color = if (i == activeIndex) category.themedAccent()
                        else category.categorySurface(MaterialTheme.colorScheme.surfaceVariant),
                // v27q — flat 2dp: selection reads through the solid
                // accent fill.
                shadowElevation = 2.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = if (sections.size > 1) 4.dp else 12.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurioIcon(
                        name = formatGlyph(s.format),
                        contentDescription = null,
                        tint = if (i == activeIndex) category.onAccent()
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 14.dp
                    )
                    Text(
                        text = "${i + 1} · ${s.format.shortName}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (i == activeIndex) category.onAccent() else MaterialTheme.colorScheme.onSurface
                    )
                    if (sections.size > 1) {
                        Surface(
                            onClick = { onRequestRemove(i) },
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            CurioIcon(
                                name = CurioIcons.Close,
                                contentDescription = "Remove take",
                                tint = if (i == activeIndex) category.onAccent()
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
        Surface(
            onClick = onAddTake,
            shape = RoundedCornerShape(50),
            color = if (AppPreferences.tintWashEffective()) category.tint else category.themedAccent(),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = null,
                    tint = if (AppPreferences.tintWashEffective()) category.categoryInk() else category.onAccent(),
                    size = 16.dp
                )
                Text(
                    text = "Add take",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (AppPreferences.tintWashEffective()) category.categoryInk() else category.onAccent()
                )
            }
        }
    }
}

/** One take (section) inside the universal multi-section picker. */
/** One format chip in the capture editor's switcher — the active chip wears
 *  the category accent; content ink flips for readable contrast in every
 *  theme (light tint wash, solid accent, pastel fills, AMOLED). Shared by
 *  the wide FlowRow and the compact horizontal-scroll row. */
@Composable
private fun FormatChip(
    fmt: CaptureFormat,
    active: CaptureSectionState,
    category: CurioCategory,
    onSwitch: (CaptureFormat) -> Unit
) {
    Surface(
        onClick = {
            if (active.format != fmt) onSwitch(fmt)
        },
        shape = RoundedCornerShape(50),
        // v27q — the tint-wash fill is the OPAQUE 20% lerp (the raw tint is
        // translucent and let the shadow bleed); elevation stays flat 2dp.
        color = if (AppPreferences.tintWashEffective() && active.format == fmt)
                lerp(MaterialTheme.colorScheme.surface, category.accent, 0.20f)
                else if (active.format == fmt) category.themedAccent()
                else category.categorySurface(MaterialTheme.colorScheme.surface),
        shadowElevation = 2.dp,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = formatGlyph(fmt),
                contentDescription = null,
                // Active chip: readable in EVERY theme — on the light tint
                // wash the ink is the category's theme-aware ink (deep in
                // light, pastel twin in dark); with the wash off
                // (AMOLED/Material) the chip is a solid accent so the
                // content flips to onAccent() — deep-accent text on a
                // deep-accent chip was invisible in AMOLED.
                tint = if (active.format == fmt)
                       (if (AppPreferences.tintWashEffective()) category.categoryInk() else category.onAccent())
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp
            )
            Text(
                text = fmt.shortName,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (active.format == fmt) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (active.format == fmt)
                        (if (AppPreferences.tintWashEffective()) category.categoryInk() else category.onAccent())
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private class CaptureSectionState(val id: Int, initialFormat: CaptureFormat) {
    var format by mutableStateOf(initialFormat)
    var canSave by mutableStateOf(false)
    var data by mutableStateOf<CaptureData?>(null)
    var seed by mutableStateOf<CaptureData?>(null)
    // The take's mood — held HERE (the topic-strip mood pill drives it, one
    // universal selector for every take) and stamped into the section's data
    // on every editor emit + mood change.
    var mood by mutableStateOf<JournalMood?>(null)
    // True while a live recording is in progress — format-switch confirmation
    // must also trigger here (data/canSave are null mid-recording).
    var busy by mutableStateOf(false)
}

/** The 5 concrete format chips offered by the universal picker. */
private val CAPTURE_FORMATS = listOf(
    CaptureFormat.SoundBite,
    CaptureFormat.ReelNotes,
    CaptureFormat.Marginalia,
    CaptureFormat.GalleryWall,
    CaptureFormat.FieldNotes
)

/** The mood stored on [data] (recursing into OpenNotebook wrappers). */
private fun CaptureData?.moodOf(): JournalMood? = when (this) {
    is CaptureData.SoundBite -> mood
    is CaptureData.ReelNotes -> mood
    is CaptureData.Marginalia -> mood
    is CaptureData.FieldNotes -> mood
    is CaptureData.GalleryWall -> mood
    is CaptureData.OpenNotebook -> subData.moodOf()
    else -> null
}

/** Returns [this] with [mood] stamped on (recursing into OpenNotebook). */
private fun CaptureData.withMood(mood: JournalMood?): CaptureData = when (this) {
    is CaptureData.SoundBite -> copy(mood = mood)
    is CaptureData.ReelNotes -> copy(mood = mood)
    is CaptureData.Marginalia -> copy(mood = mood)
    is CaptureData.FieldNotes -> copy(mood = mood)
    is CaptureData.GalleryWall -> copy(mood = mood)
    is CaptureData.OpenNotebook -> copy(subData = subData.withMood(mood))
    else -> this
}

/**
 * Recursively persists every SoundBite audio file inside [data] (through
 * OpenNotebook wrappers and Portfolio sections) so each recording gets a
 * stable internal path before saving.
 */
private suspend fun persistAudioDeep(
    context: Context,
    data: CaptureData,
    entryId: String
): CaptureData = when (data) {
    is CaptureData.SoundBite ->
        if (data.audioFilePath.isNullOrBlank()) data
        else {
            val result = AudioStorageManager.persistAudio(
                context, data.audioFilePath, entryId
            )
            data.copy(
                audioFilePath = result.persistentPath,
                fileSizeBytes = result.fileSizeBytes
            )
        }
    is CaptureData.OpenNotebook ->
        data.copy(subData = persistAudioDeep(context, data.subData, entryId))
    is CaptureData.Portfolio -> {
        val persistedSections = mutableListOf<CaptureData.CaptureSection>()
        data.sections.forEachIndexed { index, section ->
            persistedSections += section.copy(
                data = persistAudioDeep(context, section.data, "$entryId-$index")
            )
        }
        data.copy(sections = persistedSections)
    }
    else -> data
}

/**
 * The [CaptureFormat] that best represents [data] for the entry's format
 * column — the first section's format for a Portfolio.
 */
private fun formatOf(data: CaptureData): CaptureFormat = when (data) {
    is CaptureData.SoundBite -> CaptureFormat.SoundBite
    is CaptureData.ReelNotes -> CaptureFormat.ReelNotes
    is CaptureData.Marginalia -> CaptureFormat.Marginalia
    is CaptureData.GalleryWall -> CaptureFormat.GalleryWall
    is CaptureData.FieldNotes -> CaptureFormat.FieldNotes
    is CaptureData.OpenNotebook -> data.subFormat
    is CaptureData.Portfolio -> data.sections.firstOrNull()?.format ?: CaptureFormat.SoundBite
}
