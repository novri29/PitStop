package com.pitstop.ui.admin

import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.StockSteamAdapter
import com.pitstop.save.entity.DAFTAR_SATUAN_STOCK_STEAM
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.save.entity.StockSteam
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

        adapter = StockSteamAdapter(
            onDelete = { viewModel.hapusStock(it) },
            onTambahStock = { tampilkanDialogTambahStock(it) }
        )
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

    /** Update stock barang steam yang sudah ada (mis. shampo motor) tanpa perlu hapus lalu buat baru. */
    private fun tampilkanDialogTambahStock(item: StockSteam) {
        val input = EditText(this).apply {
            hint = "Jumlah yang ditambahkan"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val container = android.widget.FrameLayout(this).apply {
            setPadding(48, 0, 48, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Update Stock")
            .setMessage("Stock saat ini: ${item.stock} ${item.satuan}\n\nMasukkan jumlah stock yang ingin ditambahkan.")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Tambah") { _, _ ->
                val jumlah = input.text.toString().toDoubleOrNull()
                if (jumlah == null || jumlah <= 0) {
                    Toast.makeText(this, "Jumlah stock tidak valid", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.tambahStock(item.id, jumlah)
                Toast.makeText(this, "Stock ${item.nama} ditambahkan $jumlah ${item.satuan}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}