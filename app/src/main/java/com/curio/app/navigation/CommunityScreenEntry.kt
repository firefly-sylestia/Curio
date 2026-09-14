package com.curio.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.features.community.CommunityPostScreen
import com.curio.app.features.community.CommunityScreen as CommunityWallScreen
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionColor
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Navigation-layer entry point for Community.
 *
 * The wall remains the existing implementation. This wrapper owns the actual
 * creation destination so the Community route opens the dedicated full-screen
 * composer rather than the legacy bottom-sheet composer still embedded in the
 * wall implementation.
 */
@Composable
fun CommunityScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val account = OnlineAccount.state
    val token = account.session?.accessToken
    val onlineMode = AppPreferences.onlineModeEnabledState
    val asTab = AppPreferences.communityTabVisible
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(modifier = Modifier) {
        CommunityWallScreen(navController = navController)

        if (account.signedIn && onlineMode && token != null) {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                containerColor = curioDialogActionColor(),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = com.curio.app.ui.adaptive.wideContentEdgePadding(),
                        bottom = 20.dp + if (asTab) {
                            84.dp + WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                        } else 0.dp
                    )
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 18.dp
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 4.dp))
                Text("Post", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (creating && token != null) {
        CommunityPostScreen(
            onDismiss = { creating = false },
            onPost = { draft ->
                scope.launch {
                    CommunityApi.post(
                        accessToken = token,
                        draft = draft,
                        displayName = AppPreferences.getDisplayName(context),
                        userId = account.session?.userId
                    ).onSuccess {
                        creating = false
                    }.onFailure {
                        // CommunityPostScreen keeps its own posting state. On a
                        // failed request the destination stays open so the user
                        // can correct/retry instead of silently losing the draft.
                    }
                }
            }
        )
    }
}
