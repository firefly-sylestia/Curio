package com.curio.app.features.incursion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.IncursionStore
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioCardShadow
import com.curio.app.ui.theme.curioTintOn
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.delay

/**
 * THE ANNOUNCEMENT — the one time Incursion says it exists.
 *
 * The phrase is typed in some search field somewhere in the app, and the page it
 * opens is on Home, which may be three screens away. Without this the member
 * would type an easter egg, watch the field behave normally, and never know
 * anything happened. So the unlock is announced once, over whatever they were
 * looking at, and then gets out of the way — on a tap, or by itself after a few
 * seconds.
 *
 * It is composed at the ROOT (see `MainActivity`), not inside the search field:
 * a lock that announces itself inside a 42dp capsule could not say anything
 * legible, and this is the app's one secret, so it gets the whole screen for two
 * seconds and then leaves.
 */
@Composable
fun IncursionUnlockReveal() {
    val open = IncursionStore.justUnlocked
    // The countdown runs only while it is showing, and dismissing it early
    // cancels the countdown with it (the effect is keyed on `open`, so it
    // restarts cleanly the next time — and only the next time — the phrase is
    // accepted on this device).
    LaunchedEffect(open) {
        if (!open) return@LaunchedEffect
        delay(3200)
        IncursionStore.consumeUnlockReveal()
    }
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(280))
    ) {
        val accent = settingsRoseAccent()
        val dark = isCurioDarkTheme()
        // A slow breath behind the mark, so the reveal is alive rather than a
        // static card. One animation, three seconds — nothing to distract from
        // the words.
        val breath = rememberInfiniteTransition(label = "incursion-breath")
        val pulse by breath.animateFloat(
            initialValue = 0.86f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
            label = "incursion-pulse"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable { IncursionStore.consumeUnlockReveal() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .scale(pulse)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.34f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            // v425 — IT IS A CARD, IN THE APP'S OWN INK. The reveal used to be
            // white words on a black scrim with a filled accent disc — which is
            // an app-store notice, not a Curio page, and the member read the
            // whole thing as one ("the activation overlay it shows upgrade it").
            // It is now the card the app uses everywhere (the card step with the
            // page's accent mixed into it, the soft shadow, no drawn edge), the
            // theme's own ink for its words, and the mark in a tinted disc — so a
            // secret reads as something Curio is telling you quietly.
            val cardShape = RoundedCornerShape(28.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 26.dp)
                    .fillMaxWidth()
                    .curioCardShadow(cardShape)
                    .clip(cardShape)
                    .background(
                        curioTintOn(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            accent,
                            if (dark) 0.14f else 0.08f
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(curioTintOn(MaterialTheme.colorScheme.surfaceContainerLow, accent, if (dark) 0.24f else 0.16f))
                ) {
                    CurioIcon(
                        name = CurioIcons.Hub,
                        contentDescription = null,
                        tint = accent,
                        size = 26.dp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "INCURSION",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The watching order is yours",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Marvel, Sony and X-Men, in the order they were meant to be seen. " +
                        "The door is on your Home page, whenever you want it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            "\u201Ci love you 3000\u201D",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "works in any search field",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * THE DOOR — Home's floating Incursion button.
 *
 * Absent until the phrase is typed, and absent from every other page: the whole
 * point is that a member who has not unlocked it never sees that it exists. Once
 * unlocked it rides above the writing "+" with the same slip-away-on-scroll
 * behaviour, so the floating furniture on Home stays one family.
 */
@Composable
fun IncursionHomeButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && IncursionStore.unlocked,
        enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.82f),
        exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.88f),
        modifier = modifier
    ) {
        val accent = settingsRoseAccent()
        Surface(
            shape = RoundedCornerShape(50),
            color = accent,
            shadowElevation = 6.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 13.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Hub,
                    contentDescription = null,
                    tint = Color.White,
                    size = 17.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Incursion",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
