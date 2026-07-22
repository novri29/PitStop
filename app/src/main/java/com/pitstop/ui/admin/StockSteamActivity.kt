package com.pitstop.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.StockSteamAdapter
import com.pitstop.save.entity.JENIS_MOBIL
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

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = StockSteamAdapter(onDelete = { viewModel.hapusStock(it) })
        binding.rvStockSteam.layoutManager = LinearLayoutManager(this)
        binding.rvStockSteam.adapter = adapter

        viewModel.stockList.observe(this) { adapter.submitList(it) }

        binding.btnSimpanStock.setOnClickListener { simpanStock() }
    }

    private fun simpanStock() {
        val nama = binding.etNamaStock.text.toString().trim()
        val jenis = if (binding.rbJenisMotor.isChecked) JENIS_MOTOR else JENIS_MOBIL
        val satuan = binding.etSatuanStock.text.toString().trim()
        val jumlah = binding.etJumlahStock.text.toString().toDoubleOrNull()

        if (nama.isEmpty() || satuan.isEmpty() || jumlah == null) {
            Toast.makeText(this, "Lengkapi semua data dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.tambahStock(nama, jenis, satuan, jumlah)
        binding.etNamaStock.text.clear()
        binding.etSatuanStock.text.clear()
        binding.etJumlahStock.text.clear()
        Toast.makeText(this, "Stock '$nama' tersimpan", Toast.LENGTH_SHORT).show()
    }
}
