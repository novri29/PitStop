package com.pitstop.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.pitstop.pitstop.R
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

object BluetoothPrinterHelper {

    private const val TAG = "BluetoothPrinter"

    private const val REQUEST_BLUETOOTH_CONNECT = 2001

    /*
     * UUID standar Bluetooth Serial Port Profile
     * yang umum digunakan printer thermal Bluetooth.
     */
    private val PRINTER_UUID: UUID =
        UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )

    /*
     * Printer thermal 58mm umumnya memakai 384 dots lebar kertas cetak.
     * Ini dipakai untuk mode gambar (logo & layout sesuai desain HTML/CSS).
     */
    private const val PRINTER_WIDTH_DOTS = 384

    /*
     * Preview dirender lebih besar dari ukuran fisik printer (384px) supaya enak dilihat di
     * layar -- 384px asli kelihatan kecil terutama di layar density tinggi. Karena HTML struk
     * sudah punya <meta name="viewport" content="width=384">, WebView otomatis scale-up konten
     * dengan tajam (bukan di-zoom paksa / blur) mengikuti lebar View ini. Saat "Cetak Sekarang"
     * ditekan, bitmap preview yang lebih besar ini di-downscale balik ke PRINTER_WIDTH_DOTS
     * sebelum dikirim ke printer, supaya ukuran fisik hasil cetak tetap sama persis.
     *
     * FIX BUG "ukuran preview struk beda antara tablet & HP": dulu lebar ini dihitung dari
     * activity.resources.displayMetrics.widthPixels (lebar layar device), jadi preview-nya
     * ikut membesar/mengecil tergantung device dipakai untuk lihat struk yang SAMA. Sekarang
     * dibuat TETAP (tidak lagi bergantung ukuran layar) supaya preview struk konsisten di
     * device manapun -- dialog_preview_struk.xml dibungkus HorizontalScrollView sebagai jaga-
     * jaga kalau lebar tetap ini kebetulan lebih lebar dari layar device yang sangat kecil.
     */
    private const val PREVIEW_WIDTH_DOTS = (PRINTER_WIDTH_DOTS * 2.5f).toInt()

    /*
     * Logic capture WebView -> Bitmap (termasuk retry & deteksi "render belum selesai")
     * dipindah ke WebViewRenderHelper supaya bisa dipakai bersama oleh StrukPdfExporter
     * (fitur "Download PDF"), lihat komentar di sana untuk detail alasannya.
     */

    private var selectedPrinterName: String? = null

    private var selectedPrinterMac: String? = null

    /*
     * FIX BUG "hasil cetak ulang kadang karakter acak/mojibake": sebelumnya setiap kali
     * "Cetak Sekarang" (atau Test Print) ditekan, kode langsung membuka Thread baru yang
     * membuka SOCKET Bluetooth sendiri ke printer. Kalau ada 2 proses cetak yang jalan
     * bersamaan (mis. reprint beberapa struk lama berturut-turut dengan cepat, atau tap
     * dobel karena tidak sadar sudah terkirim), dua socket berbeda menulis ke printer yang
     * sama secara bersamaan -- byte dari kedua job SALING MENYISIP (interleave) di buffer
     * printer, sehingga perintah cetak gambar (GS v 0) jadi rusak/tidak dikenali dan printer
     * malah menafsirkan sisa datanya sebagai TEKS biasa -> keluar sebagai karakter acak.
     * Dengan single-thread executor ini, SEMUA job cetak (test print maupun cetak struk,
     * dari layar manapun) dipaksa berjalan satu-per-satu secara berurutan -- job berikutnya
     * baru mulai buka socket setelah job sebelumnya benar-benar selesai & socket ditutup.
     */
    private val printExecutor = Executors.newSingleThreadExecutor()

    /*
     * FIX BUG "pembeli 1 berhasil cetak, pembeli 2 cetak-nya jadi karakter acak": beda
     * dengan kasus di atas (yang soal 2 job BERBARENGAN), ini kejadian meski tiap job cetak
     * sudah berjalan berurutan (tidak overlap) -- pembeli 2 baru mulai transaksi & cetak
     * SETELAH pembeli 1 selesai. Ini gejala khas printer thermal murah: begitu 1 sesi Bluetooth
     * ditutup, firmware printer butuh JEDA SEJENAK untuk benar-benar reset & siap menerima
     * koneksi baru dengan bersih. Kalau koneksi berikutnya datang terlalu cepat (dalam hitungan
     * detik), firmware bisa "nyangkut" di state sisa sesi sebelumnya, sehingga sesi baru
     * (walau sudah kirim ESC @ reset di awal) tetap salah tafsir sebagai teks acak. Waktu
     * selesainya sesi cetak TERAKHIR dicatat di sini, lalu dipakai untuk memberi jeda minimum
     * sebelum sesi berikutnya mulai membuka socket -- lihat tungguJedaAntarSesiCetak().
     */
    @Volatile
    private var waktuSelesaiSesiCetakTerakhir: Long = 0L

    private const val JEDA_MINIMUM_ANTAR_SESI_CETAK_MS = 1200L

    private fun tungguJedaAntarSesiCetakJikaPerlu() {
        val sejakSesiTerakhir = System.currentTimeMillis() - waktuSelesaiSesiCetakTerakhir
        val sisaJeda = JEDA_MINIMUM_ANTAR_SESI_CETAK_MS - sejakSesiTerakhir
        if (sisaJeda > 0) {
            Log.d(TAG, "Memberi jeda ${sisaJeda}ms sebelum sesi cetak berikutnya (settle time printer).")
            try {
                Thread.sleep(sisaJeda)
            } catch (_: InterruptedException) {
            }
        }
    }

    /*
     * LAPIS FIX KEDUA untuk bug yang sama: ternyata walau job sudah tidak overlap (lihat
     * printExecutor di atas), job cetak KEDUA yang langsung nyambung lagi ke printer yang
     * sama TEPAT setelah job pertama menutup socket-nya masih bisa gagal handshake dan
     * hasilnya karakter acak -- ini keterbatasan umum Bluetooth Classic (RFCOMM) di Android:
     * setelah socket.close() dipanggil, stack Bluetooth & printer butuh sedikit waktu untuk
     * benar-benar "settle" sebelum siap menerima koneksi baru; socket.close() yang me-return
     * TIDAK berarti proses teardown-nya sudah 100% selesai di level radio/firmware printer.
     * Cooldown singkat ini dijalankan di akhir SETIAP job cetak (lihat pemanggilan
     * printExecutor.execute di bawah) supaya job berikutnya di antrian punya jeda "napas"
     * yang cukup sebelum mencoba connect lagi ke printer yang sama.
     */
    private const val COOLDOWN_ANTAR_JOB_CETAK_MS = 900L


    // ============================================================
    // CEK IZIN BLUETOOTH_CONNECT (Android 12+)
    // ============================================================

    /**
     * Wajib dipanggil (dan hasilnya di-cek) sebelum memanggil API Bluetooth apa pun
     * (bondedDevices, device.name, createRfcommSocketToServiceRecord, socket.connect, dll).
     * Di bawah Android 12 (API 31) permission ini tidak diperlukan sehingga selalu true.
     */
    private fun hasBluetoothConnectPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        return ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothConnectPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            REQUEST_BLUETOOTH_CONNECT
        )
        showToast(
            activity,
            "Izinkan akses Bluetooth lalu tekan Cetak lagi.",
            Toast.LENGTH_LONG
        )
    }


    // ============================================================
    // SET PRINTER
    // ============================================================

    fun setPrinter(
        name: String,
        macAddress: String
    ) {
        selectedPrinterName = name
        selectedPrinterMac = macAddress

        Log.d(TAG, "PRINTER DIPILIH -> Nama: $name, MAC: $macAddress")
    }


    // ============================================================
    // GET PRINTER
    // ============================================================

    fun getPrinterName(): String? = selectedPrinterName

    fun getPrinterMac(): String? = selectedPrinterMac


    // ============================================================
    // PRINT HTML (render -> tampilkan preview dulu -> baru cetak setelah dikonfirmasi user)
    // ============================================================

    fun printHtml(
        activity: Activity,
        html: String
    ) {
        Log.d(TAG, "printHtml() DIMULAI")

        if (!hasBluetoothConnectPermission(activity)) {
            requestBluetoothConnectPermission(activity)
            return
        }

        /*
         * Kalau printer belum dipilih, tampilkan daftar printer dulu.
         */
        if (selectedPrinterMac.isNullOrBlank()) {
            Log.d(TAG, "Printer belum dipilih.")
            showPrinterDialog(activity)
            return
        }

        showPreviewThenPrint(activity, html, selectedPrinterMac!!)
    }


    // ============================================================
    // PILIH PRINTER
    // ============================================================

    @SuppressLint("MissingPermission")
    private fun showPrinterDialog(
        activity: Activity
    ) {
        val adapter = BluetoothAdapter.getDefaultAdapter()

        if (adapter == null) {
            showToast(activity, "Tablet tidak mendukung Bluetooth.", Toast.LENGTH_LONG)
            return
        }

        if (!hasBluetoothConnectPermission(activity)) {
            requestBluetoothConnectPermission(activity)
            return
        }

        if (!adapter.isEnabled) {
            showToast(activity, "Bluetooth belum aktif.", Toast.LENGTH_LONG)
            return
        }

        val devices: List<BluetoothDevice>
        try {
            devices = adapter.bondedDevices.toList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Tidak bisa membaca paired device.", e)
            showToast(activity, "Tidak memiliki izin Bluetooth.", Toast.LENGTH_LONG)
            return
        }

        if (devices.isEmpty()) {
            showToast(
                activity,
                "Belum ada printer Bluetooth yang dipairing.",
                Toast.LENGTH_LONG
            )
            return
        }

        val names = devices.map { device ->
            try {
                val name = device.name ?: "Bluetooth Device"
                "$name\n${device.address}"
            } catch (e: SecurityException) {
                "Bluetooth Device"
            }
        }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle("Pilih Printer Struk")
            .setItems(names) { _, which ->
                val device = devices[which]
                try {
                    val name = device.name ?: "Bluetooth Printer"
                    setPrinter(name, device.address)
                    showToast(activity, "Printer $name dipilih.", Toast.LENGTH_SHORT)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Gagal mendapatkan informasi printer.", e)
                    showToast(activity, "Izin Bluetooth belum diberikan.", Toast.LENGTH_LONG)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }


    // ============================================================
    // RENDER HTML -> BITMAP -> TAMPILKAN PREVIEW -> KIRIM SETELAH DIKONFIRMASI
    // ============================================================

    private fun showPreviewThenPrint(
        activity: Activity,
        html: String,
        macAddress: String
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_preview_struk, null)
        val progressPreview = dialogView.findViewById<ProgressBar>(R.id.progressPreview)
        val tvStatusPreview = dialogView.findViewById<TextView>(R.id.tvStatusPreview)
        val containerWebViewPreview = dialogView.findViewById<FrameLayout>(R.id.containerWebViewPreview)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatalPreview)
        val btnCetak = dialogView.findViewById<Button>(R.id.btnCetakPreview)

        val webView = WebView(activity)
        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = false
        webView.settings.useWideViewPort = true
        webView.setInitialScale(100)
        webView.setBackgroundColor(Color.WHITE)
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        val webViewWidth = PREVIEW_WIDTH_DOTS

        containerWebViewPreview.addView(
            webView,
            FrameLayout.LayoutParams(webViewWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val dialog = Dialog(activity)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.setOnDismissListener {
            webView.stopLoading()
            webView.destroy()
        }

        var bitmapSiapCetak: Bitmap? = null

        btnBatal.setOnClickListener { dialog.dismiss() }
        btnCetak.setOnClickListener {
            val bmpPreview = bitmapSiapCetak
            if (bmpPreview != null) {
                btnCetak.isEnabled = false
                dialog.dismiss()
                val tinggiCetak = (bmpPreview.height.toFloat() * PRINTER_WIDTH_DOTS / bmpPreview.width).toInt()
                val bmpUntukCetak = Bitmap.createScaledBitmap(bmpPreview, PRINTER_WIDTH_DOTS, tinggiCetak, true)
                printExecutor.execute { sendBitmapToPrinter(activity, macAddress, bmpUntukCetak) }
            }
        }

        dialog.show()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.postVisualStateCallback(0L, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        if (!dialog.isShowing) return

                        webView.post {
                            if (!dialog.isShowing) return@post
                            captureWebViewDenganRetry(
                                webView = webView,
                                previewWidth = webViewWidth,
                                progressPreview = progressPreview,
                                tvStatusPreview = tvStatusPreview,
                                btnCetak = btnCetak,
                                percobaanKe = 1,
                                onBitmapSiap = { bitmap -> bitmapSiapCetak = bitmap }
                            )
                        }
                    }
                })
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    /** Wrapper [WebViewRenderHelper.capturePageDenganRetry] khusus preview struk satuan -- juga mengurus progress bar & status text di dialog. */
    private fun captureWebViewDenganRetry(
        webView: WebView,
        previewWidth: Int,
        progressPreview: ProgressBar,
        tvStatusPreview: TextView,
        btnCetak: Button,
        percobaanKe: Int,
        onBitmapSiap: (Bitmap) -> Unit
    ) {
        WebViewRenderHelper.capturePageDenganRetry(webView, previewWidth, percobaanKe) { bitmap ->
            if (bitmap == null) {
                progressPreview.visibility = View.GONE
                tvStatusPreview.visibility = View.VISIBLE
                tvStatusPreview.text = "Gagal membuat preview struk, coba lagi."
                btnCetak.isEnabled = false
                return@capturePageDenganRetry
            }

            progressPreview.visibility = View.GONE
            tvStatusPreview.visibility = View.GONE
            btnCetak.isEnabled = true

            onBitmapSiap(bitmap)
        }
    }

    fun testPrint(activity: Activity) {
        if (selectedPrinterMac.isNullOrBlank()) {
            showPrinterDialog(activity)
            return
        }

        val testText = """
            PITSTOP
            Cafe

            No. PIT : TEST-000001
            Tanggal : 22 Agu 2026
            Kasir : TEST

            --------------------------------
            TEST PRINT
            --------------------------------

            Printer : ${selectedPrinterName ?: "RPP02N"}
            Bluetooth : OK
            Text Print : OK
            --------------------------------

            Terima kasih atas kunjungan Anda
            Jl. Turi Raya No.102
            Tanjung Senang

            ~ Pitstop ~
        """.trimIndent()

        printExecutor.execute {
            sendTextToPrinter(activity, selectedPrinterMac!!, testText)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(activity: Activity, macAddress: String): BluetoothSocket {
        if (!hasBluetoothConnectPermission(activity)) {
            throw SecurityException("Izin BLUETOOTH_CONNECT belum diberikan.")
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: throw IOException("Bluetooth tidak tersedia.")

        val device = adapter.getRemoteDevice(macAddress)

        try {
            adapter.cancelDiscovery()
        } catch (e: Exception) {
            Log.w(TAG, "Gagal cancel discovery.", e)
        }

        var socket: BluetoothSocket
        try {
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            Log.d(TAG, "Bluetooth BERHASIL TERHUBUNG (secure).")
        } catch (e: Exception) {
            Log.w(TAG, "Secure connection gagal, coba insecure.", e)
            socket = device.createInsecureRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            Log.d(TAG, "Bluetooth BERHASIL TERHUBUNG (insecure).")
        }
        return socket
    }

    @SuppressLint("MissingPermission")
    private fun sendBitmapToPrinter(
        activity: Activity,
        macAddress: String,
        bitmap: Bitmap
    ) {
        if (!hasBluetoothConnectPermission(activity)) {
            showToast(activity, "Izin Bluetooth tidak tersedia.", Toast.LENGTH_LONG)
            return
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            tungguJedaAntarSesiCetakJikaPerlu()

            socket = openSocket(activity, macAddress)
            outputStream = socket.outputStream

            // Reset printer
            outputStream.write(byteArrayOf(0x1B, 0x40))
            outputStream.flush()
            Thread.sleep(150)

            writeInChunks(outputStream, bitmapToEscPos(bitmap))

            // Feed paper
            outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A))
            outputStream.flush()

            // [PERBAIKAN] JEDA DINAMIS
            // Hitung estimasi waktu cetak fisik berdasarkan tinggi gambar.
            // Asumsi kecepatan printer thermal portabel: ~400 dot/pixel per detik.
            val estimasiWaktuCetakMs = (bitmap.height / 400.0 * 1000).toLong()

            // Tambahkan waktu penyangga (buffer) 1 detik, dengan batas minimal delay 2500ms.
            val delayAman = maxOf(2500L, estimasiWaktuCetakMs + 1000L)

            Log.d(TAG, "Menunggu ${delayAman}ms agar fisik printer selesai mencetak sebelum socket ditutup...")
            Thread.sleep(delayAman)
            // [AKHIR PERBAIKAN]

            showToast(activity, "Struk berhasil dikirim ke printer.", Toast.LENGTH_SHORT)

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException Bluetooth.", e)
            showToast(activity, "Izin Bluetooth tidak tersedia.", Toast.LENGTH_LONG)
        } catch (e: IOException) {
            Log.e(TAG, "IOException saat print.", e)
            showToast(activity, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG)
        } catch (e: Exception) {
            Log.e(TAG, "ERROR saat print.", e)
            showToast(activity, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG)
        } finally {
            try { outputStream?.flush() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
            waktuSelesaiSesiCetakTerakhir = System.currentTimeMillis()
        }
    }

    private fun writeInChunks(
        outputStream: OutputStream,
        data: ByteArray,
        chunkSize: Int = 1024
    ) {
        var offset = 0
        while (offset < data.size) {
            val length = minOf(chunkSize, data.size - offset)
            outputStream.write(data, offset, length)
            outputStream.flush()
            offset += length

            // [PERBAIKAN] PELAMBATAN PACING DATA
            // Naikkan jeda dari 10ms menjadi 40ms agar memori
            // hardware printer punya cukup waktu untuk mengosongkan antrean.
            Thread.sleep(40)
            // [AKHIR PERBAIKAN]
        }
    }

    private fun bitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val data = ArrayList<Byte>()

        // GS v 0 m xL xH yL yH d1...dk
        data.add(0x1D)
        data.add(0x76)
        data.add(0x30)
        data.add(0x00)

        data.add((widthBytes and 0xFF).toByte())
        data.add(((widthBytes shr 8) and 0xFF).toByte())
        data.add((height and 0xFF).toByte())
        data.add(((height shr 8) and 0xFF).toByte())

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var value = 0
                for (bit in 0..7) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        if (gray < 160) {
                            value = value or (1 shl (7 - bit))
                        }
                    }
                }
                data.add(value.toByte())
            }
        }

        return data.toByteArray()
    }

    @SuppressLint("MissingPermission")
    private fun sendTextToPrinter(
        activity: Activity,
        macAddress: String,
        text: String
    ) {
        if (!hasBluetoothConnectPermission(activity)) {
            showToast(activity, "Izin Bluetooth tidak tersedia.", Toast.LENGTH_LONG)
            return
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            tungguJedaAntarSesiCetakJikaPerlu()

            socket = openSocket(activity, macAddress)
            outputStream = socket.outputStream

            outputStream.write(byteArrayOf(0x1B, 0x40))
            outputStream.flush()
            Thread.sleep(150)

            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00))
            outputStream.write(byteArrayOf(0x1B, 0x45, 0x00))

            val textBytes = text.toByteArray(charset("windows-1252"))
            writeInChunks(outputStream, textBytes)

            Thread.sleep(300)

            outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
            outputStream.flush()

            Thread.sleep(500)

            showToast(activity, "Test print berhasil dikirim.", Toast.LENGTH_SHORT)

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException Bluetooth.", e)
            showToast(activity, "Izin Bluetooth tidak tersedia.", Toast.LENGTH_LONG)
        } catch (e: IOException) {
            Log.e(TAG, "IOException saat print.", e)
            showToast(activity, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG)
        } catch (e: Exception) {
            Log.e(TAG, "ERROR saat print.", e)
            showToast(activity, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG)
        } finally {
            try { outputStream?.flush() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
            waktuSelesaiSesiCetakTerakhir = System.currentTimeMillis()
        }
    }

    private fun showToast(
        activity: Activity,
        message: String,
        duration: Int
    ) {
        Handler(Looper.getMainLooper()).post {
            if (!activity.isFinishing && !activity.isDestroyed) {
                Toast.makeText(activity, message, duration).show()
            }
        }
    }
}