package com.marco90.mlgpstest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.marco.mltheme.AccentPreview
import com.marco.mltheme.MLChoiceRow
import com.marco.mltheme.MLGroup
import com.marco.mltheme.MLNavRow
import com.marco.mltheme.MLSettingsScaffold
import com.marco.mltheme.MLSwitchRow
import com.marco.mltheme.MLThemeSettings
import com.marco.mltheme.SWITCH_STYLES
import com.marco.mltheme.SwitchStylePreview
import com.marco.mltheme.THEME_MODES
import com.marco.mltheme.ThemePreview
import com.marco.mltheme.availableAccents
import com.marco90.mlgpstest.data.AppSettings
import com.marco90.mlgpstest.data.CoordFormat
import com.marco90.mlgpstest.data.SpeedUnit
import com.marco90.mlgpstest.data.ThemeMode
import com.marco90.mlgpstest.data.UpdateState

/** Impostazioni a tutto schermo nello stile di MLauncher (MLTheme). */
@Composable
fun SettingsDialog(
    settings: AppSettings,
    update: UpdateState,
    versionName: String,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        MLSettingsScaffold("Impostazioni", onBack = onDismiss) {
            val ml = MLThemeSettings.get(LocalContext.current)
            MLGroup("Tema") {
                MLChoiceRow("Tema", THEME_MODES, settings.themeMode.key, preview = { ThemePreview(it) }) { k ->
                    settings.updateTheme(ThemeMode.values().first { it.key == k })
                }
                MLChoiceRow("Colori", availableAccents, ml.accent, preview = { AccentPreview(it) }) { ml.accent = it }
                MLChoiceRow("Stile degli interruttori", SWITCH_STYLES, ml.switchStyle, preview = { SwitchStylePreview(it) }) {
                    ml.switchStyle = it
                }
            }
            MLGroup("Misure") {
                MLChoiceRow("Unità di velocità", SpeedUnit.values().map { it.name to it.label }, settings.speedUnit.name) { k ->
                    settings.updateSpeedUnit(SpeedUnit.valueOf(k))
                }
                MLChoiceRow(
                    "Formato coordinate",
                    listOf(CoordFormat.DECIMAL.name to "Gradi decimali", CoordFormat.DMS.name to "Gradi° min′ sec″"),
                    settings.coordFormat.name,
                ) { k -> settings.updateCoordFormat(CoordFormat.valueOf(k)) }
            }
            MLGroup("Schermo") {
                MLSwitchRow("Schermo sempre acceso", "Non si spegne mentre l'app è aperta", settings.keepScreenOn) {
                    settings.updateKeepScreenOn(it)
                }
            }
            MLGroup("Aggiornamenti") {
                val status = when (update) {
                    UpdateState.Idle -> "Versione installata: $versionName"
                    UpdateState.Checking -> "Controllo in corso…"
                    is UpdateState.UpToDate -> "Sei aggiornato all'ultima versione ($versionName)"
                    is UpdateState.Available -> "Disponibile: ${update.tag}" + if (update.sizeBytes > 0) " (${update.sizeBytes / 1024 / 1024 + 1} MB)" else ""
                    is UpdateState.Downloading -> "Download in corso…"
                    is UpdateState.Ready -> "${update.tag} scaricata, pronta da installare"
                    is UpdateState.Error -> update.message
                }
                when (update) {
                    is UpdateState.Available -> MLNavRow("Scarica aggiornamento", status, icon = Icons.Filled.Refresh, onClick = onDownloadUpdate)
                    is UpdateState.Ready -> MLNavRow("Installa", status, icon = Icons.Filled.Refresh, onClick = onInstallUpdate)
                    is UpdateState.Checking, is UpdateState.Downloading -> MLNavRow("Aggiornamenti", status, icon = Icons.Filled.Refresh, trailing = null) {}
                    else -> MLNavRow("Cerca aggiornamenti", status, icon = Icons.Filled.Refresh, onClick = onCheckUpdate)
                }
                if (update is UpdateState.Downloading) {
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
                        if (update.progress >= 0f) LinearProgressIndicator(progress = { update.progress }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                if (update is UpdateState.Available && update.notes.isNotBlank()) {
                    Text(
                        update.notes.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AgpsDialog(
    onDismiss: () -> Unit,
    onClearAndUpdate: () -> Unit,
    onClear: () -> Unit,
    onUpdate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        title = { Text("A-GPS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Cancellare i dati ausiliari e riscaricarli aiuta quando il primo fix è lento. " +
                        "Alcuni telefoni ignorano questi comandi.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(onClick = onClearAndUpdate, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancella e aggiorna")
                }
                FilledTonalButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                    Text("Solo cancella")
                }
                FilledTonalButton(onClick = onUpdate, modifier = Modifier.fillMaxWidth()) {
                    Text("Solo aggiorna")
                }
            }
        },
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        title = { Text("Guida") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Stato: fix = posizione ricevuta negli ultimi 5 secondi. Precisione = errore orizzontale stimato in metri.")
                Text("C/N0 (dB-Hz): intensità del segnale. Sotto 20 è debole, sopra 30 buona, sopra 40 ottima.")
                Text("Satelliti in uso: quelli che il telefono sta usando per calcolare la posizione.")
                Text("Colonna Dati: A = almanacco, E = efemeridi, U = usato nel fix.")
                Text("CF: banda del segnale (L1, L5, E1, E5a, B1I, G1...).")
                Text("n° Sat.: in uso / con segnale / in vista.")
                Text("DOP: diluizione della precisione; più basso è meglio (sotto 2 ottimo). DOP o/v = orizzontale / verticale.")
                Text("TTPL: tempo al primo fix dopo l'avvio del GNSS.")
                Text("Skyplot: il centro è lo zenit, il bordo l'orizzonte, il nord è in alto.")
                Text("Per testare bene il GPS vai all'aperto, con cielo libero.")
            }
        },
    )
}
