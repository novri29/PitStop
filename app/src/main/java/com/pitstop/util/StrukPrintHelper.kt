package com.pitstop.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import com.pitstop.pitstop.R
import com.pitstop.save.entity.METODE_CASH
import com.pitstop.save.entity.STATUS_REFUND
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail

/**
 * Membangun HTML struk (satuan maupun gabungan/backup banyak struk sekaligus) dan
 * mencetaknya lewat Android Print Framework. Dipakai oleh NotaStrukActivity (struk 1 transaksi)
 * dan fitur "Cetak Semua Struk" di Laporan (backup struk 1 minggu/1 bulan/semua sekaligus).
 */
object StrukPrintHelper {

    private const val CSS = """
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
            margin: 0 auto 18px auto;
        }
        .receipt.batch {
            page-break-after: always;
            border-bottom: 1px dashed #999;
            padding-bottom: 14px;
        }
        .header { text-align: center; margin-bottom: 12px; }
        .logo { max-width: 170px; max-height: 55px; margin: 0 auto 4px auto; display: block; }
        .subtitle { font-size: 11px; text-transform: uppercase; margin-top: 3px; letter-spacing: 0.5px; }
        .divider { border-top: 1px dashed #000; margin: 8px 0; }
        .meta-table, .item-table, .total-table { width: 100%; border-collapse: collapse; }
        .meta-table td { font-size: 11px; padding: 1px 0; }
        .item-table td { font-size: 12px; }
        .total-table td { font-size: 12px; }
        .bold { font-weight: bold; }
        .footer { text-align: center; margin-top: 15px; font-size: 11px; }
        .banner-refund {
            border: 1.5px dashed #d32f2f;
            color: #d32f2f;
            text-align: center;
            padding: 6px;
            margin-bottom: 10px;
            font-size: 11px;
            font-weight: bold;
        }
        .batch-cover {
            text-align: center;
            margin-bottom: 20px;
            page-break-after: always;
        }
        .batch-cover .subtitle { font-size: 13px; }
        .batch-cover .info { font-size: 12px; color: #333; margin-top: 10px; }
    """

    /**
     * Ambil logo (hitam-putih, gaya cap struk toko) sebagai data URI base64, dibaca langsung
     * dari drawable/logo_struk_hitam.png. Dipakai menggantikan tulisan "CLEAN & CUP" di kop struk.
     */
    private fun logoDataUri(context: Context): String {
        val bytes = context.resources.openRawResource(R.drawable.logo_struk_hitam).use { it.readBytes() }
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }

    /** Kop struk berisi logo toko (dipakai di tiap struk & di halaman sampul backup). */
    private fun logoHeaderHtml(context: Context): String =
        """<img class="logo" src="${logoDataUri(context)}" alt="Logo" />"""

    /** Isi 1 struk saja (tanpa wrapper html/body), dipakai untuk mode satuan maupun batch. */
    private fun buildReceiptBody(context: Context, t: Transaksi, detailList: List<TransaksiDetail>, isBatch: Boolean): String {
        val itemsHtml = StringBuilder()
        detailList.forEach { d ->
            itemsHtml.append(
                """
                <tr>
                    <td style='padding: 3px 0;'>${d.namaItem} <span style='color: #666;'>x${d.qty}</span></td>
                    <td style='text-align: right; vertical-align: top; padding: 3px 0;'>${Formatter.rupiah(d.subtotal)}</td>
                </tr>
                """.trimIndent()
            )
        }

        val rowKembalian = if (t.metodePembayaran == METODE_CASH && t.kembalian > 0) {
            """
            <tr>
                <td style='padding: 2px 0;'>Kembalian</td>
                <td style='text-align: right; padding: 2px 0;'>${Formatter.rupiah(t.kembalian)}</td>
            </tr>
            """.trimIndent()
        } else ""

        val rowPlatNomor = if (t.platNomor.isNotBlank()) {
            """
            <tr>
                <td>Plat Nomor</td>
                <td style="text-align: right;">${t.platNomor}</td>
            </tr>
            """.trimIndent()
        } else ""

        val bannerRefundHtml = if (t.status == STATUS_REFUND) {
            """
            <div class="banner-refund">
                TRANSAKSI INI SUDAH DI-REFUND<br/>
                <span style="font-weight: normal;">Alasan: ${t.alasanRefund}</span>
            </div>
            """.trimIndent()
        } else ""

        val kelasReceipt = if (isBatch) "receipt batch" else "receipt"

        return """
            <div class="$kelasReceipt">
                <div class="header">
                    ${logoHeaderHtml(context)}
                    <div class="subtitle">${t.tipe}</div>
                </div>

                <div class="divider"></div>

                $bannerRefundHtml

                <table class="meta-table">
                    <tr>
                        <td>No. PIT</td>
                        <td style="text-align: right;">PIT-${t.id.toString().padStart(6, '0')}</td>
                    </tr>
                    <tr>
                        <td>Tanggal</td>
                        <td style="text-align: right;">${Formatter.tanggalWaktu(t.tanggal)}</td>
                    </tr>
                    <tr>
                        <td>Kasir</td>
                        <td style="text-align: right;">${t.kasirUsername}</td>
                    </tr>
                    $rowPlatNomor
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
                    ~ Pitstop ~
                </div>
            </div>
        """.trimIndent()
    }

    /** HTML lengkap untuk 1 struk (dipakai NotaStrukActivity: lihat & cetak 1 transaksi). */
    fun buildHtmlTunggal(context: Context, t: Transaksi, detailList: List<TransaksiDetail>): String {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><style>$CSS</style></head>
            <body>${buildReceiptBody(context, t, detailList, isBatch = false)}</body>
            </html>
        """.trimIndent()
    }

    /**
     * HTML gabungan berisi banyak struk sekaligus (dipakai fitur "Cetak Semua Struk" di Laporan
     * untuk backup 1 minggu/1 bulan/seluruhnya). Tiap struk dicetak di halaman terpisah,
     * didahului 1 halaman sampul ringkasan.
     */
    fun buildHtmlBatch(context: Context, judulPeriode: String, items: List<Pair<Transaksi, List<TransaksiDetail>>>): String {
        val cover = """
            <div class="batch-cover">
                ${logoHeaderHtml(context)}
                <div class="subtitle">BACKUP STRUK TRANSAKSI</div>
                <div class="info">
                    Periode: $judulPeriode<br/>
                    Total Struk: ${items.size}<br/>
                    Dicetak: ${Formatter.tanggalWaktu(System.currentTimeMillis())}
                </div>
            </div>
        """.trimIndent()

        val body = StringBuilder(cover)
        items.forEachIndexed { index, (t, detail) ->
            val isLast = index == items.lastIndex
            body.append(buildReceiptBody(context, t, detail, isBatch = !isLast))
        }

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><style>$CSS</style></head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }

    /** Mencetak HTML apa saja lewat Android Print Framework. */
    fun cetak(context: Context, html: String, judulDokumen: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(judulDokumen)
                printManager.print(judulDokumen, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}