package com.example.domain.protocol

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.EnumMap

object QrCodecEngine {

    private val decodeHints: Map<DecodeHintType, Any> = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
        put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
        put(DecodeHintType.TRY_HARDER, true)
        put(DecodeHintType.CHARACTER_SET, "UTF-8")
    }

    // Thread-local reusable buffers to eliminate per-frame allocations during 120Hz/60Hz camera scanning
    private val threadLocalReader = object : ThreadLocal<QRCodeReader>() {
        override fun initialValue(): QRCodeReader = QRCodeReader()
    }
    private val threadLocalYBuffer = ThreadLocal<ByteArray>()
    private val threadLocalRotatedBuffer = ThreadLocal<ByteArray>()
    private val threadLocalRowBuffer = ThreadLocal<ByteArray>()

    private fun obtainYBuffer(size: Int): ByteArray {
        val current = threadLocalYBuffer.get()
        if (current != null && current.size == size) return current
        return ByteArray(size).also { threadLocalYBuffer.set(it) }
    }

    private fun obtainRotatedBuffer(size: Int): ByteArray {
        val current = threadLocalRotatedBuffer.get()
        if (current != null && current.size == size) return current
        return ByteArray(size).also { threadLocalRotatedBuffer.set(it) }
    }

    private fun obtainRowBuffer(size: Int): ByteArray {
        val current = threadLocalRowBuffer.get()
        if (current != null && current.size >= size) return current
        return ByteArray(size).also { threadLocalRowBuffer.set(it) }
    }

    /**
     * Generates a list of QR Code [Bitmap]s concurrently across all CPU cores using [Dispatchers.Default].
     */
    suspend fun generateQrBitmapsParallel(
        contents: List<String>,
        sizePx: Int = 680,
        eccLevel: QrEccLevel = QrEccLevel.M
    ): List<Bitmap> = coroutineScope {
        contents.map { qrStr ->
            async(Dispatchers.Default) {
                generateQrBitmap(
                    content = qrStr,
                    sizePx = sizePx,
                    eccLevel = eccLevel
                )
            }
        }.awaitAll()
    }

    /**
     * Generates a high-contrast QR Code [Bitmap] from [content] for reliable optical camera reading.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 720,
        eccLevel: QrEccLevel = QrEccLevel.M
    ): Bitmap {
        val zxingEcc = when (eccLevel) {
            QrEccLevel.L -> ErrorCorrectionLevel.L
            QrEccLevel.M -> ErrorCorrectionLevel.M
            QrEccLevel.Q -> ErrorCorrectionLevel.Q
            QrEccLevel.H -> ErrorCorrectionLevel.H
        }
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, zxingEcc)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 2)
        }

        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    /**
     * Decodes a QR string from a [Bitmap] (used for Loopback Testing and Gallery/Screenshot QR import).
     */
    fun decodeFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val reader = threadLocalReader.get() ?: QRCodeReader()

        return try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            reader.decode(binaryBitmap, decodeHints).text
        } catch (_: Exception) {
            try {
                val inverted = BinaryBitmap(HybridBinarizer(InvertedLuminanceSource(source)))
                reader.decode(inverted, decodeHints).text
            } catch (_: Exception) {
                null
            }
        } finally {
            reader.reset()
        }
    }

    /**
     * Decodes a QR string from a CameraX [ImageProxy] YUV_420_888 frame in real time
     * using zero-allocation thread-local luminance buffers.
     */
    fun decodeFromImageProxy(imageProxy: ImageProxy): String? {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height

        val yBytes = obtainYBuffer(width * height)
        if (rowStride == width && pixelStride == 1) {
            buffer.rewind()
            buffer.get(yBytes, 0, width * height)
        } else {
            val rowBuffer = obtainRowBuffer(rowStride)
            buffer.rewind()
            for (row in 0 until height) {
                val bytesToRead = if (row == height - 1) {
                    minOf(rowStride, buffer.remaining())
                } else {
                    rowStride
                }
                buffer.get(rowBuffer, 0, bytesToRead)
                if (pixelStride == 1) {
                    System.arraycopy(rowBuffer, 0, yBytes, row * width, width)
                } else {
                    for (col in 0 until width) {
                        yBytes[row * width + col] = rowBuffer[col * pixelStride]
                    }
                }
            }
        }

        val source = PlanarYUVLuminanceSource(
            yBytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val reader = threadLocalReader.get() ?: QRCodeReader()

        // 1. Try normal orientation & luminance
        try {
            return reader.decode(BinaryBitmap(HybridBinarizer(source)), decodeHints).text
        } catch (_: Exception) {
            reader.reset()
        }

        // 2. Try inverted luminance
        try {
            return reader.decode(
                BinaryBitmap(HybridBinarizer(InvertedLuminanceSource(source))),
                decodeHints
            ).text
        } catch (_: Exception) {
            reader.reset()
        }

        // 3. Try 90-degree rotated Y plane (portrait phone scanning landscape/portrait QR)
        return try {
            val rotated = rotateYPlane90Into(yBytes, width, height)
            val rotatedSource = PlanarYUVLuminanceSource(
                rotated,
                height,
                width,
                0,
                0,
                height,
                width,
                false
            )
            reader.decode(BinaryBitmap(HybridBinarizer(rotatedSource)), decodeHints).text
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun rotateYPlane90Into(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = obtainRotatedBuffer(data.size)
        for (y in 0 until height) {
            val rowOffset = y * width
            val targetCol = height - y - 1
            for (x in 0 until width) {
                rotated[x * height + targetCol] = data[rowOffset + x]
            }
        }
        return rotated
    }
}
