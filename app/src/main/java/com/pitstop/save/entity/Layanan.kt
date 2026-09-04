package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Dipakai sebagai `jenis` untuk StockSteam (barang habis pakai steam masih 1 kategori: Motor) */
const val JENIS_MOTOR = "Motor"

/** 6 ukuran layanan Cuci Motor (reguler + premium). Dipakai sebagai key unik pada tabel `layanan`. */
const val UKURAN_MOTOR_KECIL = "Motor Kecil"
const val UKURAN_MOTOR_SEDANG = "Motor Sedang"
const val UKURAN_MOTOR_BESAR = "Motor Besar"
const val UKURAN_PREMIUM_KECIL = "Premium Kecil"
const val UKURAN_PREMIUM_SEDANG = "Premium Sedang"
const val UKURAN_PREMIUM_BESAR = "Premium Besar"

/** Urutan tampil standar untuk 6 ukuran di atas. */
val DAFTAR_UKURAN_MOTOR = listOf(
    UKURAN_MOTOR_KECIL, UKURAN_MOTOR_SEDANG, UKURAN_MOTOR_BESAR,
    UKURAN_PREMIUM_KECIL, UKURAN_PREMIUM_SEDANG, UKURAN_PREMIUM_BESAR
)

/**
 * Harga layanan Cuci Motor per ukuran, ditentukan Admin (dipakai Kasir saat membuat transaksi).
 * Selalu ada tepat 1 baris untuk masing-masing ukuran (Motor Kecil/Sedang/Besar dan
 * Premium Kecil/Sedang/Besar).
 *
 * hargaModal = "Harga Dasar" a.k.a HPP (Harga Pokok Penjualan), DIHITUNG OTOMATIS =
 *              total pemakaian StockSteam (bahan) + upahKaryawan + biayaListrik.
 * harga      = "Harga Jual" ke pelanggan, ditentukan Admin (boleh sama dengan hargaModal
 *              atau ditambah margin keuntungan).
 */
@Entity(tableName = "layanan")
data class Layanan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,    // contoh: "Cuci Motor Kecil"
    val ukuran: String,  // Motor Kecil / Motor Sedang / Motor Besar / Premium Kecil / Premium Sedang / Premium Besar
    val harga: Double,   // Harga Jual ke pelanggan
    val upahKaryawan: Double = 0.0, // upah/jasa karyawan untuk 1x pengerjaan, beda2 per ukuran motor
    val biayaListrik: Double = 0.0, // biaya listrik (pompa air/vacuum) untuk 1x pencucian, beda2 per ukuran motor
    val hargaModal: Double = 0.0 // "Harga Dasar" / HPP = total pemakaian StockSteam (bahan) + upahKaryawan + biayaListrik, dihitung otomatis
)