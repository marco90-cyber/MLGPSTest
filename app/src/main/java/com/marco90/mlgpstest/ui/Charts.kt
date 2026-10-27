package com.marco90.mlgpstest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marco90.mlgpstest.data.Constellations
import com.marco90.mlgpstest.data.GnssSat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Colori dei pannelli "strumento", sempre scuri come nell'app originale. */
val PanelBg = Color(0xFF0B1030)
val PanelText = Color(0xFFFFFFFF)

val SnrRed = Color(0xFFFF1744)
val SnrOrange = Color(0xFFFF9100)
val SnrYellow = Color(0xFFFFEA00)
val SnrLime = Color(0xFFAEEA00)
val SnrGreen = Color(0xFF00E676)

fun cn0Color(v: Float): Color = when {
    v < 10f -> SnrRed
    v < 20f -> SnrOrange
    v < 30f -> SnrYellow
    v < 50f -> SnrLime
    else -> SnrGreen
}

private fun polygonPath(c: Offset, r: Float, sides: Int, startDeg: Float): Path {
    val p = Path()
    for (i in 0 until sides) {
        val a = (startDeg + 360f * i / sides) * (PI.toFloat() / 180f)
        val x = c.x + r * cos(a)
        val y = c.y + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

private fun starPath(c: Offset, r: Float): Path {
    val p = Path()
    val points = 8
    for (i in 0 until points) {
        val rad = if (i % 2 == 0) r else r * 0.38f
        val a = (-90f + 360f * i / points) * (PI.toFloat() / 180f)
        val x = c.x + rad * cos(a)
        val y = c.y + rad * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

/**
 * Forma per costellazione: GPS cerchio, GLONASS triangolo, Galileo pentagono,
 * BeiDou stella, QZSS quadrato, NavIC rombo, SBAS esagono.
 */
fun DrawScope.drawSatShape(constellation: Int, c: Offset, r: Float, color: Color) {
    when (constellation) {
        Constellations.GPS -> drawCircle(color, r, c)
        Constellations.GLONASS -> drawPath(polygonPath(c, r * 1.15f, 3, -90f), color)
        Constellations.GALILEO -> drawPath(polygonPath(c, r * 1.1f, 5, -90f), color)
        Constellations.BEIDOU -> drawPath(starPath(c, r * 1.25f), color)
        Constellations.QZSS -> drawPath(polygonPath(c, r * 1.2f, 4, 45f), color)
        Constellations.NAVIC -> drawPath(polygonPath(c, r * 1.2f, 4, -90f), color)
        Constellations.SBAS -> drawPath(polygonPath(c, r * 1.1f, 6, 0f), color)
        else -> drawCircle(color, r, c)
    }
}

/** Grafico a barre del C/N0 (dB-Hz) di ogni satellite. */
@Composable
fun Cn0Chart(sats: List<GnssSat>, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val sorted = remember(sats) {
        sats.sortedWith(compareByDescending<GnssSat> { it.used }.thenByDescending { it.cn0 })
    }
    val barSlot = 40.dp
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier) {
        val needed = barSlot * sorted.size
        val total = if (needed > maxWidth) needed else maxWidth
        Box(Modifier.horizontalScroll(scroll)) {
            Canvas(Modifier.width(total).height(250.dp)) {
                val top = 24.dp.toPx()
                val labelH = 46.dp.toPx()
                val chartH = size.height - labelH - top
                val maxV = 60f
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                val small = TextStyle(color = PanelText, fontSize = 11.sp)

                for (v in listOf(20f, 40f, 60f)) {
                    val y = top + chartH * (1f - v / maxV)
                    drawLine(
                        Color(0x66FFFFFF), Offset(0f, y), Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(), pathEffect = dash,
                    )
                    drawText(tm, v.toInt().toString(), Offset(4.dp.toPx(), y - 15.dp.toPx()), small)
                }
                val baseY = top + chartH
                drawLine(PanelText, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.5.dp.toPx())

                val slot = barSlot.toPx()
                sorted.forEachIndexed { i, s ->
                    val cx = slot * (i + 0.5f)
                    val barH = if (s.cn0 <= 0f) 2.dp.toPx() else chartH * (s.cn0.coerceIn(0f, maxV) / maxV)
                    drawRoundRect(
                        color = if (s.cn0 <= 0f) SnrRed else cn0Color(s.cn0),
                        topLeft = Offset(cx - 13.dp.toPx(), baseY - barH),
                        size = Size(26.dp.toPx(), barH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    drawSatShape(s.constellation, Offset(cx, baseY + 11.dp.toPx()), 4.5.dp.toPx(), PanelText)

                    val label = String.format("%02d", s.svid)
                    val style = TextStyle(
                        color = if (s.used) Color.Black else PanelText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    val layout = tm.measure(label, style)
                    val bw = 32.dp.toPx()
                    val bh = 22.dp.toPx()
                    val bx = cx - bw / 2
                    val by = baseY + 20.dp.toPx()
                    if (s.used) {
                        drawRoundRect(PanelText, Offset(bx, by), Size(bw, bh), CornerRadius(7.dp.toPx()))
                    } else {
                        drawRoundRect(
                            PanelText, Offset(bx, by), Size(bw, bh), CornerRadius(7.dp.toPx()),
                            style = Stroke(1.2.dp.toPx()),
                        )
                    }
                    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, by + (bh - layout.size.height) / 2f))
                }
            }
        }
    }
}

/** Skyplot: centro = zenit, bordo esterno = orizzonte, nord in alto. */
@Composable
fun SkyPlot(sats: List<GnssSat>, modifier: Modifier = Modifier, detailed: Boolean = true, heading: Float? = null) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * (if (detailed) 0.92f else 0.96f)
        val center = Offset(cx, cy)
        val line = Color(0x66FFFFFF)
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        val h = heading ?: 0f

        drawCircle(PanelBg, radius, center)
        drawCircle(PanelText, radius, center, style = Stroke(if (detailed) 1.5.dp.toPx() else 1.dp.toPx()))

        // anelli di elevazione: 30° e 60°
        for (el in listOf(30f, 60f)) {
            drawCircle(line, radius * (1f - el / 90f), center, style = Stroke(1.dp.toPx(), pathEffect = dash))
        }
        // raggi ogni 30°
        for (az in 0 until 360 step 30) {
            val a = (az - h) * PI.toFloat() / 180f
            drawLine(
                line, center,
                Offset(cx + radius * sin(a), cy - radius * cos(a)),
                strokeWidth = 1.dp.toPx(), pathEffect = dash,
            )
        }

        if (detailed) {
            val tStyle = TextStyle(color = PanelText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            val marks = listOf("N" to 0f, "E" to 90f, "S" to 180f, "O" to 270f)
            for ((txt, az) in marks) {
                val a = (az - h) * PI.toFloat() / 180f
                val layout = tm.measure(txt, tStyle)
                val rr = radius - 14.dp.toPx()
                drawText(
                    layout,
                    topLeft = Offset(
                        cx + rr * sin(a) - layout.size.width / 2f,
                        cy - rr * cos(a) - layout.size.height / 2f,
                    ),
                )
            }
        }

        if (heading != null) {
            val m = if (detailed) 1f else 0.6f
            val tri = Path().apply {
                moveTo(cx, cy - radius + 2.dp.toPx() * m)
                lineTo(cx - 8.dp.toPx() * m, cy - radius - 12.dp.toPx() * m)
                lineTo(cx + 8.dp.toPx() * m, cy - radius - 12.dp.toPx() * m)
                close()
            }
            drawPath(tri, SnrYellow)
        }

        val shapeR = if (detailed) 8.dp.toPx() else 3.dp.toPx()
        for (s in sats) {
            val rr = radius * (1f - s.elevation.coerceIn(0f, 90f) / 90f)
            val a = (s.azimuth - h) * PI.toFloat() / 180f
            val pos = Offset(cx + rr * sin(a), cy - rr * cos(a))
            val color = if (s.cn0 <= 0f) SnrRed else cn0Color(s.cn0)
            drawSatShape(s.constellation, pos, shapeR, color)
            if (s.used) {
                drawCircle(PanelText, shapeR * 1.6f, pos, style = Stroke(if (detailed) 1.5.dp.toPx() else 1.dp.toPx()))
            }
            if (detailed) {
                val label = String.format("%02d", s.svid)
                val style = TextStyle(
                    color = if (s.used) Color.Black else PanelText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                val layout = tm.measure(label, style)
                val bw = layout.size.width + 10.dp.toPx()
                val bh = layout.size.height + 2.dp.toPx()
                val bx = pos.x - bw / 2f
                val by = pos.y + shapeR + 3.dp.toPx()
                if (s.used) {
                    drawRoundRect(PanelText, Offset(bx, by), Size(bw, bh), CornerRadius(6.dp.toPx()))
                } else {
                    drawRoundRect(
                        PanelText, Offset(bx, by), Size(bw, bh), CornerRadius(6.dp.toPx()),
                        style = Stroke(1.dp.toPx()),
                    )
                }
                drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, by + 1.dp.toPx()))
            }
        }
    }
}

/** Barra SNR medio con scala 0–99 e indicatore del valore. */
@Composable
fun SnrBar(avg: Float?, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        val leftPad = 52.dp.toPx()
        val rightPad = 12.dp.toPx()
        val barW = size.width - leftPad - rightPad
        val barTop = 40.dp.toPx()
        val barH = 14.dp.toPx()
        val maxV = 99f

        val segments = listOf(
            0f to 10f to SnrRed,
            10f to 20f to SnrOrange,
            20f to 30f to SnrYellow,
            30f to 50f to SnrLime,
            50f to 99f to SnrGreen,
        )
        for ((range, color) in segments) {
            val x0 = leftPad + barW * range.first / maxV
            val x1 = leftPad + barW * range.second / maxV
            drawRect(color, Offset(x0, barTop), Size(x1 - x0, barH))
        }

        val small = TextStyle(color = PanelText, fontSize = 12.sp)
        for (v in listOf(0, 10, 20, 30, 50, 99)) {
            val txt = String.format("%02d", v)
            val l = tm.measure(txt, small)
            val x = leftPad + barW * v / maxV
            drawText(l, topLeft = Offset((x - l.size.width / 2f).coerceIn(leftPad - 6.dp.toPx(), size.width - l.size.width.toFloat()), barTop + barH + 4.dp.toPx()))
        }

        val lbl = TextStyle(color = PanelText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        drawText(tm.measure("MEDIA", lbl), topLeft = Offset(4.dp.toPx(), barTop - 18.dp.toPx()))
        drawText(tm.measure("SNR", lbl), topLeft = Offset(4.dp.toPx(), barTop - 3.dp.toPx()))

        if (avg != null) {
            val x = leftPad + barW * avg.coerceIn(0f, maxV) / maxV
            val text = String.format("%.1f", avg)
            val l = tm.measure(text, TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            val bw = l.size.width + 18.dp.toPx()
            val bh = l.size.height + 6.dp.toPx()
            val bx = (x - bw / 2f).coerceIn(2.dp.toPx(), size.width - bw - 2.dp.toPx())
            val by = barTop - bh - 8.dp.toPx()
            drawRoundRect(SnrYellow, Offset(bx, by), Size(bw, bh), CornerRadius(10.dp.toPx()))
            val tri = Path().apply {
                moveTo(x - 6.dp.toPx(), by + bh - 1f)
                lineTo(x + 6.dp.toPx(), by + bh - 1f)
                lineTo(x, barTop - 1.dp.toPx())
                close()
            }
            drawPath(tri, SnrYellow)
            drawText(l, topLeft = Offset(bx + (bw - l.size.width) / 2f, by + 3.dp.toPx()))
        }
    }
}

/** Disegna una piccola bussola con ago, usata nel riquadro della dashboard. */
@Composable
fun CompassNeedle(heading: Float?, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = min(size.width, size.height) / 2f * 0.85f
        drawCircle(Color(0x55FFFFFF), r, c, style = Stroke(1.5.dp.toPx()))
        rotate(-(heading ?: 0f), pivot = c) {
            val north = Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x - r * 0.28f, c.y)
                lineTo(c.x + r * 0.28f, c.y)
                close()
            }
            val south = Path().apply {
                moveTo(c.x, c.y + r)
                lineTo(c.x - r * 0.28f, c.y)
                lineTo(c.x + r * 0.28f, c.y)
                close()
            }
            drawPath(north, Color(0xFFFF5252))
            drawPath(south, Color(0xFFEEEEEE))
        }
    }
}
