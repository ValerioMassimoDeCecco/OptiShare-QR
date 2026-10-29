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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.local.TransferRecordEntity
import com.example.domain.protocol.ChunkReceptionState
import com.example.domain.protocol.QrCodecEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.VerifiedEmerald
import com.example.viewmodel.ReceiverUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalLayoutApi::class)
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
            // 1. CameraX Viewfinder or Permission Request Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (receiverState.completedResult != null) VerifiedEmerald else OpticalCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    )
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
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = OpticalCyan
                            )
                            Text(
                                text = "Ricevitore Ottico CameraX (Offline)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (receiverState.activeSessionId != null) {
                            OutlinedButton(
                                onClick = onResetSession,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("reset_receiver_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (hasCameraPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black)
                                .testTag("camera_preview_container")
                        ) {
                            CameraXQrScannerPreview(
                                onQrStringDecoded = { qrStr ->
                                    val added = onQrStringScannedSafe(qrStr, onQrScanned)
                                    if (added) {
                                        triggerHapticTick(context)
                                    }
                                }
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val boxSize = size.minDimension * 0.68f
                                val left = (size.width - boxSize) / 2f
                                val top = (size.height - boxSize) / 2f
                                drawRoundRect(
                                    color = OpticalCyan.copy(alpha = 0.85f),
                                    topLeft = Offset(left, top),
                                    size = ComposeSize(boxSize, boxSize),
                                    cornerRadius = CornerRadius(28f, 28f),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                                drawLine(
                                    color = VerifiedEmerald.copy(alpha = 0.75f),
                                    start = Offset(left + 16f, size.height / 2f),
                                    end = Offset(left + boxSize - 16f, size.height / 2f),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = receiverState.lastScannedPacketLabel?.let { "Ultimo letto: $it" }
                                        ?: "Allinea il codice QR nel mirino ottico",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = OpticalCyan,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = stringResource(R.string.camera_permission_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.camera_permission_rationale),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("grant_camera_permission_button")
                            ) {
                                Text(stringResource(R.string.grant_camera_button))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onStartLoopbackSimulation,
                            enabled = !receiverState.isLoopbackRunning,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_loopback_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (receiverState.isLoopbackRunning) "Simulazione in corso..." else "Simula Ricezione QR",
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = { qrImagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(0.75f)
                                .testTag("import_qr_image_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Da Foto QR", maxLines = 1)
                        }
                    }
                }
            }

            // 2. Real-Time Reception Telemetry & Chunk Matrix Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (receiverState.fileName.isNotBlank()) {
                                    "File: ${receiverState.fileName}"
                                } else {
                                    "Stato Assemblaggio Pacchetti"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = receiverState.statusBanner,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = OpticalCyan.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${(receiverState.transferProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = OpticalCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { receiverState.transferProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (receiverState.completedResult != null) VerifiedEmerald else OpticalCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val acquiredCount = receiverState.chunkStates.values.count {
                            it != ChunkReceptionState.PENDING
                        }
                        Text(
                            text = "Dati: $acquiredCount/${receiverState.totalDataChunks}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                        Text(
                            text = "Parità XOR Usate: ${receiverState.parityRecoveredCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = QuantumViolet
                        )
                        Text(
                            text = "CRC Scartati: ${receiverState.crcErrorsRejected}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = if (receiverState.crcErrorsRejected > 0) SignalAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (receiverState.totalDataChunks > 0) {
                        Text(
                            text = "Matrice Integrità Pacchetti (Verde = Diretto CRC32, Viola = Ricostruito via Parità XOR):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (i in 0 until receiverState.totalDataChunks) {
                                val state = receiverState.chunkStates[i] ?: ChunkReceptionState.PENDING
                                val bgColor = when (state) {
                                    ChunkReceptionState.RECEIVED_DIRECT -> VerifiedEmerald.copy(alpha = 0.25f)
                                    ChunkReceptionState.RECOVERED_PARITY -> QuantumViolet.copy(alpha = 0.30f)
                                    ChunkReceptionState.PENDING -> MaterialTheme.colorScheme.surface
                                }
                                val borderColor = when (state) {
                                    ChunkReceptionState.RECEIVED_DIRECT -> VerifiedEmerald
                                    ChunkReceptionState.RECOVERED_PARITY -> QuantumViolet
                                    ChunkReceptionState.PENDING -> MaterialTheme.colorScheme.outline
                                }
                                Box(
                                    modifier = Modifier
                                        .size(width = 48.dp, height = 32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "D#${i + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }
                            }
                        }

                        val missing = receiverState.missingDataChunkIndices
                        if (missing.isNotEmpty() && missing.size <= 5) {
                            Text(
                                text = "Pacchetti ancora mancanti: ${missing.joinToString(", ") { "D#${it + 1}" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OpticalCyan
                            )
                        }
                    }
                }
            }

            // 3. Completed & Cryptographically Verified Result Card
            AnimatedVisibility(visible = receiverState.completedResult != null) {
                receiverState.completedResult?.let { result ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = VerifiedEmerald.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, VerifiedEmerald, RoundedCornerShape(22.dp))
                            .testTag("transfer_completed_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "File Ricostruito & Verificato!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VerifiedEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${result.fileName} (${result.rawBytes.size} byte)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = null,
                                            tint = VerifiedEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "SHA-256 Verificato: ${if (result.integrityVerified) "CORRISPONDENZA 100%" else "ERRORE"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VerifiedEmerald
                                        )
                                    }
                                    Text(
                                        text = result.actualSha256,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                receiverState.savedRecord?.let { record ->
                                    Button(
                                        onClick = { onRetransmitCompletedRecord(record) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("retransmit_received_qr_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ritrasmetti QR")
                                    }
                                }

                                OutlinedButton(
                                    onClick = onOpenVault,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_vault_after_receive_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apri in Archivio")
                                }
                            }
                        }
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
                vibrator.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25L)
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
