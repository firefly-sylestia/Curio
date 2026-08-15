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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategory
import com.curio.app.ui.components.categoryEdgeShine
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.curioPillLift
import com.curio.app.ui.theme.isCurioDarkTheme

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
 * together, plus Clear (deselect all — the fast way to start an empty mix).
 * v27t — the Everything preset is gone: the Wildcard lane already covers
 * every category, so "everything" was a redundant chip. Presets only tick
 * lanes that are visible at tap time (hidden lanes and not-yet-shipped lanes
 * drop out), so a preset never silently overrides Manage Categories.
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
    // Clear = deselect every lane (stays in multi-select so the mix can be
    // rebuilt from scratch without closing the picker).
    DeckPreset("Clear", "close", emptyList(), clearAll = true)
)

/**
 * Small pill chip for a quick-mix preset.
 *
 * v83 — theme-aware + DYNAMIC colors like [com.curio.app.features.picker.PickerPageTab]:
 * callers pass the selected fill / content inks from their context (banner
 * ink on a tear hero, category accent on the wash). The preset glyph rides
 * the same ink, so the icon color follows the selection state too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerPresetChip(
    label: String,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    accentInk: Color = MaterialTheme.colorScheme.onPrimary,
    idleInk: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    idleFill: Color? = null
) {
    val shape = RoundedCornerShape(50)
    // v86 — DARK-aware default idle fill (same fix as PickerPageTab): the
    // old default lifted toward curioPillLift() (WHITE in dark) → near-
    // white idle chips with light-grey text on the black sheet. Dark now
    // stays a dark raised glass; light keeps the cream lift.
    val resolvedIdleFill = idleFill ?: lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else curioPillLift(),
        0.82f
    )
    Surface(
        onClick = onClick,
        shape = shape,
        // v27q — selection reads as a SOLID accent fill with its content ink.
        // v33 — the UNselected pill is a proper raised pill that stands off
        // the category wash: it lifts clearly toward the page background
        // (cream in light, a lighter glass in dark) instead of the old
        // surfaceVariant blend that melted into the tinted picker.
        // v38 — the light lift rises to 0.82 (neutral cream) so the pills
        // separate from the pale pastel wash, and BOTH states carry a 3dp
        // elevation.
        color = if (selected) accent else resolvedIdleFill,
        shadowElevation = 3.dp,
        modifier = Modifier
            // v28 — soft glow + top-lit shine.
            .curioDarkGlow(3.dp, shape)
            .categoryEdgeShine(shape, accent = accent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                size = 14.dp,
                tint = if (selected) accentInk else idleInk
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accentInk else idleInk
            )
        }
    }
}
