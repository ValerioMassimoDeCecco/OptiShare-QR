package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import java.util.concurrent.Executors

@Composable
fun ReceiveScanScreen(
    receiverState: ReceiverUiState,
    onQrScanned: (String) -> Boolean,
    onResetSession: () -> Unit,
    onStartLoopbackSimulation: () -> Unit,
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
                    AssemblyMatrixInstrumentPanel(receiverState = receiverState)
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

                    VerifiedResultInstrumentPanel(
                        receiverState = receiverState,
                        onRetransmitCompletedRecord = onRetransmitCompletedRecord,
                        onOpenVault = onOpenVault
                    )

                    AssemblyMatrixInstrumentPanel(receiverState = receiverState)

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
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
                        text = "Scanner Fotocamera",
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
                    CameraXQrScannerPreview(onQrStringDecoded = onQrDecoded)

                    val bracketColor = if (isCompleted) colorScheme.tertiary else colorScheme.primary
                    // Precision Corner Brackets Reticle (Leica / Industrial Finder style)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val boxSize = size.minDimension * 0.68f
                        val left = (size.width - boxSize) / 2f
                        val top = (size.height - boxSize) / 2f
                        val right = left + boxSize
                        val bottom = top + boxSize
                        val arm = 22.dp.toPx()
                        val stroke = 2.dp.toPx()

                        // Top-Left Bracket
                        drawLine(bracketColor, Offset(left, top), Offset(left + arm, top), stroke)
                        drawLine(bracketColor, Offset(left, top), Offset(left, top + arm), stroke)
                        // Top-Right Bracket
                        drawLine(bracketColor, Offset(right - arm, top), Offset(right, top), stroke)
                        drawLine(bracketColor, Offset(right, top), Offset(right, top + arm), stroke)
                        // Bottom-Left Bracket
                        drawLine(bracketColor, Offset(left, bottom - arm), Offset(left, bottom), stroke)
                        drawLine(bracketColor, Offset(left, bottom), Offset(left + arm, bottom), stroke)
                        // Bottom-Right Bracket
                        drawLine(bracketColor, Offset(right - arm, bottom), Offset(right, bottom), stroke)
                        drawLine(bracketColor, Offset(right, bottom - arm), Offset(right, bottom), stroke)

                        // Center Crosshair Ticks
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
                            color = Color.White
                        )
                        Text(
                            text = "${(receiverState.transferProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = bracketColor
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
private fun AssemblyMatrixInstrumentPanel(receiverState: ReceiverUiState) {
    val colorScheme = MaterialTheme.colorScheme
    val acquiredCount = receiverState.chunkStates.values.count {
        it != ChunkReceptionState.PENDING
    }

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
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$acquiredCount / ${receiverState.totalDataChunks} PKT",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary
                )
            }

            Text(
                text = receiverState.statusBanner,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { receiverState.transferProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (receiverState.completedResult != null) colorScheme.tertiary else colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricCell(
                    label = "Validati CRC32",
                    value = "${receiverState.validFramesScanned}",
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "Riparati XOR",
                    value = "${receiverState.parityRecoveredCount}",
                    valueColor = colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "Scartati CRC",
                    value = "${receiverState.crcErrorsRejected}",
                    valueColor = if (receiverState.crcErrorsRejected > 0) colorScheme.error else colorScheme.onSurfaceVariant,
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
                                color = textColor
                            )
                        }
                    }
                }

                val missing = receiverState.missingDataChunkIndices
                if (missing.isNotEmpty() && missing.size <= 6) {
                    Text(
                        text = "MANCANTI: ${missing.joinToString(", ") { "D#${it + 1}" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary
                    )
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
                                text = "INTEGRITÀ SHA-256 VERIFICATA",
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
    onQrStringDecoded: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
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

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val decodedText = QrCodecEngine.decodeFromImageProxy(imageProxy)
                            if (!decodedText.isNullOrBlank()) {
                                previewView.post {
                                    onQrStringDecoded(decodedText)
                                }
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
