package com.pitstop.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pitstop.save.AppDatabase
import com.pitstop.save.dao.DetailLaporanRow
import com.pitstop.save.dao.ProdukTerlarisRow
import com.pitstop.save.entity.Bahan
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.LayananBahan
import com.pitstop.save.entity.METODE_CASH
import com.pitstop.save.entity.MenuKopi
import com.pitstop.save.entity.MenuKopiBahan
import com.pitstop.save.entity.StockSteam
import com.pitstop.save.entity.STATUS_REFUND
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.save.entity.User
import java.util.Calendar
import com.pitstop.util.NotificationHelper
import kotlin.text.get
import kotlin.text.insert
import kotlin.text.toInt
import kotlin.times
import kotlin.toString

/**
 * Satu pintu akses data untuk seluruh aplikasi (Admin & Kasir).
 * Berisi juga logika bisnis inti: hitung harga modal menu kopi & potong stock.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val userDao = db.userDao()
    private val bahanDao = db.bahanDao()
    private val menuKopiDao = db.menuKopiDao()
    private val stockSteamDao = db.stockSteamDao()
    private val transaksiDao = db.transaksiDao()

    // ---------- User / Login ----------
    suspend fun login(username: String, password: String): User? = userDao.login(username, password)

    // ---------- Bahan (Stock Cafe) ----------
    fun getBahanLive(): LiveData<List<Bahan>> = bahanDao.getAllLive()
    suspend fun getAllBahan(): List<Bahan> = bahanDao.getAll()
    suspend fun insertBahan(bahan: Bahan) = bahanDao.insert(bahan)
    suspend fun updateBahan(bahan: Bahan) = bahanDao.update(bahan)
    suspend fun deleteBahan(bahan: Bahan) = bahanDao.delete(bahan)

    // ---------- Menu Kopi (Stock Cafe) ----------
    fun getMenuKopiLive(): LiveData<List<MenuKopi>> = menuKopiDao.getAllLive()
    suspend fun getAllMenuKopi(): List<MenuKopi> = menuKopiDao.getAll()

    /**
     * Simpan menu kopi baru beserta daftar pemakaian bahan.
     * Harga modal dihitung otomatis: SUM(jumlahDigunakan * hargaPerSatuan bahan).
     */
    suspend fun simpanMenuKopi(
        nama: String,
        kategori: String,
        hargaJual: Double,
        pemakaian: List<Pair<Bahan, Double>>,
        hargaPromo: Double? = null,
        gambarPath: String? = null
    ): Long {
        val hargaModal = pemakaian.sumOf { (bahan, jumlah) -> jumlah * bahan.hargaPerSatuan }
        val menuId = menuKopiDao.insertMenu(
            MenuKopi(
                nama = nama,
                kategori = kategori,
                hargaModal = hargaModal,
                hargaJual = hargaJual,
                hargaPromo = hargaPromo,
                gambarPath = gambarPath
            )
        )
        pemakaian.forEach { (bahan, jumlah) ->
            menuKopiDao.insertBahanUsage(
                MenuKopiBahan(menuKopiId = menuId.toInt(), bahanId = bahan.id, jumlahDigunakan = jumlah)
            )
        }
        return menuId
    }

    suspend fun getKetersediaanMap(): Map<Int, Boolean> {
        val allUsage = menuKopiDao.getAllUsage()
        val stockMap = bahanDao.getAll().associate { it.id to it.stock }
        return allUsage.groupBy { it.menuKopiId }
            .mapValues { (_, list) -> list.all { (stockMap[it.bahanId] ?: 0.0) >= it.jumlahDigunakan } }
    }

    suspend fun updateMenuKopi(menuKopi: MenuKopi) = menuKopiDao.updateMenu(menuKopi)

    /**
     * Update menu kopi sekaligus mengganti seluruh komposisi bahan yang dipakai.
     * Dipakai saat admin mengubah kombinasi bahan dari halaman Kelola Produk (Minuman).
     * Data pemakaian lama dihapus lalu diganti dengan data baru, dan hargaModal (HPP)
     * dihitung ulang otomatis dari komposisi barunya.
     */
    suspend fun updateMenuKopiDenganResep(
        menuKopi: MenuKopi,
        pemakaian: List<Pair<Bahan, Double>>
    ) {
        val hargaModal = pemakaian.sumOf { (bahan, jumlah) -> jumlah * bahan.hargaPerSatuan }
        menuKopiDao.updateMenu(menuKopi.copy(hargaModal = hargaModal))
        menuKopiDao.deleteBahanUsageForMenu(menuKopi.id)
        pemakaian.forEach { (bahan, jumlah) ->
            menuKopiDao.insertBahanUsage(
                MenuKopiBahan(menuKopiId = menuKopi.id, bahanId = bahan.id, jumlahDigunakan = jumlah)
            )
        }
    }

    suspend fun hapusMenuKopi(menuKopi: MenuKopi) {
        menuKopiDao.deleteBahanUsageForMenu(menuKopi.id)
        menuKopiDao.deleteMenu(menuKopi)
    }

    suspend fun getBahanUsageForMenu(menuId: Int): List<MenuKopiBahan> = menuKopiDao.getBahanUsageForMenu(menuId)

    /** Peta menuId -> teks ringkasan komposisi, contoh: "Kopi 10gr, Gula 5gr, Susu 100ml" */
    suspend fun getKomposisiMap(): Map<Int, String> {
        val rows = menuKopiDao.getKomposisiRaw()
        return rows.groupBy { it.menuKopiId }
            .mapValues { (_, list) ->
                list.joinToString(", ") { row ->
                    val jumlahText = if (row.jumlah == row.jumlah.toInt().toDouble()) row.jumlah.toInt().toString() else row.jumlah.toString()
                    "${row.namaBahan} $jumlahText${row.satuan}"
                }
            }
    }

    /** Dipanggil saat menu kopi terjual: mengurangi stock tiap bahan sesuai qty terjual */
    suspend fun potongStockUntukMenu(menuId: Int, qty: Int) {
        val usageList = menuKopiDao.getBahanUsageForMenu(menuId)
        usageList.forEach { usage ->
            bahanDao.kurangiStock(usage.bahanId, usage.jumlahDigunakan * qty)
        }
    }

    // ---------- Stock Steam ----------
    fun getStockSteamLive(): LiveData<List<StockSteam>> = stockSteamDao.getAllLive()
    suspend fun getAllStockSteam(): List<StockSteam> = stockSteamDao.getAll()
    suspend fun insertStockSteam(item: StockSteam) = stockSteamDao.insert(item)
    suspend fun updateStockSteam(item: StockSteam) = stockSteamDao.update(item)
    suspend fun deleteStockSteam(item: StockSteam) = stockSteamDao.delete(item)

    fun getLayananLive(): LiveData<List<Layanan>> = stockSteamDao.getAllLayananLive()
    suspend fun getAllLayanan(): List<Layanan> = stockSteamDao.getAllLayanan()

    /**
     * Simpan harga layanan berdasarkan ukuran (Motor Kecil/Sedang/Besar).
     * Kalau baris untuk ukuran ini sudah ada, pakai id yang sama supaya harga di-UPDATE
     * (menimpa baris lama), bukan malah nambah baris baru dengan harga baru.
     */
    suspend fun simpanLayanan(layanan: Layanan) {
        val existing = stockSteamDao.getLayananByUkuran(layanan.ukuran)
        val toSave = if (existing != null) layanan.copy(id = existing.id) else layanan
        stockSteamDao.insertLayanan(toSave)
    }

    suspend fun getLayananById(id: Int): Layanan? = stockSteamDao.getLayananById(id)

    suspend fun getBahanUsageForLayanan(layananId: Int): List<LayananBahan> =
        stockSteamDao.getBahanUsageForLayanan(layananId)

    /** Peta layananId -> teks ringkasan komposisi, contoh: "Sabun Motor 50ml, Semir Ban 20ml" */
    suspend fun getKomposisiSteamMap(): Map<Int, String> {
        val rows = stockSteamDao.getKomposisiSteamRaw()
        return rows.groupBy { it.layananId }
            .mapValues { (_, list) ->
                list.joinToString(", ") { row ->
                    val jumlahText = if (row.jumlah == row.jumlah.toInt().toDouble()) row.jumlah.toInt().toString() else row.jumlah.toString()
                    "${row.namaBahan} $jumlahText${row.satuan}"
                }
            }
    }

    /** Ganti seluruh komposisi bahan untuk 1 layanan (hapus yang lama, simpan yang baru). */
    suspend fun simpanKomposisiLayanan(layananId: Int, pemakaian: List<Pair<StockSteam, Double>>) {
        stockSteamDao.deleteBahanUsageForLayanan(layananId)
        pemakaian.forEach { (stockSteam, jumlah) ->
            stockSteamDao.insertLayananBahan(
                LayananBahan(layananId = layananId, stockSteamId = stockSteam.id, jumlahDigunakan = jumlah)
            )
        }
    }

    /** Dipanggil saat layanan steam terjual: mengurangi stock tiap bahan sesuai qty terjual */
    suspend fun potongStockUntukLayanan(layananId: Int, qty: Int) {
        val usageList = stockSteamDao.getBahanUsageForLayanan(layananId)
        usageList.forEach { usage ->
            stockSteamDao.kurangiStock(usage.stockSteamId, usage.jumlahDigunakan * qty)
        }
    }

    // ---------- Transaksi (Kasir) ----------
    /**
     * Simpan transaksi baru. Transaksi langsung final (tercatat di Riwayat & Laporan Admin).
     */
    suspend fun simpanTransaksi(
        tipe: String,
        kasirUsername: String,
        items: List<TransaksiItemInput>,
        catatan: String = "",
        metodePembayaran: String = METODE_CASH,
        jumlahDibayar: Double = 0.0,
        kembalian: Double = 0.0,
        platNomor: String = ""
    ): Long {
        val total = items.sumOf { it.hargaSatuan * it.qty }
        val transaksiId = transaksiDao.insertTransaksi(
            Transaksi(
                tanggal = System.currentTimeMillis(),
                tipe = tipe,
                total = total,
                kasirUsername = kasirUsername,
                catatan = catatan,
                metodePembayaran = metodePembayaran,
                jumlahDibayar = jumlahDibayar,
                kembalian = kembalian,
                platNomor = platNomor
            )
        )
        items.forEach { item ->
            transaksiDao.insertDetail(
                TransaksiDetail(
                    transaksiId = transaksiId.toInt(),
                    namaItem = item.nama,
                    qty = item.qty,
                    hargaSatuan = item.hargaSatuan,
                    subtotal = item.hargaSatuan * item.qty,
                    isPromo = item.isPromo,
                    menuKopiId = item.menuKopiId,
                    layananId = item.layananId
                )
            )
            item.menuKopiId?.let { potongStockUntukMenu(it, item.qty) }
            item.layananId?.let { potongStockUntukLayanan(it, item.qty) }
        }
        val detailNotifikasi = items.map { item ->
            TransaksiDetail(
                transaksiId = transaksiId.toInt(),
                namaItem = item.nama,
                qty = item.qty,
                hargaSatuan = item.hargaSatuan,
                subtotal = item.hargaSatuan * item.qty,
                isPromo = item.isPromo,
                menuKopiId = item.menuKopiId,
                layananId = item.layananId
            )
        }

        NotificationHelper.transaksiBerhasil(
            context = appContext,
            transaksiId = transaksiId,
            tipe = tipe,
            total = total,
            items = detailNotifikasi
        )
        return transaksiId
    }

    /**
     * Refund transaksi: dipakai kasir/admin saat pelanggan tidak jadi melakukan pesanan
     * (atau alasan lain). Alasan/keterangan WAJIB diisi.
     * - Transaksi ditandai berstatus Refund beserta alasan, waktu, dan siapa yang memproses.
     * - Stock bahan (Cafe) & stock steam (Cuci Motor) yang terpakai pada transaksi ini
     *   dikembalikan otomatis sesuai komposisi masing-masing item.
     * - Transaksi yang sudah Refund otomatis tidak lagi dihitung di omzet/laporan.
     * Return false kalau transaksi tidak ditemukan atau sudah pernah direfund sebelumnya.
     */
    suspend fun refundTransaksi(transaksiId: Int, alasan: String, direfundOleh: String): Boolean {
        val transaksi = transaksiDao.getById(transaksiId) ?: return false
        if (transaksi.status == STATUS_REFUND) return false

        val detailList = transaksiDao.getDetailForTransaksi(transaksiId)
        detailList.forEach { detail ->
            detail.menuKopiId?.let { menuId ->
                val usageList = menuKopiDao.getBahanUsageForMenu(menuId)
                usageList.forEach { usage -> bahanDao.tambahStock(usage.bahanId, usage.jumlahDigunakan * detail.qty) }
            }
            detail.layananId?.let { layananId ->
                val usageList = stockSteamDao.getBahanUsageForLayanan(layananId)
                usageList.forEach { usage -> stockSteamDao.tambahStock(usage.stockSteamId, usage.jumlahDigunakan * detail.qty) }
            }
        }

        transaksiDao.setRefund(
            transaksiId,
            STATUS_REFUND,
            alasan,
            System.currentTimeMillis(),
            direfundOleh
        )

        NotificationHelper.transaksiRefund(
            context = appContext,
            transaksiId = transaksiId,
            total = transaksi.total,
            alasan = alasan,
            items = detailList
        )
        return true
    }

    fun getAllTransaksiLive(): LiveData<List<Transaksi>> = transaksiDao.getAllLive()
    fun getTransaksiByTipeLive(tipe: String): LiveData<List<Transaksi>> = transaksiDao.getByTipeLive(tipe)
    suspend fun getAllTransaksi(): List<Transaksi> = transaksiDao.getAll()

    suspend fun getTransaksiById(id: Int): Transaksi? = transaksiDao.getById(id)
    suspend fun getDetailForTransaksi(transaksiId: Int) = transaksiDao.getDetailForTransaksi(transaksiId)
    fun getTotalOmzetLive(): LiveData<Double?> = transaksiDao.getTotalOmzetLive()

    // ---------- Ringkasan Hari Ini ----------
    private fun awalHariIni(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun akhirHariIni(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getJumlahTransaksiHariIniLive(tipe: String = "SEMUA"): LiveData<Int> =
        transaksiDao.getJumlahTransaksiHariIniLive(awalHariIni(), akhirHariIni(), tipe)

    fun getOmzetHariIniLive(tipe: String = "SEMUA"): LiveData<Double?> =
        transaksiDao.getOmzetHariIniLive(awalHariIni(), akhirHariIni(), tipe)

    fun getTotalProdukTerjualHariIniLive(tipe: String = "SEMUA"): LiveData<Int?> =
        transaksiDao.getTotalProdukTerjualHariIniLive(awalHariIni(), akhirHariIni(), tipe)

    fun getStokBahanHabisLive(): LiveData<Int> = transaksiDao.getStokBahanHabisLive()

    // ---------- Laporan per Periode (Harian/Bulanan/Tahunan) ----------
    fun getJumlahTransaksiPeriodeLive(awal: Long, akhir: Long, tipe: String = "SEMUA"): LiveData<Int> =
        transaksiDao.getJumlahTransaksiHariIniLive(awal, akhir, tipe)

    fun getOmzetPeriodeLive(awal: Long, akhir: Long, tipe: String = "SEMUA"): LiveData<Double?> =
        transaksiDao.getOmzetHariIniLive(awal, akhir, tipe)

    fun getOmzetNormalPeriodeLive(awal: Long, akhir: Long, tipe: String = "SEMUA"): LiveData<Double?> =
        transaksiDao.getOmzetNormalLive(awal, akhir, tipe)

    fun getOmzetPromoPeriodeLive(awal: Long, akhir: Long, tipe: String = "SEMUA"): LiveData<Double?> =
        transaksiDao.getOmzetPromoLive(awal, akhir, tipe)

    fun getTotalProdukTerjualPeriodeLive(awal: Long, akhir: Long, tipe: String = "SEMUA"): LiveData<Int?> =
        transaksiDao.getTotalProdukTerjualHariIniLive(awal, akhir, tipe)

    fun getTransaksiPeriodeLive(awal: Long, akhir: Long, tipe: String = "SEMUA"): LiveData<List<Transaksi>> =
        transaksiDao.getTransaksiPeriodeLive(awal, akhir, tipe)

    suspend fun getTransaksiPeriode(awal: Long, akhir: Long, tipe: String = "SEMUA"): List<Transaksi> =
        transaksiDao.getTransaksiPeriode(awal, akhir, tipe)

    // ---------- Grafik: Omzet N hari terakhir (mode Harian) ----------
    suspend fun getOmzetHarianTerakhir(jumlahHari: Int = 7): List<Pair<String, Double>> {
        val kalenderAkhir = Calendar.getInstance()
        val akhir = akhirHariIni()
        val kalenderAwal = kalenderAkhir.clone() as Calendar
        kalenderAwal.add(Calendar.DAY_OF_MONTH, -(jumlahHari - 1))
        kalenderAwal.set(Calendar.HOUR_OF_DAY, 0); kalenderAwal.set(Calendar.MINUTE, 0)
        kalenderAwal.set(Calendar.SECOND, 0); kalenderAwal.set(Calendar.MILLISECOND, 0)
        val awal = kalenderAwal.timeInMillis

        val rows = transaksiDao.getOmzetHarianRaw(awal, akhir)
        val peta = rows.associate { it.tanggalStr to it.totalOmzet }

        val formatKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val formatLabel = java.text.SimpleDateFormat("dd/MM", java.util.Locale("in", "ID"))

        val hasil = mutableListOf<Pair<String, Double>>()
        val kursor = kalenderAwal.clone() as Calendar
        repeat(jumlahHari) {
            val key = formatKey.format(kursor.time)
            val label = formatLabel.format(kursor.time)
            hasil.add(label to (peta[key] ?: 0.0))
            kursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return hasil
    }

    /** Omzet per hari (dengan tanggal awal & akhir bebas), dipakai untuk hitung agregat mingguan bulan berjalan. */
    suspend fun getOmzetHarianAntara(awal: Long, akhir: Long): List<Pair<Int, Double>> {
        val rows = transaksiDao.getOmzetHarianRaw(awal, akhir)
        val peta = rows.associate { it.tanggalStr to it.totalOmzet }
        val formatKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

        val hasil = mutableListOf<Pair<Int, Double>>()
        val kursor = Calendar.getInstance().apply { timeInMillis = awal }
        val batasAkhir = Calendar.getInstance().apply { timeInMillis = akhir }
        while (!kursor.after(batasAkhir)) {
            val key = formatKey.format(kursor.time)
            hasil.add(kursor.get(Calendar.DAY_OF_MONTH) to (peta[key] ?: 0.0))
            kursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return hasil
    }

    /** Omzet per Minggu ke-1..ke-5 dalam satu bulan (mode Bulanan). */
    suspend fun getOmzetMingguanDalamBulan(kalenderBulan: Calendar): List<Pair<String, Double>> {
        val awalCal = kalenderBulan.clone() as Calendar
        awalCal.set(Calendar.DAY_OF_MONTH, 1)
        awalCal.set(Calendar.HOUR_OF_DAY, 0); awalCal.set(Calendar.MINUTE, 0)
        awalCal.set(Calendar.SECOND, 0); awalCal.set(Calendar.MILLISECOND, 0)

        val akhirCal = kalenderBulan.clone() as Calendar
        akhirCal.set(Calendar.DAY_OF_MONTH, akhirCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        akhirCal.set(Calendar.HOUR_OF_DAY, 23); akhirCal.set(Calendar.MINUTE, 59)
        akhirCal.set(Calendar.SECOND, 59); akhirCal.set(Calendar.MILLISECOND, 999)

        val harian = getOmzetHarianAntara(awalCal.timeInMillis, akhirCal.timeInMillis)
        // Kelompokkan tanggal 1-7 -> Minggu 1, 8-14 -> Minggu 2, dst (maksimal Minggu 5)
        val perMinggu = DoubleArray(5)
        harian.forEach { (tanggal, omzet) ->
            val indexMinggu = ((tanggal - 1) / 7).coerceAtMost(4)
            perMinggu[indexMinggu] += omzet
        }
        val jumlahMingguDipakai = ((akhirCal.getActualMaximum(Calendar.DAY_OF_MONTH) - 1) / 7) + 1
        return (0 until jumlahMingguDipakai).map { i -> "M${i + 1}" to perMinggu[i] }
    }

    /** Omzet per bulan (Jan-Des) dalam satu tahun (mode Tahunan). */
    suspend fun getOmzetBulananDalamTahun(kalenderTahun: Calendar): List<Pair<String, Double>> {
        val awalCal = kalenderTahun.clone() as Calendar
        awalCal.set(Calendar.DAY_OF_YEAR, 1)
        awalCal.set(Calendar.HOUR_OF_DAY, 0); awalCal.set(Calendar.MINUTE, 0)
        awalCal.set(Calendar.SECOND, 0); awalCal.set(Calendar.MILLISECOND, 0)

        val akhirCal = kalenderTahun.clone() as Calendar
        akhirCal.set(Calendar.MONTH, Calendar.DECEMBER)
        akhirCal.set(Calendar.DAY_OF_MONTH, 31)
        akhirCal.set(Calendar.HOUR_OF_DAY, 23); akhirCal.set(Calendar.MINUTE, 59)
        akhirCal.set(Calendar.SECOND, 59); akhirCal.set(Calendar.MILLISECOND, 999)

        val rows = transaksiDao.getOmzetBulananRaw(awalCal.timeInMillis, akhirCal.timeInMillis)
        val peta = rows.associate { it.tanggalStr to it.totalOmzet }
        val formatKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val labelBulan = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")

        val hasil = mutableListOf<Pair<String, Double>>()
        val kursor = awalCal.clone() as Calendar
        repeat(12) { i ->
            val key = formatKey.format(kursor.time)
            hasil.add(labelBulan[i] to (peta[key] ?: 0.0))
            kursor.add(Calendar.MONTH, 1)
        }
        return hasil
    }

    // ---------- Produk Terlaris ----------
    suspend fun getProdukTerlaris(awal: Long, akhir: Long, tipe: String = "SEMUA", limit: Int = 5) =
        transaksiDao.getProdukTerlaris(awal, akhir, tipe, limit)

    /** Semua produk terjual (tanpa batas jumlah), dipakai untuk rekap lengkap saat export laporan. */
    suspend fun getProdukTerjualLengkap(awal: Long, akhir: Long, tipe: String = "SEMUA"): List<ProdukTerlarisRow> =
        transaksiDao.getProdukTerlaris(awal, akhir, tipe, Int.MAX_VALUE)

    /** Detail item per transaksi dalam rentang tanggal, dipakai untuk export laporan lengkap. */
    suspend fun getDetailLaporanPeriode(awal: Long, akhir: Long, tipe: String = "SEMUA"): List<DetailLaporanRow> =
        transaksiDao.getDetailLaporanPeriode(awal, akhir, tipe)
}

data class TransaksiItemInput(
    val nama: String,
    val qty: Int,
    val hargaSatuan: Double,
    val menuKopiId: Int? = null,
    val layananId: Int? = null,
    val isPromo: Boolean = false
)