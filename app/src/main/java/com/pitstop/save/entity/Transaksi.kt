package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val TIPE_MOTOR = "Cuci Motor"
const val TIPE_CAFE = "Cafe"

const val METODE_CASH = "Cash"
const val METODE_QRIS = "QRIS"
const val METODE_TRANSFER = "Transfer"

/**
 * Header transaksi. Transaksi langsung dianggap final saat dibuat kasir (tercatat di
 * Riwayat Kasir & otomatis ikut Laporan/Ringkasan Admin), sesuai alur Pembayaran -> Struk.
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
    val platNomor: String = ""   // nomor plat kendaraan (khusus transaksi yang mengandung Cuci Motor)
)
