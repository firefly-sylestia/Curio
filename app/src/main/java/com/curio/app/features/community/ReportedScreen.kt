package com.curio.app.features.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.OnlineAccount

@Composable
fun ReportedScreen(navController: NavController) {
    val account = OnlineAccount.state
    val scope = rememberCoroutineScope()
    val token = account.session?.accessToken
    var reports by remember { mutableStateOf(emptyList<com.curio.app.data.supabase.CommunityReport>()) }
    var message by remember { mutableStateOf("Loading reports…") }

    LaunchedEffect(token) {
        val active = token ?: return@LaunchedEffect
        CommunityApi.isAdmin(active, account.session?.userId.orEmpty()).fold(
            onSuccess = { admin ->
                if (!admin) {
                    message = "This screen is for community admins."
                    return@fold
                }
                CommunityApi.reported(active).fold(
                    onSuccess = { reports = it; message = if (it.isEmpty()) "No reported posts." else "" },
                    onFailure = { message = it.message ?: "Couldn't load reports." }
                )
            },
            onFailure = { message = it.message ?: "Couldn't verify admin access." }
        )
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Reported", style = MaterialTheme.typography.headlineMedium)
        if (message.isNotBlank()) Text(message)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(reports, key = { it.id }) { report ->
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Post ${report.cardId}", style = MaterialTheme.typography.titleMedium)
                    Text("Reason: ${report.reason}")
                    report.note?.let { Text("Note: $it") }
                    Button(onClick = {
                        val active = token ?: return@Button
                        scope.launch {
                            CommunityApi.deleteAny(active, report.cardId).onSuccess {
                                reports = reports.filterNot { it.cardId == report.cardId }
                            }
                        }
                    }) { Text("Delete post") }
                }
            }
        }
    }
}
