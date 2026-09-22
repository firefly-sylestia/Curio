package com.curio.app.features.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioContentFilter
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
import com.curio.app.features.community.SocialConfirmDialog
import com.curio.app.features.settings.CurioTermsDialog
import com.curio.app.features.settings.settingsCardAccentInk
import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.components.AvatarCropDialog
import com.curio.app.ui.components.CurioMemberAvatar
import com.curio.app.ui.components.hasOwnPicture
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioPillTintLift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * v444 — EDIT PROFILE, AS A PAGE.
 *
 * The identity editor used to be an `AlertDialog` on the profile page: a
 * scrolling column inside a 94%-of-window plate, holding the photo, the two
 * text fields, the whole account card and the privacy door. It worked, and it
 * read like a form in a box.
 *
 * This is the member's own specification of it as a SCREEN — editorial, calm,
 * minimal, tactile: plain theme background (no gradients, no glass, no
 * decorative cards), a small way back over a large quiet title, one supporting
 * sentence, the picture centred with a small camera disc on its corner and
 * exactly two compact actions beneath, then OPEN FIELDS with a hairline under
 * them instead of boxes inside boxes, one flat ACCOUNT section, ONE tappable
 * privacy row, and Cancel / Save changes held at the foot. Type is one
 * typeface with a calm hierarchy (nothing is every-heading-bold), and the
 * spacing is the spec's 8dp base — important things get more SPACE rather than
 * another card.
 *
 * It is reached from every "Edit profile" door on the profile page (the torn
 * hero, the glass bar's action pill and the compact identity bar) through
 * [CurioRoutes.PROFILE_EDIT]; Cancel simply leaves, and Save writes the name and
 * the bio locally (mirrored to the account best-effort, so an offline save never
 * blocks) and CLAIMS a changed @handle — the one thing the server is the only
 * one able to answer for.
 *
 * What the old dialog could do, this page still does: pick a photo (with the
 * crop editor), wear the blob instead, remove the photo (the ⋯ in the expanded
 * picture), review the terms, open Privacy, and sign out. Nothing was dropped
 * in the move — see the v444 section of `app/AGENTS.md`.
 */
@Composable
fun ProfileEditScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // ── WHAT THE MEMBER IS EDITING ────────────────────────────────────────
    // Every value is read ONCE, when the page opens: this is a draft, and
    // Cancel is simply leaving without writing it.
    var name by remember { mutableStateOf(AppPreferences.getDisplayName(context)) }
    var bio by remember { mutableStateOf(AppPreferences.getCustomStreakTagline(context)) }
    var avatarPath by remember { mutableStateOf(AppPreferences.getProfileAvatarPath(context)) }
    var wearingBlob by remember { mutableStateOf(AppPreferences.profileAvatarBlobState) }
    // The editable source for the crop editor — non-null while it is open.
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }
    // The picture, expanded over the page (with the ⋯ that removes it).
    var previewing by remember { mutableStateOf(false) }
    // The terms dialog, and whether the save is WAITING on it.
    var termsOpen by remember { mutableStateOf(false) }
    var pendingClaim by remember { mutableStateOf(false) }
    // Read once and kept in step: the line under the handle flips the moment
    // the dialog accepts, without a second look at the store.
    var termsAccepted by remember { mutableStateOf(AppPreferences.hasAcceptedCurrentTerms(context)) }
    var confirmingSignOut by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // The account, so the page knows whether the ACCOUNT section is an identity
    // or one calm row. Restored here too: this page can be the first thing
    // opened after a cold start.
    val account = OnlineAccount.state
    val token = account.session?.accessToken
    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    // ── THE HANDLE ────────────────────────────────────────────────────────
    // Claimed against the SERVER's rule, checked as it is typed. `savedHandle`
    // is what the server last confirmed, so "is this a change?" is answered
    // against what is actually stored rather than against the field.
    var savedHandle by remember { mutableStateOf(AppPreferences.getUsername(context)) }
    var handle by remember { mutableStateOf(savedHandle) }
    var handleAnswer by remember { mutableStateOf<String?>(null) }
    var handleFailed by remember { mutableStateOf(false) }
    val cleanHandle = handle.trim().removePrefix("@").lowercase()
    val handleProblem = when {
        cleanHandle.isEmpty() -> null
        !cleanHandle.matches(Regex("[a-z0-9_]{3,24}")) ->
            "Usernames use 3 to 24 letters, numbers or underscores."
        CurioContentFilter.problem(cleanHandle) != null -> CurioContentFilter.problem(cleanHandle)
        else -> null
    }
    val handleChanged = cleanHandle != savedHandle.trim().removePrefix("@").lowercase()

    // ── THE PICTURE ───────────────────────────────────────────────────────
    // Declared BEFORE the picker: a local function cannot be reached from a
    // lambda that is built above it (the picker's callback calls this).
    fun saveAvatar(source: Bitmap, cropRect: IntRect?) {
        val cropped = if (cropRect == null) {
            centerSquareCrop(source)
        } else {
            val r = cropRect
            val clamped = android.graphics.Rect(
                r.left.coerceIn(0, source.width), r.top.coerceIn(0, source.height),
                r.right.coerceIn(0, source.width), r.bottom.coerceIn(0, source.height)
            )
            if (clamped.width() > 0 && clamped.height() > 0) {
                centerSquareCrop(
                    Bitmap.createBitmap(source, clamped.left, clamped.top, clamped.width(), clamped.height())
                )
            } else centerSquareCrop(source)
        }
        val avatar = scaleToMax(cropped, 512)
        val ts = System.currentTimeMillis()
        val srcFile = File(context.filesDir, "profile_avatar_src_$ts.png")
        val avatarFile = File(context.filesDir, "profile_avatar_$ts.png")
        runCatching { srcFile.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        runCatching { avatarFile.outputStream().use { avatar.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        // Replace any previous avatar/source files (the fresh names keep the
        // remember(path) bitmap caches re-keyed).
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("profile_avatar_") && it != srcFile && it != avatarFile }
            ?.forEach { it.delete() }
        avatarPath = if (avatarFile.exists() && avatarFile.length() > 0L) avatarFile.absolutePath else ""
        AppPreferences.setProfileAvatarPath(context, avatarPath)
    }
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val src = withContext(Dispatchers.IO) { decodeAvatarSource(context, uri) }
            // The pick does NOT apply immediately: the crop editor frames the
            // square first, so Apply saves and cancelling discards the pick.
            if (src != null) cropSource = src
        }
    }
    fun pickPhoto() = avatarPicker.launch("image/*")
    fun removePhoto() {
        avatarPath.takeIf { it.isNotBlank() }?.let { runCatching { File(it).delete() } }
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("profile_avatar_") }
            ?.forEach { it.delete() }
        avatarPath = ""
        cropSource = null
        AppPreferences.setProfileAvatarPath(context, "")
    }
    // Wearing the blob is its own answer — the photo on this device is left
    // exactly where it is, so switching back puts it straight on again.
    fun wearBlob(next: Boolean) {
        AppPreferences.setProfileAvatarBlob(context, next)
        wearingBlob = next
    }

    // ── SAVING ────────────────────────────────────────────────────────────
    fun answer(text: String, failed: Boolean) {
        handleAnswer = text
        handleFailed = failed
    }

    /** Writes the name and the bio: local first (the app moves at once), the
     *  account mirrored best-effort — an offline save is never blocked. */
    fun writeIdentity() {
        val nextName = name.trim().ifBlank { "Curious Explorer" }
        AppPreferences.setDisplayName(context, nextName)
        AppPreferences.setCustomStreakTagline(context, bio)
        val active = token ?: return
        scope.launch {
            runCatching { SocialApi.updateDisplayName(active, nextName) }
            runCatching { SocialApi.updateBio(active, bio) }
        }
    }

    /** Claims the changed @handle. The ONLY part of this page that has to wait
     *  on the server, so it is the only part that holds the save open. */
    fun claim(active: String) {
        saving = true
        handleAnswer = null
        handleFailed = false
        scope.launch {
            SocialApi.updateUsername(active, cleanHandle).fold(
                onSuccess = {
                    AppPreferences.setUsername(context, cleanHandle)
                    savedHandle = cleanHandle
                    saving = false
                    navController.popBackStack()
                },
                onFailure = { failure ->
                    saving = false
                    answer(failure.message ?: "Couldn't save that username. Try again.", true)
                }
            )
        }
    }

    /** Save changes. Nothing to claim (or nothing changed) leaves straight
     *  away; a changed handle is validated, then claimed — and a refusal is
     *  reported on the page rather than swallowed. */
    fun commit() {
        if (saving) return
        writeIdentity()
        if (!handleChanged) {
            navController.popBackStack()
            return
        }
        val problem = handleProblem
        if (problem != null) {
            answer(problem, true)
            return
        }
        if (cleanHandle.isEmpty()) {
            answer("Choose a username first.", true)
            return
        }
        val active = token
        if (active == null) {
            answer("Sign in to claim a username.", true)
            return
        }
        if (!AppPreferences.hasAcceptedCurrentTerms(context)) {
            // The terms are the account's, not the field's: the dialog accepts
            // them, and the claim finishes the moment it does.
            pendingClaim = true
            termsOpen = true
            return
        }
        claim(active)
    }

    // The expanded picture closes on back before the page does.
    BackHandler(enabled = previewing) { previewing = false }

    val accent = settingsRoseAccent()
    val accentInk = settingsReadableInk(accent)
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val edge = wideContentEdgePadding()
    val initial = name.trim().firstOrNull()?.uppercase().orEmpty()

    // v446 — THE PAGE WEARS THE READER'S FLOATING PILLS.
    //
    // The member: *"in edit profile make the edit profile be that floating header
    // pill style lke pdf raeder, and same floating cancel and save pill"*. So the
    // head is the reader's own object — a 50dp way-back circle and a capsule
    // carrying the page's name, both lifted 10dp off the paper — and the foot is
    // two floating pills of the same height rather than a band across the bottom.
    // The type stays the spec's (quiet, one face, nothing every-heading-bold).
    val pillTitleStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
    )
    val leadStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
    val valueStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // The page BLURS behind the expanded picture, so the picture reads as
        // lifted off it rather than pasted over it.
        val veil by animateDpAsState(
            targetValue = if (previewing) 22.dp else 0.dp,
            animationSpec = tween(CurioMotion.Durations.Push),
            label = "profileEditVeil"
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The veil is a REAL blur, so it is applied only while it is up:
                // a permanent `blur(0.dp)` would force this whole page (a
                // scrolling column of text fields) through an offscreen layer
                // on every frame for a picture nobody has opened.
                .then(if (veil.value > 0f) Modifier.blur(veil) else Modifier)
                .statusBarsPadding()
                .imePadding()
        ) {
            // ── THE HEAD — a floating pill row, the reader's own language. ──
            //
            // Out is its own circle, the page's name keeps the middle, and both
            // hover clear of the paper on the same lift, so this page reads as a
            // reading surface rather than a settings form.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = edge, end = edge, top = EditSpace.XS)
            ) {
                Surface(
                    onClick = { navController.popBackStack() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.ArrowBack,
                            "Go back",
                            tint = ink.copy(alpha = 0.85f),
                            size = 21.dp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Edit profile", style = pillTitleStyle, color = ink)
                    }
                }
            }
            Spacer(Modifier.height(EditSpace.M))

            // ── THE BODY — one column, generous gaps, no boxes in boxes. ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = edge),
                verticalArrangement = Arrangement.spacedBy(EditSpace.XL)
            ) {
                // The spec's one supporting sentence, kept as a quiet line above
                // the picture now that the name lives in the pill.
                Text("Your identity.", style = leadStyle, color = muted)

                // ── The picture ──
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(curioPillTintLift())
                                .clickable(onClickLabel = "View your picture") {
                                    focusManager.clearFocus()
                                    previewing = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasOwnPicture(avatarPath)) {
                                CurioMemberAvatar(avatarPath, Modifier.fillMaxSize())
                            } else {
                                Text(
                                    initial,
                                    style = MaterialTheme.typography.headlineMedium
                                        .copy(fontSize = 34.sp, fontWeight = FontWeight.Medium),
                                    color = muted
                                )
                            }
                        }
                        // The small camera, overlapping the picture's corner.
                        Surface(
                            onClick = { pickPhoto() },
                            shape = CircleShape,
                            color = accent,
                            contentColor = accentInk,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CurioIcon(
                                    name = CurioIcons.Screenshot,
                                    contentDescription = "Add a photo",
                                    size = 17.dp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(EditSpace.S))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EditQuietAction(
                            label = if (avatarPath.isNotBlank()) "Change photo" else "Add photo",
                            onClick = { pickPhoto() }
                        )
                        EditQuietAction(
                            label = if (wearingBlob) "Use my photo" else "Use my blob",
                            onClick = { wearBlob(!wearingBlob) }
                        )
                    }
                }

                // ── Personal information ──
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(EditSpace.M)
                ) {
                    EditSectionHeading("Personal information")
                    EditOpenField(
                        label = "Name",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Your name",
                        style = valueStyle,
                        cursorColor = settingsCardAccentInk(),
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    )
                    EditOpenField(
                        label = "Bio",
                        value = bio,
                        onValueChange = { bio = it },
                        placeholder = "Keep the spark going today.",
                        style = valueStyle,
                        cursorColor = settingsCardAccentInk(),
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences,
                        onDone = { focusManager.clearFocus() }
                    )
                }

                // ── Account ──
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(EditSpace.S)
                ) {
                    EditSectionHeading(
                        "Account",
                        badge = if (account.signedIn) "Curio" else "Signed out",
                        badgeTint = if (account.signedIn) settingsCardAccentInk() else null
                    )
                    if (account.signedIn) {
                        val email = account.email
                        if (!email.isNullOrBlank()) {
                            EditFlatValue(label = "Email", value = email, locked = true)
                        }
                        EditOpenField(
                            label = "Username",
                            value = handle,
                            onValueChange = {
                                handle = it.removePrefix("@")
                                handleAnswer = null
                                handleFailed = false
                            },
                            placeholder = "username",
                            style = valueStyle,
                            cursorColor = settingsCardAccentInk(),
                            prefix = "@",
                            enabled = !saving,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Ascii,
                            capitalization = KeyboardCapitalization.None,
                            onDone = { focusManager.clearFocus() }
                        )
                        // One line under the field, in every state that has
                        // something to say: the rule being broken, the server's
                        // verdict, or why a claim would be refused.
                        val notice: String? = when {
                            handleProblem != null -> handleProblem
                            handleAnswer != null -> handleAnswer
                            token == null -> "Sign in to claim a username."
                            else -> null
                        }
                        val noticeFailed = handleProblem != null || handleFailed
                        if (notice != null) {
                            EditBadge(
                                text = notice,
                                tint = if (noticeFailed) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    settingsCardAccentInk()
                                },
                                uppercase = false
                            )
                        }
                        // The terms, as a supporting line — never another card.
                        Text(
                            text = if (termsAccepted) {
                                "Terms and data disclosures accepted \u00b7 View"
                            } else {
                                "Review Curio's Terms and data disclosures"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = settingsCardAccentInk(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { termsOpen = true }
                        )
                    } else {
                        // Signed out: ONE calm row. The form itself lives on the
                        // account's own page, where it can be as long as it needs
                        // to be.
                        Surface(
                            onClick = {
                                navController.navigate(CurioRoutes.SETTINGS_ONLINE) {
                                    launchSingleTop = true
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = EditSpace.S, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Person,
                                    contentDescription = null,
                                    tint = muted,
                                    size = 18.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Sign in to Curio",
                                    style = MaterialTheme.typography.bodyMedium
                                        .copy(fontWeight = FontWeight.Medium),
                                    color = ink,
                                    modifier = Modifier.weight(1f)
                                )
                                CurioIcon(
                                    name = CurioIcons.ChevronRight,
                                    contentDescription = null,
                                    tint = muted,
                                    size = 18.dp
                                )
                            }
                        }
                    }
                }

                // ── Privacy — one row, entirely tappable. ──
                Column(Modifier.fillMaxWidth()) {
                    EditRule()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Open privacy settings") {
                                navController.navigate(CurioRoutes.SETTINGS_PRIVACY) {
                                    launchSingleTop = true
                                }
                            }
                            .padding(vertical = EditSpace.S),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(curioPillTintLift()),
                            contentAlignment = Alignment.Center
                        ) {
                            CurioIcon(
                                name = CurioIcons.Favorite,
                                contentDescription = null,
                                tint = settingsCardAccentInk(),
                                size = 18.dp
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Privacy", style = valueStyle, color = ink)
                            Text(
                                "Control who can see your profile and activity.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                color = muted
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        CurioIcon(
                            name = CurioIcons.ChevronRight,
                            contentDescription = null,
                            tint = muted,
                            size = 20.dp
                        )
                    }
                    EditRule()
                }

                // ── Sign out — belongs where the account is edited. ──
                if (account.signedIn) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Sign out of Curio") {
                                    confirmingSignOut = true
                                }
                                .padding(vertical = EditSpace.S),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurioIcon(
                                name = CurioIcons.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                size = 18.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Sign out of Curio",
                                style = valueStyle,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        EditRule()
                    }
                }

                Spacer(Modifier.height(EditSpace.S))
            }

            // ── THE FOOT — quiet Cancel and solid berry Save, as FLYING PILLS. ──
            //
            // Same 50dp height, same 50% radius roundness and the same lift the
            // head wears, so leaving and saving belong to the page's own object
            // family instead of a band across its bottom.
            //
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edge)
                    .padding(top = EditSpace.S, bottom = EditSpace.S)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = ink,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.bodyMedium
                                .copy(fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
                Surface(
                    onClick = { commit() },
                    enabled = !saving,
                    shape = RoundedCornerShape(50),
                    color = accent,
                    contentColor = accentInk,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (saving) {
                                CircularProgressIndicator(
                                    color = accentInk,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (saving) "Saving\u2026" else "Save changes",
                                style = MaterialTheme.typography.bodyMedium
                                    .copy(fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }

        // ── THE PICTURE, EXPANDED ─────────────────────────────────────────────
        // Tapping the picture opens it over the (blurred) page; the ⋯ in the
        // corner is where the photo is removed. It closes on a tap anywhere and
        // on back.
        AnimatedVisibility(
            visible = previewing,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(animationSpec = tween(CurioMotion.Durations.Quick)) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(CurioMotion.Durations.Push, easing = FastOutSlowInEasing)
                ),
            exit = fadeOut(animationSpec = tween(CurioMotion.Durations.Quick))
        ) {
            var menuOpen by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f))
                    .clickable(onClickLabel = "Close") { previewing = false }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(264.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasOwnPicture(avatarPath)) {
                        CurioMemberAvatar(avatarPath, Modifier.fillMaxSize())
                    } else {
                        Text(
                            initial,
                            style = MaterialTheme.typography.headlineMedium
                                .copy(fontSize = 96.sp, fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(EditSpace.S)
                ) {
                    Surface(
                        onClick = { menuOpen = true },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CurioIcon(
                                name = CurioIcons.MoreVert,
                                contentDescription = "Picture options",
                                size = 20.dp
                            )
                        }
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (avatarPath.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Remove photo") },
                                leadingIcon = {
                                    CurioIcon(name = CurioIcons.Delete, contentDescription = null, size = 18.dp)
                                },
                                onClick = {
                                    menuOpen = false
                                    previewing = false
                                    removePhoto()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (wearingBlob) "Use my photo" else "Use my blob") },
                            leadingIcon = {
                                CurioIcon(name = CurioIcons.Person, contentDescription = null, size = 18.dp)
                            },
                            onClick = {
                                menuOpen = false
                                wearBlob(!wearingBlob)
                            }
                        )
                    }
                }
            }
        }
    }

    // The crop editor, on top of everything else.
    cropSource?.let { src ->
        AvatarCropDialog(
            bitmap = src,
            onConfirm = { rect ->
                cropSource?.let { source -> saveAvatar(source, rect) }
                cropSource = null
            },
            onDismiss = { cropSource = null }
        )
    }

    if (termsOpen) {
        CurioTermsDialog(
            onDismiss = {
                termsOpen = false
                pendingClaim = false
            },
            onAccept = {
                AppPreferences.acceptCurrentTerms(context)
                termsAccepted = true
                termsOpen = false
                val active = token
                if (pendingClaim && active != null) claim(active)
                pendingClaim = false
            }
        )
    }

    if (confirmingSignOut) {
        SocialConfirmDialog(
            title = "Sign out of Curio?",
            body = "Online mode turns off and nothing online loads until you sign in again. " +
                "Your captures, recordings and conversations stay on this device.",
            confirmLabel = "Sign out",
            busy = signingOut,
            onDismiss = { if (!signingOut) confirmingSignOut = false },
            onConfirm = {
                signingOut = true
                scope.launch {
                    OnlineAccount.signOut(context)
                    signingOut = false
                    confirmingSignOut = false
                }
            }
        )
    }
}

// ─── The page's ruler ───────────────────────────────────────────────────────
// The spec's 8dp base. Nothing in this file measures itself with a number
// that is not one of these.
private object EditSpace {
    /** 8dp — inside a state, between a label and what it labels. */
    val XS = 8.dp
    /** 16dp — between a label and its field, between two actions. */
    val S = 16.dp
    /** 24dp — inside a section. */
    val M = 24.dp
    /** 32dp — head to body, title to its lead. */
    val L = 32.dp
    /** 40dp — between SECTIONS: important things get space, not cards. */
    val XL = 40.dp
}

/** The hairline that does a box's job: one pixel of the page's own ink, at
 *  1dp, under an open field (the spec's "extremely subtle border"). */
@Composable
private fun EditRule() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    )
}

/** A section's own name: small, spaced, quiet. Never bold — the spec's
 *  "don't make every heading bold; Curio should feel quiet". */
@Composable
private fun EditSectionHeading(
    text: String,
    badge: String? = null,
    badgeTint: Color? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            EditBadge(badge, badgeTint)
        }
    }
}

/**
 * A SMALL BADGE — the member's *"giving some items proer badge colors etc"*.
 *
 * One object for every state a line can be in: a tint at 12% as the fill and the
 * tint itself for the words, so a "Taken" handle, a locked email and a claimed
 * username are told apart at a glance without any of them shouting. The text is
 * uppercased for a LABEL (a section's badge); a state line passes
 * [uppercase] = false, because a sentence has no business being shouted.
 */
@Composable
private fun EditBadge(
    text: String,
    tint: Color? = null,
    uppercase: Boolean = true
) {
    val base = tint ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = base.copy(alpha = 0.12f),
        contentColor = base
    ) {
        Text(
            text = if (uppercase) text.uppercase() else text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** The field's own name — NAME, BIO, USERNAME, EMAIL. */
@Composable
private fun EditFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    )
}

/**
 * AN OPEN FIELD — the value sits on the page itself with one hairline under it,
 * rather than inside a box inside a card (the spec's own instruction).
 *
 * [prefix] draws a leading "@" for the handle; the placeholder appears only
 * while the field is empty, so an untouched word never reads as a hint that
 * cannot be typed over.
 */
@Composable
private fun EditOpenField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    cursorColor: Color,
    imeAction: ImeAction = ImeAction.Next,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    prefix: String? = null,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        EditFieldLabel(label)
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = style.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(cursorColor),
            keyboardOptions = KeyboardOptions(
                imeAction = imeAction,
                keyboardType = keyboardType,
                capitalization = capitalization
            ),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (prefix != null) {
                        Text(
                            prefix,
                            style = style,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = style.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.65f)
                                )
                            )
                        }
                        inner()
                    }
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        EditRule()
    }
}

/** A value that cannot be typed over — the account's own email. Muted, with
 *  the glyph that says why (the spec's "slightly muted/locked if it isn't
 *  editable"). */
@Composable
private fun EditFlatValue(label: String, value: String, locked: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        EditFieldLabel(label)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                color = if (locked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            if (locked) {
                // The lock is a BADGE now, not a bare glyph beside a value: the
                // member asked for proper badge colours, and "Locked" says why
                // where a padlock alone only says so to the eye that knows.
                Spacer(Modifier.width(8.dp))
                EditBadge("Locked")
            }
        }
        Spacer(Modifier.height(12.dp))
        EditRule()
    }
}

/** One of the two compact actions under the picture — quiet, same height,
 *  same radius, never a filled pill (the picture's camera disc is the solid
 *  one). */
@Composable
private fun EditQuietAction(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

// ── Avatar image pipeline (moved here from ProfileScreen in v444) ───────────
// Pick → decode (EXIF-rotated + bounded) → CENTER-SQUARE auto-crop (portrait
// photos fill the square avatar instead of squishing) → save BOTH the square
// avatar and the editable source beside it, so the crop editor can re-frame
// the original photo. The manual crop hands back a source-pixel [IntRect].

/** Decodes a picked image EXIF-correctly and bounded (never full-size, so a
 *  40MP camera photo can't OOM the decode). Uses BitmapFactory for EVERY
 *  API level: it never applies EXIF orientation itself (documented), so the
 *  framework [ExifInterface] rotation below is applied identically on all
 *  devices — ImageDecoder's EXIF behavior varies across Android versions
 *  (auto-apply on some, not on others) and has no public toggle, so a
 *  single deterministic path avoids both double-rotation and compile/API
 *  availability risk. */
private fun decodeAvatarSource(context: Context, uri: Uri): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2048) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: return@runCatching null
    // Framework ExifInterface (API 24+) handles content:// URIs — rotate to
    // upright before any cropping so the square comes from the RIGHT photo.
    val rotation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    if (rotation == ExifInterface.ORIENTATION_NORMAL) return@runCatching decoded
    val matrix = Matrix().apply {
        when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(270f)
                postScale(-1f, 1f)
            }
        }
    }
    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}.getOrNull()

/** The default avatar crop — the largest centered square (auto-crop from
 *  the MIDDLE, so tall portrait photos fill the square avatar instead of
 *  squishing). */
private fun centerSquareCrop(source: Bitmap): Bitmap {
    val size = min(source.width, source.height)
    val left = (source.width - size) / 2
    val top = (source.height - size) / 2
    return Bitmap.createBitmap(source, left, top, size, size)
}

/** Downscales to at most [maxSide] pixels on the long side (the avatar is
 *  stored at 512px — small, fast to reload, plenty for a circle). */
private fun scaleToMax(bitmap: Bitmap, maxSide: Int): Bitmap {
    if (bitmap.width <= maxSide && bitmap.height <= maxSide) return bitmap
    val scale = maxSide.toFloat() / max(bitmap.width, bitmap.height)
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).roundToInt(),
        (bitmap.height * scale).roundToInt(),
        true
    )
}
