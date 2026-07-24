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

    // ---------- Laporan per Periode (Harian/Bulanan/Tahunan) ----------
    @Query("""
        SELECT * FROM transaksi
        WHERE tanggal BETWEEN :awal AND :akhir AND (:tipe = 'SEMUA' OR tipe LIKE '%' || :tipe || '%')
        ORDER BY tanggal DESC
    """)
    fun getTransaksiPeriodeLive(awal: Long, akhir: Long, tipe: String): LiveData<List<Transaksi>>

    @Query("""
        SELECT * FROM transaksi
        WHERE tanggal BETWEEN :awal AND :akhir AND (:tipe = 'SEMUA' OR tipe LIKE '%' || :tipe || '%')
        ORDER BY tanggal DESC
    """)
    suspend fun getTransaksiPeriode(awal: Long, akhir: Long, tipe: String): List<Transaksi>

    // ---------- Grafik: Omzet harian dalam rentang tanggal ----------
    @Query("""
        SELECT strftime('%Y-%m-%d', tanggal / 1000, 'unixepoch', 'localtime') as tanggalStr,
               SUM(total) as totalOmzet
        FROM transaksi
        WHERE tanggal BETWEEN :awal AND :akhir
        GROUP BY tanggalStr
        ORDER BY tanggalStr ASC
    """)
    suspend fun getOmzetHarianRaw(awal: Long, akhir: Long): List<OmzetHarianRow>

    // ---------- Grafik: Omzet bulanan dalam rentang tanggal (untuk mode Tahunan) ----------
    @Query("""
        SELECT strftime('%Y-%m', tanggal / 1000, 'unixepoch', 'localtime') as tanggalStr,
               SUM(total) as totalOmzet
        FROM transaksi
        WHERE tanggal BETWEEN :awal AND :akhir
        GROUP BY tanggalStr
        ORDER BY tanggalStr ASC
    """)
    suspend fun getOmzetBulananRaw(awal: Long, akhir: Long): List<OmzetHarianRow>

    // ---------- Produk Terlaris dalam rentang tanggal + filter unit usaha ----------
    @Query("""
        SELECT d.namaItem as namaItem, SUM(d.qty) as totalQty, SUM(d.subtotal) as totalOmzet
        FROM transaksi_detail d
        INNER JOIN transaksi t ON t.id = d.transaksiId
        WHERE t.tanggal BETWEEN :awal AND :akhir AND (:tipe = 'SEMUA' OR t.tipe LIKE '%' || :tipe || '%')
        GROUP BY d.namaItem
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    suspend fun getProdukTerlaris(awal: Long, akhir: Long, tipe: String, limit: Int): List<ProdukTerlarisRow>
}

data class OmzetHarianRow(
    val tanggalStr: String,
    val totalOmzet: Double
)

data class ProdukTerlarisRow(
    val namaItem: String,
    val totalQty: Int,
    val totalOmzet: Double
)
