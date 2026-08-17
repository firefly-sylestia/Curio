package com.curio.app.features.capture.formats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureData
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.data.TextSpan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.curio.app.ui.components.RichTextEditor
import com.curio.app.ui.components.RichTextToolbarMode
import com.curio.app.ui.theme.paperInk

/**
 * Reel Notes format body — CURIO_SPEC §8.2 (Movies / Directors).
 *
 * Layout (top to bottom):
 *   - Optional 1-5 star rating row (tap to set, tap same star to clear)
 *   - Multi-line review text field (sentences capitalization, returns
 *     wrap; the soft-keyboard rich-text toolbar handles bold/italic/bullets)
 *   - Optional up-to-3 image attachments (poster / stills) shown as a
 *     row of [ImageThumb] placeholders + an [AddImageButton] until 3 reached
 *
 * [onCanSaveChange] fires true when the review text field has any content,
 * OR the take holds optional-only content (a star rating, attached images)
 * — optional-only drafts must still save and must still trigger the leave /
 * format-switch guards, or back/switch would silently drop them.
 */
@Composable
fun ReelNotesFormat(
    accent: Color,
    tint: Color,
    onCanSaveChange: (Boolean) -> Unit,
    onDataChanged: (CaptureData?) -> Unit = {},
    initialData: CaptureData.ReelNotes? = null
) {
    val context = LocalContext.current
    // v158 — the dictation mic shows only while the Voice-to-text settings
    // toggle is on (same experiment gate as the sound bite note box).
    val voiceToTextEnabled = AppPreferences.voiceToTextEnabledState
    // Edit mode: restore rating / review / images so re-saving an entry
    // preserves the original capture instead of silently wiping it.
    var rating by remember(initialData) { mutableStateOf(initialData?.rating ?: 0) }
    var reviewText by remember(initialData) { mutableStateOf(initialData?.reviewText ?: "") }
    // Rich-text formatting for the review — legacy entries lack it (Gson →
    // null), guard with orEmpty().
    var reviewSpans by remember(initialData) { mutableStateOf(initialData?.reviewSpans.orEmpty()) }
    // Note-paper style for the review box — legacy entries lack the field
    // (Gson → null), fall back to the take-level paperStyle → RULED.
    var reviewStyle by remember(initialData) {
        mutableStateOf(initialData?.reviewStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED)
    }
    // Note-paper color for the review box — legacy entries lack the field
    // (Gson → null), fall back to CREAM.
    var reviewColor by remember(initialData) {
        mutableStateOf(initialData?.reviewColor ?: NotePaperColor.CREAM)
    }
    // Mood — the shared "How did it make you feel?" row. Optional; legacy
    // entries have none (Gson → null).
    var mood by remember(initialData) { mutableStateOf(initialData?.mood) }
    // Quote cards — the SHARED hand-placed paper notecard section (same
    // component Marginalia / Sound Bite / Mood Board use). Owns the parallel
    // lists (text / spans / tilt / style / color); new cards inherit the
    // review box's current paper style + color.
    val quoteCards = rememberQuoteCardsState(
        initialQuotes = initialData?.quotes.orEmpty(),
        initialSpans = initialData?.quoteSpans.orEmpty(),
        initialTilts = initialData?.quoteTilts.orEmpty(),
        initialStyles = initialData?.quoteStyles.orEmpty(),
        initialColors = initialData?.quoteColors.orEmpty(),
        defaultStyle = initialData?.reviewStyle ?: initialData?.paperStyle ?: NotePaperStyle.RULED,
        defaultColor = initialData?.reviewColor ?: NotePaperColor.CREAM
    )
    var imageUris by remember(initialData) { mutableStateOf(initialData?.imageUris.orEmpty()) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        imageUris = (imageUris + uris.map { it.toString() }).take(MaxAttachedImages)
    }

    val canSave = reviewText.isNotBlank() || quoteCards.hasContent ||
        rating > 0 || imageUris.isNotEmpty()
    // Key on every input, not just canSave: rating, review text, quotes and
    // images added AFTER the first character must re-emit, or saving would
    // persist stale data (text/rating/quotes/images silently dropped from
    // the saved entry).
    LaunchedEffect(
        canSave, rating, reviewText, reviewSpans, imageUris, reviewStyle, reviewColor, mood,
        quoteCards.quotes.toList(), quoteCards.spans.toList(), quoteCards.tilts.toList(),
        quoteCards.styles.toList(), quoteCards.colors.toList()
    ) {
        onCanSaveChange(canSave)
        onDataChanged(
            if (canSave) CaptureData.ReelNotes(
                rating = rating,
                reviewText = reviewText,
                imageCount = imageUris.size,
                imageUris = imageUris,
                reviewSpans = reviewSpans,
                reviewStyle = reviewStyle,
                reviewColor = reviewColor,
                quotes = quoteCards.quotes.toList(),
                quoteSpans = quoteCards.spans.toList(),
                quoteTilts = quoteCards.tilts.toList(),
                quoteStyles = quoteCards.styles.toList(),
                quoteColors = quoteCards.colors.toList(),
                // Legacy fallback — mirror the primary field's style.
                paperStyle = reviewStyle,
                mood = mood
            )
            else null
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Star rating row ────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Your rating (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StarRating(
                rating = rating,
                onRatingChange = { newRating ->
                    rating = if (newRating == rating) 0 else newRating
                },
                accent = accent
            )
            // Subtle helper line — guides first-time raters, quiet enough
            // to ignore once the rating is set.
            Text(
                text = "Rate quality",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }

        // ── Review text field — rich text behind a small format toggle ──
        // Wears the same note-paper box as the Marginalia journal (light +
        // dark). The toolbar renders OUTSIDE the paper slip (paper mode) so
        // the ruled lines line up under the text while typing; the field
        // itself sits directly on the paper (no inner box / double margin).
        RichTextEditor(
            modifier = Modifier.fillMaxWidth(),
            text = reviewText,
            spans = reviewSpans,
            onRichTextChange = { newText, newSpans ->
                reviewText = newText
                reviewSpans = newSpans
            },
            placeholder = "What did you think of the film?",
            toolbarMode = RichTextToolbarMode.TOGGLE,
            minHeight = 140.dp,
            ink = paperInk(),
            accent = MaterialTheme.colorScheme.tertiary,
            paper = true,
            paperStyle = reviewStyle,
            onPaperStyleChange = { reviewStyle = it },
            paperColor = reviewColor,
            onPaperColorChange = { reviewColor = it },
            // Roomier slip — the review text breathes off the paper edges
            // (the ruled lines are anchored to the top padding, so they stay
            // aligned under the text lines).
            paperContentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            // v158 — dictation rides the review box's own tool dock.
            trailingAction = {
                DictationMic(
                    enabled = true,
                    visible = voiceToTextEnabled,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onInsert = { text ->
                        reviewText = if (reviewText.isBlank()) text else "$reviewText\n$text"
                    }
                )
            }
        )

        // ── Quote cards — the SHARED hand-placed paper notecard section ──
        // New cards inherit the review box's current paper style + color.
        QuoteCardsSection(
            state = quoteCards,
            newCardStyle = { reviewStyle },
            newCardColor = { reviewColor }
        )

        // ── Image attach row ───────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Attach images (optional, up to $MaxAttachedImages)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Many attachments wrap into a tidy grid of thumbs instead of
            // overflowing one long row.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                imageUris.forEachIndexed { i, uri ->
                    ImageThumb(
                        index = i + 1,
                        accent = accent,
                        tint = tint,
                        imageUri = uri,
                        onClick = { /* TODO Phase 4: open lightbox */ },
                        onRemove = { imageUris = imageUris.filterIndexed { idx, _ -> idx != i } }
                    )
                }
                if (imageUris.size < MaxAttachedImages) {
                    AddImageButton(
                        accent = accent,
                        tint = tint,
                        onClick = { imagePicker.launch(arrayOf("image/*")) }
                    )
                }
            }
        }
    }
}