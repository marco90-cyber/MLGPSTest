package com.marco90.mlgpstest.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marco90.mlgpstest.data.AppSettings
import com.marco90.mlgpstest.data.GnssState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

@Composable
fun CompassScreen(state: GnssState, settings: AppSettings, magHeading: Float?) {
    var trueNorth by remember { mutableStateOf(true) }
    val fix = state.fix
    val declination: Float? = if (fix != null) {
        GeomagneticField(
            fix.lat.toFloat(), fix.lon.toFloat(), (fix.altitude ?: 0.0).toFloat(), System.currentTimeMillis(),
        ).declination
    } else {
        null
    }
    val usingTrue = trueNorth && declination != null
    val shown: Float? = magHeading?.let {
        if (usingTrue) ((it + declination!!) % 360f + 360f) % 360f else it
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PanelBg, contentColor = PanelText),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (shown != null) "${shown.toInt()}° ${cardinal(shown)}" else "Sensore non disponibile",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                )
                CompassDial(shown, Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = trueNorth,
                onClick = { trueNorth = true },
                label = { Text("Nord vero") },
                enabled = declination != null,
            )
            FilterChip(
                selected = !trueNorth || declination == null,
                onClick = { trueNorth = false },
                label = { Text("Nord magnetico") },
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Velocità", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (declination != null) String.format("Declinazione: %.1f° E", declination) else "Declinazione: —",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        fmtSpeedNumber(if (state.hasFix) state.fix?.speed else null, settings.speedUnit),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        settings.speedUnit.label,
                        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassDial(heading: Float?, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = min(size.width, size.height) / 2f * 0.94f
        val h = heading ?: 0f

        drawCircle(Color(0xFF141B45), r, c)
        drawCircle(PanelText, r, c, style = Stroke(2.dp.toPx()))

        rotate(-h, pivot = c) {
            for (deg in 0 until 360 step 10) {
                val a = deg * PI.toFloat() / 180f
                val major = deg % 30 == 0
                val len = if (major) 14.dp.toPx() else 7.dp.toPx()
                drawLine(
                    PanelText,
                    Offset(c.x + (r - len) * sin(a), c.y - (r - len) * cos(a)),
                    Offset(c.x + r * sin(a), c.y - r * cos(a)),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                )
            }
            val style = TextStyle(color = PanelText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            for ((txt, deg) in listOf("N" to 0f, "E" to 90f, "S" to 180f, "O" to 270f)) {
                val a = deg * PI.toFloat() / 180f
                val layout = tm.measure(txt, style.copy(color = if (txt == "N") Color(0xFFFF5252) else PanelText))
                val rr = r - 38.dp.toPx()
                // il testo ruota insieme al quadrante, mantenendosi leggibile verso l'esterno
                rotate(deg, pivot = Offset(c.x + rr * sin(a), c.y - rr * cos(a))) {
                    drawText(
                        layout,
                        topLeft = Offset(
                            c.x + rr * sin(a) - layout.size.width / 2f,
                            c.y - rr * cos(a) - layout.size.height / 2f,
                        ),
                    )
                }
            }
        }

        // indicatore fisso in alto (direzione del telefono)
        val tri = Path().apply {
            moveTo(c.x, c.y - r + 2.dp.toPx())
            lineTo(c.x - 9.dp.toPx(), c.y - r - 14.dp.toPx())
            lineTo(c.x + 9.dp.toPx(), c.y - r - 14.dp.toPx())
            close()
        }
        drawPath(tri, Color(0xFFFFEA00))

        // ago centrale
        val needleN = Path().apply {
            moveTo(c.x, c.y - r * 0.55f)
            lineTo(c.x - 10.dp.toPx(), c.y)
            lineTo(c.x + 10.dp.toPx(), c.y)
            close()
        }
        val needleS = Path().apply {
            moveTo(c.x, c.y + r * 0.55f)
            lineTo(c.x - 10.dp.toPx(), c.y)
            lineTo(c.x + 10.dp.toPx(), c.y)
            close()
        }
        rotate(-h, pivot = c) {
            drawPath(needleN, Color(0xFFFF5252))
            drawPath(needleS, Color(0xFFEEEEEE))
        }
        drawCircle(Color(0xFF141B45), 5.dp.toPx(), c)
    }
}
