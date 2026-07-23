package com.pitstop.ui.kasir.order

import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
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
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.ui.admin.AdminMainActivity
import com.pitstop.ui.kasir.KasirMainActivity
import com.pitstop.util.Formatter
import com.pitstop.util.SessionManager
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class NotaStrukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotaStrukBinding
    private lateinit var viewModel: NotaStrukViewModel
    private var transaksi: Transaksi? = null
    private var detailList: List<TransaksiDetail> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotaStrukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val transaksiId = intent.getIntExtra(EXTRA_TRANSAKSI_ID, -1)
        viewModel = ViewModelProvider(this, ViewModelFactory(this))[NotaStrukViewModel::class.java]

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
        binding.tvNoTransaksi.text = "No. Transaksi: TRX-${t.id.toString().padStart(6, '0')}"
        binding.tvTanggal.text = "Tanggal: ${Formatter.tanggalWaktu(t.tanggal)}"
        binding.tvKasir.text = "Kasir: ${t.kasirUsername}"

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
        sb.append("          CLEAN & CUP            \n")
        sb.append("       ${t.tipe.uppercase()}     \n")
        sb.append("=================================\n")
        sb.append("No. TRX : TRX-${t.id.toString().padStart(6, '0')}\n")
        sb.append("Tgl     : ${Formatter.tanggalWaktu(t.tanggal)}\n")
        sb.append("Kasir   : ${t.kasirUsername}\n")
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
        val itemsHtml = StringBuilder()

        detailList.forEach { d ->
            itemsHtml.append("""
                <tr>
                    <td style='padding: 3px 0;'>${d.namaItem} <span style='color: #666;'>x${d.qty}</span></td>
                    <td style='text-align: right; vertical-align: top; padding: 3px 0;'>${Formatter.rupiah(d.subtotal)}</td>
                </tr>
            """.trimIndent())
        }

        val rowKembalian = if (t.metodePembayaran == METODE_CASH && t.kembalian > 0) {
            """
            <tr>
                <td style='padding: 2px 0;'>Kembalian</td>
                <td style='text-align: right; padding: 2px 0;'>${Formatter.rupiah(t.kembalian)}</td>
            </tr>
            """.trimIndent()
        } else ""

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 13px;
                        color: #111;
                        margin: 0;
                        padding: 10px;
                        background-color: #fff;
                    }
                    .receipt {
                        max-width: 280px;
                        margin: 0 auto;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 12px;
                    }
                    .title {
                        font-size: 18px;
                        font-weight: bold;
                        letter-spacing: 1px;
                        margin: 0;
                    }
                    .subtitle {
                        font-size: 11px;
                        text-transform: uppercase;
                        margin-top: 3px;
                        letter-spacing: 0.5px;
                    }
                    .divider {
                        border-top: 1px dashed #000;
                        margin: 8px 0;
                    }
                    .meta-table, .item-table, .total-table {
                        width: 100%;
                        border-collapse: collapse;
                    }
                    .meta-table td {
                        font-size: 11px;
                        padding: 1px 0;
                    }
                    .item-table td {
                        font-size: 12px;
                    }
                    .total-table td {
                        font-size: 12px;
                    }
                    .bold {
                        font-weight: bold;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 15px;
                        font-size: 11px;
                    }
                </style>
            </head>
            <body>
                <div class="receipt">
                    <div class="header">
                        <div class="title">CLEAN & CUP</div>
                        <div class="subtitle">${t.tipe}</div>
                    </div>
                    
                    <div class="divider"></div>
                    
                    <table class="meta-table">
                        <tr>
                            <td>No. TRX</td>
                            <td style="text-align: right;">TRX-${t.id.toString().padStart(6, '0')}</td>
                        </tr>
                        <tr>
                            <td>Tanggal</td>
                            <td style="text-align: right;">${Formatter.tanggalWaktu(t.tanggal)}</td>
                        </tr>
                        <tr>
                            <td>Kasir</td>
                            <td style="text-align: right;">${t.kasirUsername}</td>
                        </tr>
                    </table>
                    
                    <div class="divider"></div>
                    
                    <table class="item-table">
                        $itemsHtml
                    </table>
                    
                    <div class="divider"></div>
                    
                    <table class="total-table">
                        <tr class="bold">
                            <td style="padding: 3px 0;">Total</td>
                            <td style="text-align: right; padding: 3px 0;">${Formatter.rupiah(t.total)}</td>
                        </tr>
                        <tr>
                            <td style="padding: 2px 0;">Bayar (${t.metodePembayaran})</td>
                            <td style="text-align: right; padding: 2px 0;">${Formatter.rupiah(t.jumlahDibayar)}</td>
                        </tr>
                        $rowKembalian
                    </table>
                    
                    <div class="divider"></div>
                    
                    <div class="footer">
                        Terima kasih atas kunjungan Anda!<br/>
                        ~ Clean & Cup ~
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /** Mencetak struk melalui Android Print Framework */
    private fun cetakStruk() {
        val html = buildHtmlStruk()
        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = getSystemService(PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Nota_Struk")
                printManager.print(
                    "Nota Struk",
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun selesai() {
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
    }
}