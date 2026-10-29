package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.protocol.PacketKind
import com.example.domain.protocol.QrEccLevel
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.VerifiedEmerald
import com.example.viewmodel.SenderUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendQrScreen(
    senderState: SenderUiState,
    onPickFileUri: (Uri) -> Unit,
    onCapturePhotoBitmap: (android.graphics.Bitmap) -> Unit,
    onCreateTextNote: (String, String) -> Unit,
    onLoadSampleFile: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onAdvanceAutoFrame: () -> Unit,
    onFpsChange: (Int) -> Unit,
    onUpdateConfig: (Int?, QrEccLevel?) -> Unit,
    onToggleMissingChunkFilter: (Int) -> Unit,
    onClearMissingChunkFilter: () -> Unit,
    onNavigateToReceiveLoopback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTextNoteDialog by rememberSaveable { mutableStateOf(false) }
    var noteTitle by rememberSaveable { mutableStateOf("Credenziali_Critiche_Offline.txt") }
    var noteBody by rememberSaveable {
        mutableStateOf("Chiave Accesso Nodo Ottico: OPTI-9942-XKV\nIndirizzo Locale: 192.168.10.45\nNote: Verifica integrità SHA-256 obbligatoria.")
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickFileUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onCapturePhotoBitmap(bitmap)
        }
    }

    // Dynamic QR multi-frame loop
    LaunchedEffect(
        senderState.isPlaying,
        senderState.fps,
        senderState.stream?.sessionId,
        senderState.selectedMissingDataIndices
    ) {
        val totalFrames = senderState.qrBitmaps.size
        if (senderState.isPlaying && totalFrames > 1) {
            val intervalMs = (1000L / senderState.fps.coerceIn(1, 12)).coerceAtLeast(80L)
            while (true) {
                delay(intervalMs)
                onAdvanceAutoFrame()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Optical Header Banner + Source Actions
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_optical_hero),
                            contentDescription = stringResource(R.string.hero_title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 92.dp, height = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, OpticalCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.hero_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.hero_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_file_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scegli File", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        FilledTonalButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_photo_sender_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scatta Foto", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTextNoteDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_note_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nota Testo", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onLoadSampleFile,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_sample_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File Demo", maxLines = 1)
                        }
                    }
                }
            }

            // Error Alert if any
            AnimatedVisibility(visible = senderState.errorMessage != null) {
                senderState.errorMessage?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 2. Dynamic QR Optical Stage
            val stream = senderState.stream
            val bitmaps = senderState.qrBitmaps
            if (senderState.isPreparing) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Compressione GZIP, calcolo SHA-256 e generazione blocchi Parità XOR...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (stream != null && bitmaps.isNotEmpty()) {
                val safeIndex = senderState.currentFramePosition.coerceIn(0, bitmaps.lastIndex)
                val currentPacket = stream.allOrderedPackets[safeIndex]
                val currentBitmap = bitmaps[safeIndex]

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = if (currentPacket.kind == PacketKind.PARITY) QuantumViolet else OpticalCyan.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top metadata header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stream.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Originale: ${stream.originalBytes.size} B • GZIP: ${stream.compressedBytes.size} B • Sessione #${stream.sessionId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentPacket.kind == PacketKind.PARITY) {
                                    QuantumViolet.copy(alpha = 0.2f)
                                } else {
                                    OpticalCyan.copy(alpha = 0.18f)
                                }
                            ) {
                                Text(
                                    text = if (currentPacket.kind == PacketKind.PARITY) {
                                        "PARITÀ FEC ${currentPacket.displayNumber}"
                                    } else {
                                        "DATI ${currentPacket.displayNumber}/${stream.dataPackets.size}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentPacket.kind == PacketKind.PARITY) QuantumViolet else OpticalCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // High-Contrast White Optical Pad for Camera Scanning
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(12.dp)
                                .testTag("dynamic_qr_image_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "Codice QR Dinamico Pacchetto ${currentPacket.displayNumber}",
                                filterQuality = FilterQuality.None,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Frame Progress & Hash Bar
                        LinearProgressIndicator(
                            progress = { (safeIndex + 1).toFloat() / bitmaps.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (currentPacket.kind == PacketKind.PARITY) QuantumViolet else OpticalCyan
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Frame ${safeIndex + 1}/${bitmaps.size} • CRC32: ${currentPacket.crc32Hex}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "SHA256: ${stream.fileSha256.take(10)}…",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = VerifiedEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Playback Transport Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onStepFrame(-1) },
                                modifier = Modifier.testTag("prev_qr_frame_button")
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Frame precedente")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = onTogglePlayPause,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("play_pause_qr_button")
                            ) {
                                Icon(
                                    imageVector = if (senderState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (senderState.isPlaying) "Pausa" else "Riproduci"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (senderState.isPlaying) "In Trasmissione (${senderState.fps} FPS)" else "Riprendi Stream")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = { onStepFrame(1) },
                                modifier = Modifier.testTag("next_qr_frame_button")
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Frame successivo")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Instant Loopback Test Shortcut for Single-Device Verification
                        FilledTonalButton(
                            onClick = onNavigateToReceiveLoopback,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_loopback_from_sender_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testa Ricezione e Recupero Errori su questo Dispositivo")
                        }
                    }
                }

                // 3. Missing Chunk Selective Retransmission Filter
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = OpticalCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Ritrasmissione Mirata Pacchetti Mancanti",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (senderState.selectedMissingDataIndices.isNotEmpty()) {
                                TextButton(onClick = onClearMissingChunkFilter) {
                                    Text("Mostra Tutti")
                                }
                            }
                        }

                        Text(
                            text = "Se al destinatario mancano solo alcuni pacchetti, toccali qui sotto per trasmettere esclusivamente quelli in loop rapido:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (i in 0 until stream.dataPackets.size) {
                                val isSelected = senderState.selectedMissingDataIndices.contains(i)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleMissingChunkFilter(i) },
                                    label = {
                                        Text(
                                            text = "D#${i + 1}",
                                            fontFamily = JetBrainsMonoFontFamily
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OpticalCyan.copy(alpha = 0.25f),
                                        selectedLabelColor = OpticalCyan
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Optical Parameters Card (FPS, Chunk Size, ECC Level)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = OpticalCyan)
                            Text(
                                text = "Calibrazione Ottica & Correzione Errori",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Frequenza Stream QR", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${senderState.fps} frame/sec",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OpticalCyan
                                )
                            }
                            Slider(
                                value = senderState.fps.toFloat(),
                                onValueChange = { onFpsChange(it.toInt()) },
                                valueRange = 1f..12f,
                                steps = 10,
                                modifier = Modifier.testTag("fps_slider")
                            )
                        }

                        Text("Dimensione Pacchetto Dati (Densità QR)", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(240 to "240 B (Ultra-Nitido)", 380 to "380 B (Bilanciato)", 550 to "550 B (Veloce)").forEach { (size, label) ->
                                FilterChip(
                                    selected = senderState.chunkSize == size,
                                    onClick = { onUpdateConfig(size, null) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Text("Livello Correzione Errori QR (Reed-Solomon + XOR)", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QrEccLevel.entries.forEach { ecc ->
                                FilterChip(
                                    selected = senderState.eccLevel == ecc,
                                    onClick = { onUpdateConfig(null, ecc) },
                                    label = { Text(ecc.label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTextNoteDialog) {
        AlertDialog(
            onDismissRequest = { showTextNoteDialog = false },
            title = { Text("Crea Documento / Nota per Stream QR") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Nome File") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text("Contenuto da trasmettere offline") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateTextNote(noteTitle, noteBody)
                        showTextNoteDialog = false
                    }
                ) {
                    Text("Genera QR Dinamici")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextNoteDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
