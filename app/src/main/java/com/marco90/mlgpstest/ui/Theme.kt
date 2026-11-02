package com.marco90.mlgpstest.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.marco.mltheme.MLTheme
import com.marco90.mlgpstest.data.ThemeMode

private val FallbackLight = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondaryContainer = Color(0xFFCCE8E4),
    onSecondaryContainer = Color(0xFF06201D),
)

private val FallbackDark = darkColorScheme(
    primary = Color(0xFF7FD6C2),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFFB2F0E3),
    secondaryContainer = Color(0xFF2E4C47),
    onSecondaryContainer = Color(0xFFCCE8E4),
)

@Composable
fun isDarkFor(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Tema delle MLapps (MLTheme): colori e stile degli interruttori scelti nelle impostazioni. */
@Composable
fun MLGPSTheme(mode: ThemeMode, @Suppress("UNUSED_PARAMETER") dynamic: Boolean, content: @Composable () -> Unit) {
    MLTheme(mode = mode.key, content = content)
}

/** Chiave del tema per MLTheme. */
val ThemeMode.key: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "auto"
        ThemeMode.LIGHT -> "light"
        ThemeMode.DARK -> "dark"
    }
