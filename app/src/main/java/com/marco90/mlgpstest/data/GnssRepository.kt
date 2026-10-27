package com.marco90.mlgpstest.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Raccoglie posizione, stato dei satelliti e messaggi NMEA dal GNSS del telefono
 * e li espone come un unico [GnssState].
 */
class GnssRepository(private val context: Context) {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(GnssState(hasPermission = hasPermission()))
    val state: StateFlow<GnssState> = _state.asStateFlow()

    /** Callback per la registrazione del log. */
    var onLocation: ((Location) -> Unit)? = null
    var onNmea: ((String) -> Unit)? = null
    var onSatellites: ((List<GnssSat>) -> Unit)? = null

    private var started = false
    private var lastFixElapsed = 0L

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun providerEnabled(): Boolean = try {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (e: Exception) {
        false
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastFixElapsed = SystemClock.elapsedRealtime()
            val fix = FixInfo(
                lat = location.latitude,
                lon = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                speed = if (location.hasSpeed()) location.speed else null,
                speedAcc = if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond else null,
                bearing = if (location.hasBearing()) location.bearing else null,
                bearingAcc = if (location.hasBearingAccuracy()) location.bearingAccuracyDegrees else null,
                accuracy = if (location.hasAccuracy()) location.accuracy else null,
                timeMs = location.time,
            )
            _state.update { it.copy(fix = fix, hasFix = true) }
            onLocation?.invoke(location)
        }

        override fun onProviderEnabled(provider: String) {
            _state.update { it.copy(providerEnabled = providerEnabled()) }
        }

        override fun onProviderDisabled(provider: String) {
            _state.update { it.copy(providerEnabled = providerEnabled(), hasFix = false) }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onFirstFix(ttffMillis: Int) {
            _state.update { it.copy(ttffMs = ttffMillis.toLong()) }
        }

        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val list = ArrayList<GnssSat>(status.satelliteCount)
            for (i in 0 until status.satelliteCount) {
                list.add(
                    GnssSat(
                        svid = status.getSvid(i),
                        constellation = status.getConstellationType(i),
                        cn0 = status.getCn0DbHz(i),
                        elevation = status.getElevationDegrees(i),
                        azimuth = status.getAzimuthDegrees(i),
                        hasAlmanac = status.hasAlmanacData(i),
                        hasEphemeris = status.hasEphemerisData(i),
                        used = status.usedInFix(i),
                        carrierHz = if (status.hasCarrierFrequencyHz(i)) status.getCarrierFrequencyHz(i) else null,
                    ),
                )
            }
            _state.update { it.copy(sats = list) }
            onSatellites?.invoke(list)
        }
    }

    private val nmeaListener = OnNmeaMessageListener { message, _ ->
        parseNmea(message)
        onNmea?.invoke(message)
    }

    private fun parseNmea(message: String) {
        val s = message.trim()
        if (s.length < 6 || s[0] != '$') return
        val f = s.substringBefore('*').split(',')
        when (f[0].takeLast(3)) {
            "GGA" -> {
                val msl = f.getOrNull(9)?.toDoubleOrNull()
                val geoid = f.getOrNull(11)?.toDoubleOrNull()
                if (msl != null) {
                    _state.update { it.copy(mslAltitude = msl, geoidSeparation = geoid ?: it.geoidSeparation) }
                }
            }
            "GSA" -> {
                val type = f.getOrNull(2)?.toIntOrNull()
                val p = f.getOrNull(15)?.toDoubleOrNull()
                val h = f.getOrNull(16)?.toDoubleOrNull()
                val v = f.getOrNull(17)?.toDoubleOrNull()
                _state.update {
                    it.copy(
                        fixType = type ?: it.fixType,
                        pdop = p ?: it.pdop,
                        hdop = h ?: it.hdop,
                        vdop = v ?: it.vdop,
                    )
                }
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            val fresh = lastFixElapsed != 0L && SystemClock.elapsedRealtime() - lastFixElapsed < 5000
            val enabled = providerEnabled()
            _state.update {
                it.copy(
                    hasFix = fresh && enabled,
                    providerEnabled = enabled,
                    hasPermission = hasPermission(),
                    fixType = if (fresh) it.fixType else 0,
                )
            }
            handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        _state.update { it.copy(hasPermission = hasPermission(), providerEnabled = providerEnabled()) }
        if (started || !hasPermission()) return
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper())
            @Suppress("DEPRECATION")
            lm.registerGnssStatusCallback(gnssCallback, handler)
            @Suppress("DEPRECATION")
            lm.addNmeaListener(nmeaListener, handler)
            started = true
            handler.post(ticker)
        } catch (e: Exception) {
            _state.update { it.copy(providerEnabled = false) }
        }
    }

    fun stop() {
        if (!started) return
        try {
            lm.removeUpdates(locationListener)
            lm.unregisterGnssStatusCallback(gnssCallback)
            lm.removeNmeaListener(nmeaListener)
        } catch (e: Exception) {
        }
        handler.removeCallbacks(ticker)
        started = false
    }

    /** Dopo "cancella dati A-GPS": azzera il tempo al primo fix per misurarne uno nuovo. */
    fun resetTtff() {
        _state.update { it.copy(ttffMs = null) }
    }

    /** Invia un comando extra al provider GPS. Restituisce true se il sistema l'ha accettato. */
    fun sendExtra(command: String): Boolean = try {
        lm.sendExtraCommand(LocationManager.GPS_PROVIDER, command, null)
    } catch (e: Exception) {
        false
    }
}
