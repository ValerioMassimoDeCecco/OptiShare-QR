package com.example

import com.example.domain.protocol.OpticalPacketProtocol
import com.example.domain.protocol.PacketKind
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun opticalProtocol_roundTripAndXorParityRecovery_succeedsWithZeroErrors() {
        val originalText = buildString {
            repeat(40) { idx ->
                appendLine("Riga #$idx — Test trasferimento ottico QR multi-frame con parità XOR e SHA-256.")
            }
        }
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val stream = OpticalPacketProtocol.prepareStream(
            fileName = "Protocollo_Test.txt",
            mimeType = "text/plain",
            rawBytes = originalBytes,
            chunkSize = 180,
            blockSize = 3
        )

        assertTrue(stream.dataPackets.size >= 2)
        assertTrue(stream.parityPackets.isNotEmpty())

        // Simulate dropping Data Chunk #1 and recovering it from Data Chunk #0, #2 and Parity #0
        val receivedData = mutableMapOf<Int, ByteArray>()
        for (pkt in stream.dataPackets) {
            if (pkt.index != 1) {
                val qrStr = OpticalPacketProtocol.encodePacketToQrString(pkt)
                val decoded = OpticalPacketProtocol.decodePacketFromQrString(qrStr).getOrThrow()
                receivedData[decoded.index] = decoded.payload
            }
        }

        val parity0Qr = OpticalPacketProtocol.encodePacketToQrString(stream.parityPackets.first())
        val decodedParity0 = OpticalPacketProtocol.decodePacketFromQrString(parity0Qr).getOrThrow()
        assertEquals(PacketKind.PARITY, decodedParity0.kind)

        val recoveredPair = OpticalPacketProtocol.tryRecoverMissingChunkInBlock(
            blockIndex = 0,
            blockSize = stream.blockSize,
            totalDataChunks = stream.dataPackets.size,
            chunkSize = stream.chunkSize,
            receivedDataPayloads = receivedData,
            parityPacket = decodedParity0
        )

        assertNotNull("Il pacchetto mancante #1 deve essere ricostruito via Parità XOR", recoveredPair)
        assertEquals(1, recoveredPair!!.first)
        receivedData[recoveredPair.first] = recoveredPair.second

        val reconstructed = OpticalPacketProtocol.reconstructAndVerifyFile(
            sessionId = stream.sessionId,
            fileName = stream.fileName,
            mimeType = stream.mimeType,
            expectedSha256 = stream.fileSha256,
            totalDataChunks = stream.dataPackets.size,
            receivedDataPayloads = receivedData,
            parityRecoveredIndices = setOf(1)
        ).getOrThrow()

        assertTrue(reconstructed.integrityVerified)
        assertEquals(stream.fileSha256, reconstructed.actualSha256)
        assertArrayEquals(originalBytes, reconstructed.rawBytes)
    }

    @Test
    fun opticalProtocol_corruptedCrc32Packet_isRejected() {
        val raw = "Dati importanti da verificare con CRC32".toByteArray(Charsets.UTF_8)
        val stream = OpticalPacketProtocol.prepareStream(
            fileName = "test.txt",
            mimeType = "text/plain",
            rawBytes = raw
        )
        val validQr = OpticalPacketProtocol.encodePacketToQrString(stream.dataPackets.first())
        // Tamper with the CRC32 hex field
        val tamperedQr = validQr.replace(stream.dataPackets.first().crc32Hex, "00000000")
        val decodeResult = OpticalPacketProtocol.decodePacketFromQrString(tamperedQr)
        assertTrue(decodeResult.isFailure)
    }
}
