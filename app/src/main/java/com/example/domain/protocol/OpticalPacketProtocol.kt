package com.example.domain.protocol

import androidx.compose.runtime.Immutable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
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

enum class StandardQrKind(val label: String) {
    WIFI("Wi-Fi"),
    VCARD("vCard Contatto"),
    TEXT("Testo / URL")
}

@Immutable
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
    val dataChunkCrcsInBlock: List<String> = emptyList(), // Used by PARITY packet to validate reconstructed chunk
    val isEncrypted: Boolean = false
) {
    val displayNumber: String
        get() = if (kind == PacketKind.DATA) "D#${index + 1}" else "P#${index + 1}"
}

@Immutable
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
    val encodedQrStrings: List<String>,
    val isEncrypted: Boolean = false
)

@Immutable
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
    val parityRecoveredCount: Int,
    val wasEncrypted: Boolean = false
)

@Immutable
data class OpticalNackRequest(
    val sessionId: String,
    val missingDataIndices: Set<Int>
)

object OpticalPacketProtocol {
    private const val MAGIC_HEADER = "QBF1"
    private const val NACK_HEADER = "QBF1_NACK"
    private const val FIELD_SEP = "|"
    private const val ENCRYPTED_MIME_SUFFIX = ";enc=aes256gcm"
    private const val PBKDF2_ITERATIONS = 65_536
    private const val SALT_BYTES = 16
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

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
     * Packs multiple files into a single standard ZIP byte array in memory.
     */
    fun createZipArchive(entries: List<Pair<String, ByteArray>>): ByteArray {
        val bos = ByteArrayOutputStream()
        val usedNames = mutableSetOf<String>()
        ZipOutputStream(bos).use { zos ->
            zos.setLevel(6)
            for ((rawName, content) in entries) {
                val baseName = rawName.replace("/", "_").replace("\\", "_").ifBlank { "file.bin" }
                var uniqueName = baseName
                var counter = 2
                while (!usedNames.add(uniqueName)) {
                    val dot = baseName.lastIndexOf('.')
                    uniqueName = if (dot > 0) {
                        "${baseName.substring(0, dot)}_$counter${baseName.substring(dot)}"
                    } else {
                        "${baseName}_$counter"
                    }
                    counter++
                }
                val entry = ZipEntry(uniqueName)
                entry.size = content.size.toLong()
                zos.putNextEntry(entry)
                zos.write(content)
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    /**
     * Encrypts [plainBytes] using AES-256-GCM with a key derived from [password] via PBKDF2-HMAC-SHA256.
     * Output layout: [16-byte Salt][12-byte IV][Ciphertext + 16-byte GCM Tag]
     */
    fun encryptAes256Gcm(plainBytes: ByteArray, password: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(GCM_IV_BYTES).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        keySpec.clearPassword()

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainBytes)

        val out = ByteArray(SALT_BYTES + GCM_IV_BYTES + cipherBytes.size)
        System.arraycopy(salt, 0, out, 0, SALT_BYTES)
        System.arraycopy(iv, 0, out, SALT_BYTES, GCM_IV_BYTES)
        System.arraycopy(cipherBytes, 0, out, SALT_BYTES + GCM_IV_BYTES, cipherBytes.size)
        return out
    }

    /**
     * Decrypts an AES-256-GCM payload produced by [encryptAes256Gcm].
     */
    fun decryptAes256Gcm(encryptedPayload: ByteArray, password: String): ByteArray {
        require(encryptedPayload.size > SALT_BYTES + GCM_IV_BYTES + 16) {
            "Payload cifrato troppo corto o danneggiato"
        }
        val salt = encryptedPayload.copyOfRange(0, SALT_BYTES)
        val iv = encryptedPayload.copyOfRange(SALT_BYTES, SALT_BYTES + GCM_IV_BYTES)
        val cipherBytes = encryptedPayload.copyOfRange(SALT_BYTES + GCM_IV_BYTES, encryptedPayload.size)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        keySpec.clearPassword()

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    fun isEncryptedMimeType(mimeType: String): Boolean {
        return mimeType.endsWith(ENCRYPTED_MIME_SUFFIX, ignoreCase = true)
    }

    fun stripEncryptedMimeSuffix(mimeType: String): String {
        return if (isEncryptedMimeType(mimeType)) {
            mimeType.dropLast(ENCRYPTED_MIME_SUFFIX.length).ifBlank { "application/octet-stream" }
        } else {
            mimeType
        }
    }

    /**
     * Encodes a compact Reverse NACK QR string (`QBF1_NACK|<sessionId>|<idx1,idx2,...>`)
     * so the receiver can optically tell the sender which data packets are still missing.
     */
    fun encodeNackQrString(sessionId: String, missingIndices: Collection<Int>): String {
        val sortedJoined = missingIndices.sorted().joinToString(",")
        return "$NACK_HEADER$FIELD_SEP${sessionId.trim()}$FIELD_SEP$sortedJoined"
    }

    /**
     * Parses a Reverse NACK QR string scanned by the sender.
     */
    fun decodeNackQrString(qrString: String): Result<OpticalNackRequest> {
        return runCatching {
            val parts = qrString.trim().split(FIELD_SEP)
            require(parts.size == 3 && parts[0] == NACK_HEADER) {
                "QR non riconosciuto come richiesta NACK OptiShare (QBF1_NACK)"
            }
            val sessionId = parts[1].trim()
            val indices = if (parts[2].isBlank()) {
                emptySet()
            } else {
                parts[2].split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it >= 0 }.toSet()
            }
            require(indices.isNotEmpty()) {
                "Nessun indice pacchetto mancante specificato nel QR NACK"
            }
            OpticalNackRequest(sessionId = sessionId, missingDataIndices = indices)
        }
    }

    /**
     * Formats universal standard QR strings readable by any OS camera (Wi-Fi, vCard 3.0, Text/URI).
     */
    fun buildWifiStandardQr(ssid: String, password: String, security: String = "WPA", hidden: Boolean = false): String {
        fun escapeWifi(v: String) = v
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
        val auth = if (password.isBlank()) "nopass" else security
        return "WIFI:T:$auth;S:${escapeWifi(ssid.trim())};P:${escapeWifi(password)};H:${if (hidden) "true" else "false"};;"
    }

    fun buildVCardStandardQr(
        fullName: String,
        phone: String,
        email: String,
        organization: String = "",
        website: String = ""
    ): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:${fullName.trim()}")
            if (organization.isNotBlank()) appendLine("ORG:${organization.trim()}")
            if (phone.isNotBlank()) appendLine("TEL;TYPE=CELL:${phone.trim()}")
            if (email.isNotBlank()) appendLine("EMAIL:${email.trim()}")
            if (website.isNotBlank()) appendLine("URL:${website.trim()}")
            append("END:VCARD")
        }
    }

    /**
     * Prepares a complete multi-frame QR stream with GZIP compression, optional AES-256-GCM encryption,
     * CRC32 per packet, full-file SHA-256, and XOR parity packets (1 parity packet per [blockSize] data packets).
     */
    fun prepareStream(
        fileName: String,
        mimeType: String,
        rawBytes: ByteArray,
        chunkSize: Int = 400,
        blockSize: Int = 4,
        eccLevel: QrEccLevel = QrEccLevel.M,
        encryptionPassword: String? = null
    ): PreparedOpticalStream {
        val sessionId = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val fileSha256 = sha256Hex(rawBytes)
        val gzippedBytes = compressGzip(rawBytes)
        val useEncryption = !encryptionPassword.isNullOrBlank()
        val streamBytes = if (useEncryption) {
            encryptAes256Gcm(gzippedBytes, encryptionPassword!!)
        } else {
            gzippedBytes
        }

        val safeChunkSize = chunkSize.coerceIn(120, 900)
        val safeBlockSize = blockSize.coerceIn(2, 8)

        val totalDataChunks = ceil(streamBytes.size.toDouble() / safeChunkSize.toDouble())
            .toInt()
            .coerceAtLeast(1)

        val sanitizedName = fileName.replace("|", "_").replace("\n", " ").take(64).ifBlank { "file.bin" }
        val cleanBaseMime = stripEncryptedMimeSuffix(mimeType.replace("|", "_").take(40).ifBlank { "application/octet-stream" })
        val wireMime = if (useEncryption) "$cleanBaseMime$ENCRYPTED_MIME_SUFFIX" else cleanBaseMime

        val dataPackets = mutableListOf<OpticalPacket>()
        for (i in 0 until totalDataChunks) {
            val start = i * safeChunkSize
            val end = min(start + safeChunkSize, streamBytes.size)
            val slice = if (start < end) {
                streamBytes.copyOfRange(start, end)
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
                    mimeType = wireMime,
                    originalSizeBytes = rawBytes.size.toLong(),
                    compressedSizeBytes = streamBytes.size.toLong(),
                    payload = slice,
                    isEncrypted = useEncryption
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
                        mimeType = wireMime,
                        originalSizeBytes = rawBytes.size.toLong(),
                        compressedSizeBytes = streamBytes.size.toLong(),
                        payload = xorBuffer,
                        dataChunkCrcsInBlock = memberCrcs,
                        isEncrypted = useEncryption
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
            mimeType = cleanBaseMime,
            originalBytes = rawBytes,
            compressedBytes = streamBytes,
            fileSha256 = fileSha256,
            chunkSize = safeChunkSize,
            blockSize = safeBlockSize,
            eccLevel = eccLevel,
            dataPackets = dataPackets,
            parityPackets = parityPackets,
            allOrderedPackets = interleaved,
            encodedQrStrings = encodedStrings,
            isEncrypted = useEncryption
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
            val wireMimeType = parts[10]
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
                mimeType = wireMimeType,
                originalSizeBytes = originalSize,
                compressedSizeBytes = compressedSize,
                payload = payload,
                dataChunkCrcsInBlock = memberCrcs,
                isEncrypted = isEncryptedMimeType(wireMimeType)
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
     * Supports transparent AES-256-GCM decryption when [decryptionPassword] is provided for encrypted streams.
     */
    fun reconstructAndVerifyFile(
        sessionId: String,
        fileName: String,
        mimeType: String,
        expectedSha256: String,
        totalDataChunks: Int,
        receivedDataPayloads: Map<Int, ByteArray>,
        parityRecoveredIndices: Set<Int>,
        decryptionPassword: String? = null
    ): Result<ReconstructedFileResult> {
        return runCatching {
            require(receivedDataPayloads.size == totalDataChunks) {
                "Pacchetti incompleti: ricevuti ${receivedDataPayloads.size} su $totalDataChunks"
            }
            val assembledBos = ByteArrayOutputStream()
            for (i in 0 until totalDataChunks) {
                val chunk = receivedDataPayloads[i]
                    ?: throw IllegalStateException("Pacchetto #$i mancante")
                assembledBos.write(chunk)
            }
            val assembledBytes = assembledBos.toByteArray()
            val isEncrypted = isEncryptedMimeType(mimeType)
            val cleanMimeType = stripEncryptedMimeSuffix(mimeType)

            val gzipBytes = if (isEncrypted) {
                require(!decryptionPassword.isNullOrBlank()) {
                    "Password AES-256 richiesta per decifrare questo trasferimento"
                }
                try {
                    decryptAes256Gcm(assembledBytes, decryptionPassword!!)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Password errata o autenticazione AES-256-GCM fallita")
                }
            } else {
                assembledBytes
            }

            val decompressedBytes = decompressGzip(gzipBytes)
            val actualSha256 = sha256Hex(decompressedBytes)
            val verified = actualSha256.equals(expectedSha256, ignoreCase = true)

            ReconstructedFileResult(
                sessionId = sessionId,
                fileName = fileName,
                mimeType = cleanMimeType,
                rawBytes = decompressedBytes,
                compressedSizeBytes = assembledBytes.size.toLong(),
                expectedSha256 = expectedSha256,
                actualSha256 = actualSha256,
                integrityVerified = verified,
                totalDataChunks = totalDataChunks,
                parityRecoveredCount = parityRecoveredIndices.size,
                wasEncrypted = isEncrypted
            )
        }
    }
}
