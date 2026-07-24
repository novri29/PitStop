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
import com.pitstop.pitstop.databinding.ActivityProdukMinumanBinding
import com.pitstop.pitstop.databinding.DialogEditProdukBinding
import com.pitstop.save.entity.KATEGORI_COFFEE
import com.pitstop.save.entity.KATEGORI_NON_COFFEE
import com.pitstop.save.entity.KATEGORI_SNACK
import com.pitstop.save.entity.MenuKopi
import com.pitstop.util.ImagePickerHelper
import com.pitstop.util.ImageUtil
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class ProdukMinumanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProdukMinumanBinding
    private lateinit var viewModel: MenuKopiViewModel
    private lateinit var adapter: ProdukAdapter
    private var semuaMenu: List<MenuKopi> = emptyList()
    private var ketersediaanMap: Map<Int, Boolean> = emptyMap()
    private val kategoriOptions = listOf(KATEGORI_COFFEE, KATEGORI_NON_COFFEE, KATEGORI_SNACK)

    // Menyimpan path gambar yang baru dipilih untuk dialog edit yang sedang terbuka
    private var gambarPathSementara: String? = null
    private var dialogBindingAktif: DialogEditProdukBinding? = null

    private lateinit var imagePicker: ImagePickerHelper

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

        // WAJIB didaftarkan di onCreate (bukan di dalam dialog) karena aturan Activity Result API
        imagePicker = ImagePickerHelper(this) { path ->
            gambarPathSementara = path
            dialogBindingAktif?.imgPreview?.setImageBitmap(ImageUtil.loadThumbnail(path, 300, 300))
        }

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
        dialogBindingAktif = dialogBinding
        gambarPathSementara = menu.gambarPath

        dialogBinding.etNamaProduk.setText(menu.nama)
        dialogBinding.spinnerKategori.adapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, kategoriOptions)
        dialogBinding.spinnerKategori.setSelection(kategoriOptions.indexOf(menu.kategori).coerceAtLeast(0))
        dialogBinding.etHargaJual.setText(menu.hargaJual.toInt().toString())

        val thumbnail = ImageUtil.loadThumbnail(menu.gambarPath, 300, 300)
        if (thumbnail != null) dialogBinding.imgPreview.setImageBitmap(thumbnail)

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        dialogBinding.btnAmbilFoto.setOnClickListener { imagePicker.ambilFoto() }
        dialogBinding.btnPilihGaleri.setOnClickListener { imagePicker.pilihDariGaleri() }

        dialogBinding.btnSimpan.setOnClickListener {
            val namaBaru = dialogBinding.etNamaProduk.text.toString().trim()
            val hargaBaru = dialogBinding.etHargaJual.text.toString().toDoubleOrNull()

            if (namaBaru.isEmpty()) {
                Toast.makeText(this, "Nama produk tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (hargaBaru == null) {
                Toast.makeText(this, "Harga tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val kategoriBaru = kategoriOptions[dialogBinding.spinnerKategori.selectedItemPosition]
            viewModel.updateMenu(
                menu.copy(nama = namaBaru, hargaJual = hargaBaru, kategori = kategoriBaru, gambarPath = gambarPathSementara)
            )
            Toast.makeText(this, "Produk '$namaBaru' diperbarui", Toast.LENGTH_SHORT).show()
            dialogBindingAktif = null
            dialog.dismiss()
        }
        dialog.setOnDismissListener { dialogBindingAktif = null }
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
