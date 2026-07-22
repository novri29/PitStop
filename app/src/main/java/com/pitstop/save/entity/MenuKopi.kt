package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jenis minuman kopi yang dijual di cafe.
 * hargaModal dihitung otomatis dari total pemakaian bahan.
 * hargaJual diisi/diatur admin (boleh sama dengan modal atau + margin).
 */
const val KATEGORI_COFFEE = "Coffee"
const val KATEGORI_NON_COFFEE = "Non Coffee"
const val KATEGORI_SNACK = "Snack"

@Entity(tableName = "menu_kopi")
data class MenuKopi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val kategori: String = KATEGORI_COFFEE,
    val hargaModal: Double,
    val hargaJual: Double
)
