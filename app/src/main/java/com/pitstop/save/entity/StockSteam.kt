package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stock barang/bahan untuk layanan steam (contoh: shampo motor, semir ban dll)
 */
@Entity(tableName = "stock_steam")
data class StockSteam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,       // contoh: "Shampo Motor"
    val jenis: String,      // "Motor"
    val satuan: String,     // "ml" / "pcs"
    var stock: Double,
    val hargaPerSatuan: Double = 0.0 // harga modal per satuan (ml/pcs)
)

/** Satuan stock barang steam dibatasi hanya "ml" dan "pcs" supaya konsisten. */
val DAFTAR_SATUAN_STOCK_STEAM = listOf("ml", "pcs")
