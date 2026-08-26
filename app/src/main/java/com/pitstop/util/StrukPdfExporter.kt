package com.pitstop.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.pitstop.pitstop.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * Export struk (sampul + tiap struk transaksi) jadi 1 file PDF yang bisa diunduh/dibagikan
 * user -- pengganti tombol "Cetak Semua Struk" (dulu langsung cetak ke printer Bluetooth) di
 * Laporan Penjualan jadi tombol "Download PDF", supaya tidak perlu printer fisik buat lihat/
 * menyimpan backup struk.
 *
 * Tiap halaman dirender ke Bitmap SATU-PER-SATU lewat 1 WebView yang dipakai ulang (bukan
 * digabung jadi satu WebView/HTML raksasa), memakai [WebViewRenderHelper] yang sama dengan
 * [BluetoothPrinterHelper] -- alasannya sama: aman dipakai berapa pun banyaknya transaksi,
 * tanpa risiko render gagal karena kontennya kepanjangan (lihat komentar di WebViewRenderHelper
 * & StrukPrintHelper.buildHtmlBatch).
 */
object StrukPdfExporter {

    /** Lebar render tiap halaman, dalam px bitmap -- lebih tinggi dari lebar cetak fisik (384px) supaya PDF tetap tajam saat di-zoom. */
    private const val LEBAR_RENDER_PX = 768

    /** Konversi px bitmap -> point PDF (72 point = 1 inch), asumsi render ~150dpi. */
    private const val PX_PER_POINT = 150f / 72f

    /**
     * Bangun PDF dari [halamanHtml] (tiap elemen = 1 dokumen HTML lengkap, 1 halaman PDF),
     * simpan ke file, lalu tampilkan chooser "Bagikan / Buka PDF" (mekanisme unduh/simpan yang
     * sama seperti export CSV, lihat [ExcelExporter.shareFile]).
     */
    fun unduhPdfBatch(activity: Activity, namaFile: String, halamanHtml: List<String>) {
        if (halamanHtml.isEmpty()) {
            Toast.makeText(activity, "Tidak ada struk untuk diexport.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_progress_pdf, null)
        val containerWebViewPdf = dialogView.findViewById<FrameLayout>(R.id.containerWebViewPdf)
        val tvStatusPdf = dialogView.findViewById<TextView>(R.id.tvStatusPdf)

        val dialog = Dialog(activity)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)
        dialog.show()

        val webView = WebView(activity)
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.setInitialScale(100)
        webView.setBackgroundColor(Color.WHITE)
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        containerWebViewPdf.addView(webView, FrameLayout.LayoutParams(LEBAR_RENDER_PX, FrameLayout.LayoutParams.WRAP_CONTENT))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val pdfDocument = PdfDocument()
                var jumlahBerhasil = 0

                halamanHtml.forEachIndexed { index, html ->
                    if (dialog.isShowing) {
                        tvStatusPdf.text = "Membuat PDF: struk ${index + 1} dari ${halamanHtml.size}..."
                    }
                    val bitmap = renderHtmlKeBitmap(webView, LEBAR_RENDER_PX, html)
                    if (bitmap != null) {
                        jumlahBerhasil++
                        tambahHalamanPdf(pdfDocument, bitmap, halamanKe = jumlahBerhasil)
                        bitmap.recycle()
                    }
                }

                if (jumlahBerhasil == 0) {
                    pdfDocument.close()
                    dialog.dismiss()
                    Toast.makeText(activity, "Gagal membuat PDF: semua halaman gagal dirender.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val file = simpanPdf(activity, namaFile, pdfDocument)
                pdfDocument.close()

                dialog.dismiss()
                val jumlahHalamanGagal = halamanHtml.size - jumlahBerhasil
                if (jumlahHalamanGagal > 0) {
                    Toast.makeText(
                        activity,
                        "PDF tersimpan ($jumlahHalamanGagal struk gagal dirender & dilewati): ${file.name}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(activity, "PDF tersimpan: ${file.name}", Toast.LENGTH_LONG).show()
                }
                shareFile(activity, file)
            } catch (e: Exception) {
                dialog.dismiss()
                Toast.makeText(activity, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    /**
     * Load 1 halaman HTML ke [webView] lalu capture jadi Bitmap secara suspend (nunggu sampai
     * render+capture-nya benar-benar jadi, lihat [WebViewRenderHelper.capturePageDenganRetry]).
     */
    private suspend fun renderHtmlKeBitmap(webView: WebView, lebarRenderPx: Int, html: String): Bitmap? =
        suspendCancellableCoroutine { cont ->
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    webView.postVisualStateCallback(0L, object : WebView.VisualStateCallback() {
                        override fun onComplete(requestId: Long) {
                            webView.post {
                                WebViewRenderHelper.capturePageDenganRetry(webView, lebarRenderPx) { bitmap ->
                                    if (cont.isActive) cont.resume(bitmap)
                                }
                            }
                        }
                    })
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }

    private fun tambahHalamanPdf(pdfDocument: PdfDocument, bitmap: Bitmap, halamanKe: Int) {
        val lebarPoint = (bitmap.width / PX_PER_POINT).toInt().coerceAtLeast(1)
        val tinggiPoint = (bitmap.height / PX_PER_POINT).toInt().coerceAtLeast(1)
        val pageInfo = PdfDocument.PageInfo.Builder(lebarPoint, tinggiPoint, halamanKe).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, null, Rect(0, 0, lebarPoint, tinggiPoint), null)
        pdfDocument.finishPage(page)
    }

    private fun simpanPdf(context: Context, namaFile: String, pdfDocument: PdfDocument): File {
        val dir = context.getExternalFilesDir(null)
        val file = File(dir, namaFile)
        FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        return file
    }

    /** Bagikan/simpan 1 file PDF (dipakai juga untuk export struk satuan kalau suatu saat dibutuhkan). */
    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan / Simpan PDF Struk"))
    }
}
