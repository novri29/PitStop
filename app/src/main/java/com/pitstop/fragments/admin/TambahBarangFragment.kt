package com.pitstop.fragments.admin

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pitstop.pitstop.R


/**
 * Fragment untuk menambahkan barang baru ke Stok Barang:
 * nama, kategori, stok awal, harga, dan foto produk.
 */
class TambahBarangFragment : Fragment(R.layout.fragment_stock_steam) {

    private var fotoUri: Uri? = null

    private lateinit var ivFotoProduk: ImageView
    private lateinit var cardFoto: MaterialCardView
    private lateinit var tilNamaBarang: TextInputLayout
    private lateinit var etNamaBarang: TextInputEditText
    private lateinit var tilKategori: TextInputLayout
    private lateinit var actvKategori: MaterialAutoCompleteTextView
    private lateinit var tilStok: TextInputLayout
    private lateinit var etStok: TextInputEditText
    private lateinit var tilHarga: TextInputLayout
    private lateinit var etHarga: TextInputEditText
    private lateinit var btnSimpan: MaterialButton

    // Photo Picker bawaan Android — tidak perlu minta izin runtime (READ_MEDIA_IMAGES dll).
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fotoUri = uri
            ivFotoProduk.setImageURI(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupKategoriDropdown()

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cardFoto.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnSimpan.setOnClickListener { simpanBarang() }
    }

    private fun bindViews(view: View) {
        ivFotoProduk = view.findViewById(R.id.ivFotoProduk)
        cardFoto = view.findViewById(R.id.cardFoto)
        tilNamaBarang = view.findViewById(R.id.tilNamaBarang)
        etNamaBarang = view.findViewById(R.id.etNamaBarang)
        tilKategori = view.findViewById(R.id.tilKategori)
        actvKategori = view.findViewById(R.id.actvKategori)
        tilStok = view.findViewById(R.id.tilStok)
        etStok = view.findViewById(R.id.etStok)
        tilHarga = view.findViewById(R.id.tilHarga)
        etHarga = view.findViewById(R.id.etHarga)
        btnSimpan = view.findViewById(R.id.btnSimpan)
    }

    private fun setupKategoriDropdown() {
        val kategoriList = listOf("Mobil", "Motor")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, kategoriList)
        actvKategori.setAdapter(adapter)
    }

    private fun simpanBarang() {
        val nama = etNamaBarang.text?.toString()?.trim().orEmpty()
        val kategori = actvKategori.text?.toString()?.trim().orEmpty()
        val stokText = etStok.text?.toString()?.trim().orEmpty()
        val hargaText = etHarga.text?.toString()?.trim().orEmpty()

        var valid = true

        if (nama.isEmpty()) {
            tilNamaBarang.error = "Nama barang wajib diisi"
            valid = false
        } else {
            tilNamaBarang.error = null
        }

        if (kategori.isEmpty()) {
            tilKategori.error = "Pilih kategori"
            valid = false
        } else {
            tilKategori.error = null
        }

        val stok = stokText.toIntOrNull()
        if (stok == null) {
            tilStok.error = "Stok harus berupa angka"
            valid = false
        } else {
            tilStok.error = null
        }

        val harga = hargaText.toLongOrNull()
        if (harga == null) {
            tilHarga.error = "Harga harus berupa angka"
            valid = false
        } else {
            tilHarga.error = null
        }

        if (!valid) return

        // TODO: ganti bagian ini dengan penyimpanan ke database/ViewModel/API kamu.
        // Kalau kamu sudah punya model data barang sendiri, pakai itu — Barang di bawah cuma contoh.
        val barangBaru = Barang(
            nama = nama,
            kategori = kategori,
            stok = stok!!,
            harga = harga!!,
            fotoUri = fotoUri?.toString()
        )

        Toast.makeText(requireContext(), "\"$nama\" berhasil ditambahkan", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }
}

data class Barang(
    val nama: String,
    val kategori: String,
    val stok: Int,
    val harga: Long,
    val fotoUri: String?
)