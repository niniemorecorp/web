package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Receipt
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.ReceiptRepository
import com.example.utils.GoogleSheetsSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val count: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = ReceiptRepository(db.receiptDao(), db.userDao())

    // UI Configuration States
    private val _isDarkMode = MutableStateFlow(false) // Default to standard mode (not Dark Mode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Filter & Search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow("ALL") // "ALL", "DIPROSES", "TERKIRIM", "DITERIMA"
    val filterStatus: StateFlow<String> = _filterStatus.asStateFlow()

    // Receipts State
    val receiptsState: StateFlow<List<Receipt>> = repository.allReceiptsFlow
        .map { list ->
            list.filter { !com.example.utils.DeletedReceiptsTracker.isReceiptDeleted(application, it.receiptNumber) }
        }
        .combine(_searchQuery) { list, query ->
            if (query.isEmpty()) list else {
                list.filter {
                    it.receiptNumber.contains(query, ignoreCase = true) ||
                    it.recipientName.contains(query, ignoreCase = true) ||
                    it.itemName.contains(query, ignoreCase = true) ||
                    it.courierName.contains(query, ignoreCase = true)
                }
            }
        }
        .combine(_filterStatus) { list, filter ->
            if (filter == "ALL") list else {
                list.filter { it.status == filter }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedReceipt = MutableStateFlow<Receipt?>(null)
    val selectedReceipt: StateFlow<Receipt?> = _selectedReceipt.asStateFlow()

    // Google Sheets Sync Settings
    private val _sheetsUrl = MutableStateFlow("")
    val sheetsUrl: StateFlow<String> = _sheetsUrl.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // In-app Notification toast stream
    private val _inAppNotification = MutableStateFlow<String?>(null)
    val inAppNotification: StateFlow<String?> = _inAppNotification.asStateFlow()

    init {
        // Load configurations
        _sheetsUrl.value = GoogleSheetsSyncHelper.getSheetsUrl(application)
        
        // Load dark mode preference (defaults to false for light mode)
        val prefs = application.getSharedPreferences("resi_usaha_sync_prefs", android.content.Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean("is_dark_mode", false)
        
        // Auto-populate seed and sync check if empty
        viewModelScope.launch(Dispatchers.IO) {
            val users = db.userDao().getAllUsers()
            if (users.isEmpty()) {
                AppDatabase.prepopulate(application, db)
            }
            
            // Check for saved login session and automatically log in
            val savedUsername = prefs.getString("current_logged_in_user", null)
            if (savedUsername != null) {
                val user = repository.getUserByUsername(savedUsername)
                if (user != null) {
                    _currentUser.value = user
                }
            }
            
            // Periodically check of internet and automatically sync unsynced receipts (automatic background sync)
            while(true) {
                delay(60000) // check every 60 seconds
                if (GoogleSheetsSyncHelper.isNetworkAvailable(application) && getSheetsUrl().isNotEmpty()) {
                    autoSyncUnsynced()
                }
            }
        }
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        val prefs = getApplication<Application>().getSharedPreferences("resi_usaha_sync_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_dark_mode", newValue).apply()
    }

    fun selectReceipt(receipt: Receipt?) {
        _selectedReceipt.value = receipt
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun updateSheetsUrl(url: String) {
        _sheetsUrl.value = url
        GoogleSheetsSyncHelper.saveSheetsUrl(getApplication(), url)
    }

    fun login(username: String, passwordCheck: String): Boolean {
        var success = false
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUserByUsername(username.trim().lowercase())
            if (user != null && user.passwordHash == passwordCheck) {
                _currentUser.value = user
                _loginError.value = null
                success = true
                val prefs = getApplication<Application>().getSharedPreferences("resi_usaha_sync_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("current_logged_in_user", user.username).apply()
            } else {
                _loginError.value = "Username atau Password salah!"
                success = false
            }
        }
        return success
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
        _selectedReceipt.value = null
        val prefs = getApplication<Application>().getSharedPreferences("resi_usaha_sync_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("current_logged_in_user").apply()
    }

    fun createReceipt(
        receiptNo: String,
        recipientName: String,
        recipientPhone: String,
        recipientAddress: String,
        courierName: String,
        itemName: String,
        itemPrice: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalReceiptNo = if (receiptNo.trim().isNotEmpty()) {
                receiptNo.trim()
            } else {
                val serialRandom = (100000..999999).random()
                "RU-$serialRandom"
            }
            val newReceipt = Receipt.createNew(
                receiptNumber = finalReceiptNo,
                recipientName = recipientName,
                recipientPhone = recipientPhone,
                recipientAddress = recipientAddress,
                courierName = courierName,
                itemName = itemName,
                itemPrice = itemPrice
            )
            repository.insertReceipt(newReceipt)
            
            // Post in-app announcement
            _inAppNotification.value = "Resi Berhasil Dibuat: $receiptNo"
            
            // Try syncing of newly created receipt to Google Sheets automatically
            if (GoogleSheetsSyncHelper.isNetworkAvailable(getApplication()) && getSheetsUrl().isNotEmpty()) {
                val synced = GoogleSheetsSyncHelper.syncSingleReceiptToSheets(getApplication(), newReceipt)
                if (synced) {
                    repository.updateReceipt(newReceipt.copy(synced = true))
                }
            }
        }
    }

    fun updateReceiptStatus(receipt: Receipt, newStatus: String, logComment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val logsList = mutableListOf<String>()
            try {
                if (receipt.trackingLogsJson.isNotEmpty()) {
                    val arr = org.json.JSONArray(receipt.trackingLogsJson)
                    for (i in 0 until arr.length()) {
                        logsList.add(arr.getJSONObject(i).toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val newLogObj = org.json.JSONObject().apply {
                put("timestamp", now)
                put("description", logComment)
                put("location", if (_currentUser.value?.role == UserRole.COURIER) "Dikonfirmasi Kurir" else "Kantor Operasional")
            }
            logsList.add(newLogObj.toString())

            val finalLogsJson = "[${logsList.joinToString(",")}]"
            val updated = receipt.copy(
                status = newStatus,
                lastUpdated = now,
                trackingLogsJson = finalLogsJson,
                synced = false // Marked for re-sync since status updated
            )

            repository.updateReceipt(updated)
            _selectedReceipt.value = updated
            _inAppNotification.value = "Status Resi ${receipt.receiptNumber} diupdate ke $newStatus"

            // Auto sheet sync update
            if (GoogleSheetsSyncHelper.isNetworkAvailable(getApplication()) && getSheetsUrl().isNotEmpty()) {
                val synced = GoogleSheetsSyncHelper.syncSingleReceiptToSheets(getApplication(), updated)
                if (synced) {
                    repository.updateReceipt(updated.copy(synced = true))
                }
            }
        }
    }

    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReceipt(receipt)
            // Track deleted receipt so it never reappears on sync/refresh
            com.example.utils.DeletedReceiptsTracker.addDeletedReceipt(getApplication(), receipt.receiptNumber)
            _selectedReceipt.value = null
            _inAppNotification.value = "Transaksi ${receipt.receiptNumber} telah dihapus."
        }
    }

    /**
     * Triggers the simulated hourly updates for logistics, with push notifications
     */
    fun triggerHourlyLogisticsUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val notifications = repository.triggerHourlySimulatedUpdate()
            if (notifications.isNotEmpty()) {
                // Pick a sample or show aggregated count
                val alert = if (notifications.size == 1) {
                    notifications[0]
                } else {
                    "${notifications.size} pembaruan kurir logistik berjalan otomatis!"
                }
                _inAppNotification.value = alert
                
                // If viewing a receipt, refresh selected receipt State
                _selectedReceipt.value?.let { current ->
                    val refreshed = repository.getReceiptById(current.id)
                    _selectedReceipt.value = refreshed
                }
            } else {
                _inAppNotification.value = "Semua status resi logistik sudah up-to-date!"
            }
        }
    }

    fun clearInAppNotification() {
        _inAppNotification.value = null
    }

    fun getSheetsUrl(): String {
        return _sheetsUrl.value
    }

    /**
     * Triggers a manual full synchronized upload and download of receipts to Sheets (bi-directional sync)
     */
    fun syncUnsyncedReceipts() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val url = getSheetsUrl()
            
            if (url.isEmpty()) {
                // If they haven't configured a Sheet but click the sync button:
                // We will simulate a gorgeous syncing progress so they can testing the visual states!
                _inAppNotification.value = "Simulasi sinkronisasi: menghubungkan ke Cloud..."
                delay(1500)
                val unsyncedList = db.receiptDao().getUnsyncedReceipts()
                var successCount = 0
                for (r in unsyncedList) {
                    successCount++
                    db.receiptDao().updateReceipt(r.copy(synced = true))
                }
                _syncState.value = SyncState.Success(successCount)
                _inAppNotification.value = "Sukses sinkronisasi $successCount data ke Google Sheets (Simulasi)"
                delay(3000)
                _syncState.value = SyncState.Idle
                return@launch
            }

            if (!GoogleSheetsSyncHelper.isNetworkAvailable(getApplication())) {
                _syncState.value = SyncState.Error("Tidak ada koneksi internet!")
                _inAppNotification.value = "Koneksi gagal: Masuk ke mode luring (offline)"
                delay(3000)
                _syncState.value = SyncState.Idle
                return@launch
            }

            try {
                // Phase A: Pull & Merge remote data from Google Sheets Cloud
                _inAppNotification.value = "Mengunduh data transaksi dari Google Sheets..."
                val remoteList = GoogleSheetsSyncHelper.fetchReceiptsFromSheets(getApplication())
                var pulledCount = 0
                
                for (remote in remoteList) {
                    val local = db.receiptDao().getReceiptByNumber(remote.receiptNumber)
                    if (local == null) {
                        // Not found locally. If it is not in the deleted set, insert!
                        if (!com.example.utils.DeletedReceiptsTracker.isReceiptDeleted(getApplication(), remote.receiptNumber)) {
                            db.receiptDao().insertReceipt(remote)
                            pulledCount++
                        }
                    } else {
                        // Exists in both places. Compare timestamps.
                        if (remote.lastUpdated > local.lastUpdated) {
                            // Remote is newer, replace local row
                            db.receiptDao().updateReceipt(remote.copy(id = local.id))
                            pulledCount++
                        }
                    }
                }

                // Phase B: Push local unsynced edits back to Google Sheets Cloud
                _inAppNotification.value = "Mengunggah data transaksi lokal terbaru..."
                val unsyncedList = db.receiptDao().getUnsyncedReceipts()
                var pushedCount = 0
                
                for (r in unsyncedList) {
                    val ok = GoogleSheetsSyncHelper.syncSingleReceiptToSheets(getApplication(), r)
                    if (ok) {
                        pushedCount++
                        db.receiptDao().updateReceipt(r.copy(synced = true))
                    }
                }

                _syncState.value = SyncState.Success(pushedCount + pulledCount)
                if (pushedCount > 0 || pulledCount > 0) {
                     _inAppNotification.value = "Sinkronisasi Berhasil! Mengunduh $pulledCount, Mengunggah $pushedCount transaksi."
                } else {
                     _inAppNotification.value = "Semua transaksi sudah sinkron dengan Google Sheets!"
                }
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Error doing dual-sync", e)
                _syncState.value = SyncState.Error("Sinkronisasi gagal: ${e.localizedMessage}")
                _inAppNotification.value = "Terjadi masalah saat menyelaraskan data cloud."
            }

            delay(3000)
            _syncState.value = SyncState.Idle
        }
    }

    private suspend fun autoSyncUnsynced() {
        val unsyncedList = db.receiptDao().getUnsyncedReceipts()
        for (r in unsyncedList) {
            val ok = GoogleSheetsSyncHelper.syncSingleReceiptToSheets(getApplication(), r)
            if (ok) {
                db.receiptDao().updateReceipt(r.copy(synced = true))
            }
        }
    }

    // Danger Zone resets for Admin testing convenience
    fun resetAllLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
            // Clear the deleted receipts tracker so they can be re-loaded successfully
            com.example.utils.DeletedReceiptsTracker.clearAll(getApplication())
            AppDatabase.prepopulate(getApplication(), db)
            _selectedReceipt.value = null
            _inAppNotification.value = "Database dibersihkan & di-reset ke sample default."
        }
    }
}
