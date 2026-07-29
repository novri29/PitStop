package com.pitstop.ui.admin

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.pitstop.save.entity.Bahan
import com.pitstop.save.entity.Transaksi
import com.pitstop.repository.AppRepository
import com.pitstop.save.dao.DetailLaporanRow
import com.pitstop.save.dao.ProdukTerlarisRow
import com.pitstop.util.PeriodeUtil
import com.pitstop.util.TipePeriode
import java.util.Calendar

/** Kombinasi filter unit usaha + rentang tanggal, dipakai sebagai trigger query. */
data class FilterRingkasan(val awal: Long, val akhir: Long, val tipeUnit: String)

/**
 * ViewModel ringkasan hari ini yang reaktif terhadap unit usaha yang dipilih (Cuci Motor/Cafe/Semua)
 * DAN periode waktu (Harian/Bulanan/Tahunan). Dipakai bersama oleh Dashboard Admin, Dashboard Kasir,
 * dan Laporan Penjualan.
 */
class RingkasanViewModel(private val repository: AppRepository) : ViewModel() {

    // ---------- Filter lama (dipakai Dashboard Kasir - selalu "hari ini") ----------
    val tipeTerpilih = MutableLiveData("SEMUA")

    val jumlahTransaksiHariIni = tipeTerpilih.switchMap { repository.getJumlahTransaksiHariIniLive(it) }
    val omzetHariIni = tipeTerpilih.switchMap { repository.getOmzetHariIniLive(it) }
    val produkTerjualHariIni = tipeTerpilih.switchMap { repository.getTotalProdukTerjualHariIniLive(it) }
    val stokBahanHabis = repository.getStokBahanHabisLive()
    val totalOmzetKeseluruhan = repository.getTotalOmzetLive()

    fun pilihUnit(tipe: String) {
        tipeTerpilih.value = tipe
    }

    // ---------- Filter periode baru: Harian / Bulanan / Tahunan (Dashboard Admin & Laporan) ----------
    val tipePeriode = MutableLiveData(TipePeriode.HARIAN)
    val kalenderAcuan = MutableLiveData(Calendar.getInstance())
    val unitPeriode = MutableLiveData("SEMUA")

    val labelPeriode = MediatorLiveData<String>().apply {
        fun update() {
            val tipe = tipePeriode.value ?: TipePeriode.HARIAN
            val kalender = kalenderAcuan.value ?: Calendar.getInstance()
            value = PeriodeUtil.label(tipe, kalender)
        }
        addSource(tipePeriode) { update() }
        addSource(kalenderAcuan) { update() }
    }

    private val filterGabungan = MediatorLiveData<FilterRingkasan>().apply {
        fun update() {
            val tipe = tipePeriode.value ?: TipePeriode.HARIAN
            val kalender = kalenderAcuan.value ?: Calendar.getInstance()
            val unit = unitPeriode.value ?: "SEMUA"
            val (awal, akhir) = PeriodeUtil.rentang(tipe, kalender)
            value = FilterRingkasan(awal, akhir, unit)
        }
        addSource(tipePeriode) { update() }
        addSource(kalenderAcuan) { update() }
        addSource(unitPeriode) { update() }
    }

    val jumlahTransaksiPeriode = filterGabungan.switchMap { f -> repository.getJumlahTransaksiPeriodeLive(f.awal, f.akhir, f.tipeUnit) }
    val omzetPeriode = filterGabungan.switchMap { f -> repository.getOmzetPeriodeLive(f.awal, f.akhir, f.tipeUnit) }
    val omzetNormalPeriode = filterGabungan.switchMap { f -> repository.getOmzetNormalPeriodeLive(f.awal, f.akhir, f.tipeUnit) }
    val omzetPromoPeriode = filterGabungan.switchMap { f -> repository.getOmzetPromoPeriodeLive(f.awal, f.akhir, f.tipeUnit) }
    val produkTerjualPeriode = filterGabungan.switchMap { f -> repository.getTotalProdukTerjualPeriodeLive(f.awal, f.akhir, f.tipeUnit) }
    val transaksiPeriode = filterGabungan.switchMap { f -> repository.getTransaksiPeriodeLive(f.awal, f.akhir, f.tipeUnit) }

    fun pilihPeriode(tipe: TipePeriode) {
        tipePeriode.value = tipe
        kalenderAcuan.value = Calendar.getInstance() // reset ke hari/bulan/tahun sekarang tiap ganti tipe
    }

    fun pilihUnitPeriode(tipe: String) {
        unitPeriode.value = tipe
    }

    fun geserPeriode(delta: Int) {
        val tipe = tipePeriode.value ?: TipePeriode.HARIAN
        val kalender = kalenderAcuan.value ?: Calendar.getInstance()
        kalenderAcuan.value = PeriodeUtil.geser(tipe, kalender, delta)
    }

    /** Dipakai saat export Excel: ambil data list sesuai filter periode yang sedang aktif. */
    suspend fun getTransaksiPeriodeSuspend(): List<Transaksi> {
        val tipe = tipePeriode.value ?: TipePeriode.HARIAN
        val kalender = kalenderAcuan.value ?: Calendar.getInstance()
        val unit = unitPeriode.value ?: "SEMUA"
        val (awal, akhir) = PeriodeUtil.rentang(tipe, kalender)
        return repository.getTransaksiPeriode(awal, akhir, unit)
    }

    // ---------- Umum ----------
    fun getBahanList() = repository.getBahanLive()
    fun getAllTransaksi() = repository.getAllTransaksiLive()

    suspend fun getSemuaBahan(): List<Bahan> = repository.getAllBahan()
    suspend fun getSemuaTransaksi(): List<Transaksi> = repository.getAllTransaksi()
    suspend fun getDetail(transaksiId: Int) = repository.getDetailForTransaksi(transaksiId)

    // ---------- Grafik & Produk Terlaris ----------
    suspend fun getOmzet7HariTerakhir(): List<Pair<String, Double>> = repository.getOmzetHarianTerakhir(7)

    /**
     * Grafik yang otomatis menyesuaikan mode periode yang sedang aktif:
     * - Harian  -> tren 7 hari terakhir (harian)
     * - Bulanan -> tren per minggu dalam bulan yang sedang dilihat
     * - Tahunan -> tren per bulan (Jan-Des) dalam tahun yang sedang dilihat
     */
    suspend fun getGrafikSesuaiPeriode(): List<Pair<String, Double>> {
        val tipe = tipePeriode.value ?: TipePeriode.HARIAN
        val kalender = kalenderAcuan.value ?: Calendar.getInstance()
        return when (tipe) {
            TipePeriode.HARIAN -> repository.getOmzetHarianTerakhir(7)
            TipePeriode.BULANAN -> repository.getOmzetMingguanDalamBulan(kalender)
            TipePeriode.TAHUNAN -> repository.getOmzetBulananDalamTahun(kalender)
        }
    }

    /** Judul grafik yang ikut berubah sesuai mode periode aktif. */
    fun getLabelGrafik(): String {
        return when (tipePeriode.value ?: TipePeriode.HARIAN) {
            TipePeriode.HARIAN -> "Tren Omzet 7 Hari Terakhir"
            TipePeriode.BULANAN -> "Tren Omzet per Minggu (${PeriodeUtil.label(TipePeriode.BULANAN, kalenderAcuan.value ?: Calendar.getInstance())})"
            TipePeriode.TAHUNAN -> "Tren Omzet per Bulan (Tahun ${PeriodeUtil.label(TipePeriode.TAHUNAN, kalenderAcuan.value ?: Calendar.getInstance())})"
        }
    }

    suspend fun getProdukTerlarisPeriode(limit: Int = 5): List<ProdukTerlarisRow> {
        val tipe = tipePeriode.value ?: TipePeriode.HARIAN
        val kalender = kalenderAcuan.value ?: Calendar.getInstance()
        val unit = unitPeriode.value ?: "SEMUA"
        val (awal, akhir) = PeriodeUtil.rentang(tipe, kalender)
        return repository.getProdukTerlaris(awal, akhir, unit, limit)
    }

    /** Produk terlaris untuk seluruh riwayat (dipakai saat mode "Semua" di Laporan). */
    suspend fun getProdukTerlarisSemua(limit: Int = 5): List<ProdukTerlarisRow> =
        repository.getProdukTerlaris(0L, Long.MAX_VALUE, "SEMUA", limit)

    // ---------- Export Laporan Lengkap (rekap semua produk + detail item, sesuai filter aktif) ----------
    suspend fun getProdukTerjualLengkapPeriode(): List<ProdukTerlarisRow> {
        val tipe = tipePeriode.value ?: TipePeriode.HARIAN
        val kalender = kalenderAcuan.value ?: Calendar.getInstance()
        val unit = unitPeriode.value ?: "SEMUA"
        val (awal, akhir) = PeriodeUtil.rentang(tipe, kalender)
        return repository.getProdukTerjualLengkap(awal, akhir, unit)
    }

    suspend fun getProdukTerjualLengkapSemua(): List<ProdukTerlarisRow> =
        repository.getProdukTerjualLengkap(0L, Long.MAX_VALUE, "SEMUA")

    suspend fun getDetailLaporanPeriode(): List<DetailLaporanRow> {
        val tipe = tipePeriode.value ?: TipePeriode.HARIAN
        val kalender = kalenderAcuan.value ?: Calendar.getInstance()
        val unit = unitPeriode.value ?: "SEMUA"
        val (awal, akhir) = PeriodeUtil.rentang(tipe, kalender)
        return repository.getDetailLaporanPeriode(awal, akhir, unit)
    }

    suspend fun getDetailLaporanSemua(): List<DetailLaporanRow> =
        repository.getDetailLaporanPeriode(0L, Long.MAX_VALUE, "SEMUA")

    /** Label periode saat ini, dipakai sebagai judul di file export. */
    fun getLabelPeriodeSaatIni(): String = labelPeriode.value ?: PeriodeUtil.label(
        tipePeriode.value ?: TipePeriode.HARIAN, kalenderAcuan.value ?: Calendar.getInstance()
    )
}