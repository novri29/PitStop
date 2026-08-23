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
    val subtotal: Double,
    val isPromo: Boolean = false,
    val menuKopiId: Int? = null,   // referensi produk cafe (dipakai untuk kembalikan stock saat refund)
    val layananId: Int? = null,    // referensi layanan steam (dipakai untuk kembalikan stock saat refund)
    val hargaModal: Double = 0.0   // snapshot harga modal PER UNIT saat transaksi terjadi (supaya laba historis tetap akurat walau modal berubah belakangan)
)