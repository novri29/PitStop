package com.pitstop.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.pitstop.adapter.PemakaianStockAdapter
import com.pitstop.adapter.PemakaianStockItem
import com.pitstop.pitstop.databinding.ActivityLayananSteamBinding
import com.pitstop.save.entity.DAFTAR_UKURAN_MOTOR
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.StockSteam
import com.pitstop.util.RupiahTextWatcher
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Layar Admin untuk mengelola layanan Cuci Motor: harga per ukuran (Kecil/Sedang/Besar)
 * dan komposisi bahan (dipotong otomatis dari Stock Steam saat layanan tersebut terjual),
 * mirip pola "Resep Minuman" di sisi Cafe.
 */
class LayananSteamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLayananSteamBinding
    private lateinit var viewModel: StockSteamViewModel
    private lateinit var pemakaianAdapter: PemakaianStockAdapter

    private var daftarStock: List<StockSteam> = emptyList()
    private var layananMap: Map<String, Layanan> = emptyMap()
    private var ukuranTerpilih: String = DAFTAR_UKURAN_MOTOR[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLayananSteamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fix: dorong toolbar agar tidak ketutupan status bar / icon baterai di SDK 35+
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarHeader) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val originalHeight = resources.getDimensionPixelSize(
                androidx.appcompat.R.dimen.abc_action_bar_default_height_material
            )
            view.layoutParams.height = originalHeight + statusBarInsets.top
            view.requestLayout()
            view.setPadding(view.paddingLeft, statusBarInsets.top, view.paddingRight, view.paddingBottom)
            insets
        }

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }
        binding.etHarga.addTextChangedListener(RupiahTextWatcher(binding.etHarga))

        pemakaianAdapter = PemakaianStockAdapter(onDelete = { index ->
            val current = pemakaianAdapter.getItems().toMutableList()
            current.removeAt(index)
            pemakaianAdapter.setItems(current)
        })
        binding.rvPemakaian.layoutManager = LinearLayoutManager(this)
        binding.rvPemakaian.adapter = pemakaianAdapter

        viewModel.stockList.observe(this) { list ->
            daftarStock = list
            binding.spinnerBahan.adapter = ArrayAdapter(
                this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, list.map { it.nama }
            )
        }

        viewModel.layananList.observe(this) { list ->
            layananMap = list.associateBy { it.ukuran }
            muatFormUntukUkuranTerpilih()
        }

        binding.tabUkuran.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                ukuranTerpilih = DAFTAR_UKURAN_MOTOR.getOrElse(tab?.position ?: 0) { DAFTAR_UKURAN_MOTOR[0] }
                muatFormUntukUkuranTerpilih()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.btnTambahBahan.setOnClickListener { tambahPemakaian() }
        binding.btnSimpanHarga.setOnClickListener { simpanHarga() }
        binding.btnSimpanKomposisi.setOnClickListener { simpanKomposisi() }

        binding.btnStockSteam.setOnClickListener {
            startActivity(Intent(this, StockSteamActivity::class.java))
        }
    }

    private fun muatFormUntukUkuranTerpilih() {
        val layanan = layananMap[ukuranTerpilih]
        binding.etHarga.setText(layanan?.harga?.toInt()?.toString() ?: "")

        if (layanan == null) {
            pemakaianAdapter.setItems(emptyList())
            return
        }
        lifecycleScope.launch {
            val usage = viewModel.getBahanUsageUntukLayanan(layanan.id)
            val stockMap = daftarStock.associateBy { it.id }
            val items = usage.mapNotNull { u -> stockMap[u.stockSteamId]?.let { PemakaianStockItem(it, u.jumlahDigunakan) } }
            pemakaianAdapter.setItems(items)
        }
    }

    private fun tambahPemakaian() {
        val posisi = binding.spinnerBahan.selectedItemPosition
        if (posisi < 0 || daftarStock.isEmpty()) {
            Toast.makeText(this, "Belum ada data stock barang steam, silakan input dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val jumlah = binding.etJumlahPakai.text.toString().toDoubleOrNull()
        if (jumlah == null || jumlah <= 0) {
            Toast.makeText(this, "Isi jumlah pemakaian dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        val stock = daftarStock[posisi]
        val current = pemakaianAdapter.getItems().toMutableList()
        current.add(PemakaianStockItem(stock, jumlah))
        pemakaianAdapter.setItems(current)
        binding.etJumlahPakai.text.clear()
    }

    private fun simpanHarga() {
        val harga = RupiahTextWatcher.parse(binding.etHarga.text.toString()).takeIf { binding.etHarga.text.isNotBlank() }
        if (harga == null) {
            Toast.makeText(this, "Isi harga layanan dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.simpanHargaLayanan("Cuci $ukuranTerpilih", ukuranTerpilih, harga)
        Toast.makeText(this, "Harga '$ukuranTerpilih' tersimpan", Toast.LENGTH_SHORT).show()
    }

    private fun simpanKomposisi() {
        val layanan = layananMap[ukuranTerpilih]
        if (layanan == null) {
            Toast.makeText(this, "Simpan harga layanan ini dulu sebelum mengatur bahan", Toast.LENGTH_SHORT).show()
            return
        }
        val pemakaian = pemakaianAdapter.getItems().map { it.stockSteam to it.jumlah }
        viewModel.simpanKomposisi(layanan.id, pemakaian) {
            runOnUiThread {
                Toast.makeText(this, "Komposisi bahan '$ukuranTerpilih' tersimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
