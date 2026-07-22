package com.pitstop.ui.admin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.pitstop.save.entity.Bahan
import com.pitstop.save.entity.Transaksi
import com.pitstop.repository.AppRepository

/**
 * ViewModel ringkasan hari ini yang reaktif terhadap unit usaha yang dipilih (Cuci Mobil/Motor/Cafe/Semua).
 * Dipakai bersama oleh Dashboard Admin & Dashboard Kasir.
 */
class RingkasanViewModel(private val repository: AppRepository) : ViewModel() {

    val tipeTerpilih = MutableLiveData("SEMUA")

    val jumlahTransaksiHariIni = tipeTerpilih.switchMap { repository.getJumlahTransaksiHariIniLive(it) }
    val omzetHariIni = tipeTerpilih.switchMap { repository.getOmzetHariIniLive(it) }
    val produkTerjualHariIni = tipeTerpilih.switchMap { repository.getTotalProdukTerjualHariIniLive(it) }
    val stokBahanHabis = repository.getStokBahanHabisLive()
    val totalOmzetKeseluruhan = repository.getTotalOmzetLive()

    fun pilihUnit(tipe: String) {
        tipeTerpilih.value = tipe
    }

    fun getBahanList() = repository.getBahanLive()
    fun getAllTransaksi() = repository.getAllTransaksiLive()

    suspend fun getSemuaBahan(): List<Bahan> = repository.getAllBahan()
    suspend fun getSemuaTransaksi(): List<Transaksi> = repository.getAllTransaksi()
    suspend fun getDetail(transaksiId: Int) = repository.getDetailForTransaksi(transaksiId)
}
