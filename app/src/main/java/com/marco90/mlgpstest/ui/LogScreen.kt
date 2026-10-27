package com.marco90.mlgpstest.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.marco90.mlgpstest.data.LogFormat
import com.marco90.mlgpstest.data.RecState
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(
    rec: RecState,
    initialFormats: Set<LogFormat>,
    onStart: (Set<LogFormat>) -> Unit,
    onStop: () -> Unit,
    onDelete: (File) -> Unit,
) {
    val ctx = LocalContext.current
    var formats by remember { mutableStateOf(initialFormats) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Registrazione log", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (f in LogFormat.values()) {
                            FilterChip(
                                selected = f in formats,
                                enabled = !rec.recording,
                                onClick = { formats = if (f in formats) formats - f else formats + f },
                                label = { Text(f.label) },
                            )
                        }
                    }
                    if (rec.recording) {
                        Text("Registrazione in corso: ${rec.points} punti, ${rec.sampleRows} righe satelliti")
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Text("  Ferma registrazione")
                        }
                    } else {
                        Button(
                            onClick = { onStart(formats) },
                            enabled = formats.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.FiberManualRecord, contentDescription = null)
                            Text("  Avvia registrazione")
                        }
                    }
                    Text(
                        "La registrazione funziona con l'app aperta: attiva \"Schermo sempre acceso\" nelle impostazioni per lunghe tracce.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            Text("File salvati", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
        }

        if (rec.files.isEmpty()) {
            item {
                Text(
                    "Nessun log ancora.",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        items(rec.files, key = { it.absolutePath }) { file ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${humanSize(file.length())} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(file.lastModified()))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { shareFile(ctx, file) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Condividi")
                    }
                    IconButton(onClick = { onDelete(file) }, enabled = !rec.recording) {
                        Icon(Icons.Filled.Delete, contentDescription = "Elimina")
                    }
                }
            }
        }
    }
}

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
}

private fun shareFile(ctx: Context, file: File) {
    val mime = LogFormat.values().firstOrNull { it.extension == file.extension }?.mime ?: "text/plain"
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(intent, "Condividi log"))
}
