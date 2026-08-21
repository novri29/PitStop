package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val TIPE_MOTOR = "Cuci Motor"
const val TIPE_CAFE = "Cafe"

const val METODE_CASH = "Cash"
const val METODE_QRIS = "QRIS"
const val METODE_TRANSFER = "Transfer"

const val STATUS_SELESAI = "Selesai"
const val STATUS_REFUND = "Refund"

/**
 * Header transaksi. Transaksi langsung dianggap final saat dibuat kasir (tercatat di
 * Riwayat Kasir & otomatis ikut Laporan/Ringkasan Admin), sesuai alur Pembayaran -> Struk.
 *
 * Transaksi bisa diubah statusnya menjadi REFUND (mis. pelanggan tidak jadi melakukan pesanan).
 * Transaksi yang berstatus REFUND otomatis dikeluarkan dari perhitungan omzet/laporan,
 * dan stock bahan/steam yang terpakai akan dikembalikan otomatis.
 */
@Entity(tableName = "transaksi")
data class Transaksi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val tipe: String,       // Cuci Motor / Cafe (atau gabungan, mis. "Cuci Motor + Cafe")
    val total: Double,
    val kasirUsername: String,
    val catatan: String = "",
    val metodePembayaran: String = METODE_CASH,
    val jumlahDibayar: Double = 0.0,
    val kembalian: Double = 0.0,
    val platNomor: String = "",   // nomor plat kendaraan (khusus transaksi yang mengandung Cuci Motor)
    val status: String = STATUS_SELESAI,      // Selesai / Refund
    val alasanRefund: String = "",            // keterangan alasan refund (wajib diisi saat refund)
    val waktuRefund: Long? = null,
    val direfundOleh: String? = null
)