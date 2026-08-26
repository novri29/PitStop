package com.pitstop.util

import android.annotation.SuppressLint
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
import com.pitstop.util.BluetoothPrinterHelper

/**
 * Membangun HTML struk (satuan maupun gabungan/backup banyak struk sekaligus) dan
 * mencetaknya lewat Android Print Framework. Dipakai oleh NotaStrukActivity (struk 1 transaksi)
 * dan fitur "Cetak Semua Struk" di Laporan (backup struk 1 minggu/1 bulan/semua sekaligus).
 */
object StrukPrintHelper {

    private const val CSS = """
    body {
            font-family: 'Courier New', monospace;
            font-size: 22px;
            font-weight: bold;
            color: #000;
            margin: 0;
            padding: 0;
            background-color: #fff;
            width: 384px;
            box-sizing: border-box;
        }
    
        .receipt {
            width: 384px;
            max-width: 384px;
            margin: 0;
            padding: 12px 10px 45px 10px;
            box-sizing: border-box;
        }
    
        .receipt.batch {
            page-break-after: always;
            border-bottom: 2px dashed #000;
            padding-bottom: 14px;
        }
    
        .header {
            text-align: center;
            margin-bottom: 12px;
        }
    
        .logo {
            width: 310px;
            max-width: 310px;
            max-height: 95px;
            margin: 0 auto 8px auto;
            display: block;
        }
    
        .subtitle {
            font-size: 21px;
            font-weight: bold;
            text-transform: uppercase;
            margin-top: 4px;
            letter-spacing: 0.5px;
        }
    
        .divider {
            border-top: 2px dashed #000;
            margin: 8px 0;
        }
    
        .meta-table,
        .item-table,
        .total-table {
            width: 100%;
            border-collapse: collapse;
        }
    
        .meta-table td {
            font-size: 21px;
            font-weight: bold;
            padding: 3px 0;
        }
    
        .item-table td {
            font-size: 21px;
            font-weight: bold;
            padding: 4px 0;
        }
    
        .total-table td {
            font-size: 22px;
            font-weight: bold;
            padding: 4px 0;
        }
    
        .bold {
            font-weight: bold;
        }
    
        .footer {
            text-align: center;
            margin-top: 14px;
            font-size: 19px;
            font-weight: bold;
            line-height: 1.4;
        }
    
        .banner-refund {
            border: 2px dashed #000;
            color: #000;
            text-align: center;
            padding: 8px;
            margin-bottom: 10px;
            font-size: 20px;
            font-weight: bold;
        }
    
        .batch-cover {
            text-align: center;
            margin-bottom: 20px;
            page-break-after: always;
        }
    
        .batch-cover .subtitle {
            font-size: 22px;
        }
    
        .batch-cover .info {
            font-size: 19px;
            color: #000;
            margin-top: 10px;
        }
    """

    /**
     * Ambil logo (hitam-putih, gaya cap struk toko) sebagai data URI base64, dibaca langsung
     * dari drawable/logo_struk_hitam.png. Dipakai menggantikan tulisan "CLEAN & CUP" di kop struk.
     */
    @SuppressLint("ResourceType")
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
                    Terima kasih atas kunjungan Anda<br/>
                    JL. Turi Raya No.102 Tanjung Senang<br/>
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
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=384, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>$CSS</style>
            </head>
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
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=384, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>$CSS</style>
            </head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }

    /** Mencetak HTML apa saja lewat Android Print Framework. */
    fun cetak(context: Context, html: String, judulDokumen: String) {

        val activity = context as? android.app.Activity

        if (activity == null) {
            return
        }

        BluetoothPrinterHelper.printHtml(
            activity,
            html
        )
    }
}