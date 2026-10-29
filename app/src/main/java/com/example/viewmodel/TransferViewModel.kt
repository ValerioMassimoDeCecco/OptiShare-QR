package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.TransferRecordEntity
import com.example.data.repository.TransferRepository
import com.example.domain.protocol.ChunkReceptionState
import com.example.domain.protocol.OpticalPacket
import com.example.domain.protocol.OpticalPacketProtocol
import com.example.domain.protocol.PacketKind
import com.example.domain.protocol.PreparedOpticalStream
import com.example.domain.protocol.QrCodecEngine
import com.example.domain.protocol.QrEccLevel
import com.example.domain.protocol.ReconstructedFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class SenderUiState(
    val isPreparing: Boolean = false,
    val stream: PreparedOpticalStream? = null,
    val qrBitmaps: List<Bitmap> = emptyList(),
    val currentFramePosition: Int = 0,
    val isPlaying: Boolean = true,
    val fps: Int = 5,
    val chunkSize: Int = 380,
    val blockSize: Int = 4,
    val eccLevel: QrEccLevel = QrEccLevel.M,
    val selectedMissingDataIndices: Set<Int> = emptySet(),
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

data class ReceiverUiState(
    val activeSessionId: String? = null,
    val fileName: String = "",
    val mimeType: String = "",
    val expectedSha256: String = "",
    val originalSizeBytes: Long = 0L,
    val compressedSizeBytes: Long = 0L,
    val totalDataChunks: Int = 0,
    val blockSize: Int = 4,
    val chunkSize: Int = 380,
    val chunkStates: Map<Int, ChunkReceptionState> = emptyMap(),
    val receivedParityBlocks: Set<Int> = emptySet(),
    val validFramesScanned: Int = 0,
    val crcErrorsRejected: Int = 0,
    val parityRecoveredCount: Int = 0,
    val lastScannedPacketLabel: String? = null,
    val transferProgress: Float = 0f,
    val elapsedMs: Long = 0L,
    val completedResult: ReconstructedFileResult? = null,
    val savedRecord: TransferRecordEntity? = null,
    val isLoopbackRunning: Boolean = false,
    val statusBanner: String = "Inquadra il flusso QR dinamico per iniziare la ricezione automatica"
) {
    val missingDataChunkIndices: List<Int>
        get() = if (totalDataChunks == 0) emptyList()
        else (0 until totalDataChunks).filter { chunkStates[it] == null || chunkStates[it] == ChunkReceptionState.PENDING }
}

class TransferViewModel(
    private val repository: TransferRepository
) : ViewModel() {

    private val _senderState = MutableStateFlow(SenderUiState())
    val senderState: StateFlow<SenderUiState> = _senderState.asStateFlow()

    private val _receiverState = MutableStateFlow(ReceiverUiState())
    val receiverState: StateFlow<ReceiverUiState> = _receiverState.asStateFlow()

    val vaultRecords: StateFlow<List<TransferRecordEntity>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val receivedDataPayloads = mutableMapOf<Int, ByteArray>()
    private val receivedParityPackets = mutableMapOf<Int, OpticalPacket>()
    private val parityRecoveredSet = mutableSetOf<Int>()
    private var receptionStartTimeMs: Long = 0L
    private var loopbackJob: Job? = null

    init {
        loadSampleSpecDocument()
    }

    fun loadSampleSpecDocument() {
        val sampleText = """
            ==========================================================
            OPTISHARE QR — PROTOCOLLO OTTICO FOSS (F-DROID READY)
            ==========================================================
            Modalità: Trasmissione Multi-Frame QR (100% Air-Gapped Offline)
            Licenza & Dipendenze: Stack 100% Open Source (Apache 2.0)
            Permessi di Rete: NESSUNO (Zero Anti-Feature)
            
            1. ARCHITETTURA DI TRASFERIMENTO OTTICO
            - Compressione binaria GZIP pre-trasmissione.
            - Suddivisione in pacchetti indicizzati con controllo CRC32 su ogni singolo QR.
            - Correzione Errori Forward (FEC) tramite blocchi di Parità XOR: se la fotocamera
              perde un frame in un blocco, il pacchetto viene ricostruito matematicamente
              senza bisogno di attendere un intero ciclo di ripetizione.
            - Verifica finale con impronta crittografica SHA-256 sull'intero file ricostruito.
        """.trimIndent()

        prepareStreamFromBytes(
            fileName = "Specifica_OptiShare_FOSS.txt",
            mimeType = "text/plain",
            rawBytes = sampleText.toByteArray(Charsets.UTF_8),
            saveToVaultAsSent = false
        )
    }

    fun prepareStreamFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _senderState.update { it.copy(isPreparing = true, errorMessage = null) }
            try {
                val (name, mime, bytes) = withContext(Dispatchers.IO) {
                    val contentResolver = context.contentResolver
                    var displayName = "documento_condiviso.bin"
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx != -1 && cursor.moveToFirst()) {
                            displayName = cursor.getString(nameIdx) ?: displayName
                        }
                    }
                    val resolvedMime = contentResolver.getType(uri) ?: "application/octet-stream"
                    val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Impossibile leggere il file selezionato")

                    Triple(displayName, resolvedMime, raw)
                }

                val processedBytes = if (bytes.size > 35_000) {
                    bytes.copyOfRange(0, 35_000)
                } else {
                    bytes
                }

                prepareStreamFromBytes(
                    fileName = name,
                    mimeType = mime,
                    rawBytes = processedBytes,
                    saveToVaultAsSent = true
                )
            } catch (e: Exception) {
                _senderState.update {
                    it.copy(
                        isPreparing = false,
                        errorMessage = "Errore caricamento file: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun prepareStreamFromCapturedPhoto(bitmap: Bitmap, customTitle: String = "Scansione_Fotocamera.jpg") {
        viewModelScope.launch {
            _senderState.update { it.copy(isPreparing = true, errorMessage = null) }
            try {
                val compressedJpeg = withContext(Dispatchers.Default) {
                    val maxDim = 480
                    val ratio = minOf(
                        maxDim.toFloat() / bitmap.width.toFloat(),
                        maxDim.toFloat() / bitmap.height.toFloat(),
                        1f
                    )
                    val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                    val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                    val bos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 60, bos)
                    bos.toByteArray()
                }
                prepareStreamFromBytes(
                    fileName = customTitle,
                    mimeType = "image/jpeg",
                    rawBytes = compressedJpeg,
                    saveToVaultAsSent = true
                )
            } catch (e: Exception) {
                _senderState.update {
                    it.copy(
                        isPreparing = false,
                        errorMessage = "Errore compressione foto: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun prepareStreamFromTextNote(title: String, content: String) {
        val cleanTitle = if (title.endsWith(".txt", true) || title.endsWith(".md", true)) {
            title
        } else {
            "${title.ifBlank { "Nota_Rapida" }}.txt"
        }
        prepareStreamFromBytes(
            fileName = cleanTitle,
            mimeType = "text/plain",
            rawBytes = content.toByteArray(Charsets.UTF_8),
            saveToVaultAsSent = true
        )
    }

    fun loadVaultRecordIntoSender(record: TransferRecordEntity) {
        viewModelScope.launch {
            val bytes = repository.readRecordBytes(record)
            if (bytes != null) {
                prepareStreamFromBytes(
                    fileName = record.fileName,
                    mimeType = record.mimeType,
                    rawBytes = bytes,
                    saveToVaultAsSent = false
                )
            } else {
                _senderState.update {
                    it.copy(errorMessage = "File locale non trovato in archivio.")
                }
            }
        }
    }

    fun prepareStreamFromBytes(
        fileName: String,
        mimeType: String,
        rawBytes: ByteArray,
        saveToVaultAsSent: Boolean = true
    ) {
        viewModelScope.launch {
            _senderState.update { it.copy(isPreparing = true, errorMessage = null) }
            try {
                val currentChunkSize = _senderState.value.chunkSize
                val currentBlockSize = _senderState.value.blockSize
                val currentEcc = _senderState.value.eccLevel

                val (prepared, bitmaps) = withContext(Dispatchers.Default) {
                    val stream = OpticalPacketProtocol.prepareStream(
                        fileName = fileName,
                        mimeType = mimeType,
                        rawBytes = rawBytes,
                        chunkSize = currentChunkSize,
                        blockSize = currentBlockSize,
                        eccLevel = currentEcc
                    )
                    val generatedBitmaps = stream.encodedQrStrings.map { qrStr ->
                        QrCodecEngine.generateQrBitmap(
                            content = qrStr,
                            sizePx = 680,
                            eccLevel = currentEcc
                        )
                    }
                    stream to generatedBitmaps
                }

                if (saveToVaultAsSent) {
                    repository.saveTransferFileAndRecord(
                        sessionId = prepared.sessionId,
                        fileName = prepared.fileName,
                        mimeType = prepared.mimeType,
                        rawBytes = rawBytes,
                        compressedSizeBytes = prepared.compressedBytes.size.toLong(),
                        totalDataChunks = prepared.dataPackets.size,
                        parityChunks = prepared.parityPackets.size,
                        sha256Hash = prepared.fileSha256,
                        integrityVerified = true,
                        direction = "SENT"
                    )
                }

                _senderState.update {
                    it.copy(
                        isPreparing = false,
                        stream = prepared,
                        qrBitmaps = bitmaps,
                        currentFramePosition = 0,
                        isPlaying = true,
                        selectedMissingDataIndices = emptySet(),
                        statusMessage = "Stream ottico pronto: ${prepared.dataPackets.size} pacchetti dati + ${prepared.parityPackets.size} parità XOR"
                    )
                }
            } catch (e: Exception) {
                _senderState.update {
                    it.copy(
                        isPreparing = false,
                        errorMessage = "Impossibile generare i codici QR: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateSenderFps(newFps: Int) {
        _senderState.update { it.copy(fps = newFps.coerceIn(1, 12)) }
    }

    fun toggleSenderPlayPause() {
        _senderState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun stepSenderFrame(delta: Int) {
        val state = _senderState.value
        val activeIndices = getActivePacketPositions(state)
        if (activeIndices.isEmpty()) return
        val currentIdxInActive = activeIndices.indexOf(state.currentFramePosition).let {
            if (it == -1) 0 else it
        }
        val nextIdx = (currentIdxInActive + delta).mod(activeIndices.size)
        _senderState.update {
            it.copy(currentFramePosition = activeIndices[nextIdx])
        }
    }

    fun advanceToNextActiveFrame() {
        stepSenderFrame(1)
    }

    fun toggleMissingChunkFilter(dataChunkIndex: Int) {
        _senderState.update { state ->
            val updated = state.selectedMissingDataIndices.toMutableSet()
            if (updated.contains(dataChunkIndex)) {
                updated.remove(dataChunkIndex)
            } else {
                updated.add(dataChunkIndex)
            }
            state.copy(
                selectedMissingDataIndices = updated,
                currentFramePosition = 0
            )
        }
    }

    fun clearMissingChunkFilter() {
        _senderState.update { it.copy(selectedMissingDataIndices = emptySet()) }
    }

    fun updateStreamConfig(newChunkSize: Int? = null, newEccLevel: QrEccLevel? = null) {
        val state = _senderState.value
        val stream = state.stream ?: return
        val targetChunkSize = newChunkSize ?: state.chunkSize
        val targetEcc = newEccLevel ?: state.eccLevel
        _senderState.update {
            it.copy(chunkSize = targetChunkSize, eccLevel = targetEcc)
        }
        prepareStreamFromBytes(
            fileName = stream.fileName,
            mimeType = stream.mimeType,
            rawBytes = stream.originalBytes,
            saveToVaultAsSent = false
        )
    }

    private fun getActivePacketPositions(state: SenderUiState): List<Int> {
        val stream = state.stream ?: return emptyList()
        if (state.selectedMissingDataIndices.isEmpty()) {
            return stream.allOrderedPackets.indices.toList()
        }
        return stream.allOrderedPackets.mapIndexedNotNull { pos, pkt ->
            if (pkt.kind == PacketKind.DATA && state.selectedMissingDataIndices.contains(pkt.index)) {
                pos
            } else {
                null
            }
        }.ifEmpty { stream.allOrderedPackets.indices.toList() }
    }

    fun resetReceiverSession() {
        loopbackJob?.cancel()
        receivedDataPayloads.clear()
        receivedParityPackets.clear()
        parityRecoveredSet.clear()
        receptionStartTimeMs = 0L
        _receiverState.value = ReceiverUiState()
    }

    fun onQrStringScanned(rawQrString: String): Boolean {
        val currentState = _receiverState.value
        val decodeResult = OpticalPacketProtocol.decodePacketFromQrString(rawQrString)
        if (decodeResult.isFailure) {
            _receiverState.update {
                it.copy(
                    crcErrorsRejected = it.crcErrorsRejected + 1,
                    statusBanner = "Pacchetto corrotto scartato (CRC32 non valido)"
                )
            }
            return false
        }

        val packet = decodeResult.getOrThrow()
        if (currentState.completedResult != null && currentState.activeSessionId == packet.sessionId) {
            return false
        }

        if (currentState.activeSessionId != packet.sessionId) {
            receivedDataPayloads.clear()
            receivedParityPackets.clear()
            parityRecoveredSet.clear()
            receptionStartTimeMs = System.currentTimeMillis()
            val initialMap = (0 until packet.totalDataChunks).associateWith { ChunkReceptionState.PENDING }
            _receiverState.value = ReceiverUiState(
                activeSessionId = packet.sessionId,
                fileName = packet.fileName,
                mimeType = packet.mimeType,
                expectedSha256 = packet.fileSha256,
                originalSizeBytes = packet.originalSizeBytes,
                compressedSizeBytes = packet.compressedSizeBytes,
                totalDataChunks = packet.totalDataChunks,
                blockSize = packet.blockSize,
                chunkSize = packet.chunkSize,
                chunkStates = initialMap,
                statusBanner = "Agganciato stream '${packet.fileName}' (${packet.totalDataChunks} pacchetti)"
            )
        }

        var newlyAdded = false
        val updatedChunkStates = _receiverState.value.chunkStates.toMutableMap()
        val updatedParityBlocks = _receiverState.value.receivedParityBlocks.toMutableSet()

        when (packet.kind) {
            PacketKind.DATA -> {
                if (!receivedDataPayloads.containsKey(packet.index)) {
                    receivedDataPayloads[packet.index] = packet.payload
                    updatedChunkStates[packet.index] = ChunkReceptionState.RECEIVED_DIRECT
                    newlyAdded = true
                }
            }
            PacketKind.PARITY -> {
                if (!receivedParityPackets.containsKey(packet.index)) {
                    receivedParityPackets[packet.index] = packet
                    updatedParityBlocks.add(packet.index)
                    newlyAdded = true
                }
            }
        }

        if (!newlyAdded) return false

        for ((blockIdx, parityPkt) in receivedParityPackets) {
            val recovered = OpticalPacketProtocol.tryRecoverMissingChunkInBlock(
                blockIndex = blockIdx,
                blockSize = packet.blockSize,
                totalDataChunks = packet.totalDataChunks,
                chunkSize = packet.chunkSize,
                receivedDataPayloads = receivedDataPayloads,
                parityPacket = parityPkt
            )
            if (recovered != null) {
                val (recoveredIndex, recoveredBytes) = recovered
                if (!receivedDataPayloads.containsKey(recoveredIndex)) {
                    receivedDataPayloads[recoveredIndex] = recoveredBytes
                    parityRecoveredSet.add(recoveredIndex)
                    updatedChunkStates[recoveredIndex] = ChunkReceptionState.RECOVERED_PARITY
                }
            }
        }

        val receivedCount = receivedDataPayloads.size
        val progress = (receivedCount.toFloat() / packet.totalDataChunks.toFloat()).coerceIn(0f, 1f)
        val elapsed = (System.currentTimeMillis() - receptionStartTimeMs).coerceAtLeast(1L)

        _receiverState.update {
            it.copy(
                chunkStates = updatedChunkStates,
                receivedParityBlocks = updatedParityBlocks,
                validFramesScanned = it.validFramesScanned + 1,
                parityRecoveredCount = parityRecoveredSet.size,
                lastScannedPacketLabel = "${packet.displayNumber} (CRC ${packet.crc32Hex.take(6)})",
                transferProgress = progress,
                elapsedMs = elapsed,
                statusBanner = if (receivedCount < packet.totalDataChunks) {
                    "Ricevuti $receivedCount/${packet.totalDataChunks} pacchetti (${parityRecoveredSet.size} recuperati via Parità XOR)"
                } else {
                    "Tutti i pacchetti acquisiti! Verifica SHA-256 in corso..."
                }
            )
        }

        if (receivedCount == packet.totalDataChunks) {
            finalizeFileReconstruction(packet)
        }
        return true
    }

    private fun finalizeFileReconstruction(lastPacket: OpticalPacket) {
        val snapshotPayloads = receivedDataPayloads.toMap()
        val snapshotParitySet = parityRecoveredSet.toSet()

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                OpticalPacketProtocol.reconstructAndVerifyFile(
                    sessionId = lastPacket.sessionId,
                    fileName = lastPacket.fileName,
                    mimeType = lastPacket.mimeType,
                    expectedSha256 = lastPacket.fileSha256,
                    totalDataChunks = lastPacket.totalDataChunks,
                    receivedDataPayloads = snapshotPayloads,
                    parityRecoveredIndices = snapshotParitySet
                )
            }

            result.onSuccess { reconstructed ->
                val savedEntity = repository.saveTransferFileAndRecord(
                    sessionId = reconstructed.sessionId,
                    fileName = reconstructed.fileName,
                    mimeType = reconstructed.mimeType,
                    rawBytes = reconstructed.rawBytes,
                    compressedSizeBytes = reconstructed.compressedSizeBytes,
                    totalDataChunks = reconstructed.totalDataChunks,
                    parityChunks = snapshotParitySet.size,
                    sha256Hash = reconstructed.actualSha256,
                    integrityVerified = reconstructed.integrityVerified,
                    direction = "RECEIVED"
                )
                _receiverState.update {
                    it.copy(
                        completedResult = reconstructed,
                        savedRecord = savedEntity,
                        isLoopbackRunning = false,
                        statusBanner = if (reconstructed.integrityVerified) {
                            "Trasferimento completato! Integrità SHA-256 verificata al 100%."
                        } else {
                            "Attenzione: Hash SHA-256 non corrispondente."
                        }
                    )
                }
            }.onFailure { err ->
                _receiverState.update {
                    it.copy(
                        isLoopbackRunning = false,
                        statusBanner = "Errore ricostruzione file: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun startOpticalLoopbackSimulation(simulateFrameDrops: Boolean = true) {
        val senderBitmaps = _senderState.value.qrBitmaps
        val stream = _senderState.value.stream
        if (senderBitmaps.isEmpty() || stream == null) return

        resetReceiverSession()
        _receiverState.update {
            it.copy(
                isLoopbackRunning = true,
                statusBanner = "Simulazione ottica attiva: decodifica bitmap QR con ZXing..."
            )
        }

        loopbackJob = viewModelScope.launch {
            for ((idx, bitmap) in senderBitmaps.withIndex()) {
                if (_receiverState.value.completedResult != null) break
                val pkt = stream.allOrderedPackets.getOrNull(idx)

                if (simulateFrameDrops && pkt != null && pkt.kind == PacketKind.DATA && pkt.index == 1 && pkt.totalDataChunks >= 3) {
                    delay(140L)
                    _receiverState.update {
                        it.copy(statusBanner = "Simulato riflesso ottico su D#2: attesa pacchetto Parità XOR...")
                    }
                    continue
                }

                val decodedString = withContext(Dispatchers.Default) {
                    QrCodecEngine.decodeFromBitmap(bitmap)
                }
                if (decodedString != null) {
                    onQrStringScanned(decodedString)
                }
                delay(150L)
            }
            _receiverState.update { it.copy(isLoopbackRunning = false) }
        }
    }

    fun deleteVaultRecord(record: TransferRecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun renameVaultRecord(recordId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameRecord(recordId, newName)
        }
    }

    class Factory(private val repository: TransferRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransferViewModel(repository) as T
        }
    }
}
