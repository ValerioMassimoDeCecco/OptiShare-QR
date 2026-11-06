package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.protocol.PacketKind
import com.example.domain.protocol.PreparedOpticalStream
import com.example.domain.protocol.QrEccLevel
import com.example.ui.components.InstrumentActionButton
import com.example.ui.components.OptiShareCard
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.TelemetryMetricCell
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.SenderUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SendQrScreen(
    senderState: SenderUiState,
    onPickFileUri: (Uri) -> Unit,
    onCapturePhotoBitmap: (Bitmap) -> Unit,
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
    var showPhotoCaptureDialog by rememberSaveable { mutableStateOf(false) }
    var noteTitle by rememberSaveable { mutableStateOf("Credenziali_Nodo_Offline.txt") }
    var noteBody by rememberSaveable {
        mutableStateOf("NODE_ID=OPTI-9942-XKV\nIPV4_LOCAL=192.168.10.45\nVERIFY=SHA256_REQUIRED")
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        showPhotoCaptureDialog = true
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onPickFileUri(uri)
    }

    val imageGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showPhotoCaptureDialog = false
            onPickFileUri(uri)
        }
    }

    val handleOpenPhotoCapture = {
        val grantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = grantedNow
        if (grantedNow) {
            showPhotoCaptureDialog = true
        } else {
            runCatching {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }.onFailure {
                showPhotoCaptureDialog = true
            }
        }
    }

    val handleLaunchFilePicker = {
        runCatching {
            filePickerLauncher.launch("*/*")
        }.onFailure {
            Toast.makeText(
                context,
                "Selettore file non disponibile: usa Nota o Demo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val currentAdvanceCallback by rememberUpdatedState(onAdvanceAutoFrame)

    // 120Hz Choreographer VSync-locked optical frame pacer (eliminates timer drift & tearing)
    LaunchedEffect(
        senderState.isPlaying,
        senderState.fps,
        senderState.stream?.sessionId,
        senderState.selectedMissingDataIndices
    ) {
        val totalFrames = senderState.qrBitmaps.size
        if (senderState.isPlaying && totalFrames > 1) {
            val targetFps = senderState.fps.coerceIn(1, 24)
            val intervalNanos = 1_000_000_000L / targetFps
            var lastStepNanos = withFrameNanos { it }
            while (true) {
                val frameNanos = withFrameNanos { it }
                if (frameNanos - lastStepNanos >= intervalNanos) {
                    // Align to VSync cadence without accumulating drift
                    lastStepNanos += intervalNanos
                    if (frameNanos - lastStepNanos > intervalNanos) {
                        lastStepNanos = frameNanos
                    }
                    currentAdvanceCallback()
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        if (isWideLayout) {
            // Canonical 2-Column Workstation Layout for Tablets, Foldables & Desktop
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
                    OpticalStagePanel(
                        senderState = senderState,
                        onTogglePlayPause = onTogglePlayPause,
                        onStepFrame = onStepFrame,
                        onNavigateToReceiveLoopback = onNavigateToReceiveLoopback
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SourceToolbarPanel(
                        onPickFile = { handleLaunchFilePicker() },
                        onCapturePhoto = { handleOpenPhotoCapture() },
                        onOpenNoteModal = { showTextNoteDialog = true },
                        onLoadDemo = onLoadSampleFile
                    )

                    senderState.stream?.let { stream ->
                        InspectorTabbedPanel(
                            senderState = senderState,
                            stream = stream,
                            onFpsChange = onFpsChange,
                            onUpdateConfig = onUpdateConfig,
                            onToggleMissingChunkFilter = onToggleMissingChunkFilter,
                            onClearMissingChunkFilter = onClearMissingChunkFilter
                        )
                    }
                }
            }
        } else {
            // Compact Handheld Layout
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
                    SourceToolbarPanel(
                        onPickFile = { handleLaunchFilePicker() },
                        onCapturePhoto = { handleOpenPhotoCapture() },
                        onOpenNoteModal = { showTextNoteDialog = true },
                        onLoadDemo = onLoadSampleFile
                    )

                    AnimatedVisibility(visible = senderState.errorMessage != null) {
                        senderState.errorMessage?.let { err ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.error, MaterialTheme.shapes.small)
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

                    OpticalStagePanel(
                        senderState = senderState,
                        onTogglePlayPause = onTogglePlayPause,
                        onStepFrame = onStepFrame,
                        onNavigateToReceiveLoopback = onNavigateToReceiveLoopback
                    )

                    senderState.stream?.let { stream ->
                        InspectorTabbedPanel(
                            senderState = senderState,
                            stream = stream,
                            onFpsChange = onFpsChange,
                            onUpdateConfig = onUpdateConfig,
                            onToggleMissingChunkFilter = onToggleMissingChunkFilter,
                            onClearMissingChunkFilter = onClearMissingChunkFilter
                        )
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showPhotoCaptureDialog) {
        InAppPhotoCaptureDialog(
            hasCameraPermission = hasCameraPermission,
            onRequestCameraPermission = {
                runCatching { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            },
            onPhotoCaptured = { bitmap ->
                showPhotoCaptureDialog = false
                onCapturePhotoBitmap(bitmap)
            },
            onPickFromGallery = {
                runCatching {
                    imageGalleryLauncher.launch("image/*")
                }.onFailure {
                    val fallback = generateSimulatedCameraBitmap()
                    showPhotoCaptureDialog = false
                    onCapturePhotoBitmap(fallback)
                }
            },
            onDismiss = { showPhotoCaptureDialog = false }
        )
    }

    if (showTextNoteDialog) {
        AlertDialog(
            onDismissRequest = { showTextNoteDialog = false },
            title = {
                Text(
                    text = "Nuovo Payload Testuale",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Identificativo file") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text("Contenuto grezzo (UTF-8)") },
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
                    Text("Codifica Stream QR")
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

/**
 * In-App CameraX Photo Capture & Simulation Dialog.
 * Eliminates SecurityException / ActivityNotFoundException crashes caused by external
 * ACTION_IMAGE_CAPTURE intents when CAMERA permission is declared in AndroidManifest.xml
 * or when running inside a streaming emulator without a system camera app.
 */
@Composable
private fun InAppPhotoCaptureDialog(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit,
    onPickFromGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val colorScheme = MaterialTheme.colorScheme
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Acquisisci Foto da Condividere",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF101318))
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val pv = PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }
                                previewViewRef = pv
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    runCatching {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(pv.surfaceProvider)
                                        }
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview
                                        )
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                pv
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Anteprima Fotocamera Live",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Concedi il permesso Fotocamera per scattare dal vivo oppure genera subito uno scatto simulato.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = onRequestCameraPermission) {
                                Text("Autorizza Fotocamera")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onPickFromGallery,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galleria", style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = {
                            onPhotoCaptured(generateSimulatedCameraBitmap())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("capture_simulated_photo_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Foto Simulazione", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val captured = runCatching { previewViewRef?.bitmap }.getOrNull()
                        ?: generateSimulatedCameraBitmap()
                    onPhotoCaptured(captured)
                },
                modifier = Modifier.testTag("confirm_capture_photo_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scatta e Codifica QR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Generates a clean, deterministic camera test snapshot bitmap when running inside an
 * emulator or simulation environment where the hardware camera feed is unavailable.
 */
private fun generateSimulatedCameraBitmap(): Bitmap {
    val width = 400
    val height = 300
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(24, 34, 52)
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Calibration Grid
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(50, 170, 199, 255)
        strokeWidth = 2f
    }
    for (x in 0..width step 40) {
        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
    }
    for (y in 0..height step 40) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
    }

    // Accent Circle Target
    val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(59, 130, 246)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    canvas.drawCircle(width / 2f, height / 2f, 70f, targetPaint)

    // Text Stamp
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 22f
        isFakeBoldText = true
    }
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    canvas.drawText("OPTISHARE CAM SNAPSHOT", 52f, 54f, textPaint)
    textPaint.textSize = 16f
    textPaint.isFakeBoldText = false
    canvas.drawText("TIME: $timestamp • 400x300 JPEG", 76f, 260f, textPaint)

    return bitmap
}

@Composable
private fun SourceToolbarPanel(
    onPickFile: () -> Unit,
    onCapturePhoto: () -> Unit,
    onOpenNoteModal: () -> Unit,
    onLoadDemo: () -> Unit
) {
    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sorgente file da condividere",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "GZIP · XOR FEC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InstrumentActionButton(
                    label = "File",
                    icon = Icons.Default.FolderOpen,
                    onClick = onPickFile,
                    isPrimary = true,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("pick_file_button")
                )
                InstrumentActionButton(
                    label = "Foto",
                    icon = Icons.Default.AddPhotoAlternate,
                    onClick = onCapturePhoto,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capture_photo_sender_button")
                )
                InstrumentActionButton(
                    label = "Nota",
                    icon = Icons.Default.EditNote,
                    onClick = onOpenNoteModal,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_note_button")
                )
                InstrumentActionButton(
                    label = "Demo",
                    icon = Icons.Default.Description,
                    onClick = onLoadDemo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_sample_button")
                )
            }
        }
    }
}

@Composable
private fun OpticalStagePanel(
    senderState: SenderUiState,
    onTogglePlayPause: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onNavigateToReceiveLoopback: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val stream = senderState.stream
    val bitmaps = senderState.qrBitmaps

    if (senderState.isPreparing) {
        OptiShareCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Compressione GZIP e calcolo parità XOR...",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (stream != null && bitmaps.isNotEmpty()) {
        // Pre-cache ImageBitmaps once per stream so 120Hz frame updates do zero allocation
        val cachedImageBitmaps = remember(bitmaps) {
            bitmaps.map { it.asImageBitmap() }
        }
        val safeIndex = senderState.currentFramePosition.coerceIn(0, cachedImageBitmaps.lastIndex)
        val currentPacket = stream.allOrderedPackets[safeIndex]
        val currentImageBitmap = cachedImageBitmaps[safeIndex]
        val isParity = currentPacket.kind == PacketKind.PARITY
        val statusAccent = if (isParity) colorScheme.secondary else colorScheme.primary

        val smoothProgress by animateFloatAsState(
            targetValue = (safeIndex + 1).toFloat() / cachedImageBitmaps.size.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "QrFrameProgress120Hz"
        )

        OptiShareCard(
            isHighlighted = true,
            accentTint = statusAccent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Telemetry Header Bar
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
                            text = "ID ${stream.sessionId} · ${stream.originalBytes.size} B → ${stream.compressedBytes.size} B GZIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(statusAccent, RoundedCornerShape(50))
                        )
                        Text(
                            text = if (isParity) {
                                "FEC ${currentPacket.displayNumber}"
                            } else {
                                "PKT ${currentPacket.displayNumber}/${stream.dataPackets.size}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = statusAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Precision White Calibration Pad for Maximum Camera Contrast
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFD4D4D8), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .testTag("dynamic_qr_image_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentImageBitmap,
                        contentDescription = "Codice QR Pacchetto ${currentPacket.displayNumber}",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle Technical Corner Fiducials on the white pad
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tick = 8.dp.toPx()
                        val stroke = 1.2.dp.toPx()
                        val markColor = Color(0xFF18181B).copy(alpha = 0.35f)
                        // Top-left
                        drawLine(markColor, Offset(0f, 0f), Offset(tick, 0f), stroke)
                        drawLine(markColor, Offset(0f, 0f), Offset(0f, tick), stroke)
                        // Top-right
                        drawLine(markColor, Offset(size.width - tick, 0f), Offset(size.width, 0f), stroke)
                        drawLine(markColor, Offset(size.width, 0f), Offset(size.width, tick), stroke)
                        // Bottom-left
                        drawLine(markColor, Offset(0f, size.height - tick), Offset(0f, size.height), stroke)
                        drawLine(markColor, Offset(0f, size.height), Offset(tick, size.height), stroke)
                        // Bottom-right
                        drawLine(markColor, Offset(size.width - tick, size.height), Offset(size.width, size.height), stroke)
                        drawLine(markColor, Offset(size.width, size.height - tick), Offset(size.width, size.height), stroke)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Frame Progress Scrubber & Readout (120Hz Smooth Interpolated)
                LinearProgressIndicator(
                    progress = { smoothProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = statusAccent,
                    trackColor = colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "FRAME %02d/%02d · CRC32 %s".format(
                            safeIndex + 1,
                            cachedImageBitmaps.size,
                            currentPacket.crc32Hex.uppercase()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${senderState.fps} FPS · 120Hz VSync · ECC ${senderState.eccLevel.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Unified Transport & Loopback Control Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onStepFrame(-1) },
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small)
                            .testTag("prev_qr_frame_button")
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Frame precedente",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    InstrumentActionButton(
                        label = if (senderState.isPlaying) "Pausa" else "Avvia",
                        icon = if (senderState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = onTogglePlayPause,
                        isPrimary = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("play_pause_qr_button")
                    )

                    IconButton(
                        onClick = { onStepFrame(1) },
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small)
                            .testTag("next_qr_frame_button")
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Frame successivo",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    InstrumentActionButton(
                        label = "Loopback",
                        icon = Icons.Default.VerifiedUser,
                        onClick = onNavigateToReceiveLoopback,
                        modifier = Modifier
                            .weight(1.05f)
                            .testTag("run_loopback_from_sender_button")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InspectorTabbedPanel(
    senderState: SenderUiState,
    stream: PreparedOpticalStream,
    onFpsChange: (Int) -> Unit,
    onUpdateConfig: (Int?, QrEccLevel?) -> Unit,
    onToggleMissingChunkFilter: (Int) -> Unit,
    onClearMissingChunkFilter: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Telemetria", "Calibrazione", "Filtro PKT")

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SegmentedSelectorBar(
                items = tabs.indices.toList(),
                selectedItem = activeTab,
                onItemSelected = { activeTab = it },
                labelProvider = { idx ->
                    if (idx == 2 && senderState.selectedMissingDataIndices.isNotEmpty()) {
                        "Filtro (${senderState.selectedMissingDataIndices.size})"
                    } else {
                        tabs[idx]
                    }
                }
            )

            when (activeTab) {
                0 -> {
                    val ratio = if (stream.originalBytes.isNotEmpty()) {
                        val pct = (stream.compressedBytes.size.toFloat() / stream.originalBytes.size.toFloat()) * 100f
                        "%.0f%%".format(pct)
                    } else "100%"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMetricCell(
                            label = "Dimensione Originale",
                            value = "${stream.originalBytes.size} B",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCell(
                            label = "Compresso GZIP",
                            value = "${stream.compressedBytes.size} B ($ratio)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMetricCell(
                            label = "Pacchetti Dati",
                            value = "${stream.dataPackets.size} PKT",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCell(
                            label = "Parità XOR (FEC)",
                            value = "${stream.parityPackets.size} blocchi",
                            valueColor = colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Impronta SHA-256",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stream.fileSha256,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("SHA-256", stream.fileSha256))
                                Toast.makeText(context, "SHA-256 copiato", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copia SHA-256",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Velocità flusso (120Hz VSync Sync)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${senderState.fps} FPS (${1000 / senderState.fps.coerceAtLeast(1)} ms/frame)",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary
                            )
                        }
                        Slider(
                            value = senderState.fps.toFloat(),
                            onValueChange = { onFpsChange(it.toInt()) },
                            valueRange = 1f..24f,
                            steps = 22,
                            modifier = Modifier.testTag("fps_slider")
                        )
                        SegmentedSelectorBar(
                            items = listOf(6, 12, 20, 24),
                            selectedItem = senderState.fps,
                            onItemSelected = { onFpsChange(it) },
                            labelProvider = { "$it FPS" }
                        )
                    }

                    Text(
                        text = "Densità payload per ciascun QR",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SegmentedSelectorBar(
                        items = listOf(240, 380, 550),
                        selectedItem = senderState.chunkSize,
                        onItemSelected = { size -> onUpdateConfig(size, null) },
                        labelProvider = { size ->
                            when (size) {
                                240 -> "240 B (Nitido)"
                                380 -> "380 B (Std)"
                                else -> "550 B (Denso)"
                            }
                        }
                    )

                    Text(
                        text = "Correzione errori Reed-Solomon (ECC)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SegmentedSelectorBar(
                        items = QrEccLevel.entries,
                        selectedItem = senderState.eccLevel,
                        onItemSelected = { ecc -> onUpdateConfig(null, ecc) },
                        labelProvider = { it.label }
                    )
                }

                2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seleziona i pacchetti mancanti da ritrasmettere:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (senderState.selectedMissingDataIndices.isNotEmpty()) {
                            TextButton(onClick = onClearMissingChunkFilter) {
                                Text("Tutti (${stream.dataPackets.size})")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until stream.dataPackets.size) {
                            val isSelected = senderState.selectedMissingDataIndices.contains(i)
                            Box(
                                modifier = Modifier
                                    .size(width = 52.dp, height = 36.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (isSelected) colorScheme.secondaryContainer
                                        else colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { onToggleMissingChunkFilter(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "D#${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
