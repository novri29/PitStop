package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val JENIS_MOTOR = "Motor"
const val JENIS_MOBIL = "Mobil"

/**
 * Harga layanan cuci/steam yang ditentukan Admin (dipakai Kasir saat membuat transaksi).
 */
@Entity(tableName = "layanan")
data class Layanan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,   // contoh: "Cuci Motor", "Cuci Mobil"
    val jenis: String,  // Motor / Mobil
    val harga: Double
)
