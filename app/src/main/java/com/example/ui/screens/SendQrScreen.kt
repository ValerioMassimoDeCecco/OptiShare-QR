package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.protocol.OpticalPacketProtocol
import com.example.domain.protocol.PacketKind
import com.example.domain.protocol.QrCodecEngine
import com.example.domain.protocol.QrEccLevel
import com.example.domain.protocol.StandardQrKind
import com.example.ui.components.InstrumentActionButton
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.OptiShareCard
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.rememberOptiShareHaptics
import com.example.viewmodel.SenderUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun SendQrScreen(
    senderState: SenderUiState,
    onPickFileUri: (Uri) -> Unit,
    onPickMultipleFileUris: (List<Uri>) -> Unit,
    onCapturePhotoBitmap: (Bitmap) -> Unit,
    onCreateTextNote: (String, String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onAdvanceAutoFrame: () -> Unit,
    onFpsChange: (Int) -> Unit,
    onUpdateConfig: (Int?, QrEccLevel?) -> Unit,
    onUpdateEncryptionPassword: (String) -> Unit,
    onToggleAutoMaxBrightness: () -> Unit,
    onScanNackQr: (String) -> Result<Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberOptiShareHaptics()
    var showTextNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoCaptureDialog by rememberSaveable { mutableStateOf(false) }
    var showStandardQrDialog by rememberSaveable { mutableStateOf(false) }
    var showEncryptionDialog by rememberSaveable { mutableStateOf(false) }
    var showNackScannerDialog by rememberSaveable { mutableStateOf(false) }

    var noteTitle by rememberSaveable { mutableStateOf("") }
    var noteBody by rememberSaveable { mutableStateOf("") }

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
                "Selettore file non disponibile: usa Nota o Foto",
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
                        onPickFile = { handleLaunchFilePicker() },
                        onTogglePlayPause = onTogglePlayPause,
                        onStepFrame = onStepFrame,
                        onOpenEncryptionDialog = { showEncryptionDialog = true },
                        onOpenNackScanner = { handleOpenNackScanner() }
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
                        onOpenEncryptionDialog = { showEncryptionDialog = true }
                    )

                    senderState.stream?.let {
                        SenderSimpleOptionsPanel(
                            senderState = senderState,
                            onFpsChange = onFpsChange,
                            onUpdateConfig = onUpdateConfig,
                            onToggleAutoMaxBrightness = onToggleAutoMaxBrightness,
                            onOpenNackScanner = { handleOpenNackScanner() }
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
                        onPickFile = { handleLaunchFilePicker() },
                        onTogglePlayPause = onTogglePlayPause,
                        onStepFrame = onStepFrame,
                        onOpenEncryptionDialog = { showEncryptionDialog = true },
                        onOpenNackScanner = { handleOpenNackScanner() }
                    )

                    senderState.stream?.let {
                        SenderSimpleOptionsPanel(
                            senderState = senderState,
                            onFpsChange = onFpsChange,
                            onUpdateConfig = onUpdateConfig,
                            onToggleAutoMaxBrightness = onToggleAutoMaxBrightness,
                            onOpenNackScanner = { handleOpenNackScanner() }
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
                    Toast.makeText(context, "Impossibile aprire la galleria", Toast.LENGTH_SHORT).show()
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
                    text = "Nuova nota",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Titolo") },
                        placeholder = { Text("Es. Appunti") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text("Testo") },
                        placeholder = { Text("Scrivi qui cosa vuoi inviare…") },
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
                    },
                    enabled = noteBody.isNotBlank()
                ) {
                    Text("Crea QR")
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
                haptics.confirmSuccess()
                onUpdateEncryptionPassword(pass)
                showEncryptionDialog = false
            },
            onDismiss = { showEncryptionDialog = false }
        )
    }

    if (showStandardQrDialog) {
        UniversalStandardQrDialog(
            onTransmitAsOptiShareStream = { title, content ->
                haptics.confirmSuccess()
                showStandardQrDialog = false
                onCreateTextNote(title, content)
            },
            onDismiss = { showStandardQrDialog = false }
        )
    }

    if (showNackScannerDialog) {
        NackScannerDialog(
            hasCameraPermission = hasCameraPermission,
            onRequestCameraPermission = {
                runCatching { nackCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            },
            onNackScanned = { qrStr ->
                val res = onScanNackQr(qrStr)
                res.onSuccess { count ->
                    haptics.confirmSuccess()
                    Toast.makeText(
                        context,
                        "Invio solo le $count parti mancanti",
                        Toast.LENGTH_SHORT
                    ).show()
                    showNackScannerDialog = false
                }.onFailure { err ->
                    haptics.notifyError()
                    Toast.makeText(
                        context,
                        err.localizedMessage ?: "Codice QR non valido",
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
                Text("Proteggi con password", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Chi riceve il file dovrà inserire questa password per aprirlo. Lascia vuoto per inviare senza password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
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
                Text(if (passwordInput.isBlank()) "Rimuovi protezione" else "Imposta password")
            }
        },
        dismissButton = {
            if (currentPassword.isNotBlank()) {
                TextButton(onClick = { onApplyPassword("") }) {
                    Text("Rimuovi")
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
    var wifiSsid by rememberSaveable { mutableStateOf("") }
    var wifiPassword by rememberSaveable { mutableStateOf("") }
    var wifiSecurity by rememberSaveable { mutableStateOf("WPA") }

    // vCard state
    var vcardName by rememberSaveable { mutableStateOf("") }
    var vcardPhone by rememberSaveable { mutableStateOf("") }
    var vcardEmail by rememberSaveable { mutableStateOf("") }
    var vcardOrg by rememberSaveable { mutableStateOf("") }

    // Free text / URL state
    var freeText by rememberSaveable { mutableStateOf("") }

    val hasContent = when (selectedKind) {
        StandardQrKind.WIFI -> wifiSsid.isNotBlank()
        StandardQrKind.VCARD -> vcardName.isNotBlank() || vcardPhone.isNotBlank()
        StandardQrKind.TEXT -> freeText.isNotBlank()
    }

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
                ssid = wifiSsid.ifBlank { "Rete_WiFi" },
                password = wifiPassword,
                security = wifiSecurity
            )
            StandardQrKind.VCARD -> OpticalPacketProtocol.buildVCardStandardQr(
                fullName = vcardName.ifBlank { "Contatto" },
                phone = vcardPhone,
                email = vcardEmail,
                organization = vcardOrg
            )
            StandardQrKind.TEXT -> freeText.ifBlank { " " }
        }
    }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(formattedPayload, hasContent) {
        qrBitmap = if (!hasContent) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching {
                    QrCodecEngine.generateQrBitmap(
                        content = formattedPayload,
                        sizePx = 600,
                        eccLevel = QrEccLevel.M
                    )
                }.getOrNull()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "QR Semplice",
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
                    labelProvider = {
                        when (it) {
                            StandardQrKind.WIFI -> "Wi-Fi"
                            StandardQrKind.VCARD -> "Contatto"
                            StandardQrKind.TEXT -> "Testo"
                        }
                    }
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
                            contentDescription = "Codice QR semplice",
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
                            label = { Text("Nome rete Wi-Fi") },
                            placeholder = { Text("Es. Casa_WiFi") },
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
                            items = listOf("WPA", "nopass"),
                            selectedItem = if (wifiSecurity == "nopass") "nopass" else "WPA",
                            onItemSelected = { wifiSecurity = it },
                            labelProvider = {
                                if (it == "nopass") "Rete aperta" else "Con password"
                            }
                        )
                    }
                    StandardQrKind.VCARD -> {
                        OutlinedTextField(
                            value = vcardName,
                            onValueChange = { vcardName = it },
                            label = { Text("Nome e cognome") },
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
                            label = { Text("Azienda o nota (opzionale)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    StandardQrKind.TEXT -> {
                        OutlinedTextField(
                            value = freeText,
                            onValueChange = { freeText = it },
                            label = { Text("Testo o link") },
                            placeholder = { Text("Scrivi un messaggio o incolla un link…") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    text = "Questo QR si legge con la normale fotocamera di qualsiasi telefono.",
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
                        StandardQrKind.TEXT -> "Nota_QR.txt"
                    }
                    onTransmitAsOptiShareStream(fileName, formattedPayload)
                },
                enabled = hasContent
            ) {
                Text("Salva e invia")
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
                text = "Inquadra QR parti mancanti",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Inquadra il codice QR mostrato sull'altro telefono per inviare solo le parti che mancano.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
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
                        Text("Attiva fotocamera")
                    }
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
                text = "Scatta o scegli una foto",
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
                                text = "Attiva la fotocamera per scattare una foto oppure scegline una dalla galleria.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = onRequestCameraPermission) {
                                Text("Attiva fotocamera")
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scegli dalla galleria", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            if (hasCameraPermission) {
                Button(
                    onClick = {
                        previewViewRef?.bitmap?.let(onPhotoCaptured)
                    },
                    modifier = Modifier.testTag("confirm_capture_photo_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scatta foto")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun SourceToolbarPanel(
    senderState: SenderUiState,
    onPickFile: () -> Unit,
    onPickMultiZip: () -> Unit,
    onCapturePhoto: () -> Unit,
    onOpenNoteModal: () -> Unit,
    onOpenStandardQrModal: () -> Unit,
    onOpenEncryptionDialog: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isEncrypted = senderState.encryptionPassword.isNotBlank()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary action row: Pick File + ZIP + Password lock toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InstrumentActionButton(
                label = "Scegli file",
                icon = Icons.Default.FolderOpen,
                onClick = onPickFile,
                isPrimary = true,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("pick_file_button")
            )
            InstrumentActionButton(
                label = "ZIP",
                icon = Icons.Default.FolderZip,
                onClick = onPickMultiZip,
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("pick_multi_zip_button")
            )
            LiquidGlassPill(
                onClick = onOpenEncryptionDialog,
                selected = isEncrypted,
                cornerRadius = 12.dp,
                accentColor = if (isEncrypted) colorScheme.tertiary else colorScheme.primary,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("toggle_aes_encryption_chip")
            ) {
                Icon(
                    imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isEncrypted) "Password attiva" else "Imposta password",
                    tint = if (isEncrypted) colorScheme.tertiary else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isEncrypted) "Protetto" else "Password",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isEncrypted) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Secondary compact horizontal quick-source strip (no demo/simulation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactQuickChip(
                label = "Foto",
                icon = Icons.Default.AddPhotoAlternate,
                onClick = onCapturePhoto,
                testTag = "capture_photo_sender_button"
            )
            CompactQuickChip(
                label = "Nota",
                icon = Icons.Default.EditNote,
                onClick = onOpenNoteModal,
                testTag = "create_note_button"
            )
            CompactQuickChip(
                label = "QR Semplice",
                icon = Icons.Default.QrCode2,
                onClick = onOpenStandardQrModal,
                testTag = "open_standard_qr_button"
            )
        }
    }
}

@Composable
private fun CompactQuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = colorScheme.onSurfaceVariant,
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun OpticalStagePanel(
    senderState: SenderUiState,
    onPickFile: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onOpenEncryptionDialog: () -> Unit,
    onOpenNackScanner: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = rememberOptiShareHaptics()
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
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Preparazione codici QR…",
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

        val speedLabel = when {
            senderState.fps <= 5 -> "Lenta"
            senderState.fps <= 10 -> "Normale"
            else -> "Veloce"
        }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = stream.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (stream.isEncrypted) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Protetto con password",
                                tint = colorScheme.tertiary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onOpenEncryptionDialog() }
                            )
                        }
                    }

                    Text(
                        text = if (isParity) {
                            "Recupero"
                        } else {
                            "${currentPacket.displayNumber} di ${stream.dataPackets.size}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .testTag("dynamic_qr_image_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentImageBitmap,
                        contentDescription = "Codice QR ${safeIndex + 1} di ${cachedImageBitmaps.size}",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { smoothProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = statusAccent,
                    trackColor = colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QR ${safeIndex + 1} di ${cachedImageBitmaps.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Velocità: $speedLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Essential 3-button transport row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.selectionTick()
                            onStepFrame(-1)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .testTag("prev_qr_frame_button")
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "QR precedente",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    InstrumentActionButton(
                        label = if (senderState.isPlaying) "Pausa" else "Avvia",
                        icon = if (senderState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = {
                            haptics.toggle(!senderState.isPlaying)
                            onTogglePlayPause()
                        },
                        isPrimary = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("play_pause_qr_button")
                    )

                    IconButton(
                        onClick = {
                            haptics.selectionTick()
                            onStepFrame(1)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .testTag("next_qr_frame_button")
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "QR successivo",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Helpful optional action when receiver needs missing parts
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onOpenNackScanner,
                        modifier = Modifier.testTag("scan_nack_from_sender_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Parti mancanti? Inquadra QR", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    } else {
        OptiShareCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = "Pronto per inviare",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Scegli un file, crea uno ZIP, scatta una foto o scrivi una nota. L'altro telefono dovrà solo inquadrare i QR dalla scheda Ricevi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                FilledTonalButton(onClick = onPickFile) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scegli un file")
                }
            }
        }
    }
}

@Composable
private fun SenderSimpleOptionsPanel(
    senderState: SenderUiState,
    onFpsChange: (Int) -> Unit,
    onUpdateConfig: (Int?, QrEccLevel?) -> Unit,
    onToggleAutoMaxBrightness: () -> Unit,
    onOpenNackScanner: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = rememberOptiShareHaptics()
    var expanded by rememberSaveable { mutableStateOf(false) }

    val selectedSpeedTier = when {
        senderState.fps <= 5 -> 4
        senderState.fps <= 10 -> 8
        else -> 14
    }
    val speedSummary = when (selectedSpeedTier) {
        4 -> "Lenta"
        8 -> "Normale"
        else -> "Veloce"
    }
    val highReadability = senderState.chunkSize <= 240

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.selectionTick()
                        expanded = !expanded
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Opzioni invio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Velocità $speedSummary · ${if (senderState.autoMaxBrightness) "Luminosità max" else "Luminosità auto"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Comprimi opzioni" else "Espandi opzioni",
                    tint = colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Velocità dei codici QR",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SegmentedSelectorBar(
                            items = listOf(4, 8, 14),
                            selectedItem = selectedSpeedTier,
                            onItemSelected = { onFpsChange(it) },
                            labelProvider = { tier ->
                                when (tier) {
                                    4 -> "Lenta"
                                    8 -> "Normale"
                                    else -> "Veloce"
                                }
                            }
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
                            Text(
                                text = "Luminosità massima durante l'invio",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = senderState.autoMaxBrightness,
                            onCheckedChange = {
                                haptics.toggle(!senderState.autoMaxBrightness)
                                onToggleAutoMaxBrightness()
                            },
                            modifier = Modifier.testTag("switch_auto_max_brightness")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "QR più grandi e facili da leggere",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Utile se lo schermo è piccolo o c'è molta luce",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = highReadability,
                            onCheckedChange = { enabled ->
                                haptics.toggle(enabled)
                                if (enabled) {
                                    onUpdateConfig(240, QrEccLevel.Q)
                                } else {
                                    onUpdateConfig(380, QrEccLevel.M)
                                }
                            },
                            modifier = Modifier.testTag("switch_high_readability_qr")
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            haptics.selectionTick()
                            onOpenNackScanner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inquadra QR parti mancanti")
                    }
                }
            }
        }
    }
}
