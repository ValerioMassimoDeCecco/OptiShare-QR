package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max

@Immutable
data class SenderUiState(
    val isPreparing: Boolean = false,
    val stream: PreparedOpticalStream? = null,
    val qrBitmaps: List<Bitmap> = emptyList(),
    val currentFramePosition: Int = 0,
    val isPlaying: Boolean = true,
    val fps: Int = 6,
    val chunkSize: Int = 380,
    val blockSize: Int = 4,
    val eccLevel: QrEccLevel = QrEccLevel.M,
    val encryptionPassword: String = "",
    val autoMaxBrightness: Boolean = true,
    val encodingDurationMs: Long = 0L,
    val selectedMissingDataIndices: Set<Int> = emptySet(),
    val statusMessage: String? = null,
    val errorMessage: String? = null
) {
    val activeFrameCount: Int
        get() {
            val s = stream ?: return 0
            return if (selectedMissingDataIndices.isEmpty()) {
                s.allOrderedPackets.size
            } else {
                s.allOrderedPackets.count {
                    it.kind == PacketKind.DATA && selectedMissingDataIndices.contains(it.index)
                }.coerceAtLeast(1)
            }
        }

    val estimatedCycleSeconds: Float
        get() = if (fps <= 0 || activeFrameCount == 0) 0f else activeFrameCount.toFloat() / fps.toFloat()

    val theoreticalThroughputKbps: Float
        get() {
            val s = stream ?: return 0f
            val cycleSec = estimatedCycleSeconds
            if (cycleSec <= 0f) return 0f
            return (s.originalBytes.size.toFloat() / 1024f) / cycleSec
        }
}

@Immutable
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
    val isEncryptedStream: Boolean = false,
    val awaitingDecryptionPassword: Boolean = false,
    val decryptionError: String? = null,
    val chunkStates: Map<Int, ChunkReceptionState> = emptyMap(),
    val receivedParityBlocks: Set<Int> = emptySet(),
    val validFramesScanned: Int = 0,
    val crcErrorsRejected: Int = 0,
    val parityRecoveredCount: Int = 0,
    val lastScannedPacketLabel: String? = null,
    val transferProgress: Float = 0f,
    val elapsedMs: Long = 0L,
    val liveSpeedKbps: Float = 0f,
    val estimatedRemainingSeconds: Int = 0,
    val nackQrBitmap: Bitmap? = null,
    val completedResult: ReconstructedFileResult? = null,
    val savedRecord: TransferRecordEntity? = null,
    val statusBanner: String = "Inquadra il codice QR dall'altro dispositivo"
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
    private var lastReceivedPacketMeta: OpticalPacket? = null
    private var receptionStartTimeMs: Long = 0L

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

    /**
     * Compresses multiple user-selected URIs into a single in-memory ZIP archive before QR encoding.
     */
    fun prepareStreamFromMultipleUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            prepareStreamFromUri(context, uris.first())
            return
        }
        viewModelScope.launch {
            _senderState.update { it.copy(isPreparing = true, errorMessage = null) }
            try {
                val zipBytes = withContext(Dispatchers.IO) {
                    val contentResolver = context.contentResolver
                    val entries = mutableListOf<Pair<String, ByteArray>>()
                    var totalBudget = 35_000

                    for ((idx, uri) in uris.withIndex()) {
                        if (totalBudget <= 0) break
                        var name = "file_${idx + 1}.bin"
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIdx != -1 && cursor.moveToFirst()) {
                                name = cursor.getString(nameIdx) ?: name
                            }
                        }
                        val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
                        val clipped = if (raw.size > totalBudget) raw.copyOfRange(0, totalBudget) else raw
                        totalBudget -= clipped.size
                        entries.add(name to clipped)
                    }
                    require(entries.isNotEmpty()) { "Nessun file leggibile selezionato" }
                    OpticalPacketProtocol.createZipArchive(entries)
                }

                prepareStreamFromBytes(
                    fileName = "Archivio_${uris.size}_File.zip",
                    mimeType = "application/zip",
                    rawBytes = zipBytes,
                    saveToVaultAsSent = true
                )
            } catch (e: Exception) {
                _senderState.update {
                    it.copy(
                        isPreparing = false,
                        errorMessage = "Errore creazione ZIP multi-file: ${e.localizedMessage}"
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

    fun updateSenderEncryptionPassword(newPassword: String) {
        val trimmed = newPassword.trim()
        val previous = _senderState.value.encryptionPassword
        _senderState.update { it.copy(encryptionPassword = trimmed) }
        val stream = _senderState.value.stream
        if (stream != null && trimmed != previous) {
            prepareStreamFromBytes(
                fileName = stream.fileName,
                mimeType = stream.mimeType,
                rawBytes = stream.originalBytes,
                saveToVaultAsSent = false
            )
        }
    }

    fun toggleAutoMaxBrightness() {
        _senderState.update { it.copy(autoMaxBrightness = !it.autoMaxBrightness) }
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
                val password = _senderState.value.encryptionPassword.takeIf { it.isNotBlank() }

                val startTime = System.currentTimeMillis()
                val (prepared, bitmaps) = withContext(Dispatchers.Default) {
                    val stream = OpticalPacketProtocol.prepareStream(
                        fileName = fileName,
                        mimeType = mimeType,
                        rawBytes = rawBytes,
                        chunkSize = currentChunkSize,
                        blockSize = currentBlockSize,
                        eccLevel = currentEcc,
                        encryptionPassword = password
                    )
                    val generatedBitmaps = QrCodecEngine.generateQrBitmapsParallel(
                        contents = stream.encodedQrStrings,
                        sizePx = 680,
                        eccLevel = currentEcc
                    )
                    stream to generatedBitmaps
                }
                val durationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)

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
                        encodingDurationMs = durationMs,
                        selectedMissingDataIndices = emptySet(),
                        statusMessage = "Pronto per l'invio (${prepared.allOrderedPackets.size} codici QR)"
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

    /**
     * Parses a Reverse NACK QR string (`QBF1_NACK|<sessionId>|<indices>`) scanned by the sender
     * and filters the outgoing QR stream to transmit ONLY the receiver's missing packets.
     */
    fun applyScannedNackQr(rawQrString: String): Result<Int> {
        val stream = _senderState.value.stream
            ?: return Result.failure(IllegalStateException("Nessuno stream attivo sul trasmettitore"))

        return OpticalPacketProtocol.decodeNackQrString(rawQrString).mapCatching { nack ->
            val validIndices = nack.missingDataIndices.filter { it in 0 until stream.dataPackets.size }.toSet()
            require(validIndices.isNotEmpty()) {
                "Questo codice QR non appartiene al file corrente"
            }
            _senderState.update {
                it.copy(
                    selectedMissingDataIndices = validIndices,
                    currentFramePosition = 0,
                    isPlaying = true,
                    statusMessage = "Invio delle ${validIndices.size} parti mancanti"
                )
            }
            validIndices.size
        }
    }

    /**
     * Generates a Reverse NACK QR bitmap on the receiver encoding the currently missing data chunk indices.
     */
    fun generateReceiverNackQr() {
        val state = _receiverState.value
        val sessionId = state.activeSessionId ?: return
        val missing = state.missingDataChunkIndices
        if (missing.isEmpty()) return

        viewModelScope.launch {
            val nackString = OpticalPacketProtocol.encodeNackQrString(sessionId, missing)
            val bmp = withContext(Dispatchers.Default) {
                QrCodecEngine.generateQrBitmap(
                    content = nackString,
                    sizePx = 640,
                    eccLevel = QrEccLevel.Q
                )
            }
            _receiverState.update { it.copy(nackQrBitmap = bmp) }
        }
    }

    fun dismissReceiverNackQr() {
        _receiverState.update { it.copy(nackQrBitmap = null) }
    }

    fun updateSenderFps(newFps: Int) {
        _senderState.update { it.copy(fps = newFps.coerceIn(1, 24)) }
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
        receivedDataPayloads.clear()
        receivedParityPackets.clear()
        parityRecoveredSet.clear()
        lastReceivedPacketMeta = null
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
                    statusBanner = "Immagine non chiara, continua a inquadrare…"
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
            lastReceivedPacketMeta = packet
            receptionStartTimeMs = System.currentTimeMillis()
            val initialMap = (0 until packet.totalDataChunks).associateWith { ChunkReceptionState.PENDING }
            _receiverState.value = ReceiverUiState(
                activeSessionId = packet.sessionId,
                fileName = packet.fileName,
                mimeType = OpticalPacketProtocol.stripEncryptedMimeSuffix(packet.mimeType),
                expectedSha256 = packet.fileSha256,
                originalSizeBytes = packet.originalSizeBytes,
                compressedSizeBytes = packet.compressedSizeBytes,
                totalDataChunks = packet.totalDataChunks,
                blockSize = packet.blockSize,
                chunkSize = packet.chunkSize,
                isEncryptedStream = packet.isEncrypted,
                chunkStates = initialMap,
                statusBanner = "Ricezione di '${packet.fileName}' (${packet.totalDataChunks} parti)"
            )
        }
        lastReceivedPacketMeta = packet

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
        val elapsedMs = max(1L, System.currentTimeMillis() - receptionStartTimeMs)
        val elapsedSec = elapsedMs.toFloat() / 1000f
        val receivedBytesCount = receivedDataPayloads.values.sumOf { it.size }
        val speedKbps = (receivedBytesCount.toFloat() / 1024f) / elapsedSec.coerceAtLeast(0.15f)
        val remainingChunks = (packet.totalDataChunks - receivedCount).coerceAtLeast(0)
        val chunksPerSec = receivedCount.toFloat() / elapsedSec.coerceAtLeast(0.15f)
        val etaSeconds = if (remainingChunks == 0 || chunksPerSec <= 0.05f) {
            0
        } else {
            ceil(remainingChunks / chunksPerSec).toInt()
        }

        _receiverState.update {
            it.copy(
                chunkStates = updatedChunkStates,
                receivedParityBlocks = updatedParityBlocks,
                validFramesScanned = it.validFramesScanned + 1,
                parityRecoveredCount = parityRecoveredSet.size,
                lastScannedPacketLabel = if (packet.kind == PacketKind.PARITY) {
                    "Recupero ${packet.displayNumber}"
                } else {
                    "Parte $receivedCount di ${packet.totalDataChunks}"
                },
                transferProgress = progress,
                elapsedMs = elapsedMs,
                liveSpeedKbps = speedKbps,
                estimatedRemainingSeconds = etaSeconds,
                statusBanner = if (receivedCount < packet.totalDataChunks) {
                    "Ricevute $receivedCount di ${packet.totalDataChunks} parti" +
                        if (etaSeconds > 0) " · circa ${etaSeconds}s" else ""
                } else if (packet.isEncrypted) {
                    "File ricevuto! Inserisci la password per aprirlo."
                } else {
                    "File ricevuto! Controllo finale in corso…"
                }
            )
        }

        if (receivedCount == packet.totalDataChunks) {
            if (packet.isEncrypted) {
                _receiverState.update {
                    it.copy(
                        awaitingDecryptionPassword = true,
                        decryptionError = null
                    )
                }
            } else {
                finalizeFileReconstruction(packet, null)
            }
        }
        return true
    }

    fun submitReceiverDecryptionPassword(password: String) {
        val lastPacket = lastReceivedPacketMeta ?: return
        finalizeFileReconstruction(lastPacket, password.trim())
    }

    private fun finalizeFileReconstruction(lastPacket: OpticalPacket, decryptionPassword: String?) {
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
                    parityRecoveredIndices = snapshotParitySet,
                    decryptionPassword = decryptionPassword
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
                        awaitingDecryptionPassword = false,
                        decryptionError = null,
                        nackQrBitmap = null,
                        statusBanner = if (reconstructed.integrityVerified) {
                            "File ricevuto e pronto!"
                        } else {
                            "Il file ricevuto potrebbe essere danneggiato. Riprova."
                        }
                    )
                }
            }.onFailure { err ->
                _receiverState.update {
                    it.copy(
                        awaitingDecryptionPassword = lastPacket.isEncrypted,
                        decryptionError = if (lastPacket.isEncrypted) "Password non corretta" else err.localizedMessage,
                        statusBanner = "Controlla la password e riprova."
                    )
                }
            }
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
