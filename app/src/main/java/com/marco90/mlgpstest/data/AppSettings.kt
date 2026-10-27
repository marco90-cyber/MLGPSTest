package com.marco90.mlgpstest.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SpeedUnit(val label: String, val factor: Double) {
    KMH("km/h", 3.6),
    MS("m/s", 1.0),
    MPH("mph", 2.236936),
    KN("nodi", 1.943844),
}

enum class CoordFormat { DECIMAL, DMS }

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun <T : Enum<T>> read(key: String, values: Array<T>, def: T): T {
        val name = prefs.getString(key, null) ?: return def
        return values.firstOrNull { it.name == name } ?: def
    }

    var themeMode by mutableStateOf(read("theme", ThemeMode.values(), ThemeMode.SYSTEM))
        private set
    var dynamicColors by mutableStateOf(prefs.getBoolean("dynamic", true))
        private set
    var speedUnit by mutableStateOf(read("speed_unit", SpeedUnit.values(), SpeedUnit.KMH))
        private set
    var coordFormat by mutableStateOf(read("coord_format", CoordFormat.values(), CoordFormat.DECIMAL))
        private set
    var keepScreenOn by mutableStateOf(prefs.getBoolean("keep_screen_on", false))
        private set

    fun updateTheme(v: ThemeMode) {
        themeMode = v
        prefs.edit().putString("theme", v.name).apply()
    }

    fun updateDynamicColors(v: Boolean) {
        dynamicColors = v
        prefs.edit().putBoolean("dynamic", v).apply()
    }

    fun updateSpeedUnit(v: SpeedUnit) {
        speedUnit = v
        prefs.edit().putString("speed_unit", v.name).apply()
    }

    fun updateCoordFormat(v: CoordFormat) {
        coordFormat = v
        prefs.edit().putString("coord_format", v.name).apply()
    }

    fun updateKeepScreenOn(v: Boolean) {
        keepScreenOn = v
        prefs.edit().putBoolean("keep_screen_on", v).apply()
    }
}
