package com.curio.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * THE canonical Curio search box — one shared One UI style for every search
 * bar in the app (v90): a full 50dp capsule at the same 46dp height as the
 * hero action pills, a frosted glass fill, an ink hairline, and the shiny
 * glass edge in dark. Heroes (Cabinet, Topic Database, Spin filter sheet)
 * pass their banner ink + frosted category glass; pages (Settings hub,
 * Topic History) pass nothing and get the theme's own surface + ink.
 *
 * Compact magnifier + live query + one-tap clear. The placeholder text is
 * supplied by the caller; it sits behind the field and shows only while the
 * query is empty.
 *
 * @param ink hero callers pass their banner ink so the icon / placeholder /
 *   text / cursor / hairline all resolve theme-aware (the light twin on the
 *   dark frosted glass at night); null → theme onSurface (v100 — the
 *   standard theme text color, crisp on any glass).
 * @param fill the frosted container (heroes pass `lerp(bannerFill, White,
 *   0.30)`); null → theme surfaceContainerLow.
 */
@Composable
fun CurioSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    // v61 — optional text-style override so callers can tune the type while
    // the shared default stays unchanged.
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    ink: Color? = null,
    fill: Color? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions(onSearch = {})
) {
    // v100 — the standard theme TEXT color (not the muted onSurfaceVariant):
    // every search bar in the app reads crisp on its fill.
    val resolvedInk = ink ?: MaterialTheme.colorScheme.onSurface
    val resolvedFill = fill ?: MaterialTheme.colorScheme.surfaceContainerLow
    val pillShape = RoundedCornerShape(50)
    Surface(
        shape = pillShape,
        color = resolvedFill,
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            // v90 — the One UI hairline: a subtle ink outline so the search
            // bar reads as a defined field (not a floating chip), resolving
            // to the light twin on the dark frosted glass at night.
            .border(1.dp, resolvedInk.copy(alpha = 0.35f), pillShape)
            // v28 — dark mode elevation visibility (glow + hairline).
            // v81 — dark: the One UI shiny glass edge on the search pill.
            .curioDarkGlow(3.dp, pillShape)
            .curioGlassEdge(pillShape)
    ) {
        Row(
            modifier = Modifier
                // v90 — fixed 46dp height — the same as the hero action
                // pills, so every search bar in the app is ONE size.
                .heightIn(min = 46.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CurioIcon(
                name = CurioIcons.Search,
                contentDescription = null,
                tint = resolvedInk,
                size = 20.dp
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle.copy(color = resolvedInk.copy(alpha = 0.7f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = textStyle.copy(color = resolvedInk),
                    cursorBrush = SolidColor(resolvedInk),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
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
                        tint = resolvedInk.copy(alpha = 0.85f),
                        size = 18.dp
                    )
                }
            }
        }
    }
}
