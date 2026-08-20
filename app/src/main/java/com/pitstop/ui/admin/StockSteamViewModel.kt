package com.pitstop.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitstop.repository.AppRepository
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.StockSteam
import kotlinx.coroutines.launch

class StockSteamViewModel(private val repository: AppRepository) : ViewModel() {

    val stockList: LiveData<List<StockSteam>> = repository.getStockSteamLive()
    val layananList: LiveData<List<Layanan>> = repository.getLayananLive()

    fun tambahStock(nama: String, jenis: String = JENIS_MOTOR, satuan: String, stock: Double) {
        viewModelScope.launch {
            repository.insertStockSteam(StockSteam(nama = nama, jenis = jenis, satuan = satuan, stock = stock))
        }
    }

    fun hapusStock(item: StockSteam) {
        viewModelScope.launch { repository.deleteStockSteam(item) }
    }

    /** Simpan/perbarui harga layanan untuk 1 ukuran motor (Kecil/Sedang/Besar). */
    fun simpanHargaLayanan(nama: String, ukuran: String, harga: Double) {
        viewModelScope.launch {
            repository.simpanLayanan(Layanan(nama = nama, ukuran = ukuran, harga = harga))
        }
    }

    /** Simpan komposisi bahan (dipakai untuk potong stock otomatis) untuk 1 layanan. */
    fun simpanKomposisi(layananId: Int, pemakaian: List<Pair<StockSteam, Double>>, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.simpanKomposisiLayanan(layananId, pemakaian)
            onDone()
        }
    }

    suspend fun getKomposisiMap(): Map<Int, String> = repository.getKomposisiSteamMap()

    suspend fun getBahanUsageUntukLayanan(layananId: Int) = repository.getBahanUsageForLayanan(layananId)
}
