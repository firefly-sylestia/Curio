package com.curio.app.infrastructure

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * v27 — the system screen-capture consent host for the explore bubble's
 * screenshot button.
 *
 * MediaProjection consent MUST be requested from an Activity (the system
 * "start capturing?" dialog), but the floating bubble renders over other
 * apps with no Activity behind it. This transparent activity launches on
 * top of whatever the user is doing, shows the system consent dialog, and
 * — when granted — stashes the token on [ExploreSessionService] and asks
 * the (already running) explore service to capture one frame. It then
 * finishes itself so the user lands back in the app they were in.
 */
class ScreenCaptureRequestActivity : ComponentActivity() {

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // result.data is an Intent? — bind it to a local so the pair below
        // is Pair<Int, Intent> (smart-cast on the Java-backed property is
        // impossible, and Pair is invariant).
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            // Stash the granted token and let the explore service capture a
            // single frame (it is already foreground with the bubble, so the
            // capture runs without any background-start restriction).
            ExploreSessionService.captureConsent = result.resultCode to data
            ExploreSessionService.captureNow(this)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mpm = getSystemService(MediaProjectionManager::class.java)
        captureLauncher.launch(mpm.createScreenCaptureIntent())
    }
}
