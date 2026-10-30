package com.marco90.mlgpstest.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marco90.mlgpstest.data.MainViewModel
import kotlinx.coroutines.launch

private data class TabItem(val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current
    val settings = vm.settings

    MLGPSTheme(settings.themeMode, settings.dynamicColors) {
        val state by vm.gnss.collectAsStateWithLifecycle()
        val rec by vm.rec.collectAsStateWithLifecycle()
        val magHeading by rememberMagneticHeading()
        val heading = trueHeading(magHeading, state.fix)
        val dark = isDarkFor(settings.themeMode)

        var tab by rememberSaveable { mutableIntStateOf(0) }
        var menu by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var showHelp by remember { mutableStateOf(false) }
        var showAgps by remember { mutableStateOf(false) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        fun say(msg: String) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(msg)
            }
        }

        // Schermo sempre acceso
        val view = LocalView.current
        DisposableEffect(settings.keepScreenOn) {
            view.keepScreenOn = settings.keepScreenOn
            onDispose { view.keepScreenOn = false }
        }

        // Permessi di posizione
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            vm.start()
        }
        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            if (vm.repo.hasPermission()) {
                vm.start()
            } else {
                launcher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stop() }

        val tabs = listOf(
            TabItem("Stato", Icons.Filled.GpsFixed),
            TabItem("Satelliti", Icons.Filled.SatelliteAlt),
            TabItem("Cielo", Icons.Filled.Explore),
            TabItem("Registro", Icons.Filled.Timeline),
        )

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text("MLGPSTest") },
                    actions = {
                        IconButton(onClick = {
                            settings.updateTheme(if (dark) com.marco90.mlgpstest.data.ThemeMode.LIGHT else com.marco90.mlgpstest.data.ThemeMode.DARK)
                        }) {
                            Icon(
                                if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Tema chiaro/scuro",
                            )
                        }
                        IconButton(onClick = {
                            val f = state.fix
                            if (f == null || !state.hasFix) {
                                say("Nessuna posizione da condividere")
                            } else {
                                val text = String.format(
                                    java.util.Locale.US,
                                    "Posizione: %.6f, %.6f (± %.0f m)\nhttps://maps.google.com/?q=%.6f,%.6f",
                                    f.lat, f.lon, f.accuracy ?: 0f, f.lat, f.lon,
                                )
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                ctx.startActivity(Intent.createChooser(send, "Condividi posizione"))
                            }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Condividi posizione")
                        }
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("A-GPS") }, onClick = {
                                    menu = false
                                    showAgps = true
                                })
                                DropdownMenuItem(text = { Text("Impostazioni") }, onClick = {
                                    menu = false
                                    showSettings = true
                                })
                                DropdownMenuItem(text = { Text("Guida") }, onClick = {
                                    menu = false
                                    showHelp = true
                                })
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, contentDescription = t.title) },
                            label = { Text(t.title) },
                        )
                    }
                }
            },
        ) { inner ->
            Column(Modifier.padding(inner).fillMaxSize()) {
                if (!state.hasPermission) {
                    BannerCard(
                        "Serve il permesso di posizione per leggere il GPS.",
                        "Concedi",
                    ) {
                        launcher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    }
                } else if (!state.providerEnabled) {
                    BannerCard("La posizione del telefono è disattivata.", "Impostazioni") {
                        ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> DashboardScreen(state)
                        1 -> SatellitesScreen(state, settings)
                        2 -> SkyScreen(state, heading)
                        else -> LogScreen(
                            rec = rec,
                            initialFormats = vm.formats,
                            onStart = { vm.startRecording(it) },
                            onStop = { vm.stopRecording() },
                            onDelete = { vm.recorder.delete(it) },
                        )
                    }
                }
            }
        }

        if (showSettings) {
            val updCtx = androidx.compose.ui.platform.LocalContext.current
            SettingsDialog(
                settings = settings,
                update = com.marco90.mlgpstest.data.UpdateState.Idle,
                versionName = vm.updater.currentVersionName(),
                // Finestra di aggiornamento con Pausa / Riprendi / Annulla
                onCheckUpdate = { com.marco90.mlgpstest.AppUpdate.open(updCtx) },
                onDownloadUpdate = { vm.downloadUpdate() },
                onInstallUpdate = { vm.installUpdate() },
                onDismiss = { showSettings = false },
            )
        }
        if (showHelp) HelpDialog { showHelp = false }
        com.marco90.mlgpstest.AppUpdateHost()
        if (showAgps) {
            AgpsDialog(
                onDismiss = { showAgps = false },
                onClearAndUpdate = {
                    showAgps = false
                    val c = vm.agpsClear()
                    val u = vm.agpsUpdate()
                    say(if (c || u) "Comandi A-GPS inviati al sistema" else "Comandi non supportati da questo telefono")
                },
                onClear = {
                    showAgps = false
                    say(if (vm.agpsClear()) "Dati A-GPS cancellati" else "Comando non supportato da questo telefono")
                },
                onUpdate = {
                    showAgps = false
                    say(if (vm.agpsUpdate()) "Aggiornamento A-GPS richiesto" else "Comando non supportato da questo telefono")
                },
            )
        }
    }
}

@Composable
private fun BannerCard(text: String, action: String, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
