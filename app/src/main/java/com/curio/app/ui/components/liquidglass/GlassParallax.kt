/*
 * Curio — v231 GLASS PARALLAX TILT (experiment, Experiments toggle, OFF).
 *
 * A tiny gravity-sensor listener that exposes the device's current tilt as
 * two normalized axes (-1..1). The liquid-glass capsules read these in
 * their graphicsLayer blocks and sway subtly AGAINST the tilt — the depth
 * cue iOS liquid glass uses, where the glass feels like a pane floating
 * above the content rather than painted onto it.
 */
package com.curio.app.ui.components.liquidglass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

object CurioGlassParallax {

    /** Normalized device tilt, -1..1 per axis (low-pass filtered). */
    var tiltX by mutableFloatStateOf(0f)
        private set
    var tiltY by mutableFloatStateOf(0f)
        private set

    private var manager: SensorManager? = null
    private var listening = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_GRAVITY) return
            val gx = event.values.getOrNull(0) ?: return
            val gy = event.values.getOrNull(1) ?: return
            // Normalize against ~9.8 m/s² full deflection, dead-zone the
            // flat-on-table case so a resting phone doesn't jitter, and
            // low-pass smooth the raw values (the sensor is noisy).
            val nx = ((gx / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f))
                .takeIf { abs(it) > 0.12f } ?: 0f
            val ny = ((gy / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f))
                .takeIf { abs(it) > 0.12f } ?: 0f
            tiltX = tiltX + (nx - tiltX) * 0.18f
            tiltY = tiltY + (ny - tiltY) * 0.18f
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** Start/stop the gravity listener. Safe to call repeatedly. */
    fun setEnabled(context: Context, enabled: Boolean) {
        val sm = context.applicationContext
            .getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (enabled && !listening) {
            val gravity = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            if (gravity != null && sm.registerListener(listener, gravity, SensorManager.SENSOR_DELAY_UI)) {
                manager = sm
                listening = true
            }
        } else if (!enabled && listening) {
            sm?.unregisterListener(listener)
            manager = null
            listening = false
            tiltX = 0f
            tiltY = 0f
        }
    }
}
