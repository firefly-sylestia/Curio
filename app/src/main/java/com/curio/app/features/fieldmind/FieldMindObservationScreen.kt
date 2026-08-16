package com.curio.app.features.fieldmind

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreAction
import com.curio.app.data.FieldMindMetadata
import com.curio.app.data.formatElapsed
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Curio's lightweight FieldMind observation session. It intentionally writes
 * a normal Curio Field Notes entry, with optional FieldMind provenance, so it
 * does not create a second database or alter existing capture flows.
 */
@Composable
fun FieldMindObservationScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var subject by rememberSaveable { mutableStateOf("") }
    var observed by rememberSaveable { mutableStateOf("") }
    var evidence by rememberSaveable { mutableStateOf("") }
    var contextNote by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var startedAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var endedAt by rememberSaveable { mutableLongStateOf(0L) }
    var elapsed by rememberSaveable { mutableLongStateOf(0L) }
    var active by rememberSaveable { mutableStateOf(true) }
    var saving by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(active, startedAt) {
        while (active) {
            elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
            delay(1_000)
        }
    }

    fun finishSession() {
        if (active) {
            endedAt = System.currentTimeMillis()
            elapsed = (endedAt - startedAt).coerceAtLeast(0L)
            active = false
        }
    }

    fun saveObservation() {
        if (saving) return
        finishSession()
        saving = true
        val savedAt = System.currentTimeMillis()
        val metadata = FieldMindMetadata(
            recordType = "observation",
            category = "FieldMind observation session",
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(savedAt)),
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(savedAt)),
            location = location.trim(),
            durationMs = elapsed,
            startedAt = startedAt,
            endedAt = endedAt.takeIf { it > 0L },
            status = "Active"
        )
        val name = subject.trim().ifBlank { "Field observation" }
        val topic = CurioTopic(
            id = "fieldmind-session-$savedAt",
            categoryId = CategoryId.WILDCARD,
            subtype = "FieldMind observation",
            name = name,
            teaser = "A timed observation captured in Curio",
            imageUrl = "",
            exploreAction = ExploreAction(
                verb = "Observe",
                targetName = name,
                durationMinutes = (elapsed / 60_000L).toInt().coerceAtLeast(1),
                instruction = "Continue observing and record what changes."
            ),
            byline = "FieldMind"
        )
        val entry = CurioEntry(
            id = "fieldmind-session-$savedAt",
            topic = topic,
            format = CaptureFormat.FieldNotes,
            captureData = CaptureData.FieldNotes(
                observed = observed.trim(),
                surprised = evidence.trim(),
                learnNext = contextNote.trim(),
                fieldMindMetadata = metadata
            ),
            capturedAtMillis = savedAt,
            tags = listOf("fieldmind", "observation-session") + location.trim().takeIf { it.isNotBlank() }.orEmpty()
        )
        scope.launch {
            runCatching { CurioRepositoryHolder.repo.save(entry) }
                .onSuccess { navController.popBackStack() }
                .onFailure { saving = false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CurioBackButton(onClick = { navController.popBackStack() })
            Column(modifier = Modifier.weight(1f)) {
                Text("FieldMind observation", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                Text("A focused field session, in Curio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CurioIcon(CurioIcons.ScienceGlyph, null, tint = MaterialTheme.colorScheme.primary, size = 28.dp)
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CurioIcon(if (active) CurioIcons.Timer else CurioIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 28.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (active) "Session in progress" else "Session complete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(formatElapsed(elapsed), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if (active) {
                    // v114 — app pill treatment (the stock M3 button with
                    // default 20dp corners + primaryContainer fill read as a
                    // leftover next to the custom pill family).
                    Button(
                        onClick = ::finishSession,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("Finish") }
                }
            }
        }

        Text("Observation notes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        SessionField("Subject or species", subject, { subject = it })
        SessionField("What did you observe?", observed, { observed = it }, minLines = 4)
        SessionField("Evidence or what surprised you", evidence, { evidence = it }, minLines = 3)
        SessionField("Context or what to learn next", contextNote, { contextNote = it }, minLines = 3)
        SessionField("Location (optional)", location, { location = it })

        Button(
            onClick = ::saveObservation,
            enabled = !saving && (observed.isNotBlank() || evidence.isNotBlank() || contextNote.isNotBlank()),
            // v114 — app button treatment: 24dp rounded + primary fill (the
            // old default M3 button read stock, and its primaryContainer fill
            // clashed with the white icon below).
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            CurioIcon(CurioIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimary, size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text(if (saving) "Saving…" else "Save observation", color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SessionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    )
}
