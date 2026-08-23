package com.pitstop.ui.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.StockSteamAdapter
import com.pitstop.save.entity.DAFTAR_SATUAN_STOCK_STEAM
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.pitstop.databinding.ActivityStockSteamBinding
import com.pitstop.util.RupiahTextWatcher
import com.pitstop.util.ViewModelFactory

class StockSteamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockSteamBinding
    private lateinit var viewModel: StockSteamViewModel
    private lateinit var adapter: StockSteamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockSteamBinding.inflate(layoutInflater)
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

        binding.spinnerSatuanStock.adapter = ArrayAdapter(
            this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, DAFTAR_SATUAN_STOCK_STEAM
        )

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        binding.etHargaModalStock.addTextChangedListener(RupiahTextWatcher(binding.etHargaModalStock))

        adapter = StockSteamAdapter(onDelete = { viewModel.hapusStock(it) })
        binding.rvStockSteam.layoutManager = LinearLayoutManager(this)
        binding.rvStockSteam.adapter = adapter

        viewModel.stockList.observe(this) { adapter.submitList(it) }

        binding.btnSimpanStock.setOnClickListener { simpanStock() }
    }

    private fun simpanStock() {
        val nama = binding.etNamaStock.text.toString().trim()
        val posisiSatuan = binding.spinnerSatuanStock.selectedItemPosition
        val jumlah = binding.etJumlahStock.text.toString().toDoubleOrNull()
        val hargaModal = RupiahTextWatcher.parse(binding.etHargaModalStock.text.toString())

        if (nama.isEmpty() || posisiSatuan < 0 || jumlah == null) {
            Toast.makeText(this, "Lengkapi semua data dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        val satuan = DAFTAR_SATUAN_STOCK_STEAM[posisiSatuan]
        viewModel.tambahStock(nama, JENIS_MOTOR, satuan, jumlah, hargaModal)
        binding.etNamaStock.text.clear()
        binding.etJumlahStock.text.clear()
        binding.etHargaModalStock.text.clear()
        Toast.makeText(this, "Stock '$nama' tersimpan", Toast.LENGTH_SHORT).show()
    }
}