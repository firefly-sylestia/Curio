package com.curio.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.curioCorner
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * Curio's universal empty-state skeleton — see Curio empty-state contract.
 *
 * Upgraded with a gentle breathing scale on the glyph for a living,
 * breathing feel even on empty screens. Content renders all at once.
 *
 * Layout (centered column):
 *  ```
 *      [ glyph @ 96dp in category accent tint, gentle breathing pulse ]
 *      Headline (geom, 24sp heavy)
 *      Subtext (Inter neutral, 16sp muted, 1-2 lines, centered)
 *      [ optional Primary CTA — full-width pill ]
 *  ```
 */
@Composable
fun CurioEmptyState(
    glyph: String,
    headline: String,
    subtext: String,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color? = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
    ctaLabel: String? = null,
    onCtaClick: () -> Unit = {}
) {
    // Breathing scale for the glyph
    val breatheScale = rememberBreathingScale(active = true, amplitude = 0.025f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(breatheScale),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = glyph,
                    contentDescription = null,
                    tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 96.dp
                )
            }

            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (ctaLabel != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onCtaClick,
                    shape = curioCorner(24.dp, MaterialTheme.shapes.large),
                    colors = ButtonDefaults.buttonColors(
                        // v81 — dark: the deep rose fill + bright twin ink
                        // (the pale coral fill would glare on the black page).
                        containerColor = if (isCurioDarkTheme()) CurioColors.HomeRosewoodDark else MaterialTheme.colorScheme.primary,
                        contentColor = if (isCurioDarkTheme()) CurioColors.CoralBlush else MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = ctaLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
