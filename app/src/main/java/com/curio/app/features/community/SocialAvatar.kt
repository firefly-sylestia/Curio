package com.curio.app.features.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/** Code-drawn social portrait. No image data is uploaded or stored. */
@Composable
internal fun SocialAvatar(style: Int, avatarSize: Dp = 40.dp, onClick: (() -> Unit)? = null) {
    val colors = listOf(Color(0xFFE7706F), Color(0xFF5D8FEA), Color(0xFF8DBB6B), Color(0xFFE6A84F))
    val base = colors[style.coerceIn(0, 15) % colors.size]
    Box(
        modifier = Modifier.size(avatarSize).background(base, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(avatarSize * 0.72f)) {
            val c = size.minDimension
            val ink = Color(0xFFFDF5E8)
            when (style.coerceIn(0, 15) % 4) {
                0 -> { drawCircle(ink, c * .18f, Offset(c * .5f, c * .38f)); drawCircle(ink, c * .30f, Offset(c * .5f, c * .78f)) }
                1 -> { drawRect(ink, Offset(c*.24f,c*.22f), androidx.compose.ui.geometry.Size(c*.52f,c*.52f), style = Fill); drawCircle(base, c*.07f, Offset(c*.42f,c*.45f)); drawCircle(base, c*.07f, Offset(c*.58f,c*.45f)) }
                2 -> { drawCircle(ink, c*.30f, Offset(c*.5f,c*.5f)); drawCircle(base,c*.05f,Offset(c*.4f,c*.46f)); drawCircle(base,c*.05f,Offset(c*.6f,c*.46f)) }
                else -> { drawCircle(ink,c*.12f,Offset(c*.34f,c*.38f)); drawCircle(ink,c*.12f,Offset(c*.66f,c*.38f)); drawCircle(ink,c*.27f,Offset(c*.5f,c*.68f)) }
            }
        }
    }
}

@Composable
internal fun AvatarPickerIcon(style: Int, selected: Boolean, onClick: () -> Unit) {
    SocialAvatar(style, 38.dp, onClick)
    if (selected) CurioIcon(CurioIcons.Check, null, tint = MaterialTheme.colorScheme.onSurface, size = 12.dp)
}
