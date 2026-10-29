package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.TransferRecordEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.VerifiedEmerald
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    records: List<TransferRecordEntity>,
    onRetransmitViaQr: (TransferRecordEntity) -> Unit,
    onAnalyzeWithAi: (TransferRecordEntity) -> Unit,
    onRenameRecord: (Long, String) -> Unit,
    onDeleteRecord: (TransferRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var recordBeingRenamed by remember { mutableStateOf<TransferRecordEntity?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }

    val filteredRecords = remember(records, selectedFilter) {
        when (selectedFilter) {
            "SENT" -> records.filter { it.direction == "SENT" }
            "RECEIVED" -> records.filter { it.direction == "RECEIVED" }
            "AI" -> records.filter { it.direction == "AI_OPTIMIZED" || !it.aiSummary.isNullOrBlank() }
            else -> records
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            Text(
                text = "Archivio Locale Offline (Room DB)",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Tutti i file inviati, ricevuti via fotocamera o estratti con AI salvati sul dispositivo con verifica SHA-256",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "ALL" to "Tutti (${records.size})",
                    "RECEIVED" to "Ricevuti",
                    "SENT" to "Inviati",
                    "AI" to "Smart AI"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredRecords.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = OpticalCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.vault_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.vault_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("vault_records_list")
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        VaultRecordCard(
                            record = record,
                            onRetransmit = { onRetransmitViaQr(record) },
                            onAnalyzeAi = { onAnalyzeWithAi(record) },
                            onShareExternal = { shareRecordFileExternally(context, record) },
                            onOpenRename = {
                                recordBeingRenamed = record
                                renameText = record.smartSuggestedName ?: record.fileName
                            },
                            onDelete = { onDeleteRecord(record) }
                        )
                    }
                }
            }
        }
    }

    recordBeingRenamed?.let { target ->
        AlertDialog(
            onDismissRequest = { recordBeingRenamed = null },
            title = { Text("Rinomina File in Archivio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Nuovo nome file") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    target.smartSuggestedName?.let { suggested ->
                        TextButton(onClick = { renameText = suggested }) {
                            Text("Usa suggerimento AI: $suggested")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameRecord(target.id, renameText.trim())
                        }
                        recordBeingRenamed = null
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordBeingRenamed = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun VaultRecordCard(
    record: TransferRecordEntity,
    onRetransmit: () -> Unit,
    onAnalyzeAi: () -> Unit,
    onShareExternal: () -> Unit,
    onOpenRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
    val badgeColor = when (record.direction) {
        "RECEIVED" -> VerifiedEmerald
        "AI_OPTIMIZED" -> QuantumViolet
        else -> OpticalCyan
    }
    val badgeLabel = when (record.direction) {
        "RECEIVED" -> "RICEVUTO VIA FOTOCAMERA"
        "AI_OPTIMIZED" -> "ESTRATTO & OTTIMIZZATO AI"
        else -> "INVIATO VIA STREAM QR"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = dateFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${record.fileSizeBytes} B • ${record.totalDataChunks} pacchetti QR • MIME: ${record.mimeType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onOpenRename) {
                    Icon(
                        Icons.Default.DriveFileRenameOutline,
                        contentDescription = "Rinomina file"
                    )
                }
                IconButton(onClick = onShareExternal) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Condividi file"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Elimina file",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // SHA-256 Integrity Line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = VerifiedEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SHA-256: ${record.sha256Hash.take(20)}…",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = VerifiedEmerald
                )
            }

            // Optional AI Summary or Extracted Text Snippet
            record.aiSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuantumViolet.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Riassunto AI (${record.aiCategory ?: "Documento"}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumViolet
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } ?: record.extractedText?.takeIf { it.isNotBlank() }?.let { snippet ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = snippet.take(160),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetransmit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trasmetti QR")
                }

                FilledTonalButton(
                    onClick = onAnalyzeAi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analisi AI")
                }
            }
        }
    }
}

private fun shareRecordFileExternally(context: Context, record: TransferRecordEntity) {
    runCatching {
        val file = File(record.localFilePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = record.mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, record.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Condividi ${record.fileName}"))
    }
}
