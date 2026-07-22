package com.pitstop.fragments.admin

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pitstop.fragments.AdminDashboardFragment

/**
 * Fragment untuk menambahkan stok bahan baku (kopi, gula, susu, dll).
 * Jumlah diinput manual dengan satuan bebas (gram/kg/ml/liter), lalu dikonversi ke
 * satuan dasar (gram atau ml) sebelum disimpan. Data langsung ditulis ke Room database
 * lokal (bukan API), jadi tetap berfungsi tanpa koneksi internet.
 * Foto bahan bisa diambil langsung dari kamera atau dipilih dari galeri, lalu ikut
 * disimpan (path filenya) ke database bersama data bahan lainnya.
 */
class TambahBahanFragment : Fragment(R.layout.fragment_stock_bahan) {

    private lateinit var ivFotoBahan: ImageButton
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

    // Uri foto yang sedang dipilih/diambil user (belum ikut disimpan ke database)
    private var fotoUri: Uri? = null

    // Uri tujuan sementara saat kamera dibuka, dipakai untuk tahu file mana yang baru dibuat kamera
    private var kameraFotoUri: Uri? = null

    // Label yang tampil di dropdown -> (satuan dasar penyimpanan, faktor kali ke satuan dasar)
    private val opsiSatuan = listOf(
        "Gram" to ("GRAM" to 1.0),
        "Kilogram (kg)" to ("GRAM" to 1000.0),
        "Mililiter (ml)" to ("ML" to 1.0),
        "Liter (l)" to ("ML" to 1000.0)
    )

    private val angka = NumberFormat.getNumberInstance(Locale("in", "ID"))

    // Minta izin kamera dulu, kalau diizinkan langsung buka kamera
    private val izinKamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { diizinkan ->
        if (diizinkan) {
            bukaKamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera dibutuhkan untuk ambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    // Hasil jepretan kamera (foto langsung ditulis ke kameraFotoUri oleh sistem)
    private val ambilFotoKamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { berhasil ->
            if (berhasil && kameraFotoUri != null) {

                fotoUri = kameraFotoUri

                ivFotoBahan.imageTintList = null
                ivFotoBahan.setPadding(0, 0, 0, 0)
                ivFotoBahan.scaleType = ImageView.ScaleType.CENTER_CROP
                ivFotoBahan.setImageURI(fotoUri)

            }
        }

    // Hasil pilih foto dari galeri (Photo Picker bawaan Android, tidak butuh izin runtime)
    private val pilihFotoGaleri =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {

                val fileLokal = salinKeFileLokal(uri)

                fotoUri = fileLokal?.let { getUriUntukFile(it) } ?: uri

                ivFotoBahan.imageTintList = null
                ivFotoBahan.setPadding(0, 0, 0, 0)
                ivFotoBahan.scaleType = ImageView.ScaleType.CENTER_CROP
                ivFotoBahan.setImageURI(fotoUri)

            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        val header = view.findViewById<View>(R.id.headerTambahBahan)

        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->

            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            val padding16dp = (16 * resources.displayMetrics.density).toInt()

            v.setPadding(
                v.paddingLeft,
                statusBar.top + padding16dp,
                v.paddingRight,
                padding16dp
            )

            insets
        }
        setupSatuanDropdown()

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    AdminDashboardFragment()
                )
                .commit()
        }

        ivFotoBahan.setOnClickListener { tampilkanPilihanSumberFoto() }

        etJumlah.doAfterTextChanged { updateBantuan() }
        etHargaModal.doAfterTextChanged { updateBantuan() }
        actvSatuan.setOnItemClickListener { _, _, _, _ -> updateBantuan() }

        btnSimpan.setOnClickListener { simpanBahan() }
    }

    private fun bindViews(view: View) {
        ivFotoBahan = view.findViewById(R.id.ivFotoBahan)
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

    // ---------- Ambil / pilih foto ----------

    private fun tampilkanPilihanSumberFoto() {
        val opsi = arrayOf("Ambil Foto (Kamera)", "Pilih dari Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Foto Bahan")
            .setItems(opsi) { _, index ->
                when (index) {
                    0 -> mintaIzinLaluBukaKamera()
                    1 -> pilihFotoGaleri.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            .show()
    }

    private fun mintaIzinLaluBukaKamera() {
        val sudahDiizinkan = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (sudahDiizinkan) {
            bukaKamera()
        } else {
            izinKamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun bukaKamera() {
        val file = buatFileFotoBaru()
        val uri = getUriUntukFile(file)
        kameraFotoUri = uri
        ambilFotoKamera.launch(uri)
    }

    private fun buatFileFotoBaru(): File {
        val folder = File(requireContext().filesDir, "foto_bahan").apply { mkdirs() }
        return File(folder, "bahan_${System.currentTimeMillis()}.jpg")
    }

    private fun getUriUntukFile(file: File): Uri {
        // authority ini HARUS sama persis dengan android:authorities di AndroidManifest.xml
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    /** Salin foto dari galeri (content:// milik app lain) ke storage internal app sendiri. */
    private fun salinKeFileLokal(sumber: Uri): File? {
        return try {
            val file = buatFileFotoBaru()
            requireContext().contentResolver.openInputStream(sumber)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Konversi jumlah & harga ----------

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

    // ---------- Simpan ----------

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
            hargaModal = harga,
            fotoPath = fotoUri?.toString()
        )

        // Suspend function Room otomatis pindah ke background thread, aman dipanggil dari sini.
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getDatabase(requireContext()).bahanStokDao().insert(bahanBaru)
            Toast.makeText(requireContext(), "\"$nama\" berhasil disimpan", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}