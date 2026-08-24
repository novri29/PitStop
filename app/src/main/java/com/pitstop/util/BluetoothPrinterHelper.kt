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
import android.graphics.Canvas
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
     * layar HP -- 384px asli kelihatan kecil terutama di layar density tinggi. Karena HTML
     * struk sudah punya <meta name="viewport" content="width=384">, WebView otomatis
     * scale-up konten dengan tajam (bukan di-zoom paksa / blur) mengikuti lebar View ini.
     * Saat "Cetak Sekarang" ditekan, bitmap preview yang lebih besar ini di-downscale balik
     * ke PRINTER_WIDTH_DOTS sebelum dikirim ke printer, supaya ukuran fisik hasil cetak
     * tetap sama persis seperti sebelumnya.
     */
    private const val PREVIEW_SCALE = 2.5f

    /**
     * Hitung lebar preview yang aman untuk device manapun: idealnya PRINTER_WIDTH_DOTS x
     * PREVIEW_SCALE, tapi dibatasi maksimal 90% lebar layar supaya tidak overflow di HP
     * dengan resolusi/densitas kecil, dan minimal PRINTER_WIDTH_DOTS itu sendiri.
     */
    private fun previewWidthDotsUntuk(activity: Activity): Int {
        val target = (PRINTER_WIDTH_DOTS * PREVIEW_SCALE).toInt()
        val maksimalDiLayar = (activity.resources.displayMetrics.widthPixels * 0.9f).toInt()
        return target.coerceAtMost(maksimalDiLayar).coerceAtLeast(PRINTER_WIDTH_DOTS)
    }

    private var selectedPrinterName: String? = null

    private var selectedPrinterMac: String? = null


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

    /**
     * Render HTML struk ke [Bitmap] lalu tampilkan dialog preview sebelum benar-benar
     * dikirim ke printer. Tombol "Cetak Sekarang" baru aktif setelah render benar-benar
     * selesai (dideteksi lewat [WebView.postVisualStateCallback], BUKAN delay tebakan seperti
     * sebelumnya), dan bitmap yang ditampilkan di preview adalah PERSIS bitmap yang dikirim
     * ke printer -- jadi WYSIWYG: apa yang tampil di preview = apa yang keluar di kertas.
     *
     * WebView-nya sengaja ditempel ke dalam hierarchy dialog ini (bukan dibuat lepas tanpa
     * parent seperti implementasi sebelumnya), karena postVisualStateCallback() hanya bisa
     * diandalkan kalau WebView benar-benar bagian dari window yang aktif.
     */
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
        // FIX BUG "teks/kolom kanan kepotong": harus true supaya WebView memakai lebar
        // yang kita tentukan lewat <meta name="viewport" content="width=384"> di HTML
        // sebagai acuan CSS viewport, BUKAN lebar View dalam satuan dp (yang nilainya beda-beda
        // tergantung density layar device, itulah penyebab kolom kanan struk kepotong).
        webView.settings.useWideViewPort = true
        // Kunci skala render ke 100% supaya tidak ada scaling tambahan dari density layar --
        // 384 CSS px di HTML harus persis sama dengan 384 pixel fisik yang kita capture.
        webView.setInitialScale(100)
        webView.setBackgroundColor(Color.WHITE)

        // FIX BUG "hasil preview putih kosong": paksa WebView pakai software rendering.
        // WebView modern (berbasis Chromium) merender via hardware compositor terpisah, dan
        // canvas.draw() ke Bitmap biasa (software canvas) bisa gagal menangkap isinya kalau
        // WebView masih pakai hardware layer -- hasilnya blank putih. Dengan LAYER_TYPE_SOFTWARE,
        // WebView dipaksa gambar ke software canvas, jadi captured Bitmap-nya benar isinya.
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // Ditempel LANGSUNG ke area yang benar-benar tampil di layar (bukan container
        // tersembunyi 0x0 seperti sebelumnya) -- WebView bisa menganggap dirinya "tidak
        // terlihat" kalau parent-nya berukuran nol, dan ikut tidak merender apa pun.
        val webViewWidth = previewWidthDotsUntuk(activity)

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
        // Kalau dialog ditutup (Batal / ditekan di luar) sebelum render selesai,
        // hentikan WebView supaya tidak terus bekerja & membocorkan resource di background.
        dialog.setOnDismissListener {
            webView.stopLoading()
            webView.destroy()
        }

        var bitmapSiapCetak: Bitmap? = null

        btnBatal.setOnClickListener { dialog.dismiss() }
        btnCetak.setOnClickListener {
            val bmpPreview = bitmapSiapCetak
            if (bmpPreview != null) {
                dialog.dismiss()
                // Bitmap preview ukurannya diperbesar biar enak dilihat. Downscale dulu ke
                // PRINTER_WIDTH_DOTS (ukuran fisik printer sesungguhnya) sebelum dikirim,
                // supaya hasil cetak fisiknya tetap pas di kertas 58mm.
                val tinggiCetak = (bmpPreview.height.toFloat() * PRINTER_WIDTH_DOTS / bmpPreview.width).toInt()
                val bmpUntukCetak = Bitmap.createScaledBitmap(bmpPreview, PRINTER_WIDTH_DOTS, tinggiCetak, true)
                Thread { sendBitmapToPrinter(activity, macAddress, bmpUntukCetak) }.start()
            }
        }

        dialog.show()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Menunggu VISUAL STATE benar-benar siap digambar -- ini deteksi render
                // selesai yang SEBENARNYA, menggantikan delay tebakan 500ms yang lama.
                // onPageFinished() sendiri TIDAK menjamin layout & paint sudah selesai,
                // itulah sumber bug "hasil cetak kadang tidak sesuai desain" sebelumnya.
                webView.postVisualStateCallback(0L, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        if (!dialog.isShowing) return // dialog sudah ditutup user, tidak perlu lanjut
                        renderWebViewKeBitmapDanTampilkan(
                            webView = webView,
                            previewWidth = webViewWidth,
                            progressPreview = progressPreview,
                            tvStatusPreview = tvStatusPreview,
                            btnCetak = btnCetak,
                            onBitmapSiap = { bitmap -> bitmapSiapCetak = bitmap }
                        )
                    }
                })
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    /**
     * Ukur & gambar isi WebView (yang sudah dipastikan selesai render lewat
     * postVisualStateCallback) menjadi [Bitmap] berukuran preview (lebih besar dari ukuran
     * fisik printer, supaya enak dilihat). WebView tetap ditampilkan hidup di layar sebagai
     * preview-nya sendiri, ukurannya di-set ulang di sini supaya proporsinya sama persis
     * dengan bitmap yang ditangkap. Bitmap preview ini di-downscale ke ukuran fisik printer
     * (PRINTER_WIDTH_DOTS) baru saat benar-benar dikirim ke printer -- lihat callback
     * onBitmapSiap() di showPreviewThenPrint().
     */
    private fun renderWebViewKeBitmapDanTampilkan(
        webView: WebView,
        previewWidth: Int,
        progressPreview: ProgressBar,
        tvStatusPreview: TextView,
        btnCetak: Button,
        onBitmapSiap: (Bitmap) -> Unit
    ) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(previewWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        webView.measure(widthSpec, heightSpec)

        val width = previewWidth
        val height = webView.measuredHeight

        if (height <= 0) {
            progressPreview.visibility = View.GONE
            tvStatusPreview.text = "Gagal membuat preview struk."
            return
        }

        webView.layout(0, 0, width, height)
        // Samakan ukuran tampil WebView di layar dengan ukuran hasil measure, supaya tidak
        // terpotong/kosong di bagian bawah saat di-scroll.
        webView.layoutParams = FrameLayout.LayoutParams(width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        webView.draw(canvas)

        progressPreview.visibility = View.GONE
        tvStatusPreview.visibility = View.GONE
        btnCetak.isEnabled = true

        onBitmapSiap(bitmap)
    }


    // ============================================================
    // TEST PRINT (tetap mode teks, cukup untuk cek koneksi)
    // ============================================================

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

        Thread {
            sendTextToPrinter(activity, selectedPrinterMac!!, testText)
        }.start()
    }


    // ============================================================
    // KONEKSI: SECURE lalu fallback INSECURE RFCOMM
    // ============================================================

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


    // ============================================================
    // KIRIM GAMBAR STRUK (logo + layout sesuai desain HTML/CSS)
    // ============================================================

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
            socket = openSocket(activity, macAddress)
            outputStream = socket.outputStream

            // Reset printer
            outputStream.write(byteArrayOf(0x1B, 0x40))
            outputStream.flush()
            Thread.sleep(150)

            // Kirim gambar per-chunk supaya buffer printer tidak overflow
            // (penyebab umum "printer terdeteksi tapi tidak keluar cetakan").
            writeInChunks(outputStream, bitmapToEscPos(bitmap))

            // Feed paper
            outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A))
            outputStream.flush()

            // Beri jeda supaya printer sempat selesai mencetak sebelum socket ditutup
            Thread.sleep(500)

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
        }
    }

    /**
     * Kirim data besar ke printer per-potongan kecil (chunk) dengan jeda singkat.
     * Mencegah buffer printer/Bluetooth stack overflow yang membuat sebagian data
     * hilang saat dikirim sekaligus.
     */
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
            Thread.sleep(10)
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


    // ============================================================
    // KIRIM TEKS (dipakai untuk Test Print saja)
    // ============================================================

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
            socket = openSocket(activity, macAddress)
            outputStream = socket.outputStream

            outputStream.write(byteArrayOf(0x1B, 0x40))
            outputStream.flush()
            Thread.sleep(150)

            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // align left
            outputStream.write(byteArrayOf(0x1B, 0x45, 0x00)) // bold off

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
        }
    }


    // ============================================================
    // TOAST
    // ============================================================

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