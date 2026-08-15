package com.curio.app.features.bugreport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.BuildConfig
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * Curio bug report screen — simple, clean form for sending feedback.
 *
 * Users describe an issue, optionally attach crash logs, and tap the button
 * to open a pre-filled GitHub issue in the browser. The report is also
 * copied to the clipboard as a safety net.
 */
@Composable
fun BugReportScreen(navController: NavController) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var includeCrashLog by remember { mutableStateOf(false) }
    val crashHistory = remember { CurioCrashReporter.getCrashHistory(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurioBackButton(onClick = { navController.popBackStack() })
            Text(
                text = "Report a bug",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Briefly describe what happened") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Steps to reproduce, what you expected, what happened instead") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
            )

            // Include crash logs toggle
            if (crashHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Include crash logs",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${crashHistory.size} crash report(s) available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeCrashLog,
                        onCheckedChange = { includeCrashLog = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CurioColors.CoralBlush,
                            checkedTrackColor = CurioColors.CoralBlush.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Device info card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Device info (included in report)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Submit button — opens GitHub Issues with the report pre-filled.
            Button(
                onClick = {
                    openGitHubReport(context, title, description, includeCrashLog, crashHistory)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    // v81 — dark: the deep rose fill + bright light twin ink
                    // (the pale coral fill would glare on the black page).
                    containerColor = if (isCurioDarkTheme()) CurioColors.HomeRosewoodDark else CurioColors.CoralBlush,
                    contentColor = if (isCurioDarkTheme()) CurioColors.CoralBlush else CurioColors.DeepPlum
                ),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    text = "Report on GitHub",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(
                text = "Opens GitHub Issues with your report pre-filled. Just tap Submit. The report is also copied to your clipboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun openGitHubReport(
    context: Context,
    title: String,
    description: String,
    includeCrashLog: Boolean,
    crashHistory: List<String>
) {
    val body = buildString {
        appendLine(description)
        appendLine()
        appendLine("---")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Curio: ${BuildConfig.VERSION_NAME}")

        if (includeCrashLog && crashHistory.isNotEmpty()) {
            appendLine()
            appendLine("### Crash History")
            crashHistory.take(3).forEachIndexed { i, log ->
                appendLine()
                appendLine("**Crash ${i + 1}:**")
                appendLine("```")
                appendLine(log.take(2000))
                appendLine("```")
            }
        }
    }

    // Copy to clipboard — a safety net in case the browser flow is skipped.
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("Curio Bug Report", body))

    // GitHub pre-fills a new issue from the title/body query parameters, so
    // the user only has to review and tap "Submit new issue". GitHub caps
    // issue titles at 256 characters.
    val uri = Uri.parse("https://github.com/firefly-sylestia/Curio/issues/new")
        .buildUpon()
        .appendQueryParameter("title", title.take(256))
        .appendQueryParameter("body", body)
        .build()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    // Launch directly (no resolveActivity guard): on Android 11+ package
    // visibility, resolveActivity returns null for OTHER apps unless a
    // <queries> declaration exists, which would silently skip the browser
    // and leave the user thinking the button only copies. startActivity
    // resolves at the system level and works regardless; runCatching just
    // protects the rare no-browser device (the clipboard copy above is the
    // safety net either way).
    runCatching { context.startActivity(intent) }
}
