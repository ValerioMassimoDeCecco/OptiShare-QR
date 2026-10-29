package com.example.domain.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.ceil
import kotlin.math.min

enum class QrEccLevel(val label: String, val description: String) {
    L("L (7%)", "Massima densità dati"),
    M("M (15%)", "Bilanciato (Consigliato)"),
    Q("Q (25%)", "Alta resistenza riflessi"),
    H("H (30%)", "Massima correzione ottica")
}

enum class PacketKind {
    DATA,
    PARITY
}

enum class ChunkReceptionState {
    PENDING,
    RECEIVED_DIRECT,
    RECOVERED_PARITY
}

data class OpticalPacket(
    val sessionId: String,
    val kind: PacketKind,
    val index: Int, // For DATA: 0 until totalDataChunks; For PARITY: blockIndex (0 until totalParityBlocks)
    val totalDataChunks: Int,
    val blockSize: Int,
    val chunkSize: Int,
    val crc32Hex: String,
    val fileSha256: String,
    val fileName: String,
    val mimeType: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val payload: ByteArray,
    val dataChunkCrcsInBlock: List<String> = emptyList() // Used by PARITY packet to validate reconstructed chunk
) {
    val displayNumber: String
        get() = if (kind == PacketKind.DATA) "D#${index + 1}" else "P#${index + 1}"
}

data class PreparedOpticalStream(
    val sessionId: String,
    val fileName: String,
    val mimeType: String,
    val originalBytes: ByteArray,
    val compressedBytes: ByteArray,
    val fileSha256: String,
    val chunkSize: Int,
    val blockSize: Int,
    val eccLevel: QrEccLevel,
    val dataPackets: List<OpticalPacket>,
    val parityPackets: List<OpticalPacket>,
    val allOrderedPackets: List<OpticalPacket>,
    val encodedQrStrings: List<String>
)

data class ReconstructedFileResult(
    val sessionId: String,
    val fileName: String,
    val mimeType: String,
    val rawBytes: ByteArray,
    val compressedSizeBytes: Long,
    val expectedSha256: String,
    val actualSha256: String,
    val integrityVerified: Boolean,
    val totalDataChunks: Int,
    val parityRecoveredCount: Int
)

object OpticalPacketProtocol {
    private const val MAGIC_HEADER = "QBF1"
    private const val FIELD_SEP = "|"

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun crc32Hex(bytes: ByteArray): String {
        val crc = CRC32()
        crc.update(bytes)
        return "%08x".format(crc.value)
    }

    fun compressGzip(input: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(input)
        }
        return bos.toByteArray()
    }

    fun decompressGzip(compressed: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(compressed)
        val bos = ByteArrayOutputStream()
        GZIPInputStream(bis).use { gzip ->
            val buffer = ByteArray(4096)
            var read: Int
            while (gzip.read(buffer).also { read = it } != -1) {
                bos.write(buffer, 0, read)
            }
        }
        return bos.toByteArray()
    }

    /**
     * Prepares a complete multi-frame QR stream with GZIP compression, CRC32 per packet,
     * full-file SHA-256, and XOR parity packets (1 parity packet per [blockSize] data packets).
     */
    fun prepareStream(
        fileName: String,
        mimeType: String,
        rawBytes: ByteArray,
        chunkSize: Int = 400,
        blockSize: Int = 4,
        eccLevel: QrEccLevel = QrEccLevel.M
    ): PreparedOpticalStream {
        val sessionId = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val fileSha256 = sha256Hex(rawBytes)
        val compressedBytes = compressGzip(rawBytes)
        val safeChunkSize = chunkSize.coerceIn(120, 900)
        val safeBlockSize = blockSize.coerceIn(2, 8)

        val totalDataChunks = ceil(compressedBytes.size.toDouble() / safeChunkSize.toDouble())
            .toInt()
            .coerceAtLeast(1)

        val sanitizedName = fileName.replace("|", "_").replace("\n", " ").take(64).ifBlank { "file.bin" }
        val sanitizedMime = mimeType.replace("|", "_").take(40).ifBlank { "application/octet-stream" }

        val dataPackets = mutableListOf<OpticalPacket>()
        for (i in 0 until totalDataChunks) {
            val start = i * safeChunkSize
            val end = min(start + safeChunkSize, compressedBytes.size)
            val slice = if (start < end) {
                compressedBytes.copyOfRange(start, end)
            } else {
                ByteArray(0)
            }
            val crc = crc32Hex(slice)
            dataPackets.add(
                OpticalPacket(
                    sessionId = sessionId,
                    kind = PacketKind.DATA,
                    index = i,
                    totalDataChunks = totalDataChunks,
                    blockSize = safeBlockSize,
                    chunkSize = safeChunkSize,
                    crc32Hex = crc,
                    fileSha256 = fileSha256,
                    fileName = sanitizedName,
                    mimeType = sanitizedMime,
                    originalSizeBytes = rawBytes.size.toLong(),
                    compressedSizeBytes = compressedBytes.size.toLong(),
                    payload = slice
                )
            )
        }

        // Build XOR parity packets for each block of [safeBlockSize] data chunks
        val parityPackets = mutableListOf<OpticalPacket>()
        val totalBlocks = ceil(totalDataChunks.toDouble() / safeBlockSize.toDouble()).toInt()
        for (blockIdx in 0 until totalBlocks) {
            val blockStart = blockIdx * safeBlockSize
            val blockEnd = min(blockStart + safeBlockSize, totalDataChunks)
            val chunksInBlock = dataPackets.subList(blockStart, blockEnd)
            if (chunksInBlock.size >= 2) {
                // Frame each data chunk with its 2-byte length prefix before XORing so variable last-chunk length is preserved
                val framedSize = safeChunkSize + 2
                val xorBuffer = ByteArray(framedSize)
                val memberCrcs = mutableListOf<String>()

                for (dataPkt in chunksInBlock) {
                    memberCrcs.add(dataPkt.crc32Hex)
                    val framed = ByteArray(framedSize)
                    val len = dataPkt.payload.size
                    framed[0] = ((len shr 8) and 0xFF).toByte()
                    framed[1] = (len and 0xFF).toByte()
                    System.arraycopy(dataPkt.payload, 0, framed, 2, len)
                    for (b in 0 until framedSize) {
                        xorBuffer[b] = (xorBuffer[b].toInt() xor framed[b].toInt()).toByte()
                    }
                }

                val parityCrc = crc32Hex(xorBuffer)
                parityPackets.add(
                    OpticalPacket(
                        sessionId = sessionId,
                        kind = PacketKind.PARITY,
                        index = blockIdx,
                        totalDataChunks = totalDataChunks,
                        blockSize = safeBlockSize,
                        chunkSize = safeChunkSize,
                        crc32Hex = parityCrc,
                        fileSha256 = fileSha256,
                        fileName = sanitizedName,
                        mimeType = sanitizedMime,
                        originalSizeBytes = rawBytes.size.toLong(),
                        compressedSizeBytes = compressedBytes.size.toLong(),
                        payload = xorBuffer,
                        dataChunkCrcsInBlock = memberCrcs
                    )
                )
            }
        }

        // Interleave data packets and their block parity packet immediately after each block
        val interleaved = mutableListOf<OpticalPacket>()
        val parityByBlock = parityPackets.associateBy { it.index }
        for (blockIdx in 0 until totalBlocks) {
            val blockStart = blockIdx * safeBlockSize
            val blockEnd = min(blockStart + safeBlockSize, totalDataChunks)
            for (i in blockStart until blockEnd) {
                interleaved.add(dataPackets[i])
            }
            parityByBlock[blockIdx]?.let { interleaved.add(it) }
        }

        val encodedStrings = interleaved.map { encodePacketToQrString(it) }

        return PreparedOpticalStream(
            sessionId = sessionId,
            fileName = sanitizedName,
            mimeType = sanitizedMime,
            originalBytes = rawBytes,
            compressedBytes = compressedBytes,
            fileSha256 = fileSha256,
            chunkSize = safeChunkSize,
            blockSize = safeBlockSize,
            eccLevel = eccLevel,
            dataPackets = dataPackets,
            parityPackets = parityPackets,
            allOrderedPackets = interleaved,
            encodedQrStrings = encodedStrings
        )
    }

    /**
     * Serializes an [OpticalPacket] into a compact pipe-delimited string for QR encoding.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encodePacketToQrString(packet: OpticalPacket): String {
        val kindCode = if (packet.kind == PacketKind.DATA) "D" else "P"
        val base64Payload = Base64.encode(packet.payload)
        val memberCrcsJoined = packet.dataChunkCrcsInBlock.joinToString(",")
        return listOf(
            MAGIC_HEADER,
            packet.sessionId,
            kindCode,
            packet.index.toString(),
            packet.totalDataChunks.toString(),
            packet.blockSize.toString(),
            packet.chunkSize.toString(),
            packet.crc32Hex,
            packet.fileSha256,
            packet.fileName,
            packet.mimeType,
            packet.originalSizeBytes.toString(),
            packet.compressedSizeBytes.toString(),
            memberCrcsJoined,
            base64Payload
        ).joinToString(FIELD_SEP)
    }

    /**
     * Parses and validates a scanned QR string into an [OpticalPacket].
     * Verifies packet-level CRC32 immediately and returns failure if corrupted.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decodePacketFromQrString(qrString: String): Result<OpticalPacket> {
        return runCatching {
            val parts = qrString.split(FIELD_SEP, limit = 15)
            require(parts.size == 15 && parts[0] == MAGIC_HEADER) {
                "Formato QR non riconosciuto come pacchetto OptiShare (QBF1)"
            }
            val sessionId = parts[1]
            val kind = when (parts[2]) {
                "D" -> PacketKind.DATA
                "P" -> PacketKind.PARITY
                else -> throw IllegalArgumentException("Tipo pacchetto non valido: ${parts[2]}")
            }
            val index = parts[3].toInt()
            val totalDataChunks = parts[4].toInt()
            val blockSize = parts[5].toInt()
            val chunkSize = parts[6].toInt()
            val expectedCrc = parts[7].lowercase()
            val fileSha256 = parts[8].lowercase()
            val fileName = parts[9]
            val mimeType = parts[10]
            val originalSize = parts[11].toLong()
            val compressedSize = parts[12].toLong()
            val memberCrcs = if (parts[13].isBlank()) emptyList() else parts[13].split(",")
            val payload = Base64.decode(parts[14])

            val actualCrc = crc32Hex(payload).lowercase()
            require(actualCrc == expectedCrc) {
                "Errore integrità CRC32 sul pacchetto $index (atteso $expectedCrc, letto $actualCrc)"
            }

            OpticalPacket(
                sessionId = sessionId,
                kind = kind,
                index = index,
                totalDataChunks = totalDataChunks,
                blockSize = blockSize,
                chunkSize = chunkSize,
                crc32Hex = expectedCrc,
                fileSha256 = fileSha256,
                fileName = fileName,
                mimeType = mimeType,
                originalSizeBytes = originalSize,
                compressedSizeBytes = compressedSize,
                payload = payload,
                dataChunkCrcsInBlock = memberCrcs
            )
        }
    }

    /**
     * Attempts XOR parity recovery for a block of data chunks when exactly 1 data chunk is missing
     * and the block's parity packet has been received.
     */
    fun tryRecoverMissingChunkInBlock(
        blockIndex: Int,
        blockSize: Int,
        totalDataChunks: Int,
        chunkSize: Int,
        receivedDataPayloads: Map<Int, ByteArray>,
        parityPacket: OpticalPacket
    ): Pair<Int, ByteArray>? {
        val blockStart = blockIndex * blockSize
        val blockEnd = min(blockStart + blockSize, totalDataChunks)
        val indicesInBlock = (blockStart until blockEnd).toList()
        if (indicesInBlock.size < 2) return null

        val missingIndices = indicesInBlock.filter { !receivedDataPayloads.containsKey(it) }
        if (missingIndices.size != 1) return null

        val missingIndex = missingIndices.first()
        val missingPosInBlock = missingIndex - blockStart
        val framedSize = chunkSize + 2
        if (parityPacket.payload.size != framedSize) return null

        val recoveredFramed = parityPacket.payload.copyOf()
        for (idx in indicesInBlock) {
            if (idx == missingIndex) continue
            val knownPayload = receivedDataPayloads[idx] ?: return null
            val framed = ByteArray(framedSize)
            val len = knownPayload.size
            framed[0] = ((len shr 8) and 0xFF).toByte()
            framed[1] = (len and 0xFF).toByte()
            System.arraycopy(knownPayload, 0, framed, 2, len)
            for (b in 0 until framedSize) {
                recoveredFramed[b] = (recoveredFramed[b].toInt() xor framed[b].toInt()).toByte()
            }
        }

        val recoveredLength = ((recoveredFramed[0].toInt() and 0xFF) shl 8) or
            (recoveredFramed[1].toInt() and 0xFF)
        if (recoveredLength < 0 || recoveredLength > chunkSize) return null

        val recoveredSlice = recoveredFramed.copyOfRange(2, 2 + recoveredLength)
        val expectedMemberCrc = parityPacket.dataChunkCrcsInBlock.getOrNull(missingPosInBlock)
        if (expectedMemberCrc != null) {
            val actualCrc = crc32Hex(recoveredSlice)
            if (!actualCrc.equals(expectedMemberCrc, ignoreCase = true)) {
                return null
            }
        }
        return missingIndex to recoveredSlice
    }

    /**
     * Reconstructs the original file once all data chunks (0 until totalDataChunks) are present.
     */
    fun reconstructAndVerifyFile(
        sessionId: String,
        fileName: String,
        mimeType: String,
        expectedSha256: String,
        totalDataChunks: Int,
        receivedDataPayloads: Map<Int, ByteArray>,
        parityRecoveredIndices: Set<Int>
    ): Result<ReconstructedFileResult> {
        return runCatching {
            require(receivedDataPayloads.size == totalDataChunks) {
                "Pacchetti incompleti: ricevuti ${receivedDataPayloads.size} su $totalDataChunks"
            }
            val compressedBos = ByteArrayOutputStream()
            for (i in 0 until totalDataChunks) {
                val chunk = receivedDataPayloads[i]
                    ?: throw IllegalStateException("Pacchetto #$i mancante")
                compressedBos.write(chunk)
            }
            val compressedBytes = compressedBos.toByteArray()
            val decompressedBytes = decompressGzip(compressedBytes)
            val actualSha256 = sha256Hex(decompressedBytes)
            val verified = actualSha256.equals(expectedSha256, ignoreCase = true)

            ReconstructedFileResult(
                sessionId = sessionId,
                fileName = fileName,
                mimeType = mimeType,
                rawBytes = decompressedBytes,
                compressedSizeBytes = compressedBytes.size.toLong(),
                expectedSha256 = expectedSha256,
                actualSha256 = actualSha256,
                integrityVerified = verified,
                totalDataChunks = totalDataChunks,
                parityRecoveredCount = parityRecoveredIndices.size
            )
        }
    }
}
