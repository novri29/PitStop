package com.pitstop.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
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
    // PRINT HTML (mode gambar, supaya logo & layout sesuai desain)
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

        renderHtmlToBitmapAndPrint(activity, html, selectedPrinterMac!!)
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
    // RENDER HTML -> BITMAP -> KIRIM SEBAGAI GAMBAR (logo & layout ikut tercetak)
    // ============================================================

    private fun renderHtmlToBitmapAndPrint(
        activity: Activity,
        html: String,
        macAddress: String
    ) {
        val webView = WebView(activity)
        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = false
        webView.settings.useWideViewPort = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                webView.postDelayed({
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(
                        PRINTER_WIDTH_DOTS,
                        View.MeasureSpec.EXACTLY
                    )
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(
                        0,
                        View.MeasureSpec.UNSPECIFIED
                    )

                    webView.measure(widthSpec, heightSpec)

                    val width = PRINTER_WIDTH_DOTS
                    val height = webView.measuredHeight

                    if (height <= 0) {
                        showToast(activity, "Gagal membuat gambar struk.", Toast.LENGTH_LONG)
                        return@postDelayed
                    }

                    webView.layout(0, 0, width, height)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    webView.draw(canvas)

                    Thread {
                        sendBitmapToPrinter(activity, macAddress, bitmap)
                    }.start()

                }, 500)
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
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