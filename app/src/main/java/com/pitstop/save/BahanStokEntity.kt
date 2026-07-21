package com.pitstop.save

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu baris stok bahan (kopi, gula, susu, dst).
 * `jumlah` selalu disimpan dalam satuan dasar (GRAM untuk bahan berat, ML untuk bahan cair)
 * supaya perhitungan/laporan nanti tidak perlu konversi berulang-ulang.
 */
@Entity(tableName = "bahan_stok")
data class BahanStokEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val jumlah: Double,        // jumlah dalam satuan dasar
    val satuanDasar: String,   // "GRAM" atau "ML"
    val hargaModal: Long,      // total harga modal untuk jumlah di atas
    val tanggalDitambahkan: Long = System.currentTimeMillis()
)
