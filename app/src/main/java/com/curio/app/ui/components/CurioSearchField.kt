package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v108 — the canonical frosted search-bar fill for the hero call sites:
 * LIGHT lifts the banner [backdrop] toward white (the bright frosted
 * glass); DARK swaps to the filter chips' near-black raised glass
 * (`lerp(surfaceContainerHigh, Black, 0.15)`) so every search bar matches
 * the app's chip family at night instead of a muddy mid-tone.
 */
@Composable
fun curioSearchFill(backdrop: Color): Color =
    if (isCurioDarkTheme()) {
        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
    } else {
        lerp(backdrop, Color.White, 0.30f)
    }

/**
 * THE canonical Curio search box.
 *
 * v251 — iOS STYLE: the One UI chrome (ink hairline, elevation shadow,
 * glass edge) is gone — the field is now a flat gray capsule (the
 * systemGray fill at ~12% in light / ~24% in dark), SF-gray magnifier,
 * soft placeholder, one-tap clear, and while the field is FOCUSED a
 * "Cancel" text button slides in from the right (fade + horizontal
 * expand) that dismisses the keyboard and clears the query — the UISearch
 * bar contract. Heroes still pass their banner ink + frosted glass via
 * [ink]/[fill]; pages pass nothing and get the theme-resolved iOS look.
 *
 * @param ink hero callers pass their banner ink so the icon / placeholder /
 *   text / cursor resolve theme-aware; null → theme onSurface.
 * @param fill the container (heroes pass [curioSearchFill]; null → the iOS
 *   system-gray capsule fill).
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
    val resolvedInk = ink ?: MaterialTheme.colorScheme.onSurface
    // v251 — iOS systemGray fill (secondarySystemFill): flat, borderless.
    val iosFill = if (isCurioDarkTheme()) {
        Color(0xFF767680).copy(alpha = 0.24f)
    } else {
        Color(0xFF767680).copy(alpha = 0.12f)
    }
    val resolvedFill = fill ?: iosFill
    val pillShape = RoundedCornerShape(50)

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    Surface(
        shape = pillShape,
        color = resolvedFill,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                // v251 — 42dp: the iOS search field's compact height.
                .heightIn(min = 42.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurioIcon(
                name = CurioIcons.Search,
                contentDescription = null,
                tint = resolvedInk.copy(alpha = 0.55f),
                size = 18.dp
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle.copy(color = resolvedInk.copy(alpha = 0.5f)),
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
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = CurioIcons.Close,
                        contentDescription = "Clear search",
                        tint = resolvedInk.copy(alpha = 0.55f),
                        size = 16.dp
                    )
                }
            }
            // v251 — iOS CANCEL: slides in only while the field is focused;
            // taps drop focus and clear the query (the UISearchBar contract).
            AnimatedVisibility(
                visible = focused,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onQueryChange("")
                            focusManager.clearFocus()
                        }
                        .padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 2.dp)
                )
            }
        }
    }
}
