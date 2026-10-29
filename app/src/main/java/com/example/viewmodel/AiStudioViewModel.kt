package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.TransferRecordEntity
import com.example.data.repository.TransferRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AiStudioViewModel"

data class AiDocumentInsight(
    val suggestedFileName: String,
    val category: String,
    val executiveSummary: String,
    val extractedText: String,
    val keyPoints: List<String>,
    val qrOptimizationTip: String
)

sealed interface AiAnalysisUiState {
    data object Idle : AiAnalysisUiState
    data class Loading(val targetLabel: String) : AiAnalysisUiState
    data class Success(
        val insight: AiDocumentInsight,
        val sourceRecordId: Long? = null,
        val renameApplied: Boolean = false
    ) : AiAnalysisUiState
    data class Error(val message: String) : AiAnalysisUiState
}

class AiStudioViewModel(
    private val repository: TransferRepository
) : ViewModel() {

    private val documentSchema = Schema.obj(
        properties = mapOf(
            "suggestedFileName" to Schema.string(
                description = "Clean, descriptive filename with extension (e.g. Fattura_Energia_Ottobre_2026.txt)"
            ),
            "category" to Schema.string(
                description = "Document category in Italian (e.g. Documento Tecnico, Fattura, Codice, Appunti, Ricevuta)"
            ),
            "executiveSummary" to Schema.string(
                description = "Concise, clear Italian summary of the document or image content"
            ),
            "extractedText" to Schema.string(
                description = "Full OCR extracted text or cleaned text suitable for compact QR transfer"
            ),
            "keyPoints" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 key bullet points in Italian extracted from the file"
            ),
            "qrOptimizationTip" to Schema.string(
                description = "Explanation of how much size can be saved by sending the extracted structured text via QR"
            )
        )
    )

    // Mandatory: Use gemini-4-argon with ThinkingLevel.HIGH and do NOT set maxOutputTokens
    private val highThinkingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = documentSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    private val _uiState = MutableStateFlow<AiAnalysisUiState>(AiAnalysisUiState.Idle)
    val uiState: StateFlow<AiAnalysisUiState> = _uiState.asStateFlow()

    fun analyzeVaultRecord(record: TransferRecordEntity) {
        viewModelScope.launch {
            _uiState.value = AiAnalysisUiState.Loading(record.fileName)
            try {
                val bytes = repository.readRecordBytes(record)
                val fileTextSnippet = record.extractedText ?: bytes?.let {
                    runCatching { it.toString(Charsets.UTF_8).take(12_000) }.getOrNull()
                } ?: ""

                val prompt = """
                    Analizza in modo approfondito questo file trasferito tramite OptiShare QR.
                    Nome attuale del file: ${record.fileName}
                    MIME Type: ${record.mimeType}
                    Dimensione originale: ${record.fileSizeBytes} byte
                    SHA-256: ${record.sha256Hash}
                    Contenuto testuale rilevato:
                    $fileTextSnippet
                    
                    1. Estrai o ricostruisci tutto il testo significativo in modo pulito e ben formattato.
                    2. Genera un riassunto esecutivo chiaro in italiano e i punti chiave principali.
                    3. Suggerisci un nome file intelligente, semantico e privo di spazi (usa underscore) mantenendo l'estensione appropriata.
                    4. Fornisci un consiglio su come ottimizzare il contenuto per la trasmissione ottica QR.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    highThinkingModel.generateContent(prompt)
                }

                val jsonString = response.text
                    ?: throw IllegalStateException("Il modello ha restituito una risposta vuota.")
                val insight = parseInsightJson(jsonString)

                repository.updateRecordWithAiInsights(
                    recordId = record.id,
                    suggestedName = insight.suggestedFileName,
                    applyRenameImmediately = false,
                    category = insight.category,
                    summary = insight.executiveSummary,
                    extractedText = insight.extractedText,
                    keyPointsJson = JSONArray(insight.keyPoints).toString()
                )

                _uiState.value = AiAnalysisUiState.Success(
                    insight = insight,
                    sourceRecordId = record.id,
                    renameApplied = false
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI analysis timed out", e)
                _uiState.value = AiAnalysisUiState.Error(
                    "L'analisi approfondita con Gemini 4 Argon ha superato i 60 secondi. Riprova."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI analysis failed", e)
                _uiState.value = AiAnalysisUiState.Error(
                    e.localizedMessage ?: "Errore durante l'analisi AI del file."
                )
            }
        }
    }

    fun analyzeCapturedImage(bitmap: Bitmap, customInstruction: String = "") {
        viewModelScope.launch {
            _uiState.value = AiAnalysisUiState.Loading("Foto / Documento Acquisito")
            try {
                val prompt = buildString {
                    appendLine("Sei il motore ottico AI di OptiShare QR (Gemini 4 Argon High Thinking).")
                    appendLine("Esegui l'OCR completo dell'immagine, estrai tutto il testo visibile o descrivi dettagliatamente il documento,")
                    appendLine("genera un riassunto esecutivo in italiano, elenca i punti chiave e proponi un nome file semantico ideale.")
                    if (customInstruction.isNotBlank()) {
                        appendLine("Istruzione aggiuntiva dell'utente: $customInstruction")
                    }
                }

                val response = withTimeout(60_000L) {
                    highThinkingModel.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                }

                val jsonString = response.text
                    ?: throw IllegalStateException("Il modello ha restituito una risposta vuota.")
                val insight = parseInsightJson(jsonString)

                // Save the AI-extracted document into the local vault so it's immediately available offline
                val extractedBytes = insight.extractedText.ifBlank { insight.executiveSummary }
                    .toByteArray(Charsets.UTF_8)
                val sha = com.example.domain.protocol.OpticalPacketProtocol.sha256Hex(extractedBytes)
                val saved = repository.saveTransferFileAndRecord(
                    sessionId = "AI-${System.currentTimeMillis().toString().takeLast(5)}",
                    fileName = insight.suggestedFileName.ifBlank { "Documento_Estratto_AI.txt" },
                    mimeType = "text/plain",
                    rawBytes = extractedBytes,
                    compressedSizeBytes = extractedBytes.size.toLong(),
                    totalDataChunks = 1,
                    parityChunks = 0,
                    sha256Hash = sha,
                    integrityVerified = true,
                    direction = "AI_OPTIMIZED",
                    extractedText = insight.extractedText,
                    aiSummary = insight.executiveSummary,
                    smartSuggestedName = insight.suggestedFileName,
                    aiCategory = insight.category,
                    aiKeyPointsJson = JSONArray(insight.keyPoints).toString()
                )

                _uiState.value = AiAnalysisUiState.Success(
                    insight = insight,
                    sourceRecordId = saved.id,
                    renameApplied = true
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Multimodal AI analysis timed out", e)
                _uiState.value = AiAnalysisUiState.Error(
                    "Analisi immagine scaduta dopo 60 secondi. Riprova."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multimodal AI analysis failed", e)
                _uiState.value = AiAnalysisUiState.Error(
                    e.localizedMessage ?: "Impossibile completare l'analisi AI dell'immagine."
                )
            }
        }
    }

    fun analyzeCustomTextOrDocument(rawText: String, currentTitle: String) {
        if (rawText.isBlank()) {
            _uiState.value = AiAnalysisUiState.Error("Inserisci o seleziona un testo o file da analizzare.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AiAnalysisUiState.Loading(currentTitle.ifBlank { "Testo / Documento" })
            try {
                val prompt = """
                    Analizza questo documento per la trasmissione ottica via QR code.
                    Titolo provvisorio: $currentTitle
                    Testo:
                    $rawText
                    
                    Estrai e formatta il testo essenziale, genera un riassunto esecutivo in italiano,
                    individua i punti chiave e proponi un nome file intelligente con estensione .txt o .md.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    highThinkingModel.generateContent(prompt)
                }

                val jsonString = response.text
                    ?: throw IllegalStateException("Risposta vuota dal modello AI.")
                val insight = parseInsightJson(jsonString)

                val bytes = insight.extractedText.ifBlank { rawText }.toByteArray(Charsets.UTF_8)
                val sha = com.example.domain.protocol.OpticalPacketProtocol.sha256Hex(bytes)
                val saved = repository.saveTransferFileAndRecord(
                    sessionId = "AI-${System.currentTimeMillis().toString().takeLast(5)}",
                    fileName = insight.suggestedFileName.ifBlank { "Documento_Smart_AI.txt" },
                    mimeType = "text/plain",
                    rawBytes = bytes,
                    compressedSizeBytes = bytes.size.toLong(),
                    totalDataChunks = 1,
                    parityChunks = 0,
                    sha256Hash = sha,
                    integrityVerified = true,
                    direction = "AI_OPTIMIZED",
                    extractedText = insight.extractedText,
                    aiSummary = insight.executiveSummary,
                    smartSuggestedName = insight.suggestedFileName,
                    aiCategory = insight.category,
                    aiKeyPointsJson = JSONArray(insight.keyPoints).toString()
                )

                _uiState.value = AiAnalysisUiState.Success(
                    insight = insight,
                    sourceRecordId = saved.id,
                    renameApplied = true
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Text AI analysis timed out", e)
                _uiState.value = AiAnalysisUiState.Error("Timeout analisi (60s). Riprova.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Text AI analysis failed", e)
                _uiState.value = AiAnalysisUiState.Error(
                    e.localizedMessage ?: "Errore durante l'elaborazione AI."
                )
            }
        }
    }

    fun applySmartRenameToSourceRecord() {
        val state = _uiState.value as? AiAnalysisUiState.Success ?: return
        val recordId = state.sourceRecordId ?: return
        viewModelScope.launch {
            repository.renameRecord(recordId, state.insight.suggestedFileName)
            _uiState.value = state.copy(renameApplied = true)
        }
    }

    fun resetState() {
        _uiState.value = AiAnalysisUiState.Idle
    }

    private fun parseInsightJson(jsonString: String): AiDocumentInsight {
        val json = JSONObject(jsonString)
        val suggestedName = json.optString("suggestedFileName", "Documento_Ottimizzato.txt")
        val category = json.optString("category", "Documento")
        val summary = json.optString("executiveSummary", "")
        val extracted = json.optString("extractedText", "")
        val tip = json.optString("qrOptimizationTip", "Pronto per la trasmissione QR ad alta velocità.")
        val pointsArr = json.optJSONArray("keyPoints") ?: JSONArray()
        val points = (0 until pointsArr.length()).map { pointsArr.getString(it) }

        return AiDocumentInsight(
            suggestedFileName = suggestedName,
            category = category,
            executiveSummary = summary,
            extractedText = extracted,
            keyPoints = points,
            qrOptimizationTip = tip
        )
    }

    class Factory(private val repository: TransferRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiStudioViewModel(repository) as T
        }
    }
}
