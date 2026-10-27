package com.marco90.mlgpstest.ui

import com.marco90.mlgpstest.data.CoordFormat
import com.marco90.mlgpstest.data.SpeedUnit
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

const val DASH = "—"

fun fmtCoord(value: Double, isLat: Boolean, format: CoordFormat): String {
    val loc = Locale.getDefault()
    return if (format == CoordFormat.DECIMAL) {
        String.format(loc, "%.6f°", value)
    } else {
        val a = abs(value)
        val d = floor(a).toInt()
        val mFull = (a - d) * 60.0
        val m = floor(mFull).toInt()
        val s = (mFull - m) * 60.0
        val hemi = if (isLat) (if (value >= 0) "N" else "S") else (if (value >= 0) "E" else "O")
        String.format(loc, "%d° %d′ %.2f″ %s", d, m, s, hemi)
    }
}

fun fmtMeters(v: Double?, decimals: Int = 1): String =
    if (v == null) DASH else String.format(Locale.getDefault(), "%.${decimals}f m", v)

fun fmtSpeed(mps: Float?, unit: SpeedUnit): String =
    if (mps == null) DASH else String.format(Locale.getDefault(), "%.1f %s", mps * unit.factor, unit.label)

fun fmtSpeedNumber(mps: Float?, unit: SpeedUnit): String =
    if (mps == null) "0" else String.format(Locale.getDefault(), "%.0f", mps * unit.factor)

fun fmtTime(ms: Long?): String =
    if (ms == null || ms == 0L) DASH else DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(ms))

fun fmtTtff(ms: Long?): String {
    if (ms == null) return "00:00"
    val total = ms / 1000
    return String.format(Locale.getDefault(), "%02d:%02d", total / 60, total % 60)
}

fun fmtTtffLong(ms: Long?): String =
    if (ms == null) DASH else String.format(Locale.getDefault(), "%.1f s", ms / 1000.0)

fun fmtNum(v: Double?, decimals: Int = 1): String =
    if (v == null) DASH else String.format(Locale.getDefault(), "%.${decimals}f", v)

fun cardinal(deg: Float): String {
    val names = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val idx = (((deg % 360f) + 360f) % 360f / 45f + 0.5f).toInt() % 8
    return names[idx]
}
