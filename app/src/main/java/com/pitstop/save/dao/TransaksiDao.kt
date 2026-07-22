package com.pitstop.save.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail

@Dao
interface TransaksiDao {
    @Insert
    suspend fun insertTransaksi(transaksi: Transaksi): Long

    @Insert
    suspend fun insertDetail(detail: TransaksiDetail)

    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    fun getAllLive(): LiveData<List<Transaksi>>

    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    suspend fun getAll(): List<Transaksi>

    @Query("SELECT * FROM transaksi WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Transaksi?

    @Query("SELECT * FROM transaksi WHERE tipe LIKE '%' || :tipe || '%' ORDER BY tanggal DESC")
    fun getByTipeLive(tipe: String): LiveData<List<Transaksi>>

    @Query("SELECT * FROM transaksi_detail WHERE transaksiId = :transaksiId")
    suspend fun getDetailForTransaksi(transaksiId: Int): List<TransaksiDetail>

    @Query("SELECT SUM(total) FROM transaksi")
    fun getTotalOmzetLive(): LiveData<Double?>

    // ---------- Ringkasan Hari Ini ----------
    // tipe = 'SEMUA' berarti tanpa filter unit usaha.
    // Filter pakai LIKE supaya transaksi campuran (mis. "Cuci Motor + Cafe") tetap terhitung
    // di masing-masing unit usaha yang tercakup di dalamnya.
    @Query("""
        SELECT COUNT(*) FROM transaksi
        WHERE tanggal BETWEEN :awal AND :akhir AND (:tipe = 'SEMUA' OR tipe LIKE '%' || :tipe || '%')
    """)
    fun getJumlahTransaksiHariIniLive(awal: Long, akhir: Long, tipe: String): LiveData<Int>

    @Query("""
        SELECT SUM(total) FROM transaksi
        WHERE tanggal BETWEEN :awal AND :akhir AND (:tipe = 'SEMUA' OR tipe LIKE '%' || :tipe || '%')
    """)
    fun getOmzetHariIniLive(awal: Long, akhir: Long, tipe: String): LiveData<Double?>

    @Query("""
        SELECT SUM(d.qty) FROM transaksi_detail d
        INNER JOIN transaksi t ON t.id = d.transaksiId
        WHERE t.tanggal BETWEEN :awal AND :akhir AND (:tipe = 'SEMUA' OR t.tipe LIKE '%' || :tipe || '%')
    """)
    fun getTotalProdukTerjualHariIniLive(awal: Long, akhir: Long, tipe: String): LiveData<Int?>

    @Query("SELECT COUNT(*) FROM bahan WHERE stock <= 0")
    fun getStokBahanHabisLive(): LiveData<Int>
}
