package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaksi_detail",
    foreignKeys = [
        ForeignKey(entity = Transaksi::class, parentColumns = ["id"], childColumns = ["transaksiId"])
    ]
)
data class TransaksiDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transaksiId: Int,
    val namaItem: String,
    val qty: Int,
    val hargaSatuan: Double,
    val subtotal: Double
)
