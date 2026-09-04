package com.pitstop.save.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.LayananBahan
import com.pitstop.save.entity.StockSteam

@Dao
interface StockSteamDao {
    @Query("SELECT * FROM stock_steam ORDER BY jenis, nama ASC")
    fun getAllLive(): LiveData<List<StockSteam>>

    @Query("SELECT * FROM stock_steam ORDER BY jenis, nama ASC")
    suspend fun getAll(): List<StockSteam>

    @Insert
    suspend fun insert(item: StockSteam): Long

    @Update
    suspend fun update(item: StockSteam)

    @Delete
    suspend fun delete(item: StockSteam)

    @Query("DELETE FROM layanan_bahan WHERE stockSteamId = :stockSteamId")
    suspend fun deleteBahanUsageForStock(stockSteamId: Int)

    @Transaction
    suspend fun deleteStockSteamWithUsage(item: StockSteam) {
        deleteBahanUsageForStock(item.id)
        delete(item)
    }

    @Query("UPDATE stock_steam SET stock = stock - :jumlah WHERE id = :id")
    suspend fun kurangiStock(id: Int, jumlah: Double)

    /**
     * Restock ASLI (dipakai kalau suatu saat ada menu "tambah stok item yang sudah ada").
     * initialStock ikut di-reset karena ini titik "stok penuh" yang baru.
     */
    @Query("""
        UPDATE stock_steam
        SET
            stock = stock + :jumlah,
            hargaPerSatuan =
                (
                    (stock * hargaPerSatuan) + :hargaModalBaru
                )
                /
                (stock + :jumlah),
            initialStock = initialStock + :jumlah
        WHERE id = :id""")
    suspend fun tambahStock(
        id: Int,
        jumlah: Double,
        hargaModalBaru: Double
    )

    /**
     * Pengembalian stok akibat REFUND transaksi (bukan restock asli).
     * Sengaja TIDAK menyentuh initialStock, sama seperti BahanDao.kembalikanStock.
     */
    @Query("UPDATE stock_steam SET stock = stock + :jumlah WHERE id = :id")
    suspend fun kembalikanStock(id: Int, jumlah: Double)

    // ---------- Layanan (harga per ukuran motor) ----------
    @Query("SELECT * FROM layanan ORDER BY ukuran ASC")
    fun getAllLayananLive(): LiveData<List<Layanan>>

    @Query("SELECT * FROM layanan ORDER BY ukuran ASC")
    suspend fun getAllLayanan(): List<Layanan>

    @Query("SELECT * FROM layanan WHERE ukuran = :ukuran LIMIT 1")
    suspend fun getLayananByUkuran(ukuran: String): Layanan?

    @Query("SELECT * FROM layanan WHERE id = :id LIMIT 1")
    suspend fun getLayananById(id: Int): Layanan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayanan(layanan: Layanan): Long

    @Update
    suspend fun updateLayanan(layanan: Layanan)

    @Query("""
    UPDATE layanan
    SET nama = :nama, harga = :harga
    WHERE ukuran = :ukuran
""")
    suspend fun updateHargaByUkuran(
        nama: String,
        ukuran: String,
        harga: Double
    ): Int

    @Query("UPDATE layanan SET hargaModal = :hargaModal WHERE id = :id")
    suspend fun updateHargaModalLayanan(id: Int, hargaModal: Double)

    @Query("UPDATE layanan SET upahKaryawan = :upah WHERE id = :id")
    suspend fun updateUpahKaryawan(id: Int, upah: Double)

    @Query("UPDATE layanan SET biayaListrik = :biayaListrik WHERE id = :id")
    suspend fun updateBiayaListrik(id: Int, biayaListrik: Double)

    /** Total biaya bahan (komposisi StockSteam) saja untuk 1 layanan, TANPA upah karyawan. */
    @Query("""
        SELECT COALESCE(SUM(lb.jumlahDigunakan * s.hargaPerSatuan), 0)
        FROM layanan_bahan lb INNER JOIN stock_steam s ON s.id = lb.stockSteamId
        WHERE lb.layananId = :layananId
    """)
    suspend fun getTotalBiayaBahanLayanan(layananId: Int): Double

    // ---------- Komposisi bahan per layanan (mirip menu_kopi_bahan di Cafe) ----------
    @Insert
    suspend fun insertLayananBahan(usage: LayananBahan)

    @Query("SELECT * FROM layanan_bahan WHERE layananId = :layananId")
    suspend fun getBahanUsageForLayanan(layananId: Int): List<LayananBahan>

    @Query("DELETE FROM layanan_bahan WHERE layananId = :layananId")
    suspend fun deleteBahanUsageForLayanan(layananId: Int)

    @Query("""
        SELECT lb.layananId as layananId, s.nama as namaBahan, lb.jumlahDigunakan as jumlah, s.satuan as satuan
        FROM layanan_bahan lb INNER JOIN stock_steam s ON s.id = lb.stockSteamId
    """)
    suspend fun getKomposisiSteamRaw(): List<KomposisiSteamRow>

}

data class KomposisiSteamRow(
    val layananId: Int,
    val namaBahan: String,
    val jumlah: Double,
    val satuan: String
)