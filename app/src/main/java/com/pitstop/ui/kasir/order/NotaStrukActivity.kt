package com.pitstop.ui.kasir.order

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.NotaAdapter
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ActivityNotaStrukBinding
import com.pitstop.save.entity.METODE_CASH
import com.pitstop.save.entity.STATUS_REFUND
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.ui.admin.AdminMainActivity
import com.pitstop.ui.kasir.KasirMainActivity
import com.pitstop.util.Formatter
import com.pitstop.util.SessionManager
import com.pitstop.util.StrukPrintHelper
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class NotaStrukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotaStrukBinding
    private lateinit var viewModel: NotaStrukViewModel
    private var transaksi: Transaksi? = null
    private var detailList: List<TransaksiDetail> = emptyList()
    private var modeArsip: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotaStrukBinding.inflate(layoutInflater)
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

        val transaksiId = intent.getIntExtra(EXTRA_TRANSAKSI_ID, -1)
        modeArsip = intent.getBooleanExtra(EXTRA_MODE_ARSIP, false)
        viewModel = ViewModelProvider(this, ViewModelFactory(this))[NotaStrukViewModel::class.java]

        // Mode arsip (buka dari riwayat/laporan untuk lihat & cetak ulang):
        // tombol "Tutup" cukup kembali ke layar sebelumnya, tidak perlu ke Dashboard.
        if (modeArsip) {
            binding.btnSelesai.text = getString(R.string.tutup)
        }

        binding.btnBack.setOnClickListener { selesai() }
        binding.btnSelesai.setOnClickListener { selesai() }

        lifecycleScope.launch {
            transaksi = viewModel.getTransaksi(transaksiId)
            detailList = viewModel.getDetail(transaksiId)
            tampilkanData()
        }

        binding.btnShare.setOnClickListener { bagikanStruk() }
        binding.btnPrint.setOnClickListener { cetakStruk() }
    }

    private fun tampilkanData() {
        val t = transaksi ?: return
        binding.tvTipeTransaksi.text = t.tipe
        binding.tvNoTransaksi.text = "No. Transaksi: PIT-${t.id.toString().padStart(6, '0')}"
        binding.tvTanggal.text = "Tanggal: ${Formatter.tanggalWaktu(t.tanggal)}"
        binding.tvKasir.text = "Kasir: ${t.kasirUsername}"

        if (t.platNomor.isNotBlank()) {
            binding.rowPlatNomor.visibility = View.VISIBLE
            binding.rowPlatNomor.text = "Plat Nomor: ${t.platNomor}"
        } else {
            binding.rowPlatNomor.visibility = View.GONE
        }

        // Kalau lagi cetak ulang struk yang transaksinya sudah di-refund, tandai jelas
        // supaya tidak disalahartikan sebagai bukti pembayaran yang masih berlaku.
        if (t.status == STATUS_REFUND) {
            binding.bannerRefund.visibility = View.VISIBLE
            binding.bannerRefund.text = "${getString(R.string.struk_transaksi_ini_sudah_direfund)}\nAlasan: ${t.alasanRefund}"
        } else {
            binding.bannerRefund.visibility = View.GONE
        }

        binding.rvItem.layoutManager = LinearLayoutManager(this)
        binding.rvItem.adapter = NotaAdapter(detailList)

        binding.tvTotalItem.text = detailList.sumOf { it.qty }.toString()
        binding.tvTotal.text = Formatter.rupiah(t.total)
        binding.tvLabelBayar.text = "Bayar (${t.metodePembayaran})"
        binding.tvBayar.text = Formatter.rupiah(t.jumlahDibayar)

        if (t.metodePembayaran == METODE_CASH && t.kembalian > 0) {
            binding.rowKembalian.visibility = View.VISIBLE
            binding.tvKembalian.text = Formatter.rupiah(t.kembalian)
        } else {
            binding.rowKembalian.visibility = View.GONE
        }
    }

    /** Teks polos bergaya terstruktur untuk kebutuhan berbagi/share */
    private fun teksStruk(): String {
        val t = transaksi ?: return ""
        val sb = StringBuilder()
        sb.append("=================================\n")
        sb.append("             PITSTOP             \n")
        sb.append("       ${t.tipe.uppercase()}     \n")
        sb.append("=================================\n")
        if (t.status == STATUS_REFUND) {
            sb.append(">>> TRANSAKSI INI SUDAH DI-REFUND <<<\n")
            sb.append("Alasan: ${t.alasanRefund}\n")
            sb.append("---------------------------------\n")
        }
        sb.append("No. PIT : PIT-${t.id.toString().padStart(6, '0')}\n")
        sb.append("Tgl     : ${Formatter.tanggalWaktu(t.tanggal)}\n")
        sb.append("Kasir   : ${t.kasirUsername}\n")
        if (t.platNomor.isNotBlank()) {
            sb.append("Plat No : ${t.platNomor}\n")
        }
        sb.append("---------------------------------\n")

        detailList.forEach { d ->
            val lineItem = "${d.namaItem} x${d.qty}"
            val price = Formatter.rupiah(d.subtotal)
            val padding = 33 - (lineItem.length + price.length)
            val spaces = if (padding > 0) " ".repeat(padding) else " "
            sb.append("$lineItem$spaces$price\n")
        }

        sb.append("---------------------------------\n")
        sb.append("Total       : ${Formatter.rupiah(t.total)}\n")
        sb.append("Bayar (${t.metodePembayaran}) : ${Formatter.rupiah(t.jumlahDibayar)}\n")
        if (t.metodePembayaran == METODE_CASH && t.kembalian > 0) {
            sb.append("Kembalian   : ${Formatter.rupiah(t.kembalian)}\n")
        }
        sb.append("=================================\n")
        sb.append("  Terima Kasih atas Kunjungan Anda \n")
        sb.append("=================================\n")
        return sb.toString()
    }

    private fun bagikanStruk() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, teksStruk())
        }
        startActivity(Intent.createChooser(shareIntent, "Bagikan Struk"))
    }

    /** HTML Struk Modern dengan Style CSS khusus printer thermal / cetak PDF */
    private fun buildHtmlStruk(): String {
        val t = transaksi ?: return ""
        return StrukPrintHelper.buildHtmlTunggal(this, t, detailList)
    }

    /** Mencetak struk melalui Android Print Framework */
    private fun cetakStruk() {
        val html = buildHtmlStruk()
        if (html.isEmpty()) return
        StrukPrintHelper.cetak(this, html, "Nota_Struk")
    }

    private fun selesai() {
        // Mode arsip (dibuka dari riwayat/laporan untuk lihat & cetak ulang struk lama):
        // cukup kembali ke layar sebelumnya, jangan pindah ke Dashboard.
        if (modeArsip) {
            finish()
            return
        }
        val session = SessionManager(this)
        val intent = if (session.getRole() == com.pitstop.save.entity.ROLE_ADMIN) {
            Intent(this, AdminMainActivity::class.java)
        } else {
            Intent(this, KasirMainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_TRANSAKSI_ID = "extra_transaksi_id"
        const val EXTRA_MODE_ARSIP = "extra_mode_arsip"
    }
}