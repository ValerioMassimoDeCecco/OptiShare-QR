package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.protocol.OpticalPacketProtocol
import com.example.domain.protocol.PacketKind
import com.example.domain.protocol.PreparedOpticalStream
import com.example.domain.protocol.QrCodecEngine
import com.example.domain.protocol.QrEccLevel
import com.example.domain.protocol.StandardQrKind
import com.example.ui.components.InstrumentActionButton
import com.example.ui.components.OptiShareCard
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.TelemetryMetricCell
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.SenderUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun SendQrScreen(
    senderState: SenderUiState,
    onPickFileUri: (Uri) -> Unit,
    onPickMultipleFileUris: (List<Uri>) -> Unit,
    onCapturePhotoBitmap: (Bitmap) -> Unit,
    onCreateTextNote: (String, String) -> Unit,
    onLoadSampleFile: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onAdvanceAutoFrame: () -> Unit,
    onFpsChange: (Int) -> Unit,
    onUpdateConfig: (Int?, QrEccLevel?) -> Unit,
    onUpdateEncryptionPassword: (String) -> Unit,
    onToggleAutoMaxBrightness: () -> Unit,
    onScanNackQr: (String) -> Result<Int>,
    onToggleMissingChunkFilter: (Int) -> Unit,
    onClearMissingChunkFilter: () -> Unit,
    onNavigateToReceiveLoopback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTextNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoCaptureDialog by rememberSaveable { mutableStateOf(false) }
    var showStandardQrDialog by rememberSaveable { mutableStateOf(false) }
    var showEncryptionDialog by rememberSaveable { mutableStateOf(false) }
    var showNackScannerDialog by rememberSaveable { mutableStateOf(false) }

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

    // Automatic Maximum Screen Brightness & Keep Screen On while actively streaming QR frames
    DisposableEffect(senderState.isPlaying, senderState.autoMaxBrightness) {
        val activity = context as? Activity
        val window = activity?.window
        val shouldBoost = senderState.isPlaying && senderState.autoMaxBrightness
        if (window != null && shouldBoost) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        showPhotoCaptureDialog = true
    }

    val nackCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        showNackScannerDialog = true
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onPickFileUri(uri)
    }

    val multiFileZipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onPickMultipleFileUris(uris)
        }
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

    val handleOpenNackScanner = {
        val grantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = grantedNow
        if (grantedNow) {
            showNackScannerDialog = true
        } else {
            runCatching {
                nackCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }.onFailure {
                showNackScannerDialog = true
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

    val handleLaunchMultiZipPicker = {
        runCatching {
            multiFileZipPickerLauncher.launch("*/*")
        }.onFailure {
            Toast.makeText(
                context,
                "Selettore multi-file non disponibile su questo dispositivo",
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
                        onOpenEncryptionDialog = { showEncryptionDialog = true },
                        onOpenNackScanner = { handleOpenNackScanner() },
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
                        senderState = senderState,
                        onPickFile = { handleLaunchFilePicker() },
                        onPickMultiZip = { handleLaunchMultiZipPicker() },
                        onCapturePhoto = { handleOpenPhotoCapture() },
                        onOpenNoteModal = { showTextNoteDialog = true },
                        onOpenStandardQrModal = { showStandardQrDialog = true },
                        onLoadDemo = onLoadSampleFile,
                        onOpenEncryptionDialog = { showEncryptionDialog = true }
                    )

                    senderState.stream?.let { stream ->
                        InspectorTabbedPanel(
                            senderState = senderState,
                            stream = stream,
                            onFpsChange = onFpsChange,
                            onUpdateConfig = onUpdateConfig,
                            onToggleAutoMaxBrightness = onToggleAutoMaxBrightness,
                            onOpenNackScanner = { handleOpenNackScanner() },
                            onToggleMissingChunkFilter = onToggleMissingChunkFilter,
                            onClearMissingChunkFilter = onClearMissingChunkFilter
                        )
                    }
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
                    SourceToolbarPanel(
                        senderState = senderState,
                        onPickFile = { handleLaunchFilePicker() },
                        onPickMultiZip = { handleLaunchMultiZipPicker() },
                        onCapturePhoto = { handleOpenPhotoCapture() },
                        onOpenNoteModal = { showTextNoteDialog = true },
                        onOpenStandardQrModal = { showStandardQrDialog = true },
                        onLoadDemo = onLoadSampleFile,
                        onOpenEncryptionDialog = { showEncryptionDialog = true }
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
                        onOpenEncryptionDialog = { showEncryptionDialog = true },
                        onOpenNackScanner = { handleOpenNackScanner() },
                        onNavigateToReceiveLoopback = onNavigateToReceiveLoopback
                    )

                    senderState.stream?.let { stream ->
                        InspectorTabbedPanel(
                            senderState = senderState,
                            stream = stream,
                            onFpsChange = onFpsChange,
                            onUpdateConfig = onUpdateConfig,
                            onToggleAutoMaxBrightness = onToggleAutoMaxBrightness,
                            onOpenNackScanner = { handleOpenNackScanner() },
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

    if (showEncryptionDialog) {
        AesEncryptionPasswordDialog(
            currentPassword = senderState.encryptionPassword,
            onApplyPassword = { pass ->
                onUpdateEncryptionPassword(pass)
                showEncryptionDialog = false
            },
            onDismiss = { showEncryptionDialog = false }
        )
    }

    if (showStandardQrDialog) {
        UniversalStandardQrDialog(
            onTransmitAsOptiShareStream = { title, content ->
                showStandardQrDialog = false
                onCreateTextNote(title, content)
            },
            onDismiss = { showStandardQrDialog = false }
        )
    }

    if (showNackScannerDialog) {
        NackScannerDialog(
            hasCameraPermission = hasCameraPermission,
            activeSessionId = senderState.stream?.sessionId ?: "SESSION",
            totalChunks = senderState.stream?.dataPackets?.size ?: 4,
            onRequestCameraPermission = {
                runCatching { nackCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            },
            onNackScanned = { qrStr ->
                val res = onScanNackQr(qrStr)
                res.onSuccess { count ->
                    Toast.makeText(
                        context,
                        "NACK acquisito: filtrati $count pacchetti mancanti",
                        Toast.LENGTH_SHORT
                    ).show()
                    showNackScannerDialog = false
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.localizedMessage ?: "QR NACK non valido",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showNackScannerDialog = false }
        )
    }
}

@Composable
private fun AesEncryptionPasswordDialog(
    currentPassword: String,
    onApplyPassword: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passwordInput by rememberSaveable { mutableStateOf(currentPassword) }
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = colorScheme.primary)
                Text("Crittografia AES-256-GCM", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Proteggi il flusso QR con cifratura autenticata AES-256-GCM e chiave derivata PBKDF2-HMAC-SHA256 (65.536 iterazioni). Lascia vuoto per trasmettere in chiaro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Passphrase segreta AES-256") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aes_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyPassword(passwordInput) },
                modifier = Modifier.testTag("apply_aes_password_button")
            ) {
                Text(if (passwordInput.isBlank()) "Disattiva Cifratura" else "Attiva AES-256")
            }
        },
        dismissButton = {
            if (currentPassword.isNotBlank()) {
                TextButton(onClick = { onApplyPassword("") }) {
                    Text("Rimuovi Password")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Annulla")
                }
            }
        }
    )
}

@Composable
private fun UniversalStandardQrDialog(
    onTransmitAsOptiShareStream: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedKind by rememberSaveable { mutableStateOf(StandardQrKind.WIFI) }

    // Wi-Fi state
    var wifiSsid by rememberSaveable { mutableStateOf("OptiShare_Lab_5G") }
    var wifiPassword by rememberSaveable { mutableStateOf("AirGapped2026!") }
    var wifiSecurity by rememberSaveable { mutableStateOf("WPA") }

    // vCard state
    var vcardName by rememberSaveable { mutableStateOf("Valerio Massimo") }
    var vcardPhone by rememberSaveable { mutableStateOf("+39 340 0000000") }
    var vcardEmail by rememberSaveable { mutableStateOf("info@optishare.org") }
    var vcardOrg by rememberSaveable { mutableStateOf("OptiShare FOSS Lab") }

    // Free text / URL state
    var freeText by rememberSaveable { mutableStateOf("https://f-droid.org") }

    val formattedPayload = remember(
        selectedKind,
        wifiSsid,
        wifiPassword,
        wifiSecurity,
        vcardName,
        vcardPhone,
        vcardEmail,
        vcardOrg,
        freeText
    ) {
        when (selectedKind) {
            StandardQrKind.WIFI -> OpticalPacketProtocol.buildWifiStandardQr(
                ssid = wifiSsid,
                password = wifiPassword,
                security = wifiSecurity
            )
            StandardQrKind.VCARD -> OpticalPacketProtocol.buildVCardStandardQr(
                fullName = vcardName,
                phone = vcardPhone,
                email = vcardEmail,
                organization = vcardOrg
            )
            StandardQrKind.TEXT -> freeText.ifBlank { "OptiShare QR" }
        }
    }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(formattedPayload) {
        qrBitmap = withContext(Dispatchers.Default) {
            runCatching {
                QrCodecEngine.generateQrBitmap(
                    content = formattedPayload,
                    sizePx = 600,
                    eccLevel = QrEccLevel.M
                )
            }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Generatore QR Universale (Wi-Fi / vCard / Testo)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SegmentedSelectorBar(
                    items = StandardQrKind.entries,
                    selectedItem = selectedKind,
                    onItemSelected = { selectedKind = it },
                    labelProvider = { it.label }
                )

                qrBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .testTag("standard_qr_preview_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR Standard Universale",
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                when (selectedKind) {
                    StandardQrKind.WIFI -> {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("Nome Rete (SSID)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("Password Wi-Fi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SegmentedSelectorBar(
                            items = listOf("WPA", "WEP", "nopass"),
                            selectedItem = wifiSecurity,
                            onItemSelected = { wifiSecurity = it },
                            labelProvider = {
                                when (it) {
                                    "WPA" -> "WPA/WPA2/WPA3"
                                    "WEP" -> "WEP"
                                    else -> "Aperta"
                                }
                            }
                        )
                    }
                    StandardQrKind.VCARD -> {
                        OutlinedTextField(
                            value = vcardName,
                            onValueChange = { vcardName = it },
                            label = { Text("Nome e Cognome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = vcardPhone,
                            onValueChange = { vcardPhone = it },
                            label = { Text("Telefono") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = vcardEmail,
                            onValueChange = { vcardEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = vcardOrg,
                            onValueChange = { vcardOrg = it },
                            label = { Text("Organizzazione") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    StandardQrKind.TEXT -> {
                        OutlinedTextField(
                            value = freeText,
                            onValueChange = { freeText = it },
                            label = { Text("Testo o URL") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    text = "Questo QR rispetta lo standard universale ed è scansionabile da qualsiasi fotocamera Android/iOS senza app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fileName = when (selectedKind) {
                        StandardQrKind.WIFI -> "WiFi_${wifiSsid.ifBlank { "Rete" }}.txt"
                        StandardQrKind.VCARD -> "${vcardName.ifBlank { "Contatto" }}.vcf"
                        StandardQrKind.TEXT -> "Nota_QR_Standard.txt"
                    }
                    onTransmitAsOptiShareStream(fileName, formattedPayload)
                }
            ) {
                Text("Invia anche in OptiShare")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}

@Composable
private fun NackScannerDialog(
    hasCameraPermission: Boolean,
    activeSessionId: String,
    totalChunks: Int,
    onRequestCameraPermission: () -> Unit,
    onNackScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val colorScheme = MaterialTheme.colorScheme
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Scansiona QR Inverso NACK",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Inquadra il codice QR NACK mostrato sul dispositivo ricevente per trasmettere solo i pacchetti mancanti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, colorScheme.primary, RoundedCornerShape(12.dp))
                    ) {
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

                                        var handled = false
                                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                            try {
                                                if (!handled) {
                                                    val decoded = QrCodecEngine.decodeFromImageProxy(imageProxy)
                                                    if (!decoded.isNullOrBlank() && decoded.startsWith("QBF1_NACK|")) {
                                                        handled = true
                                                        previewView.post {
                                                            onNackScanned(decoded)
                                                        }
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
                } else {
                    OutlinedButton(
                        onClick = onRequestCameraPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Autorizza Fotocamera per NACK")
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val sampleMissing = listOf(0, (totalChunks - 1).coerceAtLeast(0)).distinct()
                        val simulatedNack = OpticalPacketProtocol.encodeNackQrString(
                            sessionId = activeSessionId,
                            missingIndices = sampleMissing
                        )
                        onNackScanned(simulatedNack)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_nack_scan_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simula Scansione NACK (Test Rapido)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

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

private fun generateSimulatedCameraBitmap(): Bitmap {
    val width = 400
    val height = 300
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(24, 34, 52)
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

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

    val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(59, 130, 246)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    canvas.drawCircle(width / 2f, height / 2f, 70f, targetPaint)

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
    senderState: SenderUiState,
    onPickFile: () -> Unit,
    onPickMultiZip: () -> Unit,
    onCapturePhoto: () -> Unit,
    onOpenNoteModal: () -> Unit,
    onOpenStandardQrModal: () -> Unit,
    onLoadDemo: () -> Unit,
    onOpenEncryptionDialog: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isEncrypted = senderState.encryptionPassword.isNotBlank()

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
                    text = "Sorgente file",
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenEncryptionDialog,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEncrypted) colorScheme.tertiaryContainer else colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("toggle_aes_encryption_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isEncrypted) colorScheme.onTertiaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isEncrypted) "AES-256 ON" else "AES-256 OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEncrypted) colorScheme.onTertiaryContainer else colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "GZIP · XOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
                        .weight(1f)
                        .testTag("pick_file_button")
                )
                InstrumentActionButton(
                    label = "ZIP Multi",
                    icon = Icons.Default.FolderZip,
                    onClick = onPickMultiZip,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_multi_zip_button")
                )
                InstrumentActionButton(
                    label = "Foto",
                    icon = Icons.Default.AddPhotoAlternate,
                    onClick = onCapturePhoto,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capture_photo_sender_button")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InstrumentActionButton(
                    label = "Nota",
                    icon = Icons.Default.EditNote,
                    onClick = onOpenNoteModal,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_note_button")
                )
                InstrumentActionButton(
                    label = "QR Std",
                    icon = Icons.Default.QrCode2,
                    onClick = onOpenStandardQrModal,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_standard_qr_button")
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
    onOpenEncryptionDialog: () -> Unit,
    onOpenNackScanner: () -> Unit,
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
                    text = "Generazione QR multi-core, GZIP e parità XOR...",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (stream != null && bitmaps.isNotEmpty()) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stream.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (stream.isEncrypted) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Cifrato AES-256-GCM",
                                    tint = colorScheme.tertiary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onOpenEncryptionDialog() }
                                )
                            }
                        }
                        Text(
                            text = "ID ${stream.sessionId} · ${stream.originalBytes.size} B → ${stream.compressedBytes.size} B" +
                                if (stream.isEncrypted) " (AES-256)" else " (GZIP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            color = statusAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tick = 8.dp.toPx()
                        val stroke = 1.2.dp.toPx()
                        val markColor = Color(0xFF18181B).copy(alpha = 0.35f)
                        drawLine(markColor, Offset(0f, 0f), Offset(tick, 0f), stroke)
                        drawLine(markColor, Offset(0f, 0f), Offset(0f, tick), stroke)
                        drawLine(markColor, Offset(size.width - tick, 0f), Offset(size.width, 0f), stroke)
                        drawLine(markColor, Offset(size.width, 0f), Offset(size.width, tick), stroke)
                        drawLine(markColor, Offset(0f, size.height - tick), Offset(0f, size.height), stroke)
                        drawLine(markColor, Offset(0f, size.height), Offset(tick, size.height), stroke)
                        drawLine(markColor, Offset(size.width - tick, size.height), Offset(size.width, size.height), stroke)
                        drawLine(markColor, Offset(size.width, size.height - tick), Offset(size.width, size.height), stroke)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FRAME %02d/%02d · %.1f KB/s · Ciclo %.1fs".format(
                            Locale.US,
                            safeIndex + 1,
                            cachedImageBitmaps.size,
                            senderState.theoreticalThroughputKbps,
                            senderState.estimatedCycleSeconds
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "${senderState.fps} FPS · 120Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            label = if (senderState.isPlaying) "Pausa Flusso" else "Avvia Flusso",
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
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InstrumentActionButton(
                            label = "Scansiona NACK",
                            icon = Icons.Default.QrCodeScanner,
                            onClick = onOpenNackScanner,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_nack_from_sender_button")
                        )

                        InstrumentActionButton(
                            label = "Test Loopback",
                            icon = Icons.Default.VerifiedUser,
                            onClick = onNavigateToReceiveLoopback,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_loopback_from_sender_button")
                        )
                    }
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
    onToggleAutoMaxBrightness: () -> Unit,
    onOpenNackScanner: () -> Unit,
    onToggleMissingChunkFilter: (Int) -> Unit,
    onClearMissingChunkFilter: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Telemetria", "Calibrazione", "Filtro NACK")

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
                        "NACK (${senderState.selectedMissingDataIndices.size})"
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
                            label = if (stream.isEncrypted) "GZIP + AES-256" else "Compresso GZIP",
                            value = "${stream.compressedBytes.size} B ($ratio)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMetricCell(
                            label = "Throughput Ottico",
                            value = String.format(Locale.US, "%.1f KB/s", senderState.theoreticalThroughputKbps),
                            valueColor = colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCell(
                            label = "Codifica Multi-Core",
                            value = "${senderState.encodingDurationMs} ms (${stream.allOrderedPackets.size} QR)",
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Velocità flusso (120Hz VSync)",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "${senderState.fps} FPS (${1000 / senderState.fps.coerceAtLeast(1)} ms)",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.BrightnessHigh,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Luminosità Massima Automatica",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Imposta il display al 100% durante l'invio QR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = senderState.autoMaxBrightness,
                            onCheckedChange = { onToggleAutoMaxBrightness() },
                            modifier = Modifier.testTag("switch_auto_max_brightness")
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Scansiona il QR NACK del ricevitore o tocca i pacchetti da ritrasmettere:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = onOpenNackScanner,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scansiona QR NACK dal Ricevitore",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (senderState.selectedMissingDataIndices.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtro attivo: ${senderState.selectedMissingDataIndices.size}/${stream.dataPackets.size} PKT",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            TextButton(onClick = onClearMissingChunkFilter) {
                                Text(
                                    text = "Ripristina Tutti",
                                    maxLines = 1,
                                    softWrap = false
                                )
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
