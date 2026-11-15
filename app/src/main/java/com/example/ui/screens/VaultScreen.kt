package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.rememberOptiShareHaptics
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
    val haptics = rememberOptiShareHaptics()

    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRecordId by rememberSaveable { mutableStateOf<Long?>(null) }

    var recordBeingRenamed by remember { mutableStateOf<TransferRecordEntity?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var recordPendingDelete by remember { mutableStateOf<TransferRecordEntity?>(null) }

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
                it.fileName.contains(query, ignoreCase = true)
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
            // Clean Search & Direction Filter
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Cerca per nome file…",
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
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input")
                )

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

                if (records.isNotEmpty()) {
                    Text(
                        text = "${records.size} file salvati · ${formatByteCount(totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
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
                                "Nessun file trovato per \"$searchQuery\""
                            } else {
                                stringResource(R.string.vault_empty_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Prova a cercare un altro nome o cambia filtro."
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
                            onToggleExpand = {
                                haptics.selectionTick()
                                selectedRecordId = if (selectedRecordId == record.id) null else record.id
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
            title = { Text("Rinomina file") },
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
            title = { Text("Eliminare questo file?") },
            text = {
                Text(
                    "\"${target.fileName}\" verrà rimosso dall'archivio di questo telefono."
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
private fun DirectionIconBadge(isReceived: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val badgeColor = if (isReceived) colorScheme.tertiary else colorScheme.primary
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.14f),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isReceived) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = if (isReceived) "Ricevuto" else "Inviato",
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
        }
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DirectionIconBadge(isReceived = isReceived)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${if (isReceived) "Ricevuto" else "Inviato"} · ${formatByteCount(record.fileSizeBytes.toLong())} · ${dateFormat.format(Date(record.timestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VaultExpandableRecordCard(
    record: TransferRecordEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DirectionIconBadge(isReceived = isReceived)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${if (isReceived) "Ricevuto" else "Inviato"} · ${formatByteCount(record.fileSizeBytes.toLong())} · ${dateFormat.format(Date(record.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    record.textPreview?.takeIf { it.isNotBlank() }?.let { snippet ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = snippet.take(200),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onShareExternal,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Condividi",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        FilledTonalButton(
                            onClick = onRetransmit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Invia QR",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        IconButton(onClick = onOpenRename) {
                            Icon(
                                Icons.Default.DriveFileRenameOutline,
                                contentDescription = "Rinomina file",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onRequestDelete) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Elimina file",
                                tint = colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultRecordInspectorCard(
    record: TransferRecordEntity,
    onRetransmit: () -> Unit,
    onShareExternal: () -> Unit,
    onOpenRename: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DirectionIconBadge(isReceived = isReceived)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReceived) "File ricevuto" else "File inviato",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "${formatByteCount(record.fileSizeBytes.toLong())} · ${dateFormat.format(Date(record.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

            record.textPreview?.takeIf { it.isNotBlank() }?.let { snippet ->
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
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShareExternal,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Condividi")
                }

                FilledTonalButton(
                    onClick = onRetransmit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Invia QR")
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
