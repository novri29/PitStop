package com.pitstop.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitstop.save.entity.Bahan
import com.pitstop.save.entity.MenuKopi
import com.pitstop.repository.AppRepository
import kotlinx.coroutines.launch

class MenuKopiViewModel(private val repository: AppRepository) : ViewModel() {

    val menuList: LiveData<List<MenuKopi>> = repository.getMenuKopiLive()
    val bahanList: LiveData<List<Bahan>> = repository.getBahanLive()

    fun simpanMenu(
        nama: String,
        kategori: String,
        hargaJual: Double,
        pemakaian: List<Pair<Bahan, Double>>,
        gambarPath: String? = null,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.simpanMenuKopi(nama, kategori, hargaJual, pemakaian, gambarPath)
            onDone()
        }
    }

    fun updateMenu(menu: MenuKopi) {
        viewModelScope.launch { repository.updateMenuKopi(menu) }
    }

    fun hapusMenu(menu: MenuKopi) {
        viewModelScope.launch { repository.hapusMenuKopi(menu) }
    }

    suspend fun getKomposisiMap(): Map<Int, String> = repository.getKomposisiMap()
    suspend fun getKetersediaanMap(): Map<Int, Boolean> = repository.getKetersediaanMap()
}
