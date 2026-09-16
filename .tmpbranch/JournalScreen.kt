package com.curio.app.features.journal

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Full-screen editorial journal editor; persistence is supplied by the caller. */
@Composable
fun JournalScreen(onBack: () -> Unit, onSave: (title: String, body: String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    val date = remember { SimpleDateFormat("EEEE · d MMMM yyyy", Locale.getDefault()).format(Date()) }
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = ink.copy(alpha = 0.46f)

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("‹", fontSize = 32.sp, color = ink, modifier = Modifier.padding(bottom = 3.dp)) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JOURNAL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = muted)
                Text("Writing space", style = MaterialTheme.typography.labelSmall, color = muted)
            }

            Box(
                Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { onSave(title.trim(), body.trim()) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp)) {
            Spacer(Modifier.height(26.dp))
            Text(date, style = MaterialTheme.typography.labelLarge, color = muted)
            Spacer(Modifier.height(20.dp))
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = TextStyle(color = ink, fontSize = 32.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text("A title for today...", style = TextStyle(color = muted, fontSize = 32.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold))
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(9.dp))
                Text("Take a breath. Then write.", style = MaterialTheme.typography.bodySmall, color = muted)
            }
            Spacer(Modifier.height(30.dp))
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                textStyle = TextStyle(color = ink, fontSize = 18.sp, lineHeight = 31.sp),
                modifier = Modifier.fillMaxWidth().height(620.dp),
                decorationBox = { inner ->
                    Box {
                        if (body.isEmpty()) Text(
                            "What happened today?\n\nWhat stayed with you?\n\nWhat are you feeling right now?\n\nThere is no right way to write this. Start anywhere.",
                            style = TextStyle(color = muted, fontSize = 18.sp, lineHeight = 31.sp)
                        )
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(48.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Your thoughts stay yours.", style = MaterialTheme.typography.labelMedium, color = muted)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
