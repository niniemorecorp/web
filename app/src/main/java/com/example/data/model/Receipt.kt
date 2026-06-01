package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.utils.SecurityHelper

data class TrackingLog(
    val timestamp: Long,
    val description: String,
    val location: String
)

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptNumber: String,
    val encryptedRecipientName: String,
    val encryptedRecipientPhone: String,
    val encryptedRecipientAddress: String,
    val courierName: String, // JNE, J&T, Sicepat, POS, Wahana, Lion
    val status: String, // DIPROSES, TERKIRIM, DITERIMA
    val itemName: String,
    val itemPrice: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val trackingLogsJson: String = "" // Store tracking timeline details as serialized JSON
) {
    // Decrypted transparent convenience fields for UI display
    val recipientName: String
        get() = SecurityHelper.decrypt(encryptedRecipientName)
        
    val recipientPhone: String
        get() = SecurityHelper.decrypt(encryptedRecipientPhone)
        
    val recipientAddress: String
        get() = SecurityHelper.decrypt(encryptedRecipientAddress)

    companion object {
        fun createNew(
            receiptNumber: String,
            recipientName: String,
            recipientPhone: String,
            recipientAddress: String,
            courierName: String,
            itemName: String,
            itemPrice: Double,
            status: String = "DIPROSES"
        ): Receipt {
            val now = System.currentTimeMillis()
            // Initial log entry
            val initialLogJson = """[{"timestamp":$now,"description":"Data resi usaha berhasil diinput. Menunggu penyerahan paket ke kurir $courierName.","location":"Gudang Utama Seller"}]"""
            return Receipt(
                receiptNumber = receiptNumber,
                encryptedRecipientName = SecurityHelper.encrypt(recipientName),
                encryptedRecipientPhone = SecurityHelper.encrypt(recipientPhone),
                encryptedRecipientAddress = SecurityHelper.encrypt(recipientAddress),
                courierName = courierName,
                status = status,
                itemName = itemName,
                itemPrice = itemPrice,
                timestamp = now,
                lastUpdated = now,
                synced = false,
                trackingLogsJson = initialLogJson
            )
        }
    }
}
