package com.curio.app.features.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/** Neutral Material treatment for the category picker. Category colors are accents only. */
internal object PickerNeutralVisuals {
    @Composable
    fun surface(): Color = MaterialTheme.colorScheme.surfaceContainerLow

    @Composable
    fun container(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

    @Composable
    fun selectedContainer(): Color = MaterialTheme.colorScheme.secondaryContainer

    @Composable
    fun iconContainer(selected: Boolean): Color = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    @Composable
    fun iconInk(selected: Boolean): Color = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    fun noCategoryBorder(): Modifier = Modifier

    @Composable
    fun selectedBorder(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
}

internal fun Modifier.pickerSelectedState(selected: Boolean): Modifier = if (selected) {
    border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp)
    )
} else this
