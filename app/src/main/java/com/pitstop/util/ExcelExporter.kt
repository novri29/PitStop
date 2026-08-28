package com.pitstop.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pitstop.save.dao.DetailLaporanRow
import com.pitstop.save.dao.ProdukTerlarisRow
import com.pitstop.save.entity.STATUS_REFUND
import com.pitstop.save.entity.Transaksi
import java.io.File
import java.io.FileWriter

/**
 * Export laporan penjualan ke file CSV (dibuka otomatis oleh Excel / Google Sheets) lengkap
 * dengan ringkasan, rekap produk terjual (beserta bar grafik ASCII di dalam CSV-nya), dan
 * detail per item transaksi.
 *
 * Catatan: format .csv dipilih (bukan .xlsx dengan Apache POI dsb) supaya project tetap ringan
 * tanpa dependency tambahan, dan tetap 100% kompatibel dibuka di Microsoft Excel / Google Sheets.
 */
object ExcelExporter {

    /**
     * Export laporan lengkap ke CSV: ringkasan angka, rekap SEMUA produk yang terjual (qty,
     * omzet, kontribasi %, dan bar grafik sederhana), serta detail tiap item di tiap transaksi.
     */
    fun exportLaporanLengkap(
        context: Context,
        judulPeriode: String,
        transaksiList: List<Transaksi>,
        produkTerjual: List<ProdukTerlarisRow>,
        detailItem: List<DetailLaporanRow>
    ): File {
        val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.csv"
        val dir = context.getExternalFilesDir(null)
        val file = File(dir, fileName)

        // Transaksi yang sudah direfund TIDAK boleh ikut masuk ke laporan Excel (baik ke
        // perhitungan total maupun ke daftar "RINGKASAN TRANSAKSI"), sama seperti produk
        // terjual & detail item yang query-nya sudah otomatis exclude status Refund.
        // transaksiList yang diterima di sini bisa saja masih mengandung transaksi Refund
        // (dipakai juga oleh fitur backup struk PDF yang sengaja menyertakan semua transaksi
        // termasuk yang direfund untuk keperluan arsip), jadi filter dilakukan di sini saja.
        val transaksiAktif = transaksiList.filterNot { it.status == STATUS_REFUND }

        val totalOmzet = transaksiAktif.sumOf { it.total }
        val totalModal = detailItem.sumOf { it.hargaModal * it.qty }
        val totalLaba = totalOmzet - totalModal
        val totalTransaksi = transaksiAktif.size
        val rataRata = if (totalTransaksi > 0) totalOmzet / totalTransaksi else 0.0
        val maxQty = produkTerjual.maxOfOrNull { it.totalQty } ?: 0

        fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

        FileWriter(file).use { writer ->
            writer.append("PITSTOP - LAPORAN PENJUALAN\n")
            writer.append("Periode,${csv(judulPeriode)}\n")
            writer.append("Digenerate,${csv(Formatter.tanggalWaktu(System.currentTimeMillis()))}\n")
            writer.append("\n")

            writer.append("=== RINGKASAN ===\n")
            writer.append("Total Transaksi,$totalTransaksi\n")
            writer.append("Total Omzet,$totalOmzet\n")
            writer.append("Total Modal (HPP),$totalModal\n")
            writer.append("Laba Bersih,$totalLaba\n")
            writer.append("Rata-rata per Transaksi,${"%.0f".format(rataRata)}\n")
            writer.append("\n")

            writer.append("=== PRODUK TERJUAL ===\n")
            writer.append("No,Nama Produk,Qty Terjual,Total Omzet,Total Modal,Laba Bersih,Kontribusi (%),Grafik\n")
            if (produkTerjual.isEmpty()) {
                writer.append(",${csv("Belum ada produk terjual")},,,,,,\n")
            } else {
                produkTerjual.forEachIndexed { index, row ->
                    val kontribusi = if (totalOmzet > 0) (row.totalOmzet / totalOmzet) * 100 else 0.0
                    val barLen = if (maxQty > 0) {
                        ((row.totalQty.toDouble() / maxQty) * 20).toInt().coerceAtLeast(1)
                    } else 0
                    val bar = "\u2588".repeat(barLen)
                    writer.append("${index + 1},")
                    writer.append("${csv(row.namaItem)},")
                    writer.append("${row.totalQty},")
                    writer.append("${row.totalOmzet},")
                    writer.append("${row.totalModal},")
                    writer.append("${row.totalOmzet - row.totalModal},")
                    writer.append("${"%.1f".format(kontribusi)}%,")
                    writer.append("${csv(bar)}\n")
                }
            }
            writer.append("\n")

            writer.append("=== DETAIL TRANSAKSI PER ITEM ===\n")
            writer.append("No,Tanggal,Tipe Transaksi,Kasir,Item,Qty,Harga Satuan,Subtotal,Modal,Laba Bersih,Promo\n")
            if (detailItem.isEmpty()) {
                writer.append(",${csv("Belum ada transaksi")},,,,,,,,,\n")
            } else {
                detailItem.forEachIndexed { index, row ->
                    val modalBaris = row.hargaModal * row.qty
                    writer.append("${index + 1},")
                    writer.append("${csv(Formatter.tanggalWaktu(row.tanggal))},")
                    writer.append("${csv(row.tipe)},")
                    writer.append("${csv(row.kasirUsername)},")
                    writer.append("${csv(row.namaItem)},")
                    writer.append("${row.qty},")
                    writer.append("${row.hargaSatuan},")
                    writer.append("${row.subtotal},")
                    writer.append("${modalBaris},")
                    writer.append("${row.subtotal - modalBaris},")
                    writer.append("${if (row.isPromo) "Ya" else "Tidak"}\n")
                }
            }
            writer.append("\n")

            writer.append("=== RINGKASAN TRANSAKSI ===\n")
            writer.append("No,Tanggal,Tipe Transaksi,Kasir,Total\n")
            transaksiAktif.forEachIndexed { index, t ->
                writer.append("${index + 1},")
                writer.append("${csv(Formatter.tanggalWaktu(t.tanggal))},")
                writer.append("${csv(t.tipe)},")
                writer.append("${csv(t.kasirUsername)},")
                writer.append("${t.total}\n")
            }
            writer.append(",,,Total Omzet,$totalOmzet\n")
            writer.append(",,,Total Modal (HPP),$totalModal\n")
            writer.append(",,,Laba Bersih,$totalLaba\n")
        }
        return file
    }

    /** Bagikan satu file (CSV laporan). */
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