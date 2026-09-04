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

        val nama =
            binding.etNamaStock.text.toString().trim()

        val posisiSatuan =
            binding.spinnerSatuanStock.selectedItemPosition

        val jumlah =
            binding.etJumlahStock.text
                .toString()
                .replace(",", ".")
                .toDoubleOrNull()

        val hargaModalTotal =
            RupiahTextWatcher.parse(
                binding.etHargaModalStock.text.toString()
            )

        if (
            nama.isEmpty() ||
            posisiSatuan < 0 ||
            jumlah == null ||
            jumlah <= 0 ||
            hargaModalTotal == null ||
            hargaModalTotal <= 0
        ) {
            Toast.makeText(
                this,
                "Lengkapi semua data dengan benar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val satuan =
            DAFTAR_SATUAN_STOCK_STEAM[posisiSatuan]

        // HPP per satuan
        val hargaPerSatuan =
            hargaModalTotal / jumlah

        viewModel.tambahStock(
            nama = nama,
            jenis = JENIS_MOTOR,
            satuan = satuan,
            stock = jumlah,
            hargaPerSatuan = hargaPerSatuan
        )

        binding.etNamaStock.text.clear()
        binding.etJumlahStock.text.clear()
        binding.etHargaModalStock.text.clear()

        Toast.makeText(
            this,
            "Stock '$nama' tersimpan\n" +
                    "HPP: ${com.pitstop.util.Formatter.rupiah(hargaPerSatuan)} / $satuan",
            Toast.LENGTH_LONG
        ).show()
    }
    /** Update stock barang steam yang sudah ada (mis. shampo motor) tanpa perlu hapus lalu buat baru. */
    private fun tampilkanDialogTambahStock(item: StockSteam) {

        // Input jumlah stock baru
        val inputJumlah = EditText(this).apply {
            hint = "Jumlah yang ditambahkan"
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        // Input harga modal stock baru
        val inputHargaModal = EditText(this).apply {
            hint = "Harga modal stock baru"
            inputType =
                InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }

        // Agar input harga otomatis memakai format Rupiah
        inputHargaModal.addTextChangedListener(
            RupiahTextWatcher(inputHargaModal)
        )

        // Container dialog
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 0, 48, 0)

            addView(
                inputJumlah,
                android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                inputHargaModal,
                android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            )
        }

        val hppSaatIni = item.hargaPerSatuan

        val pesan = """
        Stock saat ini: ${formatJumlah(item.stock)} ${item.satuan}
        HPP saat ini: ${com.pitstop.util.Formatter.rupiah(hppSaatIni)} / ${item.satuan}
        
        Masukkan jumlah stock dan harga modal stock yang baru ditambahkan.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Update Stock")
            .setMessage(pesan)
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Tambah") { _, _ ->

                val jumlah =
                    inputJumlah.text
                        .toString()
                        .replace(",", ".")
                        .toDoubleOrNull()

                val hargaModalBaru =
                    RupiahTextWatcher.parse(
                        inputHargaModal.text.toString()
                    )

                // Validasi jumlah
                if (jumlah == null || jumlah <= 0) {
                    Toast.makeText(
                        this,
                        "Jumlah stock tidak valid",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                // Validasi harga modal
                if (hargaModalBaru == null || hargaModalBaru <= 0) {
                    Toast.makeText(
                        this,
                        "Harga modal tidak valid",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                // Hitung HPP baru untuk ditampilkan
                val modalLama =
                    item.stock * item.hargaPerSatuan

                val modalTotal =
                    modalLama + hargaModalBaru

                val stockTotal =
                    item.stock + jumlah

                val hppBaru =
                    modalTotal / stockTotal

                // Simpan ke database
                viewModel.tambahStock(
                    id = item.id,
                    jumlah = jumlah,
                    hargaModalBaru = hargaModalBaru
                )

                Toast.makeText(
                    this,
                    "Stock ${item.nama} berhasil ditambahkan\n" +
                            "Stock: ${formatJumlah(stockTotal)} ${item.satuan}\n" +
                            "HPP baru: ${com.pitstop.util.Formatter.rupiah(hppBaru)} / ${item.satuan}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun formatJumlah(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}