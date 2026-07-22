package com.pitstop.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pitstop.save.entity.Transaksi
import java.io.File
import java.io.FileWriter

/**
 * Export laporan penjualan ke file CSV (dibuka otomatis oleh Excel / Google Sheets).
 * Catatan: format .csv dipilih karena tidak butuh library tambahan (Apache POI dsb)
 * sehingga project tetap ringan, dan tetap 100% kompatibel dibuka di Microsoft Excel.
 */
object ExcelExporter {

    fun exportLaporan(context: Context, list: List<Transaksi>): File {
        val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.csv"
        val dir = context.getExternalFilesDir(null)
        val file = File(dir, fileName)

        FileWriter(file).use { writer ->
            writer.append("No,Tanggal,Tipe Transaksi,Kasir,Total\n")
            list.forEachIndexed { index, t ->
                writer.append("${index + 1},")
                writer.append("${Formatter.tanggalWaktu(t.tanggal)},")
                writer.append("${t.tipe},")
                writer.append("${t.kasirUsername},")
                writer.append("${t.total}\n")
            }
            val totalOmzet = list.sumOf { it.total }
            writer.append(",,,Total Omzet,$totalOmzet\n")
        }
        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan / Buka Laporan"))
    }
}
