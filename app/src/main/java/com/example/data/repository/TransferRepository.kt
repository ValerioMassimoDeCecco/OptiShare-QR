package com.example.data.repository

import android.content.Context
import com.example.data.local.TransferDao
import com.example.data.local.TransferRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class TransferRepository(
    private val context: Context,
    private val transferDao: TransferDao
) {
    val allRecords: Flow<List<TransferRecordEntity>> = transferDao.getAllRecords()

    suspend fun saveTransferFileAndRecord(
        sessionId: String,
        fileName: String,
        mimeType: String,
        rawBytes: ByteArray,
        compressedSizeBytes: Long,
        totalDataChunks: Int,
        parityChunks: Int,
        sha256Hash: String,
        integrityVerified: Boolean,
        direction: String
    ): TransferRecordEntity = withContext(Dispatchers.IO) {
        val transfersDir = File(context.filesDir, "transfers").apply {
            if (!exists()) mkdirs()
        }
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "file_$sessionId.bin" }
        val targetFile = File(transfersDir, "${System.currentTimeMillis()}_$safeName")
        targetFile.writeBytes(rawBytes)

        val textPreview = if (
            mimeType.startsWith("text/") ||
            safeName.endsWith(".txt", true) ||
            safeName.endsWith(".md", true) ||
            safeName.endsWith(".json", true) ||
            safeName.endsWith(".csv", true)
        ) {
            runCatching { rawBytes.toString(Charsets.UTF_8).take(10_000) }.getOrNull()
        } else null

        val entity = TransferRecordEntity(
            sessionId = sessionId,
            fileName = fileName,
            mimeType = mimeType,
            fileSizeBytes = rawBytes.size.toLong(),
            compressedSizeBytes = compressedSizeBytes,
            totalDataChunks = totalDataChunks,
            parityChunks = parityChunks,
            sha256Hash = sha256Hash,
            integrityVerified = integrityVerified,
            direction = direction,
            localFilePath = targetFile.absolutePath,
            textPreview = textPreview
        )
        val id = transferDao.insertRecord(entity)
        entity.copy(id = id)
    }

    suspend fun renameRecord(recordId: Long, newFileName: String) = withContext(Dispatchers.IO) {
        val current = transferDao.getRecordById(recordId) ?: return@withContext
        val oldFile = File(current.localFilePath)
        val safeName = newFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { current.fileName }
        val newFile = if (oldFile.exists()) {
            val target = File(oldFile.parentFile, "${System.currentTimeMillis()}_$safeName")
            if (oldFile.renameTo(target)) target else oldFile
        } else oldFile

        transferDao.updateRecord(
            current.copy(
                fileName = newFileName,
                localFilePath = newFile.absolutePath
            )
        )
    }

    suspend fun deleteRecord(record: TransferRecordEntity) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(record.localFilePath)
            if (file.exists()) file.delete()
        }
        transferDao.deleteRecordById(record.id)
    }

    suspend fun readRecordBytes(record: TransferRecordEntity): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(record.localFilePath)
            if (file.exists()) file.readBytes() else null
        }.getOrNull()
    }
}
