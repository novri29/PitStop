package com.pitstop.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.BahanAdapter
import com.pitstop.save.entity.Bahan
import com.pitstop.pitstop.databinding.ActivityStockBahanBinding
import com.pitstop.util.ViewModelFactory

class StockBahanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockBahanBinding
    private lateinit var viewModel: BahanViewModel
    private lateinit var adapter: BahanAdapter
    private var semuaBahan: List<Bahan> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockBahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[BahanViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        adapter = BahanAdapter(onDelete = { bahan ->
            viewModel.hapus(bahan)
            Toast.makeText(this, "Bahan dihapus", Toast.LENGTH_SHORT).show()
        })
        binding.rvBahan.layoutManager = LinearLayoutManager(this)
        binding.rvBahan.adapter = adapter

        viewModel.bahanList.observe(this) { list ->
            semuaBahan = list
            terapkanFilter(binding.etSearch.text.toString())
        }

        binding.btnTambah.setOnClickListener {
            binding.formTambah.visibility =
                if (binding.formTambah.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                terapkanFilter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSimpan.setOnClickListener { simpanBahan() }
    }

    private fun terapkanFilter(keyword: String) {
        val hasil = if (keyword.isBlank()) semuaBahan
        else semuaBahan.filter { it.nama.contains(keyword, ignoreCase = true) }
        adapter.submitList(hasil)
    }

    private fun simpanBahan() {
        val nama = binding.etNama.text.toString().trim()
        val satuan = if (binding.rbGram.isChecked) "gram" else "ml"
        val stock = binding.etStock.text.toString().toDoubleOrNull()
        val harga = binding.etHargaPerSatuan.text.toString().toDoubleOrNull()

        if (nama.isEmpty() || stock == null || harga == null) {
            Toast.makeText(this, "Lengkapi semua data dengan benar", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.tambah(nama, satuan, stock, harga)
        binding.etNama.text.clear()
        binding.etStock.text.clear()
        binding.etHargaPerSatuan.text.clear()
        binding.formTambah.visibility = View.GONE
        Toast.makeText(this, "Bahan '$nama' tersimpan", Toast.LENGTH_SHORT).show()
    }
}
