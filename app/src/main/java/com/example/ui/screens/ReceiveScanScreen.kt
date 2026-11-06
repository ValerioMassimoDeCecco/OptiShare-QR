package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.local.TransferRecordEntity
import com.example.domain.protocol.ChunkReceptionState
import com.example.domain.protocol.QrCodecEngine
import com.example.ui.components.InstrumentActionButton
import com.example.ui.components.OptiShareCard
import com.example.ui.components.TelemetryMetricCell
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.ReceiverUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun ReceiveScanScreen(
    receiverState: ReceiverUiState,
    onQrScanned: (String) -> Boolean,
    onResetSession: () -> Unit,
    onStartLoopbackSimulation: () -> Unit,
    onGenerateNackQr: () -> Unit,
    onDismissNackQr: () -> Unit,
    onSubmitDecryptionPassword: (String) -> Unit,
    onRetransmitCompletedRecord: (TransferRecordEntity) -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val qrImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bmp = BitmapFactory.decodeStream(input)
                        if (bmp != null) QrCodecEngine.decodeFromBitmap(bmp) else null
                    }
                }
                if (decoded != null) {
                    val accepted = onQrScanned(decoded)
                    if (accepted) triggerHapticTick(context)
                }
            }
        }
    }

    val handleResetRequest = {
        if (receiverState.activeSessionId != null && receiverState.completedResult == null) {
            showResetConfirmDialog = true
        } else {
            onResetSession()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        if (isWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CameraScannerInstrumentPanel(
                        receiverState = receiverState,
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = {
                            runCatching {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }.onFailure {
                                hasCameraPermission = true
                            }
                        },
                        onQrDecoded = { qrStr ->
                            val added = onQrStringScannedSafe(qrStr, onQrScanned)
                            if (added) triggerHapticTick(context)
                        },
                        onResetRequest = handleResetRequest,
                        onStartLoopback = onStartLoopbackSimulation,
                        onPickImage = {
                            runCatching {
                                qrImagePickerLauncher.launch("image/*")
                            }.onFailure {
                                onStartLoopbackSimulation()
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (receiverState.awaitingDecryptionPassword) {
                        AesDecryptionUnlockPanel(
                            receiverState = receiverState,
                            onSubmitPassword = onSubmitDecryptionPassword
                        )
                    }
                    AssemblyMatrixInstrumentPanel(
                        receiverState = receiverState,
                        onGenerateNackQr = onGenerateNackQr
                    )
                    VerifiedResultInstrumentPanel(
                        receiverState = receiverState,
                        onRetransmitCompletedRecord = onRetransmitCompletedRecord,
                        onOpenVault = onOpenVault
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CameraScannerInstrumentPanel(
                        receiverState = receiverState,
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = {
                            runCatching {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }.onFailure {
                                hasCameraPermission = true
                            }
                        },
                        onQrDecoded = { qrStr ->
                            val added = onQrStringScannedSafe(qrStr, onQrScanned)
                            if (added) triggerHapticTick(context)
                        },
                        onResetRequest = handleResetRequest,
                        onStartLoopback = onStartLoopbackSimulation,
                        onPickImage = {
                            runCatching {
                                qrImagePickerLauncher.launch("image/*")
                            }.onFailure {
                                onStartLoopbackSimulation()
                            }
                        }
                    )

                    if (receiverState.awaitingDecryptionPassword) {
                        AesDecryptionUnlockPanel(
                            receiverState = receiverState,
                            onSubmitPassword = onSubmitDecryptionPassword
                        )
                    }

                    VerifiedResultInstrumentPanel(
                        receiverState = receiverState,
                        onRetransmitCompletedRecord = onRetransmitCompletedRecord,
                        onOpenVault = onOpenVault
                    )

                    AssemblyMatrixInstrumentPanel(
                        receiverState = receiverState,
                        onGenerateNackQr = onGenerateNackQr
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    receiverState.nackQrBitmap?.let { nackBmp ->
        ReverseNackQrModal(
            sessionId = receiverState.activeSessionId ?: "",
            missingIndices = receiverState.missingDataChunkIndices,
            nackBitmap = nackBmp,
            onDismiss = onDismissNackQr
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Interrompere la ricezione in corso?") },
            text = {
                Text(
                    "I pacchetti acquisiti per '${receiverState.fileName}' verranno scartati e il buffer ottico verrà azzerato."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetSession()
                    }
                ) {
                    Text("Azzera Buffer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Continua Scansione")
                }
            }
        )
    }
}

@Composable
private fun AesDecryptionUnlockPanel(
    receiverState: ReceiverUiState,
    onSubmitPassword: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var password by rememberSaveable { mutableStateOf("") }

    OptiShareCard(
        isHighlighted = true,
        accentTint = colorScheme.secondary,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aes_decryption_unlock_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = colorScheme.secondary)
                Text(
                    text = "Decifra Stream AES-256-GCM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Tutti i ${receiverState.totalDataChunks} pacchetti di '${receiverState.fileName}' sono stati ricevuti. Inserisci la passphrase AES-256 per decifrare e verificare l'hash SHA-256.",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passphrase AES-256") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receiver_aes_password_input")
            )
            receiverState.decryptionError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.error
                )
            }
            Button(
                onClick = { onSubmitPassword(password) },
                enabled = password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receiver_decrypt_submit_button")
            ) {
                Text("Decifra e Verifica SHA-256")
            }
        }
    }
}

@Composable
private fun ReverseNackQrModal(
    sessionId: String,
    missingIndices: List<Int>,
    nackBitmap: Bitmap,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "QR Inverso NACK (${missingIndices.size} Mancanti)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Mostra questo codice QR al dispositivo mittente (pulsante 'NACK' su Invia QR) per fargli trasmettere solo i ${missingIndices.size} pacchetti mancanti della sessione #$sessionId.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .testTag("nack_qr_bitmap_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = nackBitmap.asImageBitmap(),
                        contentDescription = "QR NACK Pacchetti Mancanti",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "PACCHETTI RICHIESTI: ${missingIndices.take(12).joinToString(", ") { "D#${it + 1}" }}" +
                        if (missingIndices.size > 12) "…" else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Torna allo Scanner")
            }
        }
    )
}

@Composable
private fun CameraScannerInstrumentPanel(
    receiverState: ReceiverUiState,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQrDecoded: (String) -> Unit,
    onResetRequest: () -> Unit,
    onStartLoopback: () -> Unit,
    onPickImage: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isCompleted = receiverState.completedResult != null
    var isTorchOn by rememberSaveable { mutableStateOf(false) }
    var selectedZoomRatio by rememberSaveable { mutableFloatStateOf(1.0f) }

    OptiShareCard(
        isHighlighted = true,
        accentTint = if (isCompleted) colorScheme.tertiary else colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanner Fotocamera · Zoom & Torcia",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (receiverState.activeSessionId != null) {
                            "Sessione #${receiverState.activeSessionId} · ${receiverState.fileName}"
                        } else {
                            "Inquadra il codice QR per ricevere"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (receiverState.activeSessionId != null) {
                    TextButton(
                        onClick = onResetRequest,
                        modifier = Modifier.testTag("reset_receiver_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF050608))
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .testTag("camera_preview_container")
                ) {
                    CameraXQrScannerPreview(
                        torchEnabled = isTorchOn,
                        zoomRatio = selectedZoomRatio,
                        onQrStringDecoded = onQrDecoded
                    )

                    val bracketColor = if (isCompleted) colorScheme.tertiary else colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val boxSize = size.minDimension * 0.68f
                        val left = (size.width - boxSize) / 2f
                        val top = (size.height - boxSize) / 2f
                        val right = left + boxSize
                        val bottom = top + boxSize
                        val arm = 22.dp.toPx()
                        val stroke = 2.dp.toPx()

                        drawLine(bracketColor, Offset(left, top), Offset(left + arm, top), stroke)
                        drawLine(bracketColor, Offset(left, top), Offset(left, top + arm), stroke)
                        drawLine(bracketColor, Offset(right - arm, top), Offset(right, top), stroke)
                        drawLine(bracketColor, Offset(right, top), Offset(right, top + arm), stroke)
                        drawLine(bracketColor, Offset(left, bottom - arm), Offset(left, bottom), stroke)
                        drawLine(bracketColor, Offset(left, bottom), Offset(left + arm, bottom), stroke)
                        drawLine(bracketColor, Offset(right - arm, bottom), Offset(right, bottom), stroke)
                        drawLine(bracketColor, Offset(right, bottom - arm), Offset(right, bottom), stroke)

                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val cross = 6.dp.toPx()
                        drawLine(
                            Color.White.copy(alpha = 0.45f),
                            Offset(cx - cross, cy),
                            Offset(cx + cross, cy),
                            1.dp.toPx()
                        )
                        drawLine(
                            Color.White.copy(alpha = 0.45f),
                            Offset(cx, cy - cross),
                            Offset(cx, cy + cross),
                            1.dp.toPx()
                        )
                    }

                    // Top Hardware Camera Controls Overlay (Torch + 1x / 1.5x / 2x Quick Zoom)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { isTorchOn = !isTorchOn },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTorchOn) colorScheme.primary else Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White,
                            modifier = Modifier.testTag("camera_torch_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Torcia Flash",
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isTorchOn) "Flash ON" else "Flash",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x").forEach { (ratio, label) ->
                                val active = selectedZoomRatio == ratio
                                Surface(
                                    onClick = { selectedZoomRatio = ratio },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) colorScheme.primary else Color.Black.copy(alpha = 0.65f),
                                    contentColor = Color.White,
                                    modifier = Modifier.testTag("camera_zoom_${label}_button")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Monospace Telemetry Overlay Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = receiverState.lastScannedPacketLabel?.let { "ULTIMO PKT: $it" }
                                ?: "ALLINEA IL QR NEL MIRINO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "${(receiverState.transferProgress * 100).toInt()}% · ${String.format(Locale.US, "%.1f", receiverState.liveSpeedKbps)} KB/s",
                            style = MaterialTheme.typography.labelMedium,
                            color = bracketColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.camera_permission_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.camera_permission_rationale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    InstrumentActionButton(
                        label = stringResource(R.string.grant_camera_button),
                        icon = Icons.Default.CameraAlt,
                        onClick = onRequestPermission,
                        isPrimary = true,
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InstrumentActionButton(
                    label = if (receiverState.isLoopbackRunning) "Test in corso..." else "Simula Loopback",
                    icon = Icons.Default.PlayCircleFilled,
                    onClick = onStartLoopback,
                    enabled = !receiverState.isLoopbackRunning,
                    isPrimary = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_loopback_button")
                )

                InstrumentActionButton(
                    label = "Importa Immagine QR",
                    icon = Icons.Default.ImageSearch,
                    onClick = onPickImage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_qr_image_button")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssemblyMatrixInstrumentPanel(
    receiverState: ReceiverUiState,
    onGenerateNackQr: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val acquiredCount = receiverState.chunkStates.values.count {
        it != ChunkReceptionState.PENDING
    }
    val missing = receiverState.missingDataChunkIndices

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRIA ASSEMBLAGGIO PACCHETTI",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "$acquiredCount / ${receiverState.totalDataChunks} PKT",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = receiverState.statusBanner,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface
            )

            val smoothTransferProgress by animateFloatAsState(
                targetValue = receiverState.transferProgress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "ReceiveTransferProgress120Hz"
            )

            LinearProgressIndicator(
                progress = { smoothTransferProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (receiverState.completedResult != null) colorScheme.tertiary else colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TelemetryMetricCell(
                    label = "Velocità Live",
                    value = String.format(Locale.US, "%.1f KB/s", receiverState.liveSpeedKbps),
                    valueColor = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "ETA Stimato",
                    value = if (receiverState.completedResult != null) "0s" else "${receiverState.estimatedRemainingSeconds}s",
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "Riparati XOR",
                    value = "${receiverState.parityRecoveredCount} (${receiverState.validFramesScanned} OK)",
                    valueColor = colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (receiverState.totalDataChunks > 0) {
                HorizontalDivider(color = colorScheme.outlineVariant)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until receiverState.totalDataChunks) {
                        val state = receiverState.chunkStates[i] ?: ChunkReceptionState.PENDING
                        val bgColor = when (state) {
                            ChunkReceptionState.RECEIVED_DIRECT -> colorScheme.tertiary.copy(alpha = 0.18f)
                            ChunkReceptionState.RECOVERED_PARITY -> colorScheme.secondary.copy(alpha = 0.22f)
                            ChunkReceptionState.PENDING -> colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                        val borderColor = when (state) {
                            ChunkReceptionState.RECEIVED_DIRECT -> colorScheme.tertiary
                            ChunkReceptionState.RECOVERED_PARITY -> colorScheme.secondary
                            ChunkReceptionState.PENDING -> colorScheme.outlineVariant
                        }
                        val textColor = when (state) {
                            ChunkReceptionState.RECEIVED_DIRECT -> colorScheme.tertiary
                            ChunkReceptionState.RECOVERED_PARITY -> colorScheme.secondary
                            ChunkReceptionState.PENDING -> colorScheme.onSurfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 30.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(bgColor)
                                .border(1.dp, borderColor, MaterialTheme.shapes.extraSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "D#${i + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (missing.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MANCANTI (${missing.size}): ${missing.take(8).joinToString(", ") { "D#${it + 1}" }}" +
                                if (missing.size > 8) "…" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FilledTonalButton(
                            onClick = onGenerateNackQr,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_nack_qr_button")
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mostra QR Mancanti (NACK)",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifiedResultInstrumentPanel(
    receiverState: ReceiverUiState,
    onRetransmitCompletedRecord: (TransferRecordEntity) -> Unit,
    onOpenVault: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    AnimatedVisibility(visible = receiverState.completedResult != null) {
        receiverState.completedResult?.let { result ->
            OptiShareCard(
                isHighlighted = true,
                accentTint = colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transfer_completed_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (result.wasEncrypted) {
                                    "DECIFRATO AES-256 & VERIFICATO SHA-256"
                                } else {
                                    "INTEGRITÀ SHA-256 VERIFICATA"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.tertiary
                            )
                            Text(
                                text = "${result.fileName} (${result.rawBytes.size} B)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = result.actualSha256,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        receiverState.savedRecord?.let { record ->
                            InstrumentActionButton(
                                label = "Ritrasmetti QR",
                                icon = Icons.Default.QrCode2,
                                onClick = { onRetransmitCompletedRecord(record) },
                                isPrimary = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("retransmit_received_qr_button")
                            )
                        }

                        InstrumentActionButton(
                            label = "Archivio",
                            icon = Icons.Default.FolderSpecial,
                            onClick = onOpenVault,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_vault_after_receive_button")
                        )
                    }
                }
            }
        }
    }
}

private fun onQrStringScannedSafe(
    qrString: String,
    callback: (String) -> Boolean
): Boolean {
    return runCatching { callback(qrString) }.getOrDefault(false)
}

private fun triggerHapticTick(context: Context) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20L)
            }
        }
    }
}

@Composable
private fun CameraXQrScannerPreview(
    torchEnabled: Boolean,
    zoomRatio: Float,
    onQrStringDecoded: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    DisposableEffect(activeCamera, torchEnabled) {
        runCatching {
            activeCamera?.cameraControl?.enableTorch(torchEnabled)
        }
        onDispose {}
    }

    DisposableEffect(activeCamera, zoomRatio) {
        runCatching {
            activeCamera?.cameraControl?.setZoomRatio(zoomRatio)
        }
        onDispose {}
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    var lastDispatchedQr: String? = null
                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val decodedText = QrCodecEngine.decodeFromImageProxy(imageProxy)
                            if (!decodedText.isNullOrBlank() && decodedText != lastDispatchedQr) {
                                lastDispatchedQr = decodedText
                                previewView.post {
                                    onQrStringDecoded(decodedText)
                                }
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    activeCamera = cam
                    runCatching {
                        cam.cameraControl.enableTorch(torchEnabled)
                        cam.cameraControl.setZoomRatio(zoomRatio)
                    }
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
