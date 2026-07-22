package com.pitstop.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pitstop.save.AppDatabase
import com.pitstop.save.entity.Bahan
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.METODE_CASH
import com.pitstop.save.entity.MenuKopi
import com.pitstop.save.entity.MenuKopiBahan
import com.pitstop.save.entity.StockSteam
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.save.entity.User
import java.util.Calendar

/**
 * Satu pintu akses data untuk seluruh aplikasi (Admin & Kasir).
 * Berisi juga logika bisnis inti: hitung harga modal menu kopi & potong stock.
 */
class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
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
    suspend fun simpanMenuKopi(nama: String, kategori: String, hargaJual: Double, pemakaian: List<Pair<Bahan, Double>>): Long {
        val hargaModal = pemakaian.sumOf { (bahan, jumlah) -> jumlah * bahan.hargaPerSatuan }
        val menuId = menuKopiDao.insertMenu(
            MenuKopi(
                nama = nama,
                kategori = kategori,
                hargaModal = hargaModal,
                hargaJual = hargaJual
            )
        )
        pemakaian.forEach { (bahan, jumlah) ->
            menuKopiDao.insertBahanUsage(
                MenuKopiBahan(
                    menuKopiId = menuId.toInt(),
                    bahanId = bahan.id,
                    jumlahDigunakan = jumlah
                )
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
    suspend fun simpanLayanan(layanan: Layanan) = stockSteamDao.insertLayanan(layanan)

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
        kembalian: Double = 0.0
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
                kembalian = kembalian
            )
        )
        items.forEach { item ->
            transaksiDao.insertDetail(
                TransaksiDetail(
                    transaksiId = transaksiId.toInt(),
                    namaItem = item.nama,
                    qty = item.qty,
                    hargaSatuan = item.hargaSatuan,
                    subtotal = item.hargaSatuan * item.qty
                )
            )
            item.menuKopiId?.let { potongStockUntukMenu(it, item.qty) }
        }
        return transaksiId
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
}

data class TransaksiItemInput(
    val nama: String,
    val qty: Int,
    val hargaSatuan: Double,
    val menuKopiId: Int? = null
)
