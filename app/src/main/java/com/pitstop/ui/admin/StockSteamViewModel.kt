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

    fun tambahStock(nama: String, jenis: String = JENIS_MOTOR, satuan: String, stock: Double, hargaPerSatuan: Double = 0.0) {
        viewModelScope.launch {
            repository.insertStockSteam(
                StockSteam(nama = nama, jenis = jenis, satuan = satuan, stock = stock, hargaPerSatuan = hargaPerSatuan)
            )
        }
    }

    fun hapusStock(item: StockSteam) {
        viewModelScope.launch { repository.deleteStockSteam(item) }
    }

    /** Update stock barang steam yang SUDAH ADA (mis. shampo motor restock/koreksi jumlah),
     *  jadi tidak perlu hapus lalu buat baru lagi. Sama pola dengan BahanViewModel.tambahStock di sisi Cafe. */
    fun tambahStock(id: Int, jumlah: Double) {
        viewModelScope.launch { repository.tambahStockSteam(id, jumlah) }
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

    /** Simpan upah karyawan untuk 1 layanan (beda2 per ukuran motor); HPP ikut dihitung ulang. */
    fun simpanUpahKaryawan(layananId: Int, upah: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.simpanUpahKaryawan(layananId, upah)
            onDone()
        }
    }

    suspend fun getKomposisiMap(): Map<Int, String> = repository.getKomposisiSteamMap()

    suspend fun getBahanUsageUntukLayanan(layananId: Int) = repository.getBahanUsageForLayanan(layananId)
}