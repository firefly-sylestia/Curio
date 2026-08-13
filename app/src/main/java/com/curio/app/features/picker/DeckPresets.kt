package com.curio.app.features.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategory
import com.curio.app.ui.components.categoryEdgeShine
import com.curio.app.ui.theme.CurioIcon

/**
 * One quick-mix preset: label, glyph, and the lanes it ticks.
 *
 * Shared by the full-screen CategoryPickerScreen and the Spin page's inline
 * CategoryPickerSheet so both decks offer the same one-tap mixes.
 */
data class DeckPreset(
    val label: String,
    val glyph: String,
    val ids: List<CategoryId>,
    /** v27k — a "Clear" preset: deselects every lane instead of ticking a mix. */
    val clearAll: Boolean = false
) {
    /**
     * Resolves the preset's lanes against the currently visible categories.
     * An EMPTY id list means "everything visible" (used by the Everything
     * preset, which can't be a static list because the visible set changes
     * with Manage Categories).
     */
    fun lanes(categories: List<CurioCategory>): List<CurioCategory> {
        if (ids.isEmpty()) return categories
        val visibleIds = categories.map { it.id }.toSet()
        return ids.filter { it in visibleIds }
            .mapNotNull { id -> categories.firstOrNull { it.id == id } }
    }
}

/**
 * v27k — the quick-mix presets. Real mixes this time: Science, Entertainment,
 * Arts & Stories, and History & Ideas group the lanes people actually browse
 * together, plus Everything (all visible) and Clear (deselect all — the fast
 * way to start an empty mix). Presets only tick lanes that are visible at tap
 * time (hidden lanes and not-yet-shipped lanes drop out), so a preset never
 * silently overrides Manage Categories.
 */
val deckPresets = listOf(
    DeckPreset("Science", "science", listOf(
        CategoryId.SCIENTISTS, CategoryId.DISCOVERIES,
        CategoryId.BIOLOGY, CategoryId.CHEMISTRY,
        CategoryId.ANIMALS, CategoryId.PLANTS,
        CategoryId.ASTRONOMY, CategoryId.MEDICINE,
        CategoryId.GEOLOGY, CategoryId.PSYCHOLOGY,
        CategoryId.MATHEMATICS, CategoryId.TECHNOLOGIES,
        CategoryId.ENGINEERING, CategoryId.ECONOMICS
    )),
    DeckPreset("Entertainment", "movie", listOf(
        CategoryId.FILMS, CategoryId.SERIES,
        CategoryId.ANIME, CategoryId.MANGA, CategoryId.MANHWA,
        CategoryId.GAMES, CategoryId.SPORTS,
        CategoryId.SONGS, CategoryId.ALBUMS, CategoryId.INTERNET
    )),
    DeckPreset("Arts & Stories", "palette", listOf(
        CategoryId.PAINTERS, CategoryId.ARTWORKS,
        CategoryId.ARTISTS, CategoryId.AUTHORS, CategoryId.BOOKS,
        CategoryId.DIRECTORS, CategoryId.MYTHOLOGY, CategoryId.FOOD
    )),
    DeckPreset("History & Ideas", "history", listOf(
        CategoryId.HISTORY, CategoryId.SCIENTISTS,
        CategoryId.DISCOVERIES, CategoryId.MYTHOLOGY,
        CategoryId.LANGUAGE, CategoryId.AUTHORS
    )),
    // Everything = all visible categories (empty list is resolved at tap time).
    DeckPreset("Everything", "casino", emptyList()),
    // Clear = deselect every lane (stays in multi-select so the mix can be
    // rebuilt from scratch without closing the picker).
    DeckPreset("Clear", "close", emptyList(), clearAll = true)
)

/** Small pill chip for a quick-mix preset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerPresetChip(
    label: String,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        shadowElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier
            .categoryEdgeShine(shape, accent = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                size = 14.dp,
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
