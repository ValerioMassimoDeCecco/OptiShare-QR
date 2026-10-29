package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_records")
data class TransferRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val fileName: String,
    val smartSuggestedName: String? = null,
    val mimeType: String,
    val fileSizeBytes: Long,
    val compressedSizeBytes: Long,
    val totalDataChunks: Int,
    val parityChunks: Int,
    val sha256Hash: String,
    val integrityVerified: Boolean,
    val direction: String, // "SENT", "RECEIVED", "AI_OPTIMIZED"
    val localFilePath: String,
    val extractedText: String? = null,
    val aiSummary: String? = null,
    val aiCategory: String? = null,
    val aiKeyPointsJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
