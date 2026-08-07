package com.curio.app.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests

/**
 * A cozy one-line speech bubble with a tail pointing at the pet (spec §10.7:
 * one sentence max for passive bubbles). Readable by screen readers — the
 * text is a real Text node.
 */
@Composable
fun PetSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tailOnLeft: Boolean = true
) {
    val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = modifier.widthIn(max = 260.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (tailOnLeft) Arrangement.Start else Arrangement.End
    ) {
        // The tail diamond always sits on the PET's side (the bubble's
        // edge nearest the sprite): before the bubble when tailOnLeft,
        // after it otherwise. Negative padding lets the diamond straddle
        // the bubble's edge so it reads as a tail pointing at the pet
        // (a 45°-rotated square is symmetric, so only the SIDE matters).
        if (tailOnLeft) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.padding(end = -4.dp)) {
                TailDiamond(bubbleColor)
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        if (!tailOnLeft) {
            Box(Modifier.padding(start = -4.dp)) {
                TailDiamond(bubbleColor)
            }
            Spacer(Modifier.width(6.dp))
        }
    }
}

/** The bubble's tail — a tiny rotated square wearing the bubble fill. */
@Composable
private fun TailDiamond(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .rotate(45f)
            .background(color, RoundedCornerShape(2.dp))
    )
}

/**
 * The Quests hero — the pet companion with its speech bubble, a soft XP
 * ring, level + growth line, and the XP bar (spec §10.3 + §4.1). The pet is
 * tappable: tapping opens a check-in dialog with its mood, next quest and
 * growth status.
 *
 * [accent] is the scarf/ring accent (usually the recommended lane's tint).
 * [bubbleText] is the one-shot dialogue line (null = stay quiet).
 * [celebrateKey] increments to trigger a celebration hop.
 */
@Composable
fun CurioPetHeroCard(
    accent: Color,
    bubbleText: String?,
    onGo: (String) -> Unit = {},
    celebrateKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lanes = CurioQuests.categoriesState
    val mood = CurioPet.mood(context, lanes)
    val stage = CurioPet.currentStage()
    val xp = CurioQuests.xpState
    val level = CurioQuests.levelForXp(xp)
    val (progress, _) = CurioQuests.xpProgress(xp)
    val current = CurioQuests.currentQuest()
    var showDialog by remember { mutableStateOf(false) }

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
                // Pet inside a soft XP ring.
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(96.dp),
                        strokeWidth = 3.dp,
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                if (mood == CurioPet.Mood.SLEEPY)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else accent.copy(alpha = 0.14f)
                            )
                            .clickable { showDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        CurioPetSprite(
                            stage = stage,
                            mood = mood,
                            accent = accent,
                            spriteSize = 64.dp,
                            celebrateKey = celebrateKey,
                            contentDescription = "${stage.displayName} — tap to check in"
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (bubbleText != null) {
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
    if (showDialog) {
        val info = CurioPet.tapInfo(context, lanes)
        val questRoute = CurioQuests.currentQuest()?.navRoute
        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                            accent = accent,
                            spriteSize = 48.dp,
                            contentDescription = null
                        )
                        Text(
                            CurioPet.lineFor(context, info.mood, lanes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Okay") }
            },
            dismissButton = {
                if (questRoute != null) {
                    TextButton(
                        onClick = {
                            showDialog = false
                            questRoute.let(onGo)
                        }
                    ) { Text("Go to quest") }
                }
            }
        )
    }
}
