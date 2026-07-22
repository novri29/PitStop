package com.pitstop.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitstop.save.entity.Layanan
import com.pitstop.repository.AppRepository
import com.pitstop.save.entity.StockSteam
import kotlinx.coroutines.launch

class StockSteamViewModel(private val repository: AppRepository) : ViewModel() {

    val stockList: LiveData<List<StockSteam>> = repository.getStockSteamLive()
    val layananList: LiveData<List<Layanan>> = repository.getLayananLive()

    fun tambahStock(nama: String, jenis: String, satuan: String, stock: Double) {
        viewModelScope.launch {
            repository.insertStockSteam(StockSteam(nama = nama, jenis = jenis, satuan = satuan, stock = stock))
        }
    }

    fun hapusStock(item: StockSteam) {
        viewModelScope.launch { repository.deleteStockSteam(item) }
    }

    fun simpanHargaLayanan(nama: String, jenis: String, harga: Double) {
        viewModelScope.launch {
            repository.simpanLayanan(Layanan(nama = nama, jenis = jenis, harga = harga))
        }
    }
}
