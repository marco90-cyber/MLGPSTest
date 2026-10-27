package com.marco90.mlgpstest.data

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class LogFormat(val label: String, val extension: String, val mime: String) {
    GPX("GPX (traccia)", "gpx", "application/gpx+xml"),
    NMEA("NMEA (grezzo)", "nmea", "text/plain"),
    CSV("CSV (satelliti)", "csv", "text/csv"),
}

data class RecState(
    val recording: Boolean = false,
    val points: Int = 0,
    val sampleRows: Int = 0,
    val files: List<File> = emptyList(),
)

/**
 * Scrive i log su file nella cartella dell'app: GPX (sempre valido, anche se l'app viene chiusa),
 * NMEA (sentenze grezze) e CSV (stato dei satelliti).
 */
class LogRecorder(context: Context) {

    val dir: File = (context.getExternalFilesDir("logs") ?: File(context.filesDir, "logs")).also { it.mkdirs() }

    private val _state = MutableStateFlow(RecState(files = listLogFiles()))
    val state: StateFlow<RecState> = _state.asStateFlow()

    private var gpx: RandomAccessFile? = null
    private var nmea: BufferedWriter? = null
    private var csv: BufferedWriter? = null

    private val footer = "</trkseg></trk>\n</gpx>\n".toByteArray()
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun listLogFiles(): List<File> =
        dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun refresh() {
        _state.update { it.copy(files = listLogFiles()) }
    }

    fun start(formats: Set<LogFormat>) {
        if (_state.value.recording || formats.isEmpty()) return
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = "mlgpstest_$stamp"
        try {
            if (LogFormat.GPX in formats) {
                val raf = RandomAccessFile(File(dir, "$base.gpx"), "rw")
                val header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<gpx version=\"1.1\" creator=\"MLGPSTest\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n" +
                    "<trk><name>$base</name><trkseg>\n"
                raf.write(header.toByteArray())
                raf.write(footer)
                gpx = raf
            }
            if (LogFormat.NMEA in formats) {
                nmea = BufferedWriter(FileWriter(File(dir, "$base.nmea")))
            }
            if (LogFormat.CSV in formats) {
                csv = BufferedWriter(FileWriter(File(dir, "$base.csv"))).also {
                    it.write("time_utc,constellation,svid,carrier,cn0_dbhz,elevation_deg,azimuth_deg,used_in_fix\n")
                    it.flush()
                }
            }
            _state.value = RecState(recording = true, files = listLogFiles())
        } catch (e: Exception) {
            closeAll()
            refresh()
        }
    }

    fun stop() {
        closeAll()
        _state.update { it.copy(recording = false, files = listLogFiles()) }
    }

    private fun closeAll() {
        try { gpx?.close() } catch (e: Exception) { }
        try { nmea?.close() } catch (e: Exception) { }
        try { csv?.close() } catch (e: Exception) { }
        gpx = null
        nmea = null
        csv = null
    }

    fun onLocation(l: Location) {
        val raf = gpx ?: return
        try {
            val sb = StringBuilder()
            sb.append("<trkpt lat=\"").append(String.format(Locale.US, "%.7f", l.latitude))
                .append("\" lon=\"").append(String.format(Locale.US, "%.7f", l.longitude)).append("\">")
            if (l.hasAltitude()) sb.append("<ele>").append(String.format(Locale.US, "%.2f", l.altitude)).append("</ele>")
            sb.append("<time>").append(isoFormat.format(Date(l.time))).append("</time></trkpt>\n")
            raf.seek(raf.length() - footer.size)
            raf.write(sb.toString().toByteArray())
            raf.write(footer)
            _state.update { it.copy(points = it.points + 1) }
        } catch (e: Exception) {
        }
    }

    fun onNmea(message: String) {
        val w = nmea ?: return
        try {
            w.write(message.trim())
            w.write("\n")
            w.flush()
        } catch (e: Exception) {
        }
    }

    fun onSatellites(sats: List<GnssSat>) {
        val w = csv ?: return
        try {
            val t = isoFormat.format(Date())
            for (s in sats) {
                w.write(
                    String.format(
                        Locale.US, "%s,%s,%d,%s,%.1f,%.1f,%.1f,%d\n",
                        t, Constellations.name(s.constellation), s.svid,
                        carrierLabel(s.constellation, s.carrierHz),
                        s.cn0, s.elevation, s.azimuth, if (s.used) 1 else 0,
                    ),
                )
            }
            w.flush()
            _state.update { it.copy(sampleRows = it.sampleRows + sats.size) }
        } catch (e: Exception) {
        }
    }

    fun delete(file: File) {
        file.delete()
        refresh()
    }
}
