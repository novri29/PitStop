package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stock barang/bahan untuk layanan steam (contoh: shampo mobil, shampo motor, semir ban dll)
 */
@Entity(tableName = "stock_steam")
data class StockSteam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,       // contoh: "Shampo Motor"
    val jenis: String,      // "Motor" atau "Mobil"
    val satuan: String,     // "ml" / "botol" / "pcs"
    var stock: Double
)
