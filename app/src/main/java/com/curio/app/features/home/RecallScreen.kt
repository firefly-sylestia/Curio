package com.curio.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRecall
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * v502 — THE RETURN'S OWN SCREEN.
 *
 * The member: *"give it its own screen instead of abuttom sheet"*. The Return used
 * to be answered in a bottom sheet (v489's `RecallSheet`) and the days with
 * nothing due opened a second sheet for the ladder (`RecallAheadSheet`). Two
 * sheets for one subject, and the sheet answered the half of it that was least
 * interesting: a member who pressed the Return wanted to know what is coming back
 * and when, and that answer was behind a different door.
 *
 * It is one page now, and it holds both halves in the order they matter:
 *
 *  · **the answer** — the topic that is due, how long ago it was finished, the
 *    words from last time, one field. It never nags: the answer is kept even when
 *    it is empty ("I don't remember" is a real record and the honest one), and
 *    nothing here expires.
 *  · **the ladder** — every return that is waiting, soonest first, with how long
 *    ago each topic was finished and when it comes round. A member who has ever
 *    wondered whether the feature was doing anything can read the whole schedule
 *    instead of trusting that something is scheduled somewhere.
 *
 * A return never costs a streak and never expires; answering one pays the app's
 * largest recall award ([CurioRecall.XP_PER_RECALL]) because recall is the thing
 * Curio is for. **The writes happen here and nowhere else** — a composable must
 * never score.
 */
@Composable
fun RecallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val due = CurioRecall.dueState
    // Keyed on the topic, so answering one return and being handed the next does
    // not carry the previous answer's words into the new field.
    var answer by rememberSaveable(due?.topicName ?: "") { mutableStateOf("") }
    val upcoming = CurioRecall.upcomingState
    val waiting = CurioRecall.waitingState
    // The ladder is the screen's own list, so it is asked once on arrival too:
    // this page can be the first thing opened after a topic was finished.
    LaunchedEffect(Unit) { CurioRecall.refresh(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .widthIn(max = 680.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            // ── the header, in the page's own language ───────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    CurioBackButton(onClick = onBack)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "The Return",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = scheme.onSurface
                        )
                        Text(
                            if (waiting > 0) {
                                if (waiting == 1) "1 topic is waiting to come back"
                                else "$waiting topics are waiting to come back"
                            } else {
                                "Every finished topic comes back on its own schedule"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            // ── WHAT IS DUE, AND YOUR ANSWER ─────────────────────────────
            if (due != null) {
                Text(
                    "TO REMEMBER NOW",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = accent
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    due.topicName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = scheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "You finished this ${CurioRecall.agoText(due.firstCompletedAt)}. What do you still remember?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                if (due.answer.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    // The last return, given back quietly: the member's own words,
                    // quoted as words rather than dressed up as progress.
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "LAST TIME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = scheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                due.answer,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = scheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    placeholder = { Text("A line is enough") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        // The app's own two writes for a return: the award, then
                        // the step. Order matters — `answer` moves the ladder on,
                        // and the XP is for the act, not for the record.
                        CurioQuests.awardXpOnly(context, CurioRecall.XP_PER_RECALL)
                        CurioRecall.answer(context, due, answer)
                        answer = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        "Keep it",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            } else {
                Text(
                    "Nothing is due right now",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = scheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "A finished topic comes back on its own, and this page is where you can see when.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(26.dp))

            // ── AND THE WHOLE LADDER ─────────────────────────────────────
            Text(
                "WHAT COMES BACK",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Each return widens the next gap: a day, then three, then a week.",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (upcoming.isEmpty()) {
                Text(
                    "Nothing is on the ladder yet. A topic joins it the moment you finish it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcoming.forEach { recall ->
                        val isDue = recall.dueAt <= System.currentTimeMillis()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDue) {
                                scheme.surfaceContainerHighest
                            } else {
                                scheme.surfaceContainer
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                CurioIcon(
                                    CurioIcons.Replay,
                                    null,
                                    tint = if (isDue) accent else scheme.onSurfaceVariant,
                                    size = 18.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        recall.topicName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "finished ${CurioRecall.agoText(recall.firstCompletedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    CurioRecall.aheadText(recall.dueAt),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (isDue) accent else scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
