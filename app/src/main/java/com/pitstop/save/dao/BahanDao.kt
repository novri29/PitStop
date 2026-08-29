package com.pitstop.save.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pitstop.save.entity.Bahan

@Dao
interface BahanDao {
    @Query("SELECT * FROM bahan ORDER BY nama ASC")
    fun getAllLive(): LiveData<List<Bahan>>

    @Query("SELECT * FROM bahan ORDER BY nama ASC")
    suspend fun getAll(): List<Bahan>

    @Query("SELECT * FROM bahan WHERE id = :id")
    suspend fun getById(id: Int): Bahan?

    @Insert
    suspend fun insert(bahan: Bahan): Long

    @Update
    suspend fun update(bahan: Bahan)

    @Delete
    suspend fun delete(bahan: Bahan)

    @Query("UPDATE bahan SET stock = stock - :jumlah WHERE id = :id")
    suspend fun kurangiStock(id: Int, jumlah: Double)

    /**
     * Restock dengan harga modal baru.
     *
     * Menggunakan metode weighted average:
     *
     * nilai stok lama  = stock lama × harga modal lama
     * nilai stok baru  = harga modal pembelian baru
     *
     * harga modal baru =
     * (nilai stok lama + nilai stok baru) / total stok
     */
    @Query("""
            UPDATE bahan
            SET
                stock = stock + :jumlah,
                hargaPerSatuan = (
                    (stock * hargaPerSatuan) + :hargaModalBaru
                ) / (stock + :jumlah),
                initialStock = stock + :jumlah
            WHERE id = :id
        """)
    suspend fun tambahStock(
        id: Int,
        jumlah: Double,
        hargaModalBaru: Double
    )

    /**
     * Pengembalian stok akibat REFUND transaksi (bukan restock asli).
     * Sengaja TIDAK menyentuh initialStock, supaya baseline "stok penuh" untuk
     * perhitungan ambang 30% tidak ikut ter-reset ke angka kecil pasca-refund.
     * (Ini simetris dengan kurangiStock yang juga tidak menyentuh initialStock.)
     */
    @Query("UPDATE bahan SET stock = stock + :jumlah WHERE id = :id")
    suspend fun kembalikanStock(id: Int, jumlah: Double)
}