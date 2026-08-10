package com.curio.app.ui.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor

/**
 * A cozy one-line speech bubble with a soft curved tail pointing at the pet
 * (spec §10.7: one sentence max for passive bubbles). Readable by screen
 * readers — the text is a real Text node.
 *
 * v9.x — redesigned: the hard diamond tail is now a smooth curved pointer
 * with a rounded tip, and the bubble wears squishier corners (a slightly
 * tighter corner on the tail side reads as a classic speech bubble instead
 * of a plain rounded card).
 */
@Composable
fun PetSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tailOnLeft: Boolean = true,
    // v9.x — the tour can pass a taller/wider cap so its dialogue is never
    // clipped at the passive two-line limit; reactions keep the cozy default.
    maxLines: Int = 2,
    maxWidth: Dp = 260.dp
) {
    val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (tailOnLeft) 8.dp else 20.dp,
        bottomEnd = if (tailOnLeft) 20.dp else 8.dp
    )
    Row(
        modifier = modifier.widthIn(max = maxWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (tailOnLeft) Arrangement.Start else Arrangement.End
    ) {
        // The tail always sits on the PET's side (the bubble's edge nearest
        // the sprite): before the bubble when tailOnLeft, after it
        // otherwise. It is shifted with Modifier.offset (a translation,
        // which may be negative) so it straddles the bubble's edge and
        // reads as a tail pointing at the pet — padding cannot be negative,
        // so it is never used for this.
        if (tailOnLeft) {
            Spacer(Modifier.width(2.dp))
            Box(Modifier.offset(x = 5.dp)) {
                CurvedBubbleTail(bubbleColor, pointingLeft = true)
            }
        }
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 0.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        if (!tailOnLeft) {
            Box(Modifier.offset(x = -5.dp)) {
                CurvedBubbleTail(bubbleColor, pointingLeft = false)
            }
            Spacer(Modifier.width(2.dp))
        }
    }
}

/**
 * The bubble's tail — a soft curved pointer with a rounded tip, drawn in
 * the bubble fill. [pointingLeft] aims it at the pet on the left side; the
 * base flares back into the bubble so the seam reads as one shape.
 */
@Composable
private fun CurvedBubbleTail(color: Color, pointingLeft: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 16.dp, height = 14.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                if (pointingLeft) {
                    // Rooted at the bubble's left edge, tapering to a
                    // rounded tip that points at the pet.
                    moveTo(w, h * 0.22f)
                    quadraticBezierTo(w * 0.35f, h * 0.20f, 0f, h * 0.5f)
                    quadraticBezierTo(w * 0.35f, h * 0.80f, w, h * 0.78f)
                } else {
                    moveTo(0f, h * 0.22f)
                    quadraticBezierTo(w * 0.65f, h * 0.20f, w, h * 0.5f)
                    quadraticBezierTo(w * 0.65f, h * 0.80f, 0f, h * 0.78f)
                }
                close()
            }
            drawPath(path, color)
        }
    }
}

/**
 * The Quests hero — the pet companion with its speech bubble, level +
 * growth line, and the XP bar (spec §10.3 + §4.1). The pet is tappable:
 * tapping opens a check-in dialog with its mood, next quest and growth
 * status.
 *
 * v8.10 — one fixed color scheme (the Curio light-theme brand coral): the
 * round XP ring around the bed was removed, and the bar/tints no longer
 * react to category pastels or dark mode. Tapping the bed while the pet is
 * sitting at home ([CurioPet.atHome]) brings it back out. v8.21 — opening
 * the check-in no longer hides the floating pet: it stays visible, dimmed
 * behind the dialog scrim.
 *
 * [bubbleText] is the one-shot dialogue line (null = stay quiet).
 * [celebrateKey] increments to trigger a celebration hop.
 */
@Composable
fun CurioPetHeroCard(
    bubbleText: String?,
    onGo: (String) -> Unit = {},
    celebrateKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // One fixed accent for the whole pet area — the Curio brand coral.
    val accent = CurioColors.CategoryCoral
    val lanes = CurioQuests.categoriesState
    val stage = CurioPet.currentStage()
    val xp = CurioQuests.xpState
    val level = CurioQuests.levelForXp(xp)
    val (progress, _) = CurioQuests.xpProgress(xp)
    val current = CurioQuests.currentQuest()
    var showDialog by remember { mutableStateOf(false) }
    // v8.8 — bumps the pet's hop when it wakes from the flower bed.
    var wakeCelebrate by remember { mutableIntStateOf(0) }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // The pet's flower bed (v8.8) — the pet naps here when the
                // app opens and stays asleep until tapped. Once awake the bed
                // sits vacant while the pet floats around the app (or shows
                // the pet sitting at home when [CurioPet.atHome]). v8.10 — the
                // round XP ring was removed (the bar below already shows
                // progress).
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (!CurioPet.awake)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else accent.copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CurioPetHome(
                        petInside = !CurioPet.awake ||
                            CurioPet.atHome ||
                            !AppPreferences.floatingPetEnabledState,
                        sleeping = !CurioPet.awake,
                        homeSize = 74.dp,
                        celebrateKey = celebrateKey + wakeCelebrate,
                        onTap = {
                            when {
                                !CurioPet.awake -> {
                                    CurioPet.wake()
                                    wakeCelebrate++
                                }
                                CurioPet.atHome -> {
                                    CurioPet.comeOut()
                                    wakeCelebrate++
                                }
                                else -> {
                                    showDialog = true
                                }
                            }
                        },
                        contentDescription = when {
                            !CurioPet.awake -> "${stage.displayName} asleep in its flower bed. Tap to wake"
                            CurioPet.atHome -> "${stage.displayName} sitting in its flower bed. Tap to come out"
                            else -> "${stage.displayName}'s flower bed. Tap to check in"
                        }
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // The pet only chats when it's awake — a sleeping pet
                    // stays quiet in its bed.
                    if (bubbleText != null && CurioPet.awake) {
                        PetSpeechBubble(text = bubbleText, tailOnLeft = true)
                    }
                    Text(
                        "Level $level · ${stage.displayName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$xp XP · ${CurioPet.nextStageHint(stage)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            // A quiet hint of what's next (the Recommended card owns the big
            // CTA, so the hero stays companion-focused).
            current?.let { quest ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Next up: ${quest.title} · +${quest.xpReward} XP",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // ── Pet check-in dialog — tap the pet to see its mood + next steps ──
    // v8.21 — the floating pet stays visible behind the scrim (dimmed),
    // never fully hidden.
    if (showDialog) {
        val info = CurioPet.tapInfo(context, lanes)
        val questRoute = CurioQuests.currentQuest()?.navRoute
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = {
                showDialog = false
            },
            title = { Text(info.stage.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CurioPetSprite(
                            stage = info.stage,
                            mood = info.mood,
                            spriteSize = 48.dp,
                            contentDescription = null
                        )
                        Text(
                            CurioPet.lineFor(context, info.mood, lanes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        "Personality: ${info.persona.displayName}, ${info.persona.tagline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Next growth: ${info.nextStageLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    info.nextQuestTitle?.let {
                        Text(
                            "Next quest: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // v8.43 — the pet's own developed sayings (learning brain).
                    if (info.coinedSayings > 0) {
                        Text(
                            "Its own sayings: ${info.coinedSayings}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                    },
                    colors = curioDialogActionButtonColors()
                ) { Text("Okay") }
            },
            dismissButton = {
                if (questRoute != null) {
                    TextButton(
                        onClick = {
                            showDialog = false
                            questRoute.let(onGo)
                        },
                        colors = curioDialogActionButtonColors()
                    ) { Text("Go to quest") }
                }
            }
        )
    }
}
