package com.curio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * The canonical Curio search box — see Curio surface contract.
 *
 * Compact magnifier + live query + one-tap clear, used by the Settings hub
 * (filters every section) and Topic History (filters explored topics). One
 * shared component keeps the search language (rounded surface, leading
 * glyph, placeholder, clear affordance) consistent across the app.
 *
 * The placeholder text is supplied by the caller; it sits behind the field
 * and shows only while the query is empty.
 */
@Composable
fun CurioSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    // v61 — optional text-style override so the Spin filter sheet can run
    // its bigger-type page while the shared default stays unchanged.
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    // v68 — full pill (50dp) like the Cabinet hero's search bar, so the
    // on-page search boxes (Settings hub, Spin filter sheet, Topic History)
    // read as the same search language as the hero fields.
    val pillShape = RoundedCornerShape(50)
    Surface(
        shape = pillShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            // v28 — dark mode elevation visibility (glow + hairline).
            // v81 — dark: the One UI shiny glass edge on the search pill.
            .curioDarkGlow(3.dp, pillShape)
            .curioGlassEdge(pillShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CurioIcon(
                name = CurioIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp
                    )
                }
            }
        }
    }
}
