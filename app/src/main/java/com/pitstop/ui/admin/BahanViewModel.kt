package com.pitstop.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitstop.save.entity.Bahan
import com.pitstop.repository.AppRepository
import kotlinx.coroutines.launch

class BahanViewModel(private val repository: AppRepository) : ViewModel() {

    val bahanList: LiveData<List<Bahan>> = repository.getBahanLive()

    fun tambah(nama: String, satuan: String, stock: Double, hargaPerSatuan: Double) {
        viewModelScope.launch {
            repository.insertBahan(Bahan(nama = nama, satuan = satuan, stock = stock, hargaPerSatuan = hargaPerSatuan))
        }
    }

    fun update(bahan: Bahan) {
        viewModelScope.launch { repository.updateBahan(bahan) }
    }

    fun hapus(bahan: Bahan) {
        viewModelScope.launch { repository.deleteBahan(bahan) }
    }
}
