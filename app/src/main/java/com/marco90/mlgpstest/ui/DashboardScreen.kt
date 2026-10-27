package com.marco90.mlgpstest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marco90.mlgpstest.data.AppSettings
import com.marco90.mlgpstest.data.GnssState

@Composable
fun DashboardScreen(
    state: GnssState,
    settings: AppSettings,
    heading: Float?,
    onNavigate: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(state, Modifier.weight(1f))
            AccuracyCard(state, Modifier.weight(1f))
        }

        // Grafico C/N0
        Card(
            colors = CardDefaults.cardColors(containerColor = PanelBg, contentColor = PanelText),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("In vista", style = MaterialTheme.typography.titleMedium)
                        Text("${state.inView}", fontSize = 32.sp, fontWeight = FontWeight.Light)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("In uso", style = MaterialTheme.typography.titleMedium)
                        Text("${state.inUse}", fontSize = 32.sp, fontWeight = FontWeight.Light)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Cn0Chart(state.sats, Modifier.fillMaxWidth())
            }
        }

        // SNR medio
        Card(
            colors = CardDefaults.cardColors(containerColor = PanelBg, contentColor = PanelText),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SnrBar(state.avgSnr, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }

        // Riquadri rapidi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tileColors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            Card(
                onClick = { onNavigate(2) },
                colors = CardDefaults.cardColors(containerColor = PanelBg, contentColor = PanelText),
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) {
                SkyPlot(state.sats, Modifier.fillMaxSize().padding(4.dp), detailed = false)
            }
            Card(
                onClick = { onNavigate(1) },
                colors = tileColors,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Posizione", fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            Card(
                onClick = { onNavigate(3) },
                colors = CardDefaults.cardColors(containerColor = PanelBg, contentColor = PanelText),
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) {
                CompassNeedle(heading, Modifier.fillMaxSize().padding(8.dp))
            }
            Card(
                onClick = { onNavigate(3) },
                colors = tileColors,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        fmtSpeedNumber(state.fix?.speed, settings.speedUnit),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.hasFix) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                    Text(settings.speedUnit.label, fontSize = 11.sp)
                }
            }
            Card(
                onClick = { onNavigate(1) },
                colors = tileColors,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        fmtTtff(state.ttffMs),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.ttffMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                    Text("1° fix", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: GnssState, modifier: Modifier) {
    val (label, sub, dot) = when {
        !state.hasPermission -> Triple("Permesso", "posizione negato", Color(0xFFE53935))
        !state.providerEnabled -> Triple("GPS spento", "attiva la posizione", Color(0xFFE53935))
        state.hasFix -> Triple(
            "Fix",
            when (state.fixType) {
                3 -> "3D"
                2 -> "2D"
                else -> ""
            },
            Color(0xFF43A047),
        )
        else -> Triple("Nessun fix", "", Color(0xFFFF9800))
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier.height(150.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Stato GNSS", style = MaterialTheme.typography.titleMedium)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).background(dot, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(label, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                }
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 32.dp))
                }
            }
        }
    }
}

@Composable
private fun AccuracyCard(state: GnssState, modifier: Modifier) {
    val acc = state.fix?.accuracy
    val text = if (state.hasFix && acc != null) {
        if (acc < 10f) String.format("%.1f", acc) else String.format("%.0f", acc)
    } else {
        "0"
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = modifier.height(150.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Precisione (± m)", style = MaterialTheme.typography.titleMedium)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Text(
                    text,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.hasFix) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
    }
}
