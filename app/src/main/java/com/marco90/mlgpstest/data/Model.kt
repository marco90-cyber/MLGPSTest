package com.marco90.mlgpstest.data

import kotlin.math.abs

/** Un satellite (o meglio una coppia satellite + frequenza) riportato da GnssStatus. */
data class GnssSat(
    val svid: Int,
    val constellation: Int,
    val cn0: Float,
    val elevation: Float,
    val azimuth: Float,
    val hasAlmanac: Boolean,
    val hasEphemeris: Boolean,
    val used: Boolean,
    val carrierHz: Float?,
)

/** Ultima posizione ricevuta. */
data class FixInfo(
    val lat: Double,
    val lon: Double,
    val altitude: Double?,
    val speed: Float?,
    val speedAcc: Float?,
    val bearing: Float?,
    val bearingAcc: Float?,
    val accuracy: Float?,
    val timeMs: Long,
)

data class GnssState(
    val hasPermission: Boolean = false,
    val providerEnabled: Boolean = true,
    val hasFix: Boolean = false,
    val fix: FixInfo? = null,
    val sats: List<GnssSat> = emptyList(),
    val ttffMs: Long? = null,
    val pdop: Double? = null,
    val hdop: Double? = null,
    val vdop: Double? = null,
    val fixType: Int = 0, // 1 = nessun fix, 2 = 2D, 3 = 3D (da NMEA GSA)
    val mslAltitude: Double? = null,
    val geoidSeparation: Double? = null,
) {
    val inView: Int get() = sats.size
    val withSignal: Int get() = sats.count { it.cn0 > 0f }
    val inUse: Int get() = sats.count { it.used }
    val sbasCount: Int get() = sats.count { it.constellation == Constellations.SBAS }
    val avgSnr: Float?
        get() {
            val v = sats.filter { it.cn0 > 0f }
            return if (v.isEmpty()) null else v.map { it.cn0 }.average().toFloat()
        }
}

object Constellations {
    const val GPS = 1
    const val SBAS = 2
    const val GLONASS = 3
    const val QZSS = 4
    const val BEIDOU = 5
    const val GALILEO = 6
    const val NAVIC = 7

    /** Ordine di visualizzazione nelle tabelle. */
    val order = listOf(GPS, GLONASS, GALILEO, BEIDOU, QZSS, NAVIC, SBAS)

    fun name(c: Int) = when (c) {
        GPS -> "GPS"
        SBAS -> "SBAS"
        GLONASS -> "GLONASS"
        QZSS -> "QZSS"
        BEIDOU -> "BeiDou"
        GALILEO -> "Galileo"
        NAVIC -> "NavIC"
        else -> "Sconosciuto"
    }

    fun flag(c: Int) = when (c) {
        GPS -> "🇺🇸"      // US
        SBAS -> "🛰️"           // satellite
        GLONASS -> "🇷🇺"  // RU
        QZSS -> "🇯🇵"     // JP
        BEIDOU -> "🇨🇳"   // CN
        GALILEO -> "🇪🇺"  // EU
        NAVIC -> "🇮🇳"    // IN
        else -> "?"
    }

    fun orderIndex(c: Int): Int {
        val i = order.indexOf(c)
        return if (i < 0) 99 else i
    }
}

/** Etichetta della banda (L1, E1, B1I...) a partire dalla frequenza portante. */
fun carrierLabel(constellation: Int, hz: Float?): String {
    if (hz == null) {
        return when (constellation) {
            Constellations.BEIDOU -> "B1I"
            Constellations.GALILEO -> "E1"
            else -> "L1"
        }
    }
    val mhz = hz / 1_000_000f
    fun near(v: Float, tol: Float = 3f) = abs(mhz - v) < tol
    return when {
        near(1575.42f) -> when (constellation) {
            Constellations.GALILEO -> "E1"
            Constellations.BEIDOU -> "B1C"
            else -> "L1"
        }
        near(1176.45f) -> when (constellation) {
            Constellations.GALILEO -> "E5a"
            Constellations.BEIDOU -> "B2a"
            else -> "L5"
        }
        near(1227.60f) -> "L2"
        near(1561.098f) -> "B1I"
        near(1207.14f) -> if (constellation == Constellations.GALILEO) "E5b" else "B2I"
        near(1191.795f) -> "E5"
        near(1268.52f) -> "B3I"
        mhz in 1598f..1610f -> "G1"
        mhz in 1242f..1252f -> "G2"
        near(2492.028f) -> "S"
        else -> "${mhz.toInt()} MHz"
    }
}
