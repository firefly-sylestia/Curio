package com.curio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User-facing master switch for Curio's experimental "alive" motion layer. */
object CurioAlivePreferences {
    private const val PREFS = "curio_prefs"
    private const val KEY = "curio_alive_motion"

    var enabledState by mutableStateOf(false)
        private set

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, enabled)
            .apply()
        enabledState = enabled
    }

    fun seed(context: Context) {
        enabledState = isEnabled(context)
    }
}
