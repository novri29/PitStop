package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Relasi layanan steam (ukuran motor) <-> barang stock steam yang dipakai beserta jumlah
 * pemakaian per 1x transaksi (ml/pcs/dll). Sama pola dengan MenuKopiBahan di sisi Cafe:
 * dipakai untuk memotong stock otomatis saat layanan tersebut terjual.
 */
@Entity(
    tableName = "layanan_bahan",
    foreignKeys = [
        ForeignKey(entity = Layanan::class, parentColumns = ["id"], childColumns = ["layananId"]),
        ForeignKey(entity = StockSteam::class, parentColumns = ["id"], childColumns = ["stockSteamId"])
    ]
)
data class LayananBahan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val layananId: Int,
    val stockSteamId: Int,
    val jumlahDigunakan: Double // dalam ml/pcs sesuai satuan StockSteam terkait
)
