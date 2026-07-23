package com.pitstop.ui.kasir.order

import androidx.lifecycle.ViewModel
import com.pitstop.repository.AppRepository
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail

class NotaStrukViewModel(private val repository: AppRepository) : ViewModel() {

    suspend fun getTransaksi(id: Int): Transaksi? = repository.getAllTransaksi().find { it.id == id }
    suspend fun getDetail(id: Int): List<TransaksiDetail> = repository.getDetailForTransaksi(id)
}
