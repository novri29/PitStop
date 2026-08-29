package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Dipakai sebagai `jenis` untuk StockSteam (barang habis pakai steam masih 1 kategori: Motor) */
const val JENIS_MOTOR = "Motor"

/** 3 ukuran layanan Cuci Motor. Dipakai sebagai key unik pada tabel `layanan`. */
const val UKURAN_MOTOR_KECIL = "Motor Kecil"
const val UKURAN_MOTOR_SEDANG = "Motor Sedang"
const val UKURAN_MOTOR_BESAR = "Motor Besar"

/** Urutan tampil standar untuk 3 ukuran di atas. */
val DAFTAR_UKURAN_MOTOR = listOf(UKURAN_MOTOR_KECIL, UKURAN_MOTOR_SEDANG, UKURAN_MOTOR_BESAR)

/**
 * Harga layanan Cuci Motor per ukuran, ditentukan Admin (dipakai Kasir saat membuat transaksi).
 * Selalu ada tepat 1 baris untuk masing-masing ukuran (Kecil/Sedang/Besar).
 */
@Entity(tableName = "layanan")
data class Layanan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,    // contoh: "Cuci Motor Kecil"
    val ukuran: String,  // Motor Kecil / Motor Sedang / Motor Besar
    val harga: Double,
    val upahKaryawan: Double = 0.0, // upah/jasa karyawan untuk 1x pengerjaan, beda2 per ukuran motor
    val hargaModal: Double = 0.0 // HPP = total pemakaian StockSteam (bahan) + upahKaryawan, dihitung otomatis
)