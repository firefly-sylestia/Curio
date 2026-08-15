package com.curio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * Small streak counter pill shown on the Home screen — see Curio Home contract.
 *
 * Renders a tiny "local_fire_department" Material Symbols glyph + the streak
 * count if [days] > 0, otherwise returns nothing. Tapping pops a small info
 * dialog explaining how streaks work (placeholder phase: no-op).
 *
 * The pill is intentionally small (~32dp tall) — it's auxiliary context,
 * never the primary CTA.
 */
@Composable
fun CurioStreakPill(
    days: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    if (days <= 0) return

    // v81 — dark: the pill sits on the pitch-black page, so the ink flips to
    // the BRIGHT butter twin (the deep plum would vanish on the darkened
    // butter wash over black); the fill stays the translucent butter.
    val ink = if (isCurioDarkTheme()) CurioColors.ButterYellow else CurioColors.DeepPlum
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = CurioColors.ButterYellow.copy(alpha = 0.40f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = "local_fire_department",
                contentDescription = null,
                tint = ink,
                size = 18.dp
            )
            Text(
                text = "$days-day streak",
                style = MaterialTheme.typography.labelMedium,
                color = ink
            )
        }
    }
}

/**
 * Small text-button styled link with optional leading icon, used in the
 * Category Picker (§4), Manage Categories (§13.4), and Entry Detail (§10)
 * overflow menu actions — anything that's a low-emphasis affordance,
 * more prominent than a plain Text but lighter than a filled Button.
 */
@Composable
fun CurioSecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingGlyph: String? = null,
    tint: Color? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingGlyph != null) {
                CurioIcon(
                    name = leadingGlyph,
                    contentDescription = null,
                    tint = tint ?: MaterialTheme.colorScheme.primary,
                    size = 18.dp
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint ?: MaterialTheme.colorScheme.primary
            )
        }
    }
}
