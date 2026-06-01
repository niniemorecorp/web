package com.example.data.db

import androidx.room.*
import com.example.data.model.Receipt
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    fun getAllReceiptsFlow(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    suspend fun getAllReceipts(): List<Receipt>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun getReceiptByIdFlow(id: Int): Flow<Receipt?>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Int): Receipt?

    @Query("SELECT * FROM receipts WHERE receiptNumber = :receiptNumber")
    suspend fun getReceiptByNumber(receiptNumber: String): Receipt?

    @Query("SELECT * FROM receipts WHERE synced = 0")
    suspend fun getUnsyncedReceipts(): List<Receipt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: Receipt): Long

    @Update
    suspend fun updateReceipt(receipt: Receipt)

    @Delete
    suspend fun deleteReceipt(receipt: Receipt)

    @Query("DELETE FROM receipts")
    suspend fun deleteAll()
}
