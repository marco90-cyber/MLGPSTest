package com.marco90.mlgpstest.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings = AppSettings(app)
    val repo = GnssRepository(app)
    val recorder = LogRecorder(app)

    val gnss: StateFlow<GnssState> = repo.state
    val rec: StateFlow<RecState> = recorder.state

    /** Formati scelti per la registrazione. */
    var formats: Set<LogFormat> = setOf(LogFormat.GPX)

    init {
        repo.onLocation = { recorder.onLocation(it) }
        repo.onNmea = { recorder.onNmea(it) }
        repo.onSatellites = { recorder.onSatellites(it) }
    }

    fun start() = repo.start()

    fun stop() {
        // Durante la registrazione i sensori restano attivi.
        if (!recorder.state.value.recording) repo.stop()
    }

    fun startRecording(formats: Set<LogFormat>) {
        this.formats = formats
        recorder.start(formats)
    }

    fun stopRecording() = recorder.stop()

    /** A-GPS: cancella i dati ausiliari memorizzati. */
    fun agpsClear(): Boolean {
        val ok = repo.sendExtra("delete_aiding_data")
        repo.resetTtff()
        return ok
    }

    /** A-GPS: forza il download di tempo e dati XTRA/efemeridi. */
    fun agpsUpdate(): Boolean {
        val t = repo.sendExtra("force_time_injection")
        val x = repo.sendExtra("force_xtra_injection")
        return t || x
    }

    override fun onCleared() {
        recorder.stop()
        repo.stop()
    }
}
