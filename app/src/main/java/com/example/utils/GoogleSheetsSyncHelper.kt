package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.model.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object GoogleSheetsSyncHelper {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Default preference key to store custom Google Sheets webapp URL
    private const val PREFS_NAME = "resi_usaha_sync_prefs"
    private const val KEY_SHEETS_URL = "google_sheets_webapp_url"

    fun saveSheetsUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SHEETS_URL, url).apply()
    }

    fun getSheetsUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SHEETS_URL, "") ?: ""
        return if (saved.isNotBlank()) saved else "https://script.google.com/macros/s/AKfycbzboeV5_sPQ8DnJd-uEePUUTp81UEr4jHD-hAUbJDbetTS8H2EINouY0DHxYmlU0Tc0nQ/exec"
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * Attempts to post un-synced receipt to the Google Sheet App Script WebApp URL.
     * Returns true if successful.
     */
    suspend fun syncSingleReceiptToSheets(context: Context, receipt: Receipt): Boolean {
        val url = getSheetsUrl(context)
        if (url.isEmpty()) {
            // No URL configured yet. Return true if we are simulating success.
            // This allows the user to see a fully functional sync flow without strict URL configuration.
            return false
        }

        if (!isNetworkAvailable(context)) {
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDate = sdf.format(Date(receipt.timestamp))

                val json = JSONObject().apply {
                    put("receiptNumber", receipt.receiptNumber)
                    put("recipientName", receipt.recipientName)
                    put("recipientPhone", receipt.recipientPhone)
                    put("recipientAddress", receipt.recipientAddress)
                    put("courierName", receipt.courierName)
                    put("itemName", receipt.itemName)
                    put("itemPrice", receipt.itemPrice)
                    put("status", receipt.status)
                    put("dateInput", formattedDate)
                    put("lastUpdated", sdf.format(Date(receipt.lastUpdated)))
                }

                val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val isSuccessful = response.isSuccessful
                    Log.d("SheetsSync", "Syncing ${receipt.receiptNumber} returned code ${response.code}. Success: $isSuccessful")
                    isSuccessful
                }
            } catch (e: Exception) {
                Log.e("SheetsSync", "Failed syncing receipt: ${receipt.receiptNumber}", e)
                false
            }
        }
    }

    /**
     * Fetch receipts remotely from Google Sheet App Script using GET.
     */
    suspend fun fetchReceiptsFromSheets(context: Context): List<Receipt> {
        val url = getSheetsUrl(context)
        if (url.isEmpty() || !isNetworkAvailable(context)) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val jsonObj = JSONObject(bodyString)
                        if (jsonObj.optString("status") == "success") {
                            val rArray = jsonObj.optJSONArray("receipts") ?: return@withContext emptyList()
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val list = mutableListOf<Receipt>()
                            
                            for (i in 0 until rArray.length()) {
                                val rObj = rArray.getJSONObject(i)
                                val receiptNo = rObj.getString("receiptNumber")
                                
                                // Skip if the receipt is locally deleted by caller
                                if (DeletedReceiptsTracker.isReceiptDeleted(context, receiptNo)) {
                                    continue
                                }
                                
                                val itemName = rObj.getString("itemName")
                                val itemPrice = rObj.getDouble("itemPrice")
                                val status = rObj.getString("status")
                                val courierName = rObj.getString("courierName")
                                val recipientName = rObj.getString("recipientName")
                                val recipientPhone = rObj.getString("recipientPhone")
                                val recipientAddress = rObj.getString("recipientAddress")
                                
                                val dateInputStr = rObj.optString("dateInput")
                                val dateInputTimestamp = try {
                                    sdf.parse(dateInputStr)?.time ?: System.currentTimeMillis()
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                                
                                val lastUpdatedStr = rObj.optString("lastUpdated")
                                val lastUpdatedTimestamp = try {
                                    sdf.parse(lastUpdatedStr)?.time ?: dateInputTimestamp
                                } catch (e: Exception) {
                                    dateInputTimestamp
                                }
                                
                                val docLogsJson = """[{"timestamp":$dateInputTimestamp,"description":"Sinkron dengan Google Sheets cloud.","location":"Google Sheets Server"}]"""

                                val receipt = Receipt(
                                    receiptNumber = receiptNo,
                                    encryptedRecipientName = SecurityHelper.encrypt(recipientName),
                                    encryptedRecipientPhone = SecurityHelper.encrypt(recipientPhone),
                                    encryptedRecipientAddress = SecurityHelper.encrypt(recipientAddress),
                                    courierName = courierName,
                                    status = status,
                                    itemName = itemName,
                                    itemPrice = itemPrice,
                                    timestamp = dateInputTimestamp,
                                    lastUpdated = lastUpdatedTimestamp,
                                    synced = true,
                                    trackingLogsJson = docLogsJson
                                )
                                list.add(receipt)
                            }
                            list
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("SheetsSync", "Failed pulling receipts from sheets webapp", e)
                emptyList()
            }
        }
    }

    /**
     * Get the Google Apps Script code for copy-pasting.
     */
    fun getGoogleAppsScriptTemplate(): String {
        return """
            // COPY-PASTE GOOGLE APPS SCRIPT KODE BERIKUT KE INSTANSI GOOGLE SHEETS ANDA:
            // 1. Buka Google Sheets Anda. Di menu atas pilih Extensions -> Apps Script.
            // 2. Clear editor, paste kode ini, lalu klik Save.
            // 3. Klik Deploy -> New Deployment. Pilih type "Web App".
            // 4. Set "Execute as" ke "Me" (Email Anda), dan "Who has access" ke "Anyone".
            // 5. Salin URL Web App yang dihasilkan, paste ke menu Sync Settings di Aplikasi HP ini!

            function doGet(e) {
              try {
                var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
                var data = [];
                var lastRow = sheet.getLastRow();
                
                if (lastRow > 1) {
                  var range = sheet.getRange(2, 1, lastRow - 1, 10);
                  var values = range.getValues();
                  for (var i = 0; i < values.length; i++) {
                    var row = values[i];
                    data.push({
                      receiptNumber: String(row[0]),
                      recipientName: String(row[1]),
                      recipientPhone: String(row[2]).replace(/^'/, ''), // remove quotes indicator
                      recipientAddress: String(row[3]),
                      courierName: String(row[4]),
                      itemName: String(row[5]),
                      itemPrice: Number(row[6]),
                      status: String(row[7]),
                      dateInput: String(row[8]),
                      lastUpdated: String(row[9])
                    });
                  }
                }
                
                return ContentService.createTextOutput(JSON.stringify({
                  "status": "success",
                  "receipts": data
                })).setMimeType(ContentService.MimeType.JSON);
                
              } catch (err) {
                return ContentService.createTextOutput(JSON.stringify({
                  "status": "error",
                  "message": err.toString()
                })).setMimeType(ContentService.MimeType.JSON);
              }
            }
            
            function doPost(e) {
              try {
                var jsonString = e.postData.contents;
                var data = JSON.parse(jsonString);
                
                var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
                
                // Tambahkan headers jika sheet masih kosong kosong
                if (sheet.getLastRow() === 0) {
                  sheet.appendRow([
                    "Nomor Resi", "Nama Penerima", "No HP", "Alamat Lengkap",
                    "Ekspedisi", "Nama Barang", "Harga Barang (Rp)", "Status", 
                    "Tanggal Input", "Update Terakhir"
                  ]);
                }
                
                var lastRow = sheet.getLastRow();
                var foundRow = -1;
                
                if (lastRow > 1) {
                  var range = sheet.getRange(2, 1, lastRow - 1, 1);
                  var values = range.getValues();
                  for (var i = 0; i < values.length; i++) {
                    if (String(values[i][0]).toUpperCase() === String(data.receiptNo || data.receiptNumber).toUpperCase()) {
                      foundRow = i + 2; // +2 for 1-index and headers
                      break;
                    }
                  }
                }
                
                var rowData = [
                  data.receiptNo || data.receiptNumber,
                  data.recipientName,
                  "'" + data.recipientPhone, // safe string format
                  data.recipientAddress,
                  data.courierName,
                  data.itemName,
                  Number(data.itemPrice),
                  data.status,
                  data.dateInput || data.timestamp,
                  data.lastUpdated || data.timestamp
                ];
                
                if (foundRow !== -1) {
                  sheet.getRange(foundRow, 1, 1, 10).setValues([rowData]);
                } else {
                  sheet.appendRow(rowData);
                }
                
                return ContentService.createTextOutput(JSON.stringify({
                  "status": "success",
                  "message": "Resi " + (data.receiptNo || data.receiptNumber) + " berhasil disinkronisasikan!"
                })).setMimeType(ContentService.MimeType.JSON);
                
              } catch (err) {
                return ContentService.createTextOutput(JSON.stringify({
                  "status": "error",
                  "message": err.toString()
                })).setMimeType(ContentService.MimeType.JSON);
              }
            }
        """.trimIndent()
    }
}
