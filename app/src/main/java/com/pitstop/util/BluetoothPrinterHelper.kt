package com.pitstop.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import java.util.UUID

object BluetoothPrinterHelper {

    private const val TAG = "BluetoothPrinter"

    private const val REQUEST_BLUETOOTH_CONNECT = 2001

    // UUID standar Bluetooth Serial Port Profile (SPP)
    private val PRINTER_UUID: UUID =
        UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )

    // Printer thermal 58mm
    // Umumnya 384 dots
    private const val PRINTER_WIDTH = 384


    // =========================================================
    // PRINT HTML
    // =========================================================

    fun printHtml(
        activity: Activity,
        html: String
    ) {

        Log.d(TAG, "====================================")
        Log.d(TAG, "printHtml() DIMULAI")
        Log.d(TAG, "====================================")


        // =====================================================
        // CEK PERMISSION BLUETOOTH
        // =====================================================

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(
                TAG,
                "BLUETOOTH_CONNECT belum diizinkan"
            )

            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                REQUEST_BLUETOOTH_CONNECT
            )

            Toast.makeText(
                activity,
                "Izinkan akses Bluetooth lalu tekan cetak lagi.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // =====================================================
        // CEK BLUETOOTH
        // =====================================================

        val adapter =
            BluetoothAdapter.getDefaultAdapter()

        if (adapter == null) {

            Toast.makeText(
                activity,
                "Perangkat tidak mendukung Bluetooth.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        if (!adapter.isEnabled) {

            Toast.makeText(
                activity,
                "Bluetooth belum aktif.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // =====================================================
        // AMBIL DEVICE YANG SUDAH DIPAIRING
        // =====================================================

        val devices =
            try {

                adapter.bondedDevices.toList()

            } catch (e: SecurityException) {

                Log.e(
                    TAG,
                    "Tidak bisa membaca paired device.",
                    e
                )

                Toast.makeText(
                    activity,
                    "Tidak memiliki izin Bluetooth.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }


        if (devices.isEmpty()) {

            Toast.makeText(
                activity,
                "Belum ada printer Bluetooth yang dipairing.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        Log.d(
            TAG,
            "Jumlah paired device: ${devices.size}"
        )


        // =====================================================
        // BUAT LIST DEVICE
        // =====================================================

        val names =
            devices.map { device ->

                val deviceName =
                    try {

                        device.name
                            ?: "Bluetooth Device"

                    } catch (
                        _: SecurityException
                    ) {

                        "Bluetooth Device"
                    }

                "$deviceName\n${device.address}"

            }.toTypedArray()


        // =====================================================
        // DIALOG PILIH PRINTER
        // =====================================================

        AlertDialog.Builder(activity)

            .setTitle(
                "Pilih Printer Struk"
            )

            .setItems(
                names
            ) { _, which ->

                val selectedDevice =
                    devices[which]


                val deviceName =
                    try {

                        selectedDevice.name
                            ?: "Bluetooth Device"

                    } catch (
                        _: SecurityException
                    ) {

                        "Bluetooth Device"
                    }


                Log.d(
                    TAG,
                    "===================================="
                )

                Log.d(
                    TAG,
                    "PRINTER DIPILIH"
                )

                Log.d(
                    TAG,
                    "Nama: $deviceName"
                )

                Log.d(
                    TAG,
                    "MAC: ${selectedDevice.address}"
                )

                Log.d(
                    TAG,
                    "===================================="
                )


                Toast.makeText(
                    activity,
                    "Printer dipilih: $deviceName",
                    Toast.LENGTH_SHORT
                ).show()


                // =================================================
                // RENDER HTML
                // =================================================

                renderHtmlToBitmap(
                    activity = activity,
                    html = html,
                    device = selectedDevice
                )
            }

            .setNegativeButton(
                "Batal",
                null
            )

            .show()
    }


    // =========================================================
    // TES CETAK TEKS POLOS (KHUSUS DEBUGGING)
    // =========================================================
    //
    // Fungsi ini SENGAJA tidak menyentuh WebView atau bitmap
    // sama sekali — cuma kirim teks ASCII biasa. Gunanya untuk
    // memastikan printer ini memang bisa mencetak APAPUN lewat
    // Bluetooth SPP + ESC/POS, sebelum lanjut curigai kode
    // bitmap/ESC * / GS v0.
    //
    // Panggil BluetoothPrinterHelper.printTestText(activity)
    // dari tombol mana saja (sementara, khusus buat tes).
    //
    // =========================================================

    @SuppressLint("MissingPermission")
    fun printTestText(activity: Activity) {

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Toast.makeText(
                activity,
                "Izin Bluetooth belum diberikan.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val adapter =
            BluetoothAdapter.getDefaultAdapter()

        if (adapter == null || !adapter.isEnabled) {

            Toast.makeText(
                activity,
                "Bluetooth tidak aktif.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val devices =
            try {

                adapter.bondedDevices.toList()

            } catch (e: SecurityException) {

                Toast.makeText(
                    activity,
                    "Tidak memiliki izin Bluetooth.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }


        if (devices.isEmpty()) {

            Toast.makeText(
                activity,
                "Belum ada printer Bluetooth yang dipairing.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val names =
            devices.map { device ->

                val deviceName =
                    try {
                        device.name ?: "Bluetooth Device"
                    } catch (_: SecurityException) {
                        "Bluetooth Device"
                    }

                "$deviceName\n${device.address}"

            }.toTypedArray()


        AlertDialog.Builder(activity)

            .setTitle("Pilih Printer (Tes Teks)")

            .setItems(names) { _, which ->

                val device = devices[which]

                Thread {

                    try {

                        val socket =
                            try {

                                device
                                    .createRfcommSocketToServiceRecord(PRINTER_UUID)
                                    .also { it.connect() }

                            } catch (secureException: Exception) {

                                device
                                    .createInsecureRfcommSocketToServiceRecord(PRINTER_UUID)
                                    .also { it.connect() }
                            }


                        val out =
                            socket.outputStream


                        // Reset printer
                        out.write(byteArrayOf(0x1B, 0x40))
                        out.flush()

                        Thread.sleep(100)


                        // Teks ASCII polos — TIDAK ada WebView,
                        // TIDAK ada bitmap, TIDAK ada ESC*/GS v0
                        val text =
                            "TES CETAK - PITSTOP\n" +
                                    "Kalau baris ini muncul,\n" +
                                    "printer & kertas OK.\n" +
                                    "Berarti masalahnya ada di\n" +
                                    "bagian bitmap/gambar.\n\n\n"

                        out.write(
                            text.toByteArray(Charsets.US_ASCII)
                        )
                        out.flush()

                        Thread.sleep(300)

                        out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                        out.flush()

                        Thread.sleep(300)

                        out.close()
                        socket.close()


                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                "Tes teks terkirim.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {

                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                "Gagal tes cetak:\n${e.javaClass.simpleName}\n${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                }.start()
            }

            .setNegativeButton("Batal", null)

            .show()
    }


    // =========================================================
    // HTML → WEBVIEW → BITMAP
    // =========================================================

    private fun renderHtmlToBitmap(
        activity: Activity,
        html: String,
        device: BluetoothDevice
    ) {

        Log.d(
            TAG,
            "renderHtmlToBitmap() DIMULAI"
        )


        val webView =
            WebView(activity)


        // =====================================================
        // WEBVIEW SETTINGS
        // =====================================================

        webView.settings.apply {

            javaScriptEnabled = true

            loadWithOverviewMode = false

            useWideViewPort = false

            setSupportZoom(false)

            builtInZoomControls = false

            displayZoomControls = false

            defaultFontSize = 14

            defaultFixedFontSize = 14
        }


        webView.setBackgroundColor(
            Color.WHITE
        )


        // =====================================================
        // WEBVIEW CLIENT
        // =====================================================

        webView.webViewClient =
            object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )


                    Log.d(
                        TAG,
                        "WebView onPageFinished()"
                    )


                    if (view == null) {

                        Log.e(
                            TAG,
                            "WebView = null"
                        )

                        return
                    }


                    Handler(
                        Looper.getMainLooper()
                    ).postDelayed({

                        createBitmapFromWebView(
                            activity = activity,
                            webView = view,
                            device = device
                        )

                    }, 700)
                }
            }


        // =====================================================
        // HTML UNTUK KERTAS 58MM
        // =====================================================

        val finalHtml = """
 
            <!DOCTYPE html>
 
            <html>
 
            <head>
 
                <meta
                    name="viewport"
                    content="width=384,
                    initial-scale=1.0,
                    maximum-scale=1.0,
                    user-scalable=no"
                >
 
                <style>
 
                    * {
                        box-sizing: border-box;
                    }
 
                    html {
                        margin: 0;
                        padding: 0;
                        width: 384px;
                        background: #ffffff;
                    }
 
                    body {
                        margin: 0;
                        padding: 8px;
                        width: 384px;
                        background: #ffffff;
                        color: #000000;
 
                        font-family:
                            Arial,
                            Helvetica,
                            sans-serif;
 
                        font-size: 14px;
                    }
 
                    img {
                        max-width: 100%;
                        height: auto;
                    }
 
                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }
 
                    td,
                    th {
                        color: #000000;
                    }
 
                </style>
 
            </head>
 
            <body>
 
                $html
 
            </body>
 
            </html>
 
        """.trimIndent()


        Log.d(
            TAG,
            "Memuat HTML ke WebView..."
        )


        webView.loadDataWithBaseURL(
            null,
            finalHtml,
            "text/html",
            "UTF-8",
            null
        )
    }


    // =========================================================
    // WEBVIEW → BITMAP
    // =========================================================

    private fun createBitmapFromWebView(
        activity: Activity,
        webView: WebView,
        device: BluetoothDevice
    ) {

        try {

            Log.d(
                TAG,
                "Membuat bitmap..."
            )


            // =================================================
            // UKUR WEBVIEW
            // =================================================

            val widthSpec =
                View.MeasureSpec.makeMeasureSpec(
                    PRINTER_WIDTH,
                    View.MeasureSpec.EXACTLY
                )


            val heightSpec =
                View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED
                )


            webView.measure(
                widthSpec,
                heightSpec
            )


            val width =
                PRINTER_WIDTH


            val height =
                webView.measuredHeight


            Log.d(
                TAG,
                "Bitmap size: ${width} x ${height}"
            )


            if (height <= 0) {

                Log.e(
                    TAG,
                    "Tinggi bitmap = 0"
                )

                Toast.makeText(
                    activity,
                    "Gagal membuat gambar struk.",
                    Toast.LENGTH_LONG
                ).show()

                webView.destroy()

                return
            }


            // =================================================
            // LAYOUT WEBVIEW
            // =================================================

            webView.layout(
                0,
                0,
                width,
                height
            )


            // =================================================
            // BUAT BITMAP
            // =================================================

            val bitmap =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )


            val canvas =
                Canvas(bitmap)


            canvas.drawColor(
                Color.WHITE
            )


            webView.draw(
                canvas
            )


            Log.d(
                TAG,
                "Bitmap berhasil dibuat."
            )


            Log.d(
                TAG,
                "Ukuran bitmap: ${bitmap.width} x ${bitmap.height}"
            )


            Toast.makeText(
                activity,
                "Menghubungkan ke printer...",
                Toast.LENGTH_SHORT
            ).show()


            webView.destroy()


            // =================================================
            // KIRIM KE PRINTER
            // =================================================

            Thread {

                sendBitmapToPrinter(
                    activity = activity,
                    device = device,
                    bitmap = bitmap
                )

            }.start()


        } catch (e: Exception) {

            Log.e(
                TAG,
                "Gagal membuat bitmap.",
                e
            )


            activity.runOnUiThread {

                Toast.makeText(
                    activity,
                    "Gagal membuat gambar struk:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // BLUETOOTH CONNECT + PRINT
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun sendBitmapToPrinter(
        activity: Activity,
        device: BluetoothDevice,
        bitmap: Bitmap
    ) {

        Log.d(
            TAG,
            "===================================="
        )

        Log.d(
            TAG,
            "sendBitmapToPrinter() DIMULAI"
        )

        Log.d(
            TAG,
            "Printer: ${device.name}"
        )

        Log.d(
            TAG,
            "MAC: ${device.address}"
        )


        /*
         * Socket dibuat non-null.
         */
        val socket: BluetoothSocket


        try {

            // =================================================
            // HENTIKAN BLUETOOTH DISCOVERY
            // =================================================

            val adapter =
                BluetoothAdapter.getDefaultAdapter()


            try {

                if (adapter.isDiscovering) {

                    adapter.cancelDiscovery()

                    Log.d(
                        TAG,
                        "Bluetooth discovery dihentikan."
                    )
                }

            } catch (
                _: SecurityException
            ) {
            }


            // =================================================
            // SECURE RFCOMM
            // =================================================

            Log.d(
                TAG,
                "Mencoba SECURE RFCOMM..."
            )


            socket =
                try {

                    device
                        .createRfcommSocketToServiceRecord(
                            PRINTER_UUID
                        )
                        .also {

                            it.connect()
                        }

                } catch (
                    secureException: Exception
                ) {

                    Log.e(
                        TAG,
                        "Secure RFCOMM gagal.",
                        secureException
                    )


                    // =================================================
                    // INSECURE RFCOMM
                    // =================================================

                    Log.d(
                        TAG,
                        "Mencoba INSECURE RFCOMM..."
                    )


                    device
                        .createInsecureRfcommSocketToServiceRecord(
                            PRINTER_UUID
                        )
                        .also {

                            it.connect()
                        }
                }


            Log.d(
                TAG,
                "Bluetooth BERHASIL TERHUBUNG."
            )


            // =================================================
            // OUTPUT STREAM
            // =================================================

            val outputStream =
                socket.outputStream


            Log.d(
                TAG,
                "OutputStream berhasil dibuat."
            )


            // =================================================
            // RESET PRINTER
            // =================================================

            outputStream.write(
                byteArrayOf(
                    0x1B,
                    0x40
                )
            )

            outputStream.flush()


            Thread.sleep(150)


            // =================================================
            // BITMAP → ESC/POS
            // =================================================

            Log.d(
                TAG,
                "Mengubah bitmap menjadi ESC/POS..."
            )


            val printData =
                bitmapToEscPos(
                    bitmap
                )


            Log.d(
                TAG,
                "Ukuran data print: ${printData.size} bytes"
            )


            // =================================================
            // KIRIM DATA
            // =================================================

            outputStream.write(
                printData
            )

            outputStream.flush()


            Log.d(
                TAG,
                "DATA BITMAP BERHASIL DIKIRIM."
            )


            Thread.sleep(300)


            // =================================================
            // FEED PAPER
            // =================================================

            outputStream.write(
                byteArrayOf(
                    0x0A,
                    0x0A,
                    0x0A,
                    0x0A
                )
            )

            outputStream.flush()


            Thread.sleep(500)


            // =================================================
            // CLOSE
            // =================================================

            outputStream.close()

            socket.close()


            Log.d(
                TAG,
                "Socket ditutup."
            )


            // =================================================
            // SUCCESS
            // =================================================

            activity.runOnUiThread {

                Toast.makeText(
                    activity,
                    "Struk berhasil dikirim ke printer.",
                    Toast.LENGTH_LONG
                ).show()
            }


        } catch (e: Exception) {

            Log.e(
                TAG,
                "GAGAL MENCETAK.",
                e
            )


            activity.runOnUiThread {

                Toast.makeText(
                    activity,
                    "Gagal mencetak:\n${e.javaClass.simpleName}\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // BITMAP → ESC/POS
    // =========================================================
    //
    // Menggunakan GS v 0 (raster bit image) — lihat penjelasan
    // lengkap di dalam fungsi, tepat sebelum data dibentuk.
    //
    // =========================================================

    private fun bitmapToEscPos(
        originalBitmap: Bitmap
    ): ByteArray {

        Log.d(
            TAG,
            "bitmapToEscPos() DIMULAI"
        )


        // =====================================================
        // PASTIKAN LEBAR 384 DOT
        // =====================================================

        val bitmap =
            if (
                originalBitmap.width != PRINTER_WIDTH
            ) {

                val ratio =
                    PRINTER_WIDTH.toFloat() /
                            originalBitmap.width.toFloat()


                val newHeight =
                    (
                            originalBitmap.height *
                                    ratio
                            ).toInt()


                Log.d(
                    TAG,
                    "Resize bitmap menjadi ${PRINTER_WIDTH} x $newHeight"
                )


                Bitmap.createScaledBitmap(
                    originalBitmap,
                    PRINTER_WIDTH,
                    newHeight,
                    true
                )

            } else {

                originalBitmap
            }


        val width =
            bitmap.width


        val height =
            bitmap.height


        Log.d(
            TAG,
            "Bitmap final: ${width} x ${height}"
        )


        val output =
            ArrayList<Byte>()


        // =====================================================
        // GANTI KE GS v 0 (raster bit image)
        // =====================================================
        //
        // Sebelumnya pakai ESC * mode 33 (24-dot bit image).
        // Setelah header lebar & bit-packing diperbaiki, ukuran
        // data sudah benar (terbukti dari log: persis sama
        // dengan hasil hitungan manual), tapi tetap tidak ada
        // yang tercetak. Ini pola khas printer thermal generic/
        // clone (termasuk banyak printer bertipe RPP0x) yang
        // firmware-nya tidak mengimplementasikan ESC * dengan
        // benar, meski printer mengaku "berhasil terhubung" dan
        // menerima datanya tanpa error.
        //
        // GS v 0 jauh lebih universal didukung: seluruh gambar
        // dikirim dalam SATU perintah (tidak perlu dipecah per
        // 24 baris seperti ESC *), jadi juga menghilangkan
        // potensi celah bug di pemisahan band.
        //
        // Format:
        // GS v 0 m xL xH yL yH d1...dk
        //
        // - m       : mode normal = 0
        // - xL, xH  : lebar gambar dalam BYTE (bukan dot!)
        //             xBytes = ceil(width / 8)
        // - yL, yH  : tinggi gambar dalam DOT (baris pixel)
        // - data    : xBytes byte per baris, MSB = pixel
        //             paling kiri, diulang untuk setiap baris
        //             dari atas ke bawah
        //
        // =====================================================

        val widthBytes =
            (width + 7) / 8


        // GS v 0
        output.add(0x1D.toByte())
        output.add(0x76.toByte())
        output.add(0x30.toByte())

        // mode normal
        output.add(0x00.toByte())

        // xL, xH -> lebar dalam BYTE
        output.add((widthBytes and 0xFF).toByte())
        output.add(((widthBytes shr 8) and 0xFF).toByte())

        // yL, yH -> tinggi dalam DOT
        output.add((height and 0xFF).toByte())
        output.add(((height shr 8) and 0xFF).toByte())


        // =====================================================
        // DATA GAMBAR (per baris, dari atas ke bawah)
        // =====================================================

        for (
        yRow in 0 until height
        ) {

            for (
            xByte in 0 until widthBytes
            ) {

                var value =
                    0

                for (
                bit in 0 until 8
                ) {

                    val x =
                        xByte * 8 + bit

                    if (
                        x >= width
                    ) {

                        continue
                    }

                    val pixel =
                        bitmap.getPixel(
                            x,
                            yRow
                        )

                    val red =
                        Color.red(
                            pixel
                        )

                    val green =
                        Color.green(
                            pixel
                        )

                    val blue =
                        Color.blue(
                            pixel
                        )

                    val gray =
                        (
                                red * 0.299 +
                                        green * 0.587 +
                                        blue * 0.114
                                ).toInt()

                    if (
                        gray < 180
                    ) {

                        value =
                            value or
                                    (
                                            1 shl
                                                    (7 - bit)
                                            )
                    }
                }

                output.add(
                    value.toByte()
                )
            }
        }


        val result =
            output.toByteArray()


        Log.d(
            TAG,
            "ESC/POS (GS v 0) data selesai: ${result.size} bytes"
        )


        return result
    }
}
