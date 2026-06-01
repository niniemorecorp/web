package com.example.data.repository

import com.example.data.db.ReceiptDao
import com.example.data.db.UserDao
import com.example.data.model.Receipt
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class ReceiptRepository(
    private val receiptDao: ReceiptDao,
    private val userDao: UserDao
) {
    val allReceiptsFlow: Flow<List<Receipt>> = receiptDao.getAllReceiptsFlow()

    suspend fun getAllReceipts(): List<Receipt> = receiptDao.getAllReceipts()

    fun getReceiptByIdFlow(id: Int): Flow<Receipt?> = receiptDao.getReceiptByIdFlow(id)

    suspend fun getReceiptById(id: Int): Receipt? = receiptDao.getReceiptById(id)

    suspend fun getReceiptByNumber(receiptNumber: String): Receipt? = receiptDao.getReceiptByNumber(receiptNumber)

    suspend fun insertReceipt(receipt: Receipt): Long {
        return receiptDao.insertReceipt(receipt)
    }

    suspend fun updateReceipt(receipt: Receipt) {
        receiptDao.updateReceipt(receipt)
    }

    suspend fun deleteReceipt(receipt: Receipt) {
        receiptDao.deleteReceipt(receipt)
    }

    suspend fun deleteAll() = receiptDao.deleteAll()

    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    // Simulates an hourly background logistics progress tick.
    // Finds active receipts in "DIPROSES" or "TERKIRIM" status and appends a logical logistics milestone log.
    // If threshold met, updates status (e.g. DIPROSES -> TERKIRIM or TERKIRIM -> DITERIMA).
    // Returns a List of notification messages about what changed.
    suspend fun triggerHourlySimulatedUpdate(): List<String> {
        val all = receiptDao.getAllReceipts()
        val notifications = mutableListOf<String>()
        val now = System.currentTimeMillis()
        val checkpoints = listOf(
            "Berangkat dari Hub transit",
            "Dalam perjalanan ke pusat distribusi transit wilayah",
            "Paket tiba di gudang penyortiran utama regional",
            "Sedang diserahterimakan ke kurir lokal pengantar",
            "Paket sedang diantar oleh kurir menuju alamat rumah penerima"
        )

        for (receipt in all) {
            if (receipt.status == "DITERIMA") continue

            val logsList = mutableListOf<JSONObject>()
            try {
                if (receipt.trackingLogsJson.isNotEmpty()) {
                    val arr = JSONArray(receipt.trackingLogsJson)
                    for (i in 0 until arr.length()) {
                        logsList.add(arr.getJSONObject(i))
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }

            var nextStatus = receipt.status
            var logText = ""
            var logLocation = "Logistik Transit Center"

            if (receipt.status == "DIPROSES") {
                // Progress representing handoff to Courier
                logText = "Paket diserahkan oleh seller ke partner ekspedisi ${receipt.courierName}."
                logLocation = "Sinyal Logistik Terdekat"
                nextStatus = "TERKIRIM"
                notifications.add("Resi ${receipt.receiptNumber}: Paket diserahkan ke ${receipt.courierName}.")
            } else if (receipt.status == "TERKIRIM") {
                if (logsList.size >= 4) {
                    // Final Delivery
                    logText = "Paket berhasil diserahkan dan ditandatangani oleh penerima (${receipt.recipientName})."
                    logLocation = "Alamat Penerima"
                    nextStatus = "DITERIMA"
                    notifications.add("Resi ${receipt.receiptNumber}: Paket telah sampai & Diterima oleh ${receipt.recipientName}!")
                } else {
                    // Regular transit checkpoint
                    val rCheck = checkpoints.random()
                    logText = "$rCheck (Estimasi lancar)."
                    logLocation = listOf("Hub Transit Jakarta", "DC Surabaya", "Hub Bandung", "DC Makassar", "Transit Solo").random()
                    notifications.add("Resi ${receipt.receiptNumber}: Perkembangan kurir - $logLocation.")
                }
            }

            val newLog = JSONObject()
            newLog.put("timestamp", now)
            newLog.put("description", logText)
            newLog.put("location", logLocation)
            logsList.add(newLog)

            val updatedArr = JSONArray()
            logsList.forEach { updatedArr.put(it) }

            val updatedReceipt = receipt.copy(
                status = nextStatus,
                lastUpdated = now,
                trackingLogsJson = updatedArr.toString(),
                synced = false // Marked for re-sync
            )
            receiptDao.updateReceipt(updatedReceipt)
        }
        return notifications
    }
}
