package com.pitstop.ui.kasir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitstop.repository.AppRepository
import com.pitstop.repository.TransaksiItemInput
import com.pitstop.save.entity.METODE_CASH
import kotlinx.coroutines.launch

class TransaksiViewModel(private val repository: AppRepository) : ViewModel() {

    fun simpanTransaksi(
        tipe: String,
        kasirUsername: String,
        items: List<TransaksiItemInput>,
        catatan: String = "",
        metodePembayaran: String = METODE_CASH,
        jumlahDibayar: Double = 0.0,
        kembalian: Double = 0.0,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.simpanTransaksi(
                tipe, kasirUsername, items, catatan, metodePembayaran, jumlahDibayar, kembalian
            )
            onDone(id)
        }
    }
}