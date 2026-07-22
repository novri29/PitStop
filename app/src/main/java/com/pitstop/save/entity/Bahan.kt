package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stock bahan pembuatan kopi (mis: kopi bubuk, gula, susu).
 * stock disimpan dalam satuan gram/ml, hargaPerSatuan = harga modal per gram/ml.
 */
@Entity(tableName = "bahan")
data class Bahan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val satuan: String,       // "gram" atau "ml"
    var stock: Double,        // sisa stock saat ini
    val hargaPerSatuan: Double // harga modal per gram/ml
)
