package com.pitstop.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.BahanAdapter
import com.pitstop.save.entity.Bahan
import com.pitstop.pitstop.databinding.ActivityStockBahanBinding
import com.pitstop.util.RupiahTextWatcher
import com.pitstop.util.ViewModelFactory
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.pitstop.util.Formatter

class StockBahanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockBahanBinding
    private lateinit var viewModel: BahanViewModel
    private lateinit var adapter: BahanAdapter
    private var semuaBahan: List<Bahan> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockBahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fix: dorong toolbar agar tidak ketutupan status bar / icon baterai di SDK 35+
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarHeader) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            // Simpan tinggi asli toolbar sekali saja (sebelum ditambah padding)
            val originalHeight = resources.getDimensionPixelSize(
                androidx.appcompat.R.dimen.abc_action_bar_default_height_material
            )

            view.layoutParams.height = originalHeight + statusBarInsets.top
            view.requestLayout()

            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[BahanViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        binding.etHargaPerSatuan.addTextChangedListener(RupiahTextWatcher(binding.etHargaPerSatuan))

        adapter = BahanAdapter(
            onDelete = { bahan ->
                viewModel.hapus(bahan)

                Toast.makeText(
                    this,
                    "Bahan dihapus",
                    Toast.LENGTH_SHORT
                ).show()
            },

            onTambahStock = { bahan ->
                tampilkanDialogTambahStock(bahan)
            }
        )
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

        val satuan = when {
            binding.rbGram.isChecked -> "gram"
            binding.rbMl.isChecked -> "ml"
            else -> "pcs"
        }

        val stock = binding.etStock.text.toString().toDoubleOrNull()

        val hargaModal = RupiahTextWatcher
            .parse(binding.etHargaPerSatuan.text.toString())
            .takeIf {
                binding.etHargaPerSatuan.text.isNotBlank()
            }

        if (nama.isEmpty() || stock == null || stock <= 0 || hargaModal == null) {
            Toast.makeText(
                this,
                "Lengkapi semua data dengan benar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Harga modal yang dimasukkan adalah harga TOTAL stock.
        // Contoh:
        // 1000 gram = Rp164.000
        // Maka harga per gram = Rp164.000 / 1000 = Rp164
        val hargaPerSatuan = hargaModal / stock

        viewModel.tambah(
            nama,
            satuan,
            stock,
            hargaPerSatuan
        )

        binding.etNama.text.clear()
        binding.etStock.text.clear()
        binding.etHargaPerSatuan.text.clear()

        binding.formTambah.visibility = View.GONE

        Toast.makeText(
            this,
            "Bahan '$nama' tersimpan",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun tampilkanDialogTambahStock(
        bahan: Bahan
    ) {

        val inputJumlah = EditText(this).apply {
            hint = "Jumlah stock baru"
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val inputHarga = EditText(this).apply {
            hint = "Harga modal stock baru"
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        // Format rupiah untuk harga modal
        inputHarga.addTextChangedListener(
            RupiahTextWatcher(inputHarga)
        )

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 0, 48, 0)

            addView(
                inputJumlah,
                android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                inputHarga,
                android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Tambah Stock")
            .setMessage(
                "Stock saat ini: ${bahan.stock} ${bahan.satuan}\n" +
                        "Harga modal saat ini: ${
                            Formatter.rupiah(bahan.hargaPerSatuan)
                        } / ${bahan.satuan}\n\n" +
                        "Masukkan jumlah dan harga modal stock baru."
            )
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Tambah") { _, _ ->

                val jumlah =
                    inputJumlah.text.toString().toDoubleOrNull()

                val hargaModalBaru =
                    RupiahTextWatcher
                        .parse(inputHarga.text.toString())

                if (jumlah == null || jumlah <= 0) {

                    Toast.makeText(
                        this,
                        "Jumlah stock tidak valid",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                if (hargaModalBaru <= 0) {

                    Toast.makeText(
                        this,
                        "Harga modal tidak valid",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                viewModel.tambahStock(
                    bahan.id,
                    jumlah,
                    hargaModalBaru
                )

                Toast.makeText(
                    this,
                    "Stock ${bahan.nama} ditambahkan $jumlah ${bahan.satuan}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }
}