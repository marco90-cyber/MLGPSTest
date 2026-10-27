package com.marco90.mlgpstest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marco90.mlgpstest.data.AppSettings
import com.marco90.mlgpstest.data.Constellations
import com.marco90.mlgpstest.data.GnssSat
import com.marco90.mlgpstest.data.GnssState
import com.marco90.mlgpstest.data.carrierLabel

private enum class SortBy(val label: String) {
    CONSTELLATION("Costellazione"),
    ID("ID"),
    CN0("C/N0"),
    ELEVATION("Elevazione"),
}

@Composable
fun SatellitesScreen(state: GnssState, settings: AppSettings) {
    var sortBy by remember { mutableStateOf(SortBy.CONSTELLATION) }
    var onlyUsed by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(setOf<Int>()) }
    var sortMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }

    val present = state.sats.map { it.constellation }.distinct().sortedBy { Constellations.orderIndex(it) }
    val shown = state.sats
        .filter { it.constellation !in hidden && (!onlyUsed || it.used) }
        .let { list ->
            when (sortBy) {
                SortBy.CONSTELLATION -> list.sortedWith(
                    compareBy<GnssSat> { Constellations.orderIndex(it.constellation) }.thenBy { it.svid },
                )
                SortBy.ID -> list.sortedBy { it.svid }
                SortBy.CN0 -> list.sortedByDescending { it.cn0 }
                SortBy.ELEVATION -> list.sortedByDescending { it.elevation }
            }
        }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { InfoCard(state, settings) }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Satelliti",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        Box {
                            IconButton(onClick = { sortMenu = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Ordina")
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                for (o in SortBy.values()) {
                                    DropdownMenuItem(
                                        text = { Text(o.label) },
                                        leadingIcon = { RadioButton(selected = sortBy == o, onClick = null) },
                                        onClick = {
                                            sortBy = o
                                            sortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { filterMenu = true }) {
                                Icon(Icons.Filled.FilterList, contentDescription = "Filtra")
                            }
                            DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                                if (present.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Nessun satellite") }, onClick = { filterMenu = false })
                                }
                                for (c in present) {
                                    DropdownMenuItem(
                                        text = { Text("${Constellations.flag(c)} ${Constellations.name(c)}") },
                                        leadingIcon = { Checkbox(checked = c !in hidden, onCheckedChange = null) },
                                        onClick = { hidden = if (c in hidden) hidden - c else hidden + c },
                                    )
                                }
                            }
                        }
                        Switch(checked = onlyUsed, onCheckedChange = { onlyUsed = it })
                    }
                    Text(
                        "Interruttore: solo satelliti in uso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer8()
                    TableHeader()
                    HorizontalDivider()
                    if (shown.isEmpty()) {
                        Text(
                            "Nessun satellite da mostrare",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    shown.forEach { SatRow(it) }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.sbasCount > 0) "Segnali SBAS disponibili: ${state.sbasCount}" else "Nessun segnale SBAS disponibile",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
}

private val colWeights = floatArrayOf(0.8f, 0.9f, 1.0f, 1.1f, 1.1f, 1.1f, 1.2f)

@Composable
private fun TableHeader() {
    val heads = listOf("ID", "GNSS", "CF", "C/N0", "Dati", "Elev.", "Azim.")
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        heads.forEachIndexed { i, h ->
            Text(h, Modifier.weight(colWeights[i]), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SatRow(s: GnssSat) {
    val data = listOfNotNull(
        if (s.hasAlmanac) "A" else null,
        if (s.hasEphemeris) "E" else null,
        if (s.used) "U" else null,
    ).joinToString(" ")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${s.svid}", Modifier.weight(colWeights[0]), fontSize = 14.sp, fontWeight = if (s.used) FontWeight.Bold else FontWeight.Normal)
        Text(Constellations.flag(s.constellation), Modifier.weight(colWeights[1]), fontSize = 16.sp)
        Text(carrierLabel(s.constellation, s.carrierHz), Modifier.weight(colWeights[2]), fontSize = 14.sp)
        Text(if (s.cn0 > 0f) String.format("%.1f", s.cn0) else "", Modifier.weight(colWeights[3]), fontSize = 14.sp)
        Text(data, Modifier.weight(colWeights[4]), fontSize = 14.sp)
        Text("${s.elevation.toInt()}°", Modifier.weight(colWeights[5]), fontSize = 14.sp)
        Text("${s.azimuth.toInt()}°", Modifier.weight(colWeights[6]), fontSize = 14.sp)
    }
}

@Composable
private fun InfoCard(state: GnssState, settings: AppSettings) {
    val f = state.fix
    val fresh = state.hasFix
    val unit = settings.speedUnit
    val msl = state.mslAltitude
    val ellipsoid = if (msl != null && state.geoidSeparation != null) msl + state.geoidSeparation else f?.altitude

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow("Lat.", if (f != null) fmtCoord(f.lat, true, settings.coordFormat) else DASH)
                InfoRow("Long.", if (f != null) fmtCoord(f.lon, false, settings.coordFormat) else DASH)
                InfoRow("Alt.", fmtMeters(ellipsoid))
                InfoRow("Alt. (s.l.m.)", fmtMeters(msl ?: f?.altitude))
                InfoRow("Velocità", if (fresh) fmtSpeed(f?.speed, unit) else DASH)
                InfoRow("Prec. vel.", if (fresh && f?.speedAcc != null) fmtSpeed(f.speedAcc, unit) else DASH)
                InfoRow("DOP", fmtNum(state.pdop, 1))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow("Ora", fmtTime(f?.timeMs))
                InfoRow("TTPL", fmtTtffLong(state.ttffMs))
                InfoRow("Prec. orizz.", if (f?.accuracy != null) fmtMeters(f.accuracy.toDouble()) else DASH)
                InfoRow("n° Sat.", "${state.inUse}/${state.withSignal}/${state.inView}")
                InfoRow("Direzione", if (fresh && f?.bearing != null) String.format("%.0f° %s", f.bearing, cardinal(f.bearing)) else DASH)
                InfoRow("Prec. dir.", if (fresh && f?.bearingAcc != null) String.format("%.0f°", f.bearingAcc) else DASH)
                InfoRow(
                    "DOP o/v",
                    if (state.hdop != null || state.vdop != null) "${fmtNum(state.hdop, 1)} / ${fmtNum(state.vdop, 1)}" else DASH,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "$label:",
            modifier = Modifier.weight(0.9f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
        Text(
            value,
            modifier = Modifier.weight(1.1f).padding(start = 6.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
