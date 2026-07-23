package com.pitstop.ui.admin

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.ProdukAdapter
import com.pitstop.save.entity.KATEGORI_COFFEE
import com.pitstop.save.entity.KATEGORI_NON_COFFEE
import com.pitstop.save.entity.KATEGORI_SNACK
import com.pitstop.save.entity.MenuKopi
import com.pitstop.pitstop.databinding.ActivityProdukMinumanBinding
import com.pitstop.pitstop.databinding.DialogEditProdukBinding
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class ProdukMinumanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProdukMinumanBinding
    private lateinit var viewModel: MenuKopiViewModel
    private lateinit var adapter: ProdukAdapter
    private var semuaMenu: List<MenuKopi> = emptyList()
    private var ketersediaanMap: Map<Int, Boolean> = emptyMap()
    private val kategoriOptions = listOf(KATEGORI_COFFEE, KATEGORI_NON_COFFEE, KATEGORI_SNACK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProdukMinumanBinding.inflate(layoutInflater)
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

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[MenuKopiViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        adapter = ProdukAdapter(
            onEdit = { tampilkanDialogEdit(it) },
            onHapus = { tampilkanMenuHapus(it) }
        )
        binding.rvProduk.layoutManager = LinearLayoutManager(this)
        binding.rvProduk.adapter = adapter

        viewModel.menuList.observe(this) { list ->
            semuaMenu = list
            lifecycleScope.launch {
                ketersediaanMap = viewModel.getKetersediaanMap()
                terapkanFilter(binding.etSearch.text.toString())
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                terapkanFilter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun terapkanFilter(keyword: String) {
        val hasil = if (keyword.isBlank()) semuaMenu else semuaMenu.filter { it.nama.contains(keyword, ignoreCase = true) }
        adapter.submitData(hasil, ketersediaanMap)
    }

    private fun tampilkanDialogEdit(menu: MenuKopi) {
        val dialogBinding = DialogEditProdukBinding.inflate(layoutInflater)
        dialogBinding.tvNamaProduk.text = menu.nama
        dialogBinding.spinnerKategori.adapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, kategoriOptions)
        dialogBinding.spinnerKategori.setSelection(kategoriOptions.indexOf(menu.kategori).coerceAtLeast(0))
        dialogBinding.etHargaJual.setText(menu.hargaJual.toInt().toString())

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        dialogBinding.btnSimpan.setOnClickListener {
            val hargaBaru = dialogBinding.etHargaJual.text.toString().toDoubleOrNull()
            if (hargaBaru == null) {
                Toast.makeText(this, "Harga tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val kategoriBaru = kategoriOptions[dialogBinding.spinnerKategori.selectedItemPosition]
            viewModel.updateMenu(menu.copy(hargaJual = hargaBaru, kategori = kategoriBaru))
            Toast.makeText(this, "Produk '${menu.nama}' diperbarui", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun tampilkanMenuHapus(menu: MenuKopi) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Produk")
            .setMessage("Hapus produk '${menu.nama}' beserta resepnya?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.hapusMenu(menu)
                Toast.makeText(this, "Produk dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
