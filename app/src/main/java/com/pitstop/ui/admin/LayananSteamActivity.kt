package com.pitstop.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.pitstop.adapter.PemakaianStockAdapter
import com.pitstop.adapter.PemakaianStockItem
import com.pitstop.pitstop.databinding.ActivityLayananSteamBinding
import com.pitstop.save.entity.DAFTAR_UKURAN_MOTOR
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.StockSteam
import com.pitstop.util.Formatter
import com.pitstop.util.RupiahTextWatcher
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Layar Admin untuk mengelola layanan Cuci Motor: harga jual per ukuran
 * (Motor Kecil/Sedang/Besar dan Premium Kecil/Sedang/Besar), komposisi bahan (dipotong otomatis dari Stock Steam saat
 * layanan tersebut terjual), upah karyawan, dan biaya listrik per pencucian -- 3 komponen
 * biaya terakhir dijumlah otomatis jadi Harga Dasar (HPP), mirip pola "Resep Minuman" di
 * sisi Cafe ditambah 2 komponen biaya tambahan yang khusus dibutuhkan Steam.
 */
class LayananSteamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLayananSteamBinding
    private lateinit var viewModel: StockSteamViewModel
    private lateinit var pemakaianAdapter: PemakaianStockAdapter

    private var daftarStock: List<StockSteam> = emptyList()
    private var layananMap: Map<String, Layanan> = emptyMap()
    private var ukuranTerpilih: String = DAFTAR_UKURAN_MOTOR[0]

    companion object {
        /** Tarif listrik PLN golongan bisnis B-2/TR 2026 (Rp/kWh), dipakai sebagai nilai awal
         *  kalkulator biaya listrik. Admin tetap bisa mengubahnya sesuai tarif/golongan sendiri. */
        const val DEFAULT_TARIF_LISTRIK_PER_KWH = 1445.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLayananSteamBinding.inflate(layoutInflater)
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

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        binding.etHarga.addTextChangedListener(RupiahTextWatcher(binding.etHarga))
        binding.etUpahKaryawan.addTextChangedListener(RupiahTextWatcher(binding.etUpahKaryawan) { updateEstimasiModal() })
        binding.etBiayaListrik.addTextChangedListener(RupiahTextWatcher(binding.etBiayaListrik) { updateEstimasiModal() })
        binding.etTarifListrik.setText(DEFAULT_TARIF_LISTRIK_PER_KWH.toInt().toString())

        pemakaianAdapter = PemakaianStockAdapter(onDelete = { index ->
            val current = pemakaianAdapter.getItems().toMutableList()
            current.removeAt(index)
            pemakaianAdapter.setItems(current)
            updateEstimasiModal()
        })
        binding.rvPemakaian.layoutManager = LinearLayoutManager(this)
        binding.rvPemakaian.adapter = pemakaianAdapter

        viewModel.stockList.observe(this) { list ->
            daftarStock = list
            binding.spinnerBahan.adapter = ArrayAdapter(
                this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, list.map { it.nama }
            )
        }

        viewModel.layananList.observe(this) { list ->
            layananMap = list.associateBy { it.ukuran }
            muatFormUntukUkuranTerpilih()
        }

        binding.tabUkuran.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                ukuranTerpilih = DAFTAR_UKURAN_MOTOR.getOrElse(tab?.position ?: 0) { DAFTAR_UKURAN_MOTOR[0] }
                muatFormUntukUkuranTerpilih()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.btnTambahBahan.setOnClickListener { tambahPemakaian() }
        binding.btnSimpanHarga.setOnClickListener { simpanHarga() }
        binding.btnSimpanUpah.setOnClickListener { simpanUpah() }
        binding.btnHitungListrik.setOnClickListener { hitungBiayaListrikOtomatis() }
        binding.btnSimpanListrik.setOnClickListener { simpanBiayaListrik() }
        binding.btnSimpanKomposisi.setOnClickListener { simpanKomposisi() }

        binding.btnStockSteam.setOnClickListener {
            startActivity(Intent(this, StockSteamActivity::class.java))
        }
    }

    private fun muatFormUntukUkuranTerpilih() {
        val layanan = layananMap[ukuranTerpilih]
        binding.etHarga.setText(layanan?.harga?.toInt()?.toString() ?: "")
        binding.etUpahKaryawan.setText(layanan?.upahKaryawan?.takeIf { it > 0 }?.toInt()?.toString() ?: "")
        binding.etBiayaListrik.setText(layanan?.biayaListrik?.takeIf { it > 0 }?.toInt()?.toString() ?: "")

        if (layanan == null) {
            pemakaianAdapter.setItems(emptyList())
            updateEstimasiModal()
            return
        }
        lifecycleScope.launch {
            val usage = viewModel.getBahanUsageUntukLayanan(layanan.id)
            val stockMap = daftarStock.associateBy { it.id }
            val items = usage.mapNotNull { u -> stockMap[u.stockSteamId]?.let { PemakaianStockItem(it, u.jumlahDigunakan) } }
            pemakaianAdapter.setItems(items)
            updateEstimasiModal()
        }
    }

    /**
     * Total estimasi Harga Dasar (HPP) = biaya bahan (komposisi StockSteam yang sedang
     * disusun) + upah karyawan + biaya listrik yang sedang diisi untuk ukuran terpilih,
     * dihitung live saat salah satu dari ketiganya ditambah/diubah -- sama pola dengan
     * ResepMinumanActivity.updateEstimasiModal() di sisi Cafe, ditambah komponen upah &
     * listrik karena Steam butuh biaya jasa tenaga kerja + operasional alat juga.
     */
    private fun updateEstimasiModal() {
        val biayaBahan = pemakaianAdapter.getItems().sumOf { it.jumlah * it.stockSteam.hargaPerSatuan }
        val upah = RupiahTextWatcher.parse(binding.etUpahKaryawan.text.toString())
        val listrik = RupiahTextWatcher.parse(binding.etBiayaListrik.text.toString())
        val total = biayaBahan + upah + listrik
        binding.tvEstimasiModal.text = "Estimasi Harga Dasar (HPP): ${Formatter.rupiah(total)}"
    }

    /**
     * Ide/cara menghitung biaya listrik per pencucian: Daya alat (Watt) x Lama pencucian
     * (menit) x Tarif listrik (Rp/kWh), dikonversi ke kWh. Contoh: pompa air 350 Watt dipakai
     * 15 menit dengan tarif Rp1.445/kWh -> (350/1000) x (15/60) x 1445 = Rp126,4/pencucian.
     * Hasilnya otomatis dituliskan ke kolom Biaya Listrik (masih bisa diubah manual sebelum
     * disimpan, mis. dibulatkan atau ditambah buffer alat lain seperti vacuum/kompresor).
     */
    private fun hitungBiayaListrikOtomatis() {
        val watt = binding.etDayaListrik.text.toString().toDoubleOrNull()
        val menit = binding.etDurasiPencucian.text.toString().toDoubleOrNull()
        val tarif = RupiahTextWatcher.parse(binding.etTarifListrik.text.toString())
        if (watt == null || watt <= 0 || menit == null || menit <= 0 || tarif <= 0) {
            Toast.makeText(this, "Isi Daya (Watt), Lama Pencucian (menit), dan Tarif Listrik dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        val kwh = (watt / 1000.0) * (menit / 60.0)
        val biaya = kwh * tarif
        binding.etBiayaListrik.setText(biaya.toInt().toString())
        Toast.makeText(this, "Estimasi biaya listrik: ${Formatter.rupiah(biaya)} / pencucian", Toast.LENGTH_SHORT).show()
    }

    private fun tambahPemakaian() {
        val posisi = binding.spinnerBahan.selectedItemPosition
        if (posisi < 0 || daftarStock.isEmpty()) {
            Toast.makeText(this, "Belum ada data stock barang steam, silakan input dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val jumlah = binding.etJumlahPakai.text.toString().toDoubleOrNull()
        if (jumlah == null || jumlah <= 0) {
            Toast.makeText(this, "Isi jumlah pemakaian dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        val stock = daftarStock[posisi]
        val current = pemakaianAdapter.getItems().toMutableList()
        current.add(PemakaianStockItem(stock, jumlah))
        pemakaianAdapter.setItems(current)
        binding.etJumlahPakai.text.clear()
        updateEstimasiModal()
    }

    private fun simpanHarga() {
        val harga = RupiahTextWatcher.parse(binding.etHarga.text.toString()).takeIf { binding.etHarga.text.isNotBlank() }
        if (harga == null) {
            Toast.makeText(this, "Isi harga layanan dengan benar", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.simpanHargaLayanan("Cuci $ukuranTerpilih", ukuranTerpilih, harga)
        Toast.makeText(this, "Harga '$ukuranTerpilih' tersimpan", Toast.LENGTH_SHORT).show()
    }

    private fun simpanUpah() {
        val layanan = layananMap[ukuranTerpilih]
        if (layanan == null) {
            Toast.makeText(this, "Simpan harga layanan ini dulu sebelum mengatur upah karyawan", Toast.LENGTH_SHORT).show()
            return
        }
        val upah = RupiahTextWatcher.parse(binding.etUpahKaryawan.text.toString())
        viewModel.simpanUpahKaryawan(layanan.id, upah) {
            runOnUiThread {
                Toast.makeText(this, "Upah karyawan '$ukuranTerpilih' tersimpan, HPP diperbarui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun simpanBiayaListrik() {
        val layanan = layananMap[ukuranTerpilih]
        if (layanan == null) {
            Toast.makeText(this, "Simpan harga layanan ini dulu sebelum mengatur biaya listrik", Toast.LENGTH_SHORT).show()
            return
        }
        val biayaListrik = RupiahTextWatcher.parse(binding.etBiayaListrik.text.toString())
        viewModel.simpanBiayaListrik(layanan.id, biayaListrik) {
            runOnUiThread {
                Toast.makeText(this, "Biaya listrik '$ukuranTerpilih' tersimpan, HPP diperbarui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun simpanKomposisi() {
        val layanan = layananMap[ukuranTerpilih]
        if (layanan == null) {
            Toast.makeText(this, "Simpan harga layanan ini dulu sebelum mengatur bahan", Toast.LENGTH_SHORT).show()
            return
        }
        val pemakaian = pemakaianAdapter.getItems().map { it.stockSteam to it.jumlah }
        viewModel.simpanKomposisi(layanan.id, pemakaian) {
            runOnUiThread {
                Toast.makeText(this, "Komposisi bahan '$ukuranTerpilih' tersimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}