package com.pitstop.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.OutputStream
import android.annotation.SuppressLint
import java.util.UUID

object BluetoothPrinterHelper {

    private const val REQUEST_BLUETOOTH_CONNECT = 2001

    // UUID standar untuk printer Bluetooth thermal ESC/POS
    private val PRINTER_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // 58mm thermal printer umumnya memakai 384 dots
    private const val PRINTER_WIDTH = 384

    fun printHtml(activity: Activity, html: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_CONNECT
            )

            Toast.makeText(
                activity,
                "Izinkan akses perangkat Bluetooth lalu tekan Cetak lagi.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()

        if (adapter == null) {
            Toast.makeText(
                activity,
                "Tablet tidak mendukung Bluetooth.",
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

        val devices = adapter.bondedDevices.toList()

        if (devices.isEmpty()) {
            Toast.makeText(
                activity,
                "Belum ada printer Bluetooth yang dipairing.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val names = devices.map {
            "${it.name ?: "Bluetooth Device"}\n${it.address}"
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Pilih Printer Struk")
            .setItems(names) { _, which ->
                renderAndPrint(
                    activity,
                    html,
                    devices[which]
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun renderAndPrint(
        activity: Activity,
        html: String,
        device: BluetoothDevice
    ) {

        val webView = WebView(activity)

        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = false
        webView.settings.useWideViewPort = false

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                webView.postDelayed({

                    val widthSpec = View.MeasureSpec.makeMeasureSpec(
                        PRINTER_WIDTH,
                        View.MeasureSpec.EXACTLY
                    )

                    val heightSpec = View.MeasureSpec.makeMeasureSpec(
                        0,
                        View.MeasureSpec.UNSPECIFIED
                    )

                    webView.measure(widthSpec, heightSpec)

                    val width = PRINTER_WIDTH
                    val height = webView.measuredHeight

                    if (height <= 0) {
                        Toast.makeText(
                            activity,
                            "Gagal membuat gambar struk.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@postDelayed
                    }

                    webView.layout(
                        0,
                        0,
                        width,
                        height
                    )

                    val bitmap = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    )

                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    webView.draw(canvas)

                    Thread {
                        sendBitmapToPrinter(
                            activity,
                            device,
                            bitmap
                        )
                    }.start()

                }, 500)
            }
        }

        webView.loadDataWithBaseURL(
            null,
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendBitmapToPrinter(
        activity: Activity,
        device: BluetoothDevice,
        bitmap: Bitmap
    ) {

        var outputStream: OutputStream? = null

        try {

            val socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)

            socket.connect()

            outputStream = socket.outputStream

            // Reset printer
            outputStream.write(
                byteArrayOf(
                    0x1B,
                    0x40
                )
            )

            // Print image
            outputStream.write(
                bitmapToEscPos(bitmap)
            )

            // Feed paper
            outputStream.write(
                byteArrayOf(
                    0x0A,
                    0x0A,
                    0x0A
                )
            )

            outputStream.flush()

            socket.close()

            activity.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Struk berhasil dikirim ke printer.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            activity.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Gagal mencetak: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun bitmapToEscPos(bitmap: Bitmap): ByteArray {

        val width = bitmap.width
        val height = bitmap.height

        val widthBytes = (width + 7) / 8

        val data = ArrayList<Byte>()

        // GS v 0
        data.add(0x1D)
        data.add(0x76)
        data.add(0x30)
        data.add(0x00)

        // Width
        data.add((widthBytes and 0xFF).toByte())
        data.add(((widthBytes shr 8) and 0xFF).toByte())

        // Height
        data.add((height and 0xFF).toByte())
        data.add(((height shr 8) and 0xFF).toByte())

        for (y in 0 until height) {

            for (xByte in 0 until widthBytes) {

                var value = 0

                for (bit in 0..7) {

                    val x = xByte * 8 + bit

                    if (x < width) {

                        val pixel = bitmap.getPixel(
                            x,
                            y
                        )

                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)

                        val gray =
                            (r * 0.299 +
                                    g * 0.587 +
                                    b * 0.114).toInt()

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
}