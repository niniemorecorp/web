package com.example.utils

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Receipt
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    /**
     * Exports receipts database to a clean, Excel-compatible CSV dataset
     * and triggers a Sharesheet Intent for the user to export or send.
     */
    fun exportToExcelCsv(context: Context, receipts: List<Receipt>) {
        try {
            val csvHeader = "ID,Nomor Resi,Tanggal,Nama Penerima,Nomor HP,Alamat,Kurir,Nama Barang,Harga Barang,Status,Sinkronisasi\n"
            val sb = StringBuilder(csvHeader)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            for (r in receipts) {
                // Escape commas to avoid broken structures in CSV
                val cleanName = r.recipientName.replace(",", " ")
                val cleanAddr = r.recipientAddress.replace(",", " ").replace("\n", " ")
                val cleanItem = r.itemName.replace(",", " ")
                val dateStr = sdf.format(Date(r.timestamp))
                
                sb.append("${r.id},")
                  .append("${r.receiptNumber},")
                  .append("$dateStr,")
                  .append("$cleanName,")
                  .append("'${r.recipientPhone},") // Prepended single-quote preserves leading zeros in Excel
                  .append("$cleanAddr,")
                  .append("${r.courierName},")
                  .append("$cleanItem,")
                  .append("${r.itemPrice.toInt()},")
                  .append("${r.status},")
                  .append("${if (r.synced) "Selesai" else "Tertunda"}\n")
            }

            // Write to app directory
            val directory = context.cacheDir
            val file = File(directory, "Laporan_Resi_Usaha_${System.currentTimeMillis()}.csv")
            file.writeText(sb.toString(), Charsets.UTF_8)

            // Trigger system share sheet
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Resi Usaha (Format Excel)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Ekspor Laporan Ke Excel"))

        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengekspor data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Renders a highly-stylized business invoice/report in HTML
     * and passes it to the Android system Print Job, allowing PDF generation or printer output.
     */
    fun exportToPdfAndPrint(context: Context, receipts: List<Receipt>) {
        try {
            val formatter = DecimalFormat("#,###")
            val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
            val dateGenerated = sdf.format(Date())

            val totalRevenue = receipts.sumOf { it.itemPrice }
            val countDiproses = receipts.count { it.status == "DIPROSES" }
            val countTerkirim = receipts.count { it.status == "TERKIRIM" }
            val countDiterima = receipts.count { it.status == "DITERIMA" }

            // Build beautifully styled HTML
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                <style>
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #2D3748;
                        margin: 20px;
                        font-size: 11px;
                        line-height: 1.4;
                    }
                    .header-table {
                        width: 100%;
                        margin-bottom: 25px;
                        border-collapse: collapse;
                    }
                    .brand {
                        font-size: 24px;
                        font-weight: bold;
                        color: #1A365D;
                    }
                    .info {
                        text-align: right;
                        color: #718096;
                    }
                    .summary-box {
                        display: flex;
                        justify-content: space-between;
                        background: #F7FAFC;
                        border: 1px solid #E2E8F0;
                        padding: 12px;
                        border-radius: 6px;
                        margin-bottom: 25px;
                    }
                    .summary-item {
                        text-align: center;
                        flex: 1;
                    }
                    .summary-item h4 {
                        margin: 0 0 5px 0;
                        color: #4A5568;
                        text-transform: uppercase;
                        font-size: 9px;
                        letter-spacing: 0.5px;
                    }
                    .summary-item p {
                        margin: 0;
                        font-size: 14px;
                        font-weight: bold;
                        color: #2B6CB0;
                    }
                    table.data-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 15px;
                    }
                    table.data-table th {
                        background-color: #2B6CB0;
                        color: white;
                        text-align: left;
                        padding: 10px;
                        font-weight: bold;
                        font-size: 10px;
                    }
                    table.data-table td {
                        padding: 8px 10px;
                        border-bottom: 1px solid #E2E8F0;
                        font-size: 9.5px;
                    }
                    table.data-table tr:nth-child(even) {
                        background-color: #F8FAFC;
                    }
                    .badge {
                        padding: 3px 6px;
                        border-radius: 4px;
                        font-weight: bold;
                        font-size: 8px;
                        text-transform: uppercase;
                        display: inline-block;
                    }
                    .badge-diproses { background-color: #FEFCBF; color: #B7791F; }
                    .badge-terkirim { background-color: #EBF8FF; color: #2B6CB0; }
                    .badge-diterima { background-color: #C6F6D5; color: #22543D; }
                    .footer {
                        margin-top: 40px;
                        text-align: center;
                        font-size: 9px;
                        color: #A0AEC0;
                        border-top: 1px dashed #E2E8F0;
                        padding-top: 15px;
                    }
                </style>
                </head>
                <body>
                    <table class="header-table">
                        <tr>
                            <td>
                                <div class="brand">RESI USAHA</div>
                                <div style="color: #4A5568; font-size: 12px; margin-top: 3px;">Ringkasan Laporan Penjualan & Pengiriman</div>
                            </td>
                            <td class="info">
                                <strong>Tanggal Cetak:</strong> $dateGenerated<br>
                                <strong>Dokumen ID:</strong> RU-DOC-${System.currentTimeMillis() / 1000}
                            </td>
                        </tr>
                    </table>

                    <div class="summary-box">
                        <div class="summary-item">
                            <h4>Total Omzet</h4>
                            <p>Rp ${formatter.format(totalRevenue)}</p>
                        </div>
                        <div class="summary-item" style="border-left: 1px solid #E2E8F0; border-right: 1px solid #E2E8F0;">
                            <h4>Jumlah Resi</h4>
                            <p>${receipts.size}</p>
                        </div>
                        <div class="summary-item">
                            <h4>Status Pengiriman</h4>
                            <p style="font-size: 11px;">
                                <span style="color:#B7791F;">Diproses: $countDiproses</span> | 
                                <span style="color:#2B6CB0;">Dikirim: $countTerkirim</span> | 
                                <span style="color:#22543D;">Diterima: $countDiterima</span>
                            </p>
                        </div>
                    </div>

                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>No.</th>
                                <th>No Resi</th>
                                <th>Tanggal</th>
                                <th>Penerima & Alamat</th>
                                <th>Ekspedisi</th>
                                <th>Nama Barang</th>
                                <th>Harga Barang</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
            """.trimIndent()

            val sb = java.lang.StringBuilder(html)
            for ((index, r) in receipts.withIndex()) {
                val badgeClass = when(r.status) {
                    "DIPROSES" -> "badge-diproses"
                    "TERKIRIM" -> "badge-terkirim"
                    else -> "badge-diterima"
                }
                
                sb.append("<tr>")
                  .append("<td>${index + 1}</td>")
                  .append("<td><strong>${r.receiptNumber}</strong></td>")
                  .append("<td>${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(r.timestamp))}</td>")
                  .append("<td><strong>${r.recipientName}</strong><br><span style='color:#718096; font-size:8.5px;'>${r.recipientAddress}</span></td>")
                  .append("<td>${r.courierName}</td>")
                  .append("<td>${r.itemName}</td>")
                  .append("<td>Rp ${formatter.format(r.itemPrice)}</td>")
                  .append("<td><span class='badge $badgeClass'>${r.status}</span></td>")
                  .append("</tr>")
            }

            sb.append("""
                        </tbody>
                    </table>

                    <div class="footer">
                        Dokumen ini digenerate secara otomatis oleh sistem keamanan Resi Usaha.<br>
                        Semua rincian alamat terenkripsi end-to-end dalam database internal perangkat.
                    </div>
                </body>
                </html>
            """.trimIndent())

            // Render PDF via PrintManager and offscreen WebView
            try {
                val webView = WebView(context)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        try {
                            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                            val printAdapter = webView.createPrintDocumentAdapter("Laporan_Resi_Usaha_${System.currentTimeMillis()}")
                            
                            val jobName = "Laporan Resi Usaha PDF"
                            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            Toast.makeText(context, "Gagal menyiapkan printer: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                webView.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF-8", null)
            } catch (t: Throwable) {
                t.printStackTrace()
                // Graceful fallback for environments missing the system WebView package (e.g. emulators or simplified headless engines)
                Toast.makeText(context, "Layanan cetak WebView tidak tersedia di perangkat ini.", Toast.LENGTH_LONG).show()
            }

        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mencetak dokumen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
