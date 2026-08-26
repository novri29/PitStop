package com.pitstop.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout

/**
 * Render 1 WebView (yang HTML-nya sudah di-load & WebView-nya ditempel ke layout yang benar-
 * benar attached di layar -- WAJIB, WebView modern berbasis Chromium SENGAJA tidak merender
 * konten kalau dianggap "tidak terlihat") jadi Bitmap.
 *
 * Dipakai bersama oleh alur cetak Bluetooth ([BluetoothPrinterHelper]) maupun export PDF
 * ([StrukPdfExporter]) supaya logic retry & deteksi "render belum selesai"-nya konsisten &
 * tidak terduplikasi di 2 tempat.
 */
object WebViewRenderHelper {

    private const val TAG = "WebViewRenderHelper"

    /*
     * Kadang postVisualStateCallback() sudah bilang "selesai render", tapi capture
     * webView.draw(canvas) berikutnya masih menangkap frame kosong/putih polos -- race
     * condition antara compositor thread WebView dan main thread. Untuk itu capture dicoba
     * beberapa kali (dengan delay progresif) sebelum benar-benar menyerah.
     */
    private const val MAKS_PERCOBAAN_CAPTURE = 6
    private const val JEDA_RETRY_CAPTURE_MS = 300L

    /*
     * Pita kosong (putih polos) yang lebih tinggi dari ini di dalam hasil capture dianggap
     * tanda render BELUM SELESAI, bukan sekadar jarak antar elemen struk (padding/margin
     * antar elemen struk paling besar ~70px, jadi 250px sudah jauh lebih dari cukup untuk
     * membedakan "jarak wajar" dari "belum sempat dirender").
     */
    private const val BATAS_TINGGI_PITA_KOSONG_PX = 250

    /**
     * Ukur, resize, lalu screenshot 1 WebView ke Bitmap, dengan retry kalau hasilnya masih ada
     * bagian kosong (lihat [bitmapBelumSelesaiDirender]). Fungsi murni -- tidak menyentuh UI
     * apa pun selain WebView itu sendiri. [onSelesai] dipanggil dengan `null` kalau tetap gagal
     * setelah [MAKS_PERCOBAAN_CAPTURE] percobaan.
     */
    fun capturePageDenganRetry(
        webView: WebView,
        previewWidth: Int,
        percobaanKe: Int = 1,
        onSelesai: (Bitmap?) -> Unit
    ) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(previewWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        webView.measure(widthSpec, heightSpec)

        val width = previewWidth
        val height = webView.measuredHeight

        if (height <= 0) {
            if (percobaanKe < MAKS_PERCOBAAN_CAPTURE) {
                Log.w(TAG, "Capture: tinggi WebView masih 0, coba lagi ($percobaanKe/$MAKS_PERCOBAAN_CAPTURE)...")
                jadwalkanRetryCapture(webView, previewWidth, percobaanKe, onSelesai)
                return
            }
            Log.e(TAG, "Capture: tinggi WebView tetap 0 setelah $MAKS_PERCOBAAN_CAPTURE percobaan.")
            onSelesai(null)
            return
        }

        webView.layout(0, 0, width, height)
        webView.layoutParams = FrameLayout.LayoutParams(width, height)
        webView.invalidate()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        webView.draw(canvas)

        if (bitmapBelumSelesaiDirender(bitmap)) {
            if (percobaanKe < MAKS_PERCOBAAN_CAPTURE) {
                Log.w(TAG, "Capture masih ada bagian kosong (belum selesai render), coba lagi ($percobaanKe/$MAKS_PERCOBAAN_CAPTURE)...")
                jadwalkanRetryCapture(webView, previewWidth, percobaanKe, onSelesai)
                return
            }
            Log.e(TAG, "Capture masih ada bagian kosong setelah $MAKS_PERCOBAAN_CAPTURE percobaan.")
            onSelesai(null)
            return
        }

        onSelesai(bitmap)
    }

    private fun jadwalkanRetryCapture(
        webView: WebView,
        previewWidth: Int,
        percobaanKe: Int,
        onSelesai: (Bitmap?) -> Unit
    ) {
        // Delay progresif (percobaan ke-1 = 300ms, ke-2 = 600ms, dst) -- struk/halaman yang
        // lebih tinggi butuh jeda lebih lama di percobaan belakangan supaya Chromium benar-
        // benar sempat merender bagian yang tadinya di luar layar.
        Handler(Looper.getMainLooper()).postDelayed({
            capturePageDenganRetry(webView, previewWidth, percobaanKe + 1, onSelesai)
        }, JEDA_RETRY_CAPTURE_MS * percobaanKe)
    }

    /**
     * Cek apakah hasil capture masih punya PITA (band) kosong putih polos yang lebih tinggi
     * dari [BATAS_TINGGI_PITA_KOSONG_PX] -- tanda render belum selesai. Tiap baris di-scan
     * penuh (bukan sampling sparse per titik) supaya pita kosong tidak lolos terlewat cuma
     * karena sampling meleset.
     */
    private fun bitmapBelumSelesaiDirender(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        // Dibatasi maksimal ~500 baris yang benar-benar discan supaya tetap ringan walau
        // bitmap-nya sangat tinggi.
        val langkahY = maxOf(1, height / 500)
        val baris = IntArray(width)

        var tinggiPitaKosongBerturutTurut = 0
        var y = 0
        while (y < height) {
            bitmap.getPixels(baris, 0, width, 0, y, width, 1)
            val barisKosong = baris.all { pixel ->
                Color.red(pixel) >= 250 && Color.green(pixel) >= 250 && Color.blue(pixel) >= 250
            }
            if (barisKosong) {
                tinggiPitaKosongBerturutTurut += langkahY
                if (tinggiPitaKosongBerturutTurut > BATAS_TINGGI_PITA_KOSONG_PX) return true
            } else {
                tinggiPitaKosongBerturutTurut = 0
            }
            y += langkahY
        }
        return false
    }
}
