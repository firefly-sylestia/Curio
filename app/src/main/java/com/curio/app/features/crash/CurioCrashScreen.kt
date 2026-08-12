package com.curio.app.features.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Intent
import androidx.navigation.NavController
import com.curio.app.MainActivity
import com.curio.app.navigation.CurioRoutes
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * Curio crash recovery screen — clean, calm, Compose-native.
 *
 * Shows a crash summary, recovery options, and lets the user
 * copy or share the crash log before restarting.
 */
@Composable
fun CurioCrashScreen(navController: NavController) {
    val context = LocalContext.current
    val crashLog = remember { CurioCrashReporter.getLastCrash(context) ?: "No crash log available." }
    // True when the crash-loop guard flipped: repeated crashes were detected
    // and the explore service + reminders were paused so the app could open.
    val safeMode = remember { CurioCrashReporter.isSafeMode(context) }
    var showFullLog by remember { mutableStateOf(false) }

    val cat = detectCategory(crashLog)
    val excName = extractException(crashLog)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Warning icon
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CurioIcon(
                    name = CurioIcons.ErrorOutline,
                    contentDescription = null,
                    tint = curioRoseInk(),
                    size = 36.dp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Curio stopped unexpectedly",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = cat.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        // Category chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cat.color.copy(alpha = 0.12f)
        ) {
            Text(
                text = "${cat.label} · $excName",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cat.color,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Crash-loop guard notice — repeated crashes paused the background
        // explore service + reminders so the app could open on this screen.
        if (safeMode) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Repeated crashes detected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Curio paused the explore timer, bubble and reminders so the app could open safely. " +
                            "The crash below is the latest one. Tap Restart Curio to start clean and continue normally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Recovery suggestion
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Recovery",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = cat.recovery,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                // Primary action — a single crash already runs in a fresh
                // process, so Home is clean; a crash LOOP restarts the whole
                // task so no stale state (or re-armed service) survives.
                Button(
                    onClick = {
                        if (CurioCrashReporter.isSafeMode(context)) {
                            CurioCrashReporter.resetLoopGuard(context)
                            val restart = Intent(context, MainActivity::class.java).apply {
                                addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }
                            context.startActivity(restart)
                            (context as? Activity)?.finishAffinity()
                        } else {
                            CurioCrashReporter.clearPendingCrash(context)
                            navController.navigate(CurioRoutes.HOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cat.color,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        text = "Restart Curio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Report actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showFullLog = !showFullLog },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (showFullLog) "Hide log" else "View log",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = {
                    val share = Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, crashLog)
                    }
                    context.startActivity(Intent.createChooser(share, "Share crash report"))
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Share", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Report the crash through the full bug-report form — one tap right
        // after a crash, so the user can file a GitHub issue with the saved
        // crash logs attached via the form's "Include crash logs" toggle.
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { navController.navigate(CurioRoutes.BUG_REPORT) { launchSingleTop = true } },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Report a bug", style = MaterialTheme.typography.labelLarge)
        }

        // Expandable log
        if (showFullLog) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = crashLog,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

private data class CrashCategory(
    val label: String, val description: String, val recovery: String, val color: Color
)

private fun detectCategory(log: String): CrashCategory {
    val l = log.lowercase()
    return when {
        l.contains("room") || l.contains("sqlite") || l.contains("database") || l.contains("cursor") ->
            CrashCategory("Storage", "The local database encountered an error.",
                "Your data is safe. A restart should resolve this.", curioSageInk())
        l.contains("compose") || l.contains("recompose") || l.contains("modifier") || l.contains("layout") ->
            CrashCategory("UI", "The interface hit a rendering error.",
                "A quick restart usually fixes this.", CurioColors.Lilac)
        l.contains("outofmemory") || l.contains("out of memory") ->
            CrashCategory("Memory", "The device ran low on memory.",
                "Close other apps and restart Curio.", CurioColors.Peach)
        l.contains("network") || l.contains("http") || l.contains("timeout") || l.contains("socket") ->
            CrashCategory("Network", "A network request failed unexpectedly.",
                "Check your connection and try again.", CurioColors.DustyBlue)
        else ->
            CrashCategory("Unknown", "Something went wrong unexpectedly.",
                "Restarting the app should resolve most issues.", CurioColors.CoralBlush)
    }
}

private fun extractException(log: String): String {
    for (line in log.lines()) {
        if (line.contains("Exception:")) {
            val p = line.substringAfter("Exception:").trim()
            return p.substringAfterLast(".").ifBlank { p }
        }
    }
    return "Unknown error"
}
