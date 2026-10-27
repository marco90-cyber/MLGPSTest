package com.marco90.mlgpstest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marco90.mlgpstest.data.Constellations
import com.marco90.mlgpstest.data.GnssState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkyScreen(state: GnssState, heading: Float?) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                SkyPlot(state.sats, Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp), detailed = true, heading = heading)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Legenda", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (c in Constellations.order) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val tint = MaterialTheme.colorScheme.primary
                            Canvas(Modifier.size(16.dp)) {
                                drawSatShape(c, Offset(size.width / 2f, size.height / 2f), 6.dp.toPx(), tint)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(Constellations.name(c), fontSize = 13.sp)
                        }
                    }
                }
                Text(
                    "Colore = intensità del segnale: rosso < 10, arancione 10–20, giallo 20–30, " +
                        "lime 30–50, verde > 50 dB-Hz. Cerchio bianco = usato nel calcolo della posizione. " +
                        "Il cielo ruota con la bussola: il triangolo giallo indica dove punta il telefono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
