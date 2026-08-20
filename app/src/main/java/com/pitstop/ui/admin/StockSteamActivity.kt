package com.pitstop.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.StockSteamAdapter
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.pitstop.databinding.ActivityStockSteamBinding
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

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        adapter = StockSteamAdapter(onDelete = { viewModel.hapusStock(it) })
        binding.rvStockSteam.layoutManager = LinearLayoutManager(this)
        binding.rvStockSteam.adapter = adapter

        viewModel.stockList.observe(this) { adapter.submitList(it) }

        binding.btnSimpanStock.setOnClickListener { simpanStock() }
    }

    private fun simpanStock() {
        val nama = binding.etNamaStock.text.toString().trim()
        val satuan = binding.etSatuanStock.text.toString().trim()
        val jumlah = binding.etJumlahStock.text.toString().toDoubleOrNull()

        if (nama.isEmpty() || satuan.isEmpty() || jumlah == null) {
            Toast.makeText(this, "Lengkapi semua data dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.tambahStock(nama, JENIS_MOTOR, satuan, jumlah)
        binding.etNamaStock.text.clear()
        binding.etSatuanStock.text.clear()
        binding.etJumlahStock.text.clear()
        Toast.makeText(this, "Stock '$nama' tersimpan", Toast.LENGTH_SHORT).show()
    }
}
