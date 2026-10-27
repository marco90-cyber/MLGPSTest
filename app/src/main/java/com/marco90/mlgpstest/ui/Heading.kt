package com.marco90.mlgpstest.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.marco90.mlgpstest.data.FixInfo

/** Direzione magnetica del telefono in gradi (0 = nord), o null se manca il sensore. */
@Composable
fun rememberMagneticHeading(): State<Float?> {
    val ctx = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rot = FloatArray(9)
        val orient = FloatArray(3)
        var smooth: Float? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, event.values)
                SensorManager.getOrientation(rot, orient)
                var az = Math.toDegrees(orient[0].toDouble()).toFloat()
                if (az < 0f) az += 360f
                val prev = smooth
                val v = if (prev == null) {
                    az
                } else {
                    var d = az - prev
                    while (d > 180f) d -= 360f
                    while (d < -180f) d += 360f
                    (prev + d * 0.2f + 360f) % 360f
                }
                smooth = v
                heading.value = v
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return heading
}

/** Direzione rispetto al nord vero (se c'è una posizione per calcolare la declinazione), altrimenti magnetica. */
fun trueHeading(magnetic: Float?, fix: FixInfo?): Float? {
    if (magnetic == null) return null
    if (fix == null) return magnetic
    val decl = GeomagneticField(
        fix.lat.toFloat(), fix.lon.toFloat(), (fix.altitude ?: 0.0).toFloat(), System.currentTimeMillis(),
    ).declination
    return ((magnetic + decl) % 360f + 360f) % 360f
}
