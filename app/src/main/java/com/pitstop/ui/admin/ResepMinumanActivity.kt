package com.pitstop.ui.admin

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.PemakaianBahanAdapter
import com.pitstop.adapter.PemakaianItem
import com.pitstop.adapter.ResepAdapter
import com.pitstop.save.entity.Bahan
import com.pitstop.save.entity.KATEGORI_COFFEE
import com.pitstop.save.entity.KATEGORI_NON_COFFEE
import com.pitstop.save.entity.KATEGORI_SNACK
import com.pitstop.save.entity.MenuKopi
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.ActivityResepMinumanBinding
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class ResepMinumanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResepMinumanBinding
    private lateinit var viewModel: MenuKopiViewModel
    private lateinit var pemakaianAdapter: PemakaianBahanAdapter
    private lateinit var resepAdapter: ResepAdapter

    private var daftarBahan: List<Bahan> = emptyList()
    private var semuaMenu: List<MenuKopi> = emptyList()
    private var komposisiMap: Map<Int, String> = emptyMap()
    private val kategoriOptions = listOf(KATEGORI_COFFEE, KATEGORI_NON_COFFEE, KATEGORI_SNACK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResepMinumanBinding.inflate(layoutInflater)
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

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[MenuKopiViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        binding.spinnerKategori.adapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, kategoriOptions)

        pemakaianAdapter = PemakaianBahanAdapter(onDelete = { index ->
            val current = pemakaianAdapter.getItems().toMutableList()
            current.removeAt(index)
            pemakaianAdapter.setItems(current)
            updateEstimasiModal()
        })
        binding.rvPemakaian.layoutManager = LinearLayoutManager(this)
        binding.rvPemakaian.adapter = pemakaianAdapter

        resepAdapter = ResepAdapter()
        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = resepAdapter

        viewModel.menuList.observe(this) { list ->
            semuaMenu = list
            lifecycleScope.launch {
                komposisiMap = viewModel.getKomposisiMap()
                terapkanFilter(binding.etSearch.text.toString())
            }
        }

        viewModel.bahanList.observe(this) { list ->
            daftarBahan = list
            val namaBahan = list.map { it.nama }
            binding.spinnerBahan.adapter = ArrayAdapter(
                this, R.layout.simple_spinner_dropdown_item, namaBahan
            )
        }

        binding.btnTambah.setOnClickListener {
            binding.formTambah.visibility = if (binding.formTambah.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                terapkanFilter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnTambahBahan.setOnClickListener { tambahPemakaian() }
        binding.btnSimpanMenu.setOnClickListener { simpanMenu() }
    }

    private fun terapkanFilter(keyword: String) {
        val hasil = if (keyword.isBlank()) semuaMenu else semuaMenu.filter { it.nama.contains(keyword, ignoreCase = true) }
        resepAdapter.submitData(hasil, komposisiMap)
    }

    private fun tambahPemakaian() {
        val posisi = binding.spinnerBahan.selectedItemPosition
        if (posisi < 0 || daftarBahan.isEmpty()) {
            Toast.makeText(this, "Belum ada data bahan, silakan input bahan dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val jumlah = binding.etJumlahPakai.text.toString().toDoubleOrNull()
        if (jumlah == null || jumlah <= 0) {
            Toast.makeText(this, "Isi jumlah pemakaian dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        val bahan = daftarBahan[posisi]
        val current = pemakaianAdapter.getItems().toMutableList()
        current.add(PemakaianItem(bahan, jumlah))
        pemakaianAdapter.setItems(current)
        binding.etJumlahPakai.text.clear()
        updateEstimasiModal()
    }

    private fun updateEstimasiModal() {
        val total = pemakaianAdapter.getItems().sumOf { it.jumlah * it.bahan.hargaPerSatuan }
        binding.tvEstimasiModal.text = "Estimasi Harga Modal (HPP): ${Formatter.rupiah(total)}"
    }

    private fun simpanMenu() {
        val nama = binding.etNamaMenu.text.toString().trim()
        val kategori = kategoriOptions.getOrElse(binding.spinnerKategori.selectedItemPosition) { KATEGORI_COFFEE }
        val hargaJual = binding.etHargaJual.text.toString().toDoubleOrNull()
        val pemakaian = pemakaianAdapter.getItems()

        if (nama.isEmpty()) {
            Toast.makeText(this, "Nama menu wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        if (pemakaian.isEmpty()) {
            Toast.makeText(this, "Tambahkan minimal 1 bahan", Toast.LENGTH_SHORT).show()
            return
        }
        if (hargaJual == null) {
            Toast.makeText(this, "Isi harga jual dengan benar", Toast.LENGTH_SHORT).show()
            return
        }

        val pasangan = pemakaian.map { it.bahan to it.jumlah }
        viewModel.simpanMenu(nama, kategori, hargaJual, pasangan) {
            runOnUiThread {
                Toast.makeText(this, "Resep '$nama' tersimpan", Toast.LENGTH_SHORT).show()
                binding.etNamaMenu.text.clear()
                binding.etHargaJual.text.clear()
                pemakaianAdapter.setItems(emptyList())
                updateEstimasiModal()
                binding.formTambah.visibility = View.GONE
            }
        }
    }
}
