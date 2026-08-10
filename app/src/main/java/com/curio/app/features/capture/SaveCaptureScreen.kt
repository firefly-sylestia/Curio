package com.curio.app.features.capture

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioStorageManager
import com.curio.app.data.CaptureConverters
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureDraftStore
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CaptureRepository
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
import com.curio.app.data.shortName
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
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
import com.curio.app.ui.components.formatGlyph
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryBorder
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
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
                    val entry = if (existingEntry != null) {
                        // Edit mode: keep id/topic/title/timestamp, swap the data.
                        existingEntry.copy(
                            format = formatOf(persistedData),
                            captureData = persistedData,
                            tags = tags
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
                            tags = tags
                        )
                    }
                    runCatching { CurioRepositoryHolder.repo.save(entry) }
                        .onSuccess {
                            savedEntryId = entry.id
                            saveError = null
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
        val tintWash = AppPreferences.tintWashEffective()
        val stripColor = if (tintWash) cat.tint else MaterialTheme.colorScheme.surfaceContainerHigh
        val stripInk = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurface
        Surface(
            color = stripColor,
            shape = RoundedCornerShape(20.dp),
            border = cat.categoryBorder(),
            modifier = Modifier
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
                    color = if (tintWash) cat.themedAccent().copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    CurioIcon(
                        name = cat.iconGlyph,
                        contentDescription = null,
                        tint = if (tintWash) cat.categoryInk() else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 22.dp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Column {
                    Text(
                        text = topic?.name ?: "Loading…",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = stripInk
                    )
                    Text(
                        text = cat.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = stripInk.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ── Scrollable format body ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                            // Dispatch on the SAVED entry's format in edit mode so
                            // the right body renders regardless of category default.
                            entryFormat = editingEntry?.format,
                            // Edit mode seeds from the saved entry; a fresh capture
                            // seeds from the RESUMED autosaved draft (if any).
                            initialData = editingEntry?.captureData ?: resumedDraftData,
                            // Reuse the saved entry's id-derived seed so the editor's
                            // watermark pattern matches the saved view exactly.
                            boardSeed = editEntryId?.hashCode(),
                            onCanSaveChange = { canSave = it && topic != null && (editEntryId == null || editingEntry != null) },
                            onDraftChange = { hasAnyDraft = it },
                            onDataChanged = { currentCaptureData = it },
                            onDraftDataChanged = { draftData = it }
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
                        color = if (AppPreferences.tintWashEffective()) tint.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
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
 * Universal capture body — the redesigned "How do you want to capture this
 * one?" picker, now offered for EVERY category (the Wildcard pick-any style
 * became universal). A compact chip row pre-selects the category's dedicated
 * format; users can add multiple different takes (sections) which all save
 * into ONE entry. On save, a single section stores bare data (backward
 * compatible) while 2+ sections store a [CaptureData.Portfolio] — the detail
 * page then shows a section switcher.
 *
 * Section state (format + live data) is held here so switching sections
 * never loses in-progress content: each editor emits its data continuously,
 * and the outgoing section's data is snapshotted as the next activation's
 * seed. [initialData] (edit mode) may be a Portfolio, a legacy
 * OpenNotebook, or a bare format payload — all are unwrapped into sections.
 */
@Composable
private fun FormatBodyForCategory(
    category: CurioCategory,
    onCanSaveChange: (Boolean) -> Unit,
    onDataChanged: (CaptureData?) -> Unit,
    onDraftChange: (Boolean) -> Unit = {},
    entryFormat: CaptureFormat? = null,
    initialData: CaptureData? = null,
    boardSeed: Int? = null,
    onDraftDataChanged: (CaptureData?) -> Unit = {}
) {
    // Wildcard has no dedicated page — default its first take to Voice.
    val defaultFormat = if (category.defaultFormat == CaptureFormat.OpenNotebook)
        CaptureFormat.SoundBite else category.defaultFormat

    val sections = remember(entryFormat, initialData) {
        mutableStateListOf<CaptureSectionState>().apply {
            when {
                initialData is CaptureData.Portfolio && initialData.sections.isNotEmpty() ->
                    initialData.sections.forEachIndexed { i, s ->
                        add(CaptureSectionState(i, s.format).apply {
                            seed = s.data
                            data = s.data
                            mood = s.data.moodOf()
                            canSave = true
                        })
                    }
                initialData is CaptureData.OpenNotebook ->
                    add(CaptureSectionState(0, initialData.subFormat).apply {
                        seed = initialData.subData
                        data = initialData.subData
                        mood = initialData.subData.moodOf()
                        canSave = true
                    })
                initialData != null ->
                    add(CaptureSectionState(0, entryFormat ?: defaultFormat).apply {
                        seed = initialData
                        data = initialData
                        mood = initialData.moodOf()
                        canSave = true
                    })
                else -> add(CaptureSectionState(0, defaultFormat))
            }
        }
    }
    // Mood-board edits reopen on their board section; everything else starts
    // on the first take.
    var activeIndex by remember(entryFormat, initialData) {
        mutableIntStateOf(
            (initialData as? CaptureData.Portfolio)
                ?.sections?.indexOfFirst { it.format == CaptureFormat.GalleryWall }
                ?.coerceAtLeast(0) ?: 0
        )
    }
    var nextId by remember(entryFormat, initialData) {
        mutableIntStateOf(sections.maxOfOrNull { it.id }?.plus(1) ?: 0)
    }

    // Snapshot the outgoing section's data so switching back restores it.
    fun snapshotActive() {
        sections.getOrNull(activeIndex)?.let { it.seed = it.data }
    }

    // Removes a take and re-anchors the active index — shared by the X
    // button's direct-remove path and the remove-confirmation dialog so the
    // two can never drift apart.
    fun removeSection(i: Int) {
        if (i < activeIndex) activeIndex--
        sections.removeAt(i)
        if (activeIndex >= sections.size) activeIndex = sections.size - 1
    }

    // Switching a FILLED section's format clears its content — confirm first
    // so a fat-finger on the format chips never silently wipes a take.
    var pendingFormatSwitch by remember { mutableStateOf<CaptureFormat?>(null) }
    // Removing a take that holds drafted content (or a live recording) also
    // confirms first — in edit mode every take arrives prefilled, so the X
    // must never silently throw away drafted changes.
    var pendingRemoveIndex by remember { mutableStateOf<Int?>(null) }
    fun applyFormat(section: CaptureSectionState, fmt: CaptureFormat) {
        section.format = fmt
        section.canSave = false
        section.data = null
        section.seed = null
        section.busy = false
    }

    // ── Aggregate: all sections must be filled to save ──────────────────
    val allReady = sections.isNotEmpty() && sections.all { it.canSave && it.data != null }
    val combinedData: CaptureData? = when {
        !allReady -> null
        sections.size == 1 -> sections[0].data
        else -> CaptureData.Portfolio(
            sections.map { CaptureData.CaptureSection(it.format, it.data!!) }
        )
    }
    // ANY take holding drafted content (or a live recording) — the leave /
    // switch / remove guards key on this, so a rating, images or a voice
    // note without the required text still count as "content you'd lose".
    val hasAnyDraft = sections.any { it.data != null || it.busy }
    // BEST-EFFORT draft snapshot for autosave — unlike [combinedData] this
    // is non-null even before every section is complete, so a partial
    // multi-section draft (one take filled, one empty) still autosaves the
    // filled content instead of nothing.
    val draftData: CaptureData? = when {
        sections.isEmpty() -> null
        sections.size == 1 -> sections[0].data
        else -> {
            val filled = sections.mapNotNull { s ->
                s.data?.let { CaptureData.CaptureSection(s.format, it) }
            }
            if (filled.isNotEmpty()) CaptureData.Portfolio(filled) else null
        }
    }
    LaunchedEffect(allReady, combinedData, hasAnyDraft, sections.toList()) {
        onCanSaveChange(allReady)
        onDraftChange(hasAnyDraft)
        onDataChanged(combinedData)
        onDraftDataChanged(draftData)
    }

    // Wide windows wrap the six format chips (see below); compact phones
    // keep the horizontal scroll.
    val wide = windowWidthSizeClass().isWide

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Universal mood — ONE "How did it make you feel?" row for every
        // format, sitting right above the capture options. It reflects the
        // ACTIVE take's mood; picking one writes it into that take's data
        // (behind the "Entry date & mood" setting).
        val active = sections.getOrNull(activeIndex)
        // Every format (including the mood board) carries the shared mood
        // row now — GalleryWall gained a mood field so a picked mood persists.
        // OpenNotebook wraps a sub-format which always carries mood too.
        val moodCapable = true
        if (AppPreferences.entryMetaEnabledState && active != null && moodCapable) {
            MoodChipsRow(
                mood = active.mood,
                accent = category.themedAccent(),
                onMoodChange = { m ->
                    active.mood = m
                    // Stamp into the take's live data so the saved entry +
                    // meta card see it even before the editor re-emits.
                    active.data = active.data?.withMood(m)
                }
            )
        }

        Text(
            text = "How do you want to capture this one?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // ── Compact format chips — control the ACTIVE section ────────────
        // Wide windows wrap the six chips so the whole format set is visible
        // at a glance; compact phones keep the horizontal scroll.
        if (active != null) {
            val onSwitch = { target: CaptureFormat ->
                // Confirm when this take holds ANY content — text, quotes, a
                // rating, images, a voice note, tiles or a live recording.
                // canSave's rule (primary text only) let optional-only drafts
                // switch silently and vanish; hasAnyDraft keeps every draft
                // protected. An empty take switches freely.
                if (active.data != null || active.busy) {
                    pendingFormatSwitch = target
                } else {
                    applyFormat(active, target)
                }
            }
            if (wide) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CAPTURE_FORMATS.forEach { fmt ->
                        FormatChip(fmt = fmt, active = active, category = category, onSwitch = onSwitch)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CAPTURE_FORMATS.forEach { fmt ->
                        FormatChip(fmt = fmt, active = active, category = category, onSwitch = onSwitch)
                    }
                }
            }
        }

        // ── Section tabs + add another take ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEachIndexed { i, s ->
                Surface(
                    onClick = { snapshotActive(); activeIndex = i },
                    shape = RoundedCornerShape(50),
                    color = if (i == activeIndex) category.themedAccent()
                            else category.categorySurface(MaterialTheme.colorScheme.surfaceVariant),
                    border = if (i == activeIndex) null else category.categoryBorder(),
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
                                onClick = {
                                    val section = sections.getOrNull(i)
                                    // Confirm removal when the take holds ANY
                                    // drafted content (text, quotes, a rating,
                                    // images, a voice note, tiles) or a live
                                    // recording; an empty take removes freely.
                                    if (section != null && (section.data != null || section.busy)) {
                                        pendingRemoveIndex = i
                                    } else {
                                        removeSection(i)
                                    }
                                },
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
                onClick = {
                    snapshotActive()
                    sections.add(CaptureSectionState(nextId++, defaultFormat))
                    activeIndex = sections.lastIndex
                },
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

        // ── Active section's format body ─────────────────────────────────
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

    // ── Confirm before removing a take with drafted content ─────────────
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

    // ── Confirm before switching a filled take's format ──────────────────
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
        color = if (AppPreferences.tintWashEffective() && active.format == fmt) category.tint
                else if (active.format == fmt) category.themedAccent()
                else category.categorySurface(MaterialTheme.colorScheme.surface),
        border = if (active.format == fmt) BorderStroke(
            1.dp,
            category.themedAccent().copy(alpha = 0.5f)
        ) else category.categoryBorder(
            fallback = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ),
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
    // The take's mood — held HERE (one universal row above the format
    // options drives it) and stamped into the section's data on every
    // editor emit + mood change, so all formats share one picker.
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
