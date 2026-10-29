package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TransferRecordEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.VerifiedEmerald
import com.example.viewmodel.AiAnalysisUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiStudioScreen(
    uiState: AiAnalysisUiState,
    recentRecords: List<TransferRecordEntity>,
    onAnalyzeBitmap: (Bitmap, String) -> Unit,
    onAnalyzeCustomText: (String, String) -> Unit,
    onAnalyzeVaultRecord: (TransferRecordEntity) -> Unit,
    onApplySmartRename: () -> Unit,
    onSendExtractedTextViaQr: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customTitle by rememberSaveable { mutableStateOf("Appunti_Tecnici_Progetto.txt") }
    var customInputText by rememberSaveable {
        mutableStateOf(
            "Verbale Riunione Architettura Ottica - 29 Ottobre 2026.\n" +
                "Partecipanti: Team Ingegneria Mobile & Sicurezza Crittografica.\n" +
                "Decisioni: Adozione codici QR multi-frame con blocchi di parità XOR ogni 4 pacchetti dati, " +
                "compressione GZIP automatica e verifica SHA-256 lato ricevitore prima del salvataggio su Room DB."
        )
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onAnalyzeBitmap(bitmap, customTitle)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                if (bmp != null) {
                    onAnalyzeBitmap(bmp, customTitle)
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
            // 1. Header Card with High Thinking Badge
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, QuantumViolet.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = QuantumViolet,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "AI Document Studio",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "OCR, Riassunto Esecutivo & Rinomina Smart",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = QuantumViolet.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "HIGH THINKING",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumViolet,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { takePictureLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumViolet,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_capture_document_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraEnhance, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scansiona Foto OCR")
                        }

                        FilledTonalButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_pick_image_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Da Galleria")
                        }
                    }
                }
            }

            // 2. Text / File Input Analysis Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Analizza Testo o File Recente con Gemini 4 Argon",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Nome File / Contesto") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_title_input")
                    )

                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        label = { Text("Testo o Contenuto Documentale da Riassumere e Rinominare") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_text_input")
                    )

                    Button(
                        onClick = { onAnalyzeCustomText(customInputText, customTitle) },
                        enabled = uiState !is AiAnalysisUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_analyze_text_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ai_analyze_button))
                    }

                    if (recentRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Oppure seleziona un file dall'Archivio locale:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            recentRecords.take(3).forEach { rec ->
                                OutlinedButton(
                                    onClick = { onAnalyzeVaultRecord(rec) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Analizza: ${rec.fileName} (${rec.fileSizeBytes} B)",
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. AI Result State
            when (uiState) {
                is AiAnalysisUiState.Idle -> Unit

                is AiAnalysisUiState.Loading -> {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = QuantumViolet)
                            Text(
                                text = "Ragionamento approfondito (ThinkingLevel.HIGH) in corso su '${uiState.targetLabel}'...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                is AiAnalysisUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_error_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Errore Analisi AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                is AiAnalysisUiState.Success -> {
                    val insight = uiState.insight
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, VerifiedEmerald, RoundedCornerShape(22.dp))
                            .testTag("ai_result_card")
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
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = OpticalCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = insight.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OpticalCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (uiState.renameApplied) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = VerifiedEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Salvato in Archivio",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VerifiedEmerald
                                        )
                                    }
                                }
                            }

                            // Smart Suggested File Name Box
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Rinomina Intelligente Proposta:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = insight.suggestedFileName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = VerifiedEmerald
                                    )

                                    if (uiState.sourceRecordId != null && !uiState.renameApplied) {
                                        FilledTonalButton(
                                            onClick = onApplySmartRename,
                                            modifier = Modifier.testTag("apply_smart_rename_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.apply_smart_rename))
                                        }
                                    }
                                }
                            }

                            // Executive Summary
                            Text(
                                text = "Riassunto Esecutivo",
                                style = MaterialTheme.typography.titleSmall,
                                color = OpticalCyan
                            )
                            Text(
                                text = insight.executiveSummary,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            // Key Points
                            if (insight.keyPoints.isNotEmpty()) {
                                Text(
                                    text = "Punti Chiave Estratti",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QuantumViolet
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    insight.keyPoints.forEach { pt ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("•", color = QuantumViolet, fontWeight = FontWeight.Bold)
                                            Text(pt, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }

                            // Extracted OCR / Full Text
                            if (insight.extractedText.isNotBlank()) {
                                Text(
                                    text = "Testo Estratto / Ottimizzato per QR",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = VerifiedEmerald
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = insight.extractedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Consiglio Ottico: ${insight.qrOptimizationTip}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Send Extracted Content directly via Dynamic QR
                            Button(
                                onClick = {
                                    val payloadText = buildString {
                                        appendLine("# ${insight.suggestedFileName}")
                                        appendLine("Categoria: ${insight.category}")
                                        appendLine()
                                        appendLine("## Riassunto")
                                        appendLine(insight.executiveSummary)
                                        appendLine()
                                        appendLine("## Testo Estratto")
                                        appendLine(insight.extractedText)
                                    }
                                    onSendExtractedTextViaQr(insight.suggestedFileName, payloadText)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_ai_extracted_qr_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.send_extracted_via_qr))
                            }
                        }
                    }
                }
            }
        }
    }
}
