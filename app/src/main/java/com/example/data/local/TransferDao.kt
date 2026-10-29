package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TransferRecordEntity>>

    @Query("SELECT * FROM transfer_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): TransferRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TransferRecordEntity): Long

    @Update
    suspend fun updateRecord(record: TransferRecordEntity)

    @Query("UPDATE transfer_records SET fileName = :newFileName WHERE id = :id")
    suspend fun renameRecord(id: Long, newFileName: String)

    @Query("DELETE FROM transfer_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
