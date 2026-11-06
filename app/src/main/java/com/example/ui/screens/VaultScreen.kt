package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.TransferRecordEntity
import com.example.ui.components.OptiShareCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.TelemetryMetricCell
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    records: List<TransferRecordEntity>,
    onRetransmitViaQr: (TransferRecordEntity) -> Unit,
    onRenameRecord: (Long, String) -> Unit,
    onDeleteRecord: (TransferRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRecordId by rememberSaveable { mutableStateOf<Long?>(null) }

    var recordBeingRenamed by remember { mutableStateOf<TransferRecordEntity?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var recordPendingDelete by remember { mutableStateOf<TransferRecordEntity?>(null) }
    var copiedDigestId by remember { mutableStateOf<Long?>(null) }

    val filteredRecords = remember(records, selectedFilter, searchQuery) {
        val byDirection = when (selectedFilter) {
            "SENT" -> records.filter { it.direction == "SENT" }
            "RECEIVED" -> records.filter { it.direction == "RECEIVED" }
            else -> records
        }
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            byDirection
        } else {
            byDirection.filter {
                it.fileName.contains(query, ignoreCase = true) ||
                    it.sha256Hash.contains(query, ignoreCase = true) ||
                    it.mimeType.contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(filteredRecords) {
        if (selectedRecordId == null || filteredRecords.none { it.id == selectedRecordId }) {
            selectedRecordId = filteredRecords.firstOrNull()?.id
        }
    }

    val totalBytes = remember(records) { records.sumOf { it.fileSizeBytes.toLong() } }
    val receivedCount = remember(records) { records.count { it.direction == "RECEIVED" } }
    val sentCount = remember(records) { records.count { it.direction == "SENT" } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val isWideLayout = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = if (isWideLayout) 1140.dp else 640.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Telemetry Ledger Bar
            OptiShareCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderRow(
                        overline = "Archivio sul dispositivo · ${records.size} file",
                        title = "File Condivisi e Ricevuti"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMetricCell(
                            label = "VOLUME DATI",
                            value = formatByteCount(totalBytes),
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCell(
                            label = "RICEVUTI (RX)",
                            value = "$receivedCount file",
                            valueColor = colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCell(
                            label = "INVIATI (TX)",
                            value = "$sentCount file",
                            valueColor = colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Filtra per nome file, MIME o hash SHA-256…",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Cancella ricerca",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vault_search_input")
                        )
                    }

                    val filterOptions = listOf(
                        "ALL" to "Tutti (${records.size})",
                        "RECEIVED" to "Ricevuti ($receivedCount)",
                        "SENT" to "Inviati ($sentCount)"
                    )
                    SegmentedSelectorBar(
                        items = filterOptions,
                        selectedItem = filterOptions.firstOrNull { it.first == selectedFilter } ?: filterOptions.first(),
                        onItemSelected = { selectedFilter = it.first },
                        labelProvider = { it.second }
                    )
                }
            }

            if (filteredRecords.isEmpty()) {
                OptiShareCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
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
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Nessun risultato per \"$searchQuery\""
                            } else {
                                stringResource(R.string.vault_empty_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Prova a cancellare il filtro di ricerca o a cambiare categoria."
                            } else {
                                stringResource(R.string.vault_empty_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isWideLayout) {
                // Master-Detail Workstation Layout
                val activeRecord = filteredRecords.find { it.id == selectedRecordId } ?: filteredRecords.first()

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .testTag("vault_records_list")
                    ) {
                        items(filteredRecords, key = { it.id }) { record ->
                            VaultCompactRowItem(
                                record = record,
                                isSelected = record.id == activeRecord.id,
                                onClick = { selectedRecordId = record.id }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                    ) {
                        VaultRecordInspectorCard(
                            record = activeRecord,
                            isDigestCopied = copiedDigestId == activeRecord.id,
                            onCopyDigest = {
                                copyHashToClipboard(context, activeRecord.sha256Hash)
                                copiedDigestId = activeRecord.id
                            },
                            onRetransmit = { onRetransmitViaQr(activeRecord) },
                            onShareExternal = { shareRecordFileExternally(context, activeRecord) },
                            onOpenRename = {
                                recordBeingRenamed = activeRecord
                                renameText = activeRecord.fileName
                            },
                            onRequestDelete = { recordPendingDelete = activeRecord }
                        )
                    }
                }
            } else {
                // Handheld Vertical Ledger with Expandable Inspector
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 104.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("vault_records_list")
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        VaultExpandableRecordCard(
                            record = record,
                            isExpanded = selectedRecordId == record.id,
                            isDigestCopied = copiedDigestId == record.id,
                            onToggleExpand = {
                                selectedRecordId = if (selectedRecordId == record.id) null else record.id
                            },
                            onCopyDigest = {
                                copyHashToClipboard(context, record.sha256Hash)
                                copiedDigestId = record.id
                            },
                            onRetransmit = { onRetransmitViaQr(record) },
                            onShareExternal = { shareRecordFileExternally(context, record) },
                            onOpenRename = {
                                recordBeingRenamed = record
                                renameText = record.fileName
                            },
                            onRequestDelete = { recordPendingDelete = record }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    recordBeingRenamed?.let { target ->
        AlertDialog(
            onDismissRequest = { recordBeingRenamed = null },
            title = { Text("Rinomina File in Archivio") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nome file") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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

    // Delete Confirmation Dialog
    recordPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text("Conferma Eliminazione") },
            text = {
                Text(
                    "Vuoi rimuovere definitivamente \"${target.fileName}\" dall'archivio locale del dispositivo?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRecord(target)
                        recordPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun VaultCompactRowItem(
    record: TransferRecordEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
    val isReceived = record.direction == "RECEIVED"
    val badgeColor = if (isReceived) colorScheme.tertiary else colorScheme.primary

    OptiShareCard(
        isHighlighted = isSelected,
        accentTint = badgeColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isReceived) "RX" else "TX",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatByteCount(record.fileSizeBytes.toLong())} • ${record.totalDataChunks} PKT • ${dateFormat.format(Date(record.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.Verified,
                contentDescription = "Integrità verificata",
                tint = colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun VaultExpandableRecordCard(
    record: TransferRecordEntity,
    isExpanded: Boolean,
    isDigestCopied: Boolean,
    onToggleExpand: () -> Unit,
    onCopyDigest: () -> Unit,
    onRetransmit: () -> Unit,
    onShareExternal: () -> Unit,
    onOpenRename: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
    val isReceived = record.direction == "RECEIVED"
    val badgeColor = if (isReceived) colorScheme.tertiary else colorScheme.primary

    OptiShareCard(
        isHighlighted = isExpanded,
        accentTint = badgeColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = if (isReceived) "RX • FOTOCAMERA" else "TX • STREAM QR",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(record.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenRename,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.DriveFileRenameOutline,
                            contentDescription = "Rinomina file",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Elimina file",
                            tint = colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatByteCount(record.fileSizeBytes.toLong())} • ${record.totalDataChunks} pacchetti QR • MIME: ${record.mimeType}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable(onClick = onCopyDigest)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SHA-256: ${record.sha256Hash.take(24)}…",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (isDigestCopied) "COPIATO" else "COPIA",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    record.textPreview?.takeIf { it.isNotBlank() }?.let { snippet ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    width = 1.dp,
                                    color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = snippet.take(240),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetransmit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trasmetti QR",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onShareExternal,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Condividi",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultRecordInspectorCard(
    record: TransferRecordEntity,
    isDigestCopied: Boolean,
    onCopyDigest: () -> Unit,
    onRetransmit: () -> Unit,
    onShareExternal: () -> Unit,
    onOpenRename: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }
    val isReceived = record.direction == "RECEIVED"

    OptiShareCard(
        isHighlighted = true,
        accentTint = if (isReceived) colorScheme.tertiary else colorScheme.primary,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeaderRow(
                overline = if (isReceived) "RICEVUTO VIA FOTOCAMERA" else "INVIATO VIA STREAM QR",
                title = "Ispettore File Archiviato"
            )

            Text(
                text = record.fileName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricCell(
                    label = "DIMENSIONE",
                    value = formatByteCount(record.fileSizeBytes.toLong()),
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "PACCHETTI QR",
                    value = "${record.totalDataChunks} PKT",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricCell(
                    label = "MIME TYPE",
                    value = record.mimeType,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "DATA REGISTRAZIONE",
                    value = dateFormat.format(Date(record.timestamp)),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable(onClick = onCopyDigest)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDigestCopied) "DIGEST SHA-256 COPIATO" else "DIGEST INTEGRITÀ SHA-256",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.tertiary
                    )
                    Text(
                        text = record.sha256Hash,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copia SHA-256",
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            record.textPreview?.takeIf { it.isNotBlank() }?.let { snippet ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ANTEPRIMA CONTENUTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = snippet.take(300),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetransmit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trasmetti QR")
                }

                FilledTonalButton(
                    onClick = onShareExternal,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Condividi")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenRename,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rinomina")
                }

                OutlinedButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Elimina", color = colorScheme.error)
                }
            }
        }
    }
}

private fun formatByteCount(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}

private fun copyHashToClipboard(context: Context, hash: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("SHA-256 Digest", hash))
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
