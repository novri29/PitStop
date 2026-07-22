package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Relasi menu kopi <-> bahan yang dipakai beserta jumlah pemakaian (gram/ml).
 */
@Entity(
    tableName = "menu_kopi_bahan",
    foreignKeys = [
        ForeignKey(entity = MenuKopi::class, parentColumns = ["id"], childColumns = ["menuKopiId"]),
        ForeignKey(entity = Bahan::class, parentColumns = ["id"], childColumns = ["bahanId"])
    ]
)
data class MenuKopiBahan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val menuKopiId: Int,
    val bahanId: Int,
    val jumlahDigunakan: Double // dalam gram/ml
)
