package com.pitstop.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pitstop.save.AppDatabase
import com.pitstop.save.BahanStokEntity
import com.pitstop.pitstop.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment untuk menambahkan stok bahan baku (kopi, gula, susu, dll).
 * Jumlah diinput manual dengan satuan bebas (gram/kg/ml/liter), lalu dikonversi ke
 * satuan dasar (gram atau ml) sebelum disimpan. Data langsung ditulis ke Room database
 * lokal (bukan API), jadi tetap berfungsi tanpa koneksi internet.
 */
class TambahBahanFragment : Fragment(R.layout.fragment_stock_bahan) {

    private lateinit var etNamaBahan: TextInputEditText
    private lateinit var tilNamaBahan: TextInputLayout
    private lateinit var etJumlah: TextInputEditText
    private lateinit var tilJumlah: TextInputLayout
    private lateinit var actvSatuan: MaterialAutoCompleteTextView
    private lateinit var tvKonversi: TextView
    private lateinit var etHargaModal: TextInputEditText
    private lateinit var tilHargaModal: TextInputLayout
    private lateinit var tvHargaPerSatuan: TextView
    private lateinit var btnSimpan: MaterialButton

    // Label yang tampil di dropdown -> (satuan dasar penyimpanan, faktor kali ke satuan dasar)
    private val opsiSatuan = listOf(
        "Gram" to ("GRAM" to 1.0),
        "Kilogram (kg)" to ("GRAM" to 1000.0),
        "Mililiter (ml)" to ("ML" to 1.0),
        "Liter (l)" to ("ML" to 1000.0)
    )

    private val angka = NumberFormat.getNumberInstance(Locale("in", "ID"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupSatuanDropdown()

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        etJumlah.doAfterTextChanged { updateBantuan() }
        etHargaModal.doAfterTextChanged { updateBantuan() }
        actvSatuan.setOnItemClickListener { _, _, _, _ -> updateBantuan() }

        btnSimpan.setOnClickListener { simpanBahan() }
    }

    private fun bindViews(view: View) {
        tilNamaBahan = view.findViewById(R.id.tilNamaBahan)
        etNamaBahan = view.findViewById(R.id.etNamaBahan)
        tilJumlah = view.findViewById(R.id.tilJumlah)
        etJumlah = view.findViewById(R.id.etJumlah)
        actvSatuan = view.findViewById(R.id.actvSatuan)
        tvKonversi = view.findViewById(R.id.tvKonversi)
        tilHargaModal = view.findViewById(R.id.tilHargaModal)
        etHargaModal = view.findViewById(R.id.etHargaModal)
        tvHargaPerSatuan = view.findViewById(R.id.tvHargaPerSatuan)
        btnSimpan = view.findViewById(R.id.btnSimpan)
    }

    private fun setupSatuanDropdown() {
        val labels = opsiSatuan.map { it.first }
        actvSatuan.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        actvSatuan.setText(labels.first(), false) // default: Gram
    }

    /** (jumlah dalam satuan dasar, "GRAM"/"ML") berdasarkan input saat ini, atau null kalau input belum valid. */
    private fun hitungJumlahDasar(): Pair<Double, String>? {
        val jumlah = etJumlah.text?.toString()?.trim()?.toDoubleOrNull() ?: return null
        val (satuanDasar, faktor) = opsiSatuan.firstOrNull { it.first == actvSatuan.text.toString() }?.second
            ?: return null
        return (jumlah * faktor) to satuanDasar
    }

    /** Update teks bantuan: hasil konversi ke satuan dasar, dan harga per gram/ml. */
    private fun updateBantuan() {
        val hasil = hitungJumlahDasar()
        if (hasil == null) {
            tvKonversi.text = ""
            tvHargaPerSatuan.text = ""
            return
        }
        val (jumlahDasar, satuanDasar) = hasil
        val labelSatuan = if (satuanDasar == "GRAM") "gram" else "ml"
        tvKonversi.text = "≈ ${angka.format(jumlahDasar)} $labelSatuan"

        val harga = etHargaModal.text?.toString()?.trim()?.toLongOrNull()
        tvHargaPerSatuan.text = if (harga != null && jumlahDasar > 0) {
            "≈ Rp ${angka.format(harga / jumlahDasar)} / $labelSatuan"
        } else {
            ""
        }
    }

    private fun simpanBahan() {
        val nama = etNamaBahan.text?.toString()?.trim().orEmpty()
        val hasilKonversi = hitungJumlahDasar()
        val harga = etHargaModal.text?.toString()?.trim()?.toLongOrNull()

        var valid = true

        if (nama.isEmpty()) {
            tilNamaBahan.error = "Nama bahan wajib diisi"
            valid = false
        } else {
            tilNamaBahan.error = null
        }

        if (hasilKonversi == null) {
            tilJumlah.error = "Jumlah harus berupa angka"
            valid = false
        } else {
            tilJumlah.error = null
        }

        if (harga == null) {
            tilHargaModal.error = "Harga modal harus berupa angka"
            valid = false
        } else {
            tilHargaModal.error = null
        }

        if (!valid || hasilKonversi == null || harga == null) return

        val (jumlahDasar, satuanDasar) = hasilKonversi

        val bahanBaru = BahanStokEntity(
            nama = nama,
            jumlah = jumlahDasar,
            satuanDasar = satuanDasar,
            hargaModal = harga
        )

        // Suspend function Room otomatis pindah ke background thread, aman dipanggil dari sini.
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getDatabase(requireContext()).bahanStokDao().insert(bahanBaru)
            Toast.makeText(requireContext(), "\"$nama\" berhasil disimpan", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
