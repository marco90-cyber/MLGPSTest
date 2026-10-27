package com.marco90.mlgpstest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marco90.mlgpstest.data.AppSettings
import com.marco90.mlgpstest.data.CoordFormat
import com.marco90.mlgpstest.data.SpeedUnit
import com.marco90.mlgpstest.data.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(settings: AppSettings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        title = { Text("Impostazioni") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Tema", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "Sistema",
                        ThemeMode.LIGHT to "Chiaro",
                        ThemeMode.DARK to "Scuro",
                    ).forEach { (m, label) ->
                        FilterChip(
                            selected = settings.themeMode == m,
                            onClick = { settings.updateTheme(m) },
                            label = { Text(label) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Colori dinamici (Material You)", Modifier.weight(1f))
                    Switch(checked = settings.dynamicColors, onCheckedChange = { settings.updateDynamicColors(it) })
                }

                Text("Unità di velocità", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeedUnit.values().forEach { u ->
                        FilterChip(
                            selected = settings.speedUnit == u,
                            onClick = { settings.updateSpeedUnit(u) },
                            label = { Text(u.label) },
                        )
                    }
                }

                Text("Formato coordinate", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.coordFormat == CoordFormat.DECIMAL,
                        onClick = { settings.updateCoordFormat(CoordFormat.DECIMAL) },
                        label = { Text("Gradi decimali") },
                    )
                    FilterChip(
                        selected = settings.coordFormat == CoordFormat.DMS,
                        onClick = { settings.updateCoordFormat(CoordFormat.DMS) },
                        label = { Text("Gradi° min′ sec″") },
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Schermo sempre acceso", Modifier.weight(1f))
                    Switch(checked = settings.keepScreenOn, onCheckedChange = { settings.updateKeepScreenOn(it) })
                }
            }
        },
    )
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
