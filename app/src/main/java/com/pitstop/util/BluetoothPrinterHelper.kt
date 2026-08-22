package com.pitstop.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
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
     * Printer thermal 58mm umumnya:
     *
     * 32 karakter untuk font normal.
     */
    private const val PRINTER_WIDTH = 32

    private var selectedPrinterName: String? = null

    private var selectedPrinterMac: String? = null


    // ============================================================
    // SET PRINTER
    // ============================================================

    fun setPrinter(
        name: String,
        macAddress: String
    ) {

        selectedPrinterName = name
        selectedPrinterMac = macAddress

        Log.d(TAG, "====================================")
        Log.d(TAG, "PRINTER DIPILIH")
        Log.d(TAG, "Nama: $name")
        Log.d(TAG, "MAC: $macAddress")
        Log.d(TAG, "====================================")
    }


    // ============================================================
    // GET PRINTER
    // ============================================================

    fun getPrinterName(): String? {
        return selectedPrinterName
    }

    fun getPrinterMac(): String? {
        return selectedPrinterMac
    }


    // ============================================================
    // PRINT HTML
    // ============================================================

    fun printHtml(
        activity: Activity,
        html: String
    ) {

        Log.d(TAG, "====================================")
        Log.d(TAG, "printHtml() DIMULAI")
        Log.d(TAG, "====================================")


        /*
         * Kalau printer belum dipilih,
         * tampilkan daftar printer.
         */
        if (selectedPrinterMac.isNullOrBlank()) {

            Log.d(
                TAG,
                "Printer belum dipilih."
            )

            showPrinterDialog(
                activity
            )

            return
        }


        /*
         * HTML dari NotaStrukActivity
         * diubah menjadi text biasa.
         */
        val text =
            htmlToText(html)


        Log.d(
            TAG,
            "HTML berhasil dikonversi menjadi TEXT."
        )


        /*
         * Printing dilakukan di background thread
         * agar UI tidak freeze.
         */
        Thread {

            sendTextToPrinter(
                activity,
                selectedPrinterMac!!,
                text
            )

        }.start()
    }


    // ============================================================
    // PILIH PRINTER
    // ============================================================

    private fun showPrinterDialog(
        activity: Activity
    ) {

        Log.d(TAG, "====================================")
        Log.d(TAG, "MEMBUKA DAFTAR PRINTER")
        Log.d(TAG, "====================================")


        val adapter =
            BluetoothAdapter.getDefaultAdapter()


        if (adapter == null) {

            showToast(
                activity,
                "Tablet tidak mendukung Bluetooth.",
                Toast.LENGTH_LONG
            )

            return
        }


        /*
         * Android 12+
         */
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                REQUEST_BLUETOOTH_CONNECT
            )


            showToast(
                activity,
                "Izinkan akses Bluetooth lalu tekan Cetak lagi.",
                Toast.LENGTH_LONG
            )

            return
        }


        if (!adapter.isEnabled) {

            showToast(
                activity,
                "Bluetooth belum aktif.",
                Toast.LENGTH_LONG
            )

            return
        }


        val devices: List<BluetoothDevice>

        try {

            devices =
                adapter.bondedDevices.toList()

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "Tidak bisa membaca paired device.",
                e
            )

            showToast(
                activity,
                "Tidak memiliki izin Bluetooth.",
                Toast.LENGTH_LONG
            )

            return
        }


        Log.d(
            TAG,
            "Jumlah paired device: ${devices.size}"
        )


        if (devices.isEmpty()) {

            showToast(
                activity,
                "Belum ada printer Bluetooth yang dipairing.",
                Toast.LENGTH_LONG
            )

            return
        }


        val names =
            devices.map { device ->

                try {

                    val name =
                        device.name
                            ?: "Bluetooth Device"

                    val address =
                        device.address

                    "$name\n$address"

                } catch (
                    e: SecurityException
                ) {

                    "Bluetooth Device"
                }

            }.toTypedArray()


        AlertDialog.Builder(activity)
            .setTitle("Pilih Printer Struk")
            .setItems(names) { _, which ->

                val device =
                    devices[which]


                try {

                    val name =
                        device.name
                            ?: "Bluetooth Printer"

                    val address =
                        device.address


                    setPrinter(
                        name,
                        address
                    )


                    /*
                     * Setelah memilih printer,
                     * langsung simpan printer.
                     *
                     * Tidak langsung mencetak.
                     */
                    showToast(
                        activity,
                        "Printer $name dipilih.",
                        Toast.LENGTH_SHORT
                    )


                } catch (
                    e: SecurityException
                ) {

                    Log.e(
                        TAG,
                        "Gagal mendapatkan informasi printer.",
                        e
                    )

                    showToast(
                        activity,
                        "Izin Bluetooth belum diberikan.",
                        Toast.LENGTH_LONG
                    )
                }
            }
            .setNegativeButton(
                "Batal",
                null
            )
            .show()
    }


    // ============================================================
    // TEST PRINT
    // ============================================================

    fun testPrint(
        activity: Activity
    ) {

        if (
            selectedPrinterMac.isNullOrBlank()
        ) {

            showPrinterDialog(
                activity
            )

            return
        }


        val testText =
            """
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

            sendTextToPrinter(
                activity,
                selectedPrinterMac!!,
                testText
            )

        }.start()
    }


    // ============================================================
    // SEND TEXT TO PRINTER
    // ============================================================

    private fun sendTextToPrinter(
        activity: Activity,
        macAddress: String,
        text: String
    ) {

        var socket: BluetoothSocket? = null

        var outputStream: OutputStream? = null


        try {

            Log.d(TAG, "====================================")
            Log.d(
                TAG,
                "sendTextToPrinter() DIMULAI"
            )

            Log.d(
                TAG,
                "Printer: ${selectedPrinterName ?: "Unknown"}"
            )

            Log.d(
                TAG,
                "MAC: $macAddress"
            )

            Log.d(TAG, "====================================")


            val adapter =
                BluetoothAdapter.getDefaultAdapter()


            if (adapter == null) {

                showToast(
                    activity,
                    "Bluetooth tidak tersedia.",
                    Toast.LENGTH_LONG
                )

                return
            }


            /*
             * Android 12+
             */
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                showToast(
                    activity,
                    "Izin Bluetooth belum diberikan.",
                    Toast.LENGTH_LONG
                )

                return
            }


            val device =
                adapter.getRemoteDevice(
                    macAddress
                )


            /*
             * Hentikan Bluetooth discovery
             * sebelum connect.
             */
            try {

                adapter.cancelDiscovery()

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "Gagal cancel discovery.",
                    e
                )
            }


            // ====================================================
            // SECURE RFCOMM
            // ====================================================

            try {

                Log.d(
                    TAG,
                    "Membuat SECURE RFCOMM..."
                )


                socket =
                    device.createRfcommSocketToServiceRecord(
                        PRINTER_UUID
                    )


                Log.d(
                    TAG,
                    "Mencoba connect..."
                )


                socket!!.connect()


                Log.d(
                    TAG,
                    "Bluetooth BERHASIL TERHUBUNG."
                )


            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "Secure connection gagal.",
                    e
                )


                try {

                    socket?.close()

                } catch (_: Exception) {
                }


                // =================================================
                // INSECURE RFCOMM
                // =================================================

                Log.d(
                    TAG,
                    "Mencoba INSECURE RFCOMM..."
                )


                socket =
                    device.createInsecureRfcommSocketToServiceRecord(
                        PRINTER_UUID
                    )


                socket!!.connect()


                Log.d(
                    TAG,
                    "Bluetooth INSECURE BERHASIL TERHUBUNG."
                )
            }


            // ====================================================
            // OUTPUT STREAM
            // ====================================================

            outputStream =
                socket!!.outputStream


            Log.d(
                TAG,
                "OutputStream berhasil dibuat."
            )


            // ====================================================
            // RESET PRINTER
            // ====================================================

            outputStream.write(
                byteArrayOf(
                    0x1B,
                    0x40
                )
            )

            outputStream.flush()

            Thread.sleep(150)


            // ====================================================
            // ALIGN LEFT
            // ====================================================

            outputStream.write(
                byteArrayOf(
                    0x1B,
                    0x61,
                    0x00
                )
            )


            // ====================================================
            // BOLD OFF
            // ====================================================

            outputStream.write(
                byteArrayOf(
                    0x1B,
                    0x45,
                    0x00
                )
            )


            // ====================================================
            // FORMAT RECEIPT
            // ====================================================

            val formattedText =
                formatReceipt58mm(
                    text
                )


            // ====================================================
            // ENCODING
            // ====================================================

            val textBytes =
                formattedText.toByteArray(
                    charset("windows-1252")
                )


            Log.d(
                TAG,
                "Mengirim TEXT: ${textBytes.size} bytes"
            )


            // ====================================================
            // SEND TEXT
            // ====================================================

            outputStream.write(
                textBytes
            )

            outputStream.flush()


            Log.d(
                TAG,
                "TEXT BERHASIL DIKIRIM."
            )


            Thread.sleep(300)


            // ====================================================
            // FEED PAPER
            // ====================================================

            outputStream.write(
                byteArrayOf(
                    0x0A,
                    0x0A,
                    0x0A,
                    0x0A
                )
            )

            outputStream.flush()


            Thread.sleep(1000)


            Log.d(TAG, "====================================")
            Log.d(
                TAG,
                "PRINT TEXT SELESAI."
            )
            Log.d(TAG, "====================================")


            showToast(
                activity,
                "Struk berhasil dikirim ke printer.",
                Toast.LENGTH_SHORT
            )


        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "SecurityException Bluetooth.",
                e
            )


            showToast(
                activity,
                "Izin Bluetooth tidak tersedia.",
                Toast.LENGTH_LONG
            )


        } catch (e: IOException) {

            Log.e(
                TAG,
                "IOException saat print.",
                e
            )


            showToast(
                activity,
                "Gagal mencetak: ${e.message}",
                Toast.LENGTH_LONG
            )


        } catch (e: Exception) {

            Log.e(
                TAG,
                "ERROR saat print.",
                e
            )


            showToast(
                activity,
                "Gagal mencetak: ${e.message}",
                Toast.LENGTH_LONG
            )


        } finally {

            try {

                outputStream?.flush()

            } catch (_: Exception) {
            }


            try {

                outputStream?.close()

            } catch (_: Exception) {
            }


            try {

                socket?.close()

                Log.d(
                    TAG,
                    "Socket ditutup."
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Gagal menutup socket.",
                    e
                )
            }
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

        Handler(
            Looper.getMainLooper()
        ).post {

            if (
                !activity.isFinishing &&
                !activity.isDestroyed
            ) {

                Toast.makeText(
                    activity,
                    message,
                    duration
                ).show()
            }
        }
    }


    // ============================================================
    // FORMAT RECEIPT 58MM
    // ============================================================

    private fun formatReceipt58mm(
        text: String
    ): String {

        val lines =
            text
                .replace(
                    "\r\n",
                    "\n"
                )
                .replace(
                    "\r",
                    "\n"
                )
                .split("\n")
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }


        val result =
            StringBuilder()


        val width =
            PRINTER_WIDTH


        // ========================================================
        // CENTER
        // ========================================================

        fun center(
            value: String
        ) {

            val clean =
                value.trim()


            if (
                clean.length >= width
            ) {

                result.append(
                    clean.take(width)
                )

            } else {

                val spaces =
                    (width - clean.length) / 2

                result.append(
                    " ".repeat(
                        maxOf(
                            0,
                            spaces
                        )
                    )
                )

                result.append(
                    clean
                )
            }

            result.append(
                "\r\n"
            )
        }


        // ========================================================
        // LEFT
        // ========================================================

        fun left(
            value: String
        ) {

            val clean =
                value.trim()


            if (
                clean.length <= width
            ) {

                result.append(
                    clean
                )

                result.append(
                    "\r\n"
                )

            } else {

                var remaining =
                    clean


                while (
                    remaining.length > width
                ) {

                    result.append(
                        remaining.take(width)
                    )

                    result.append(
                        "\r\n"
                    )

                    remaining =
                        remaining.drop(width)
                }


                if (
                    remaining.isNotEmpty()
                ) {

                    result.append(
                        remaining
                    )

                    result.append(
                        "\r\n"
                    )
                }
            }
        }


        // ========================================================
        // LABEL : VALUE
        // ========================================================

        fun labelValue(
            label: String,
            value: String
        ) {

            val cleanLabel =
                label.trim()

            val cleanValue =
                value.trim()


            val line =
                "$cleanLabel : $cleanValue"


            left(line)
        }


        // ========================================================
        // LEFT RIGHT
        // ========================================================

        fun leftRight(
            leftText: String,
            rightText: String
        ) {

            val left =
                leftText.trim()

            val right =
                rightText.trim()


            val spaces =
                width -
                        left.length -
                        right.length


            if (
                spaces >= 1
            ) {

                result.append(
                    left
                )

                result.append(
                    " ".repeat(
                        spaces
                    )
                )

                result.append(
                    right
                )

                result.append(
                    "\r\n"
                )

                return
            }


            /*
             * Jika nama produk terlalu panjang,
             * harga tetap ditempatkan di kanan
             * selama memungkinkan.
             */

            val available =
                width -
                        right.length -
                        1


            if (
                available > 0
            ) {

                result.append(
                    left.take(
                        available
                    )
                )

                result.append(" ")

                result.append(
                    right
                )

            } else {

                result.append(
                    right.take(width)
                )
            }


            result.append(
                "\r\n"
            )
        }


        // ========================================================
        // SEPARATOR
        // ========================================================

        fun separator() {

            result.append(
                "-".repeat(width)
            )

            result.append(
                "\r\n"
            )
        }


        // ========================================================
        // PROCESS
        // ========================================================

        var i = 0


        while (
            i < lines.size
        ) {

            val line =
                lines[i].trim()


            // ====================================================
            // CAFE
            // ====================================================

            if (
                line.equals(
                    "Cafe",
                    ignoreCase = true
                )
            ) {

                center(line)

                i++

                continue
            }


            // ====================================================
            // NO. PIT
            // ====================================================

            if (
                line.equals(
                    "No. PIT",
                    ignoreCase = true
                )
            ) {

                if (
                    i + 1 < lines.size
                ) {

                    labelValue(
                        "No. PIT",
                        lines[i + 1]
                    )

                    i += 2

                    continue
                }
            }


            // ====================================================
            // TANGGAL
            // ====================================================

            if (
                line.equals(
                    "Tanggal",
                    ignoreCase = true
                )
            ) {

                if (
                    i + 1 < lines.size
                ) {

                    labelValue(
                        "Tanggal",
                        lines[i + 1]
                    )

                    i += 2

                    continue
                }
            }


            // ====================================================
            // KASIR
            // ====================================================

            if (
                line.equals(
                    "Kasir",
                    ignoreCase = true
                )
            ) {

                if (
                    i + 1 < lines.size
                ) {

                    labelValue(
                        "Kasir",
                        lines[i + 1]
                    )

                    i += 2

                    continue
                }
            }


            // ====================================================
            // SEPARATOR
            // ====================================================

            if (
                line.isNotEmpty() &&
                line.all {
                    it == '-' ||
                            it == '=' ||
                            it == '_'
                }
            ) {

                separator()

                i++

                continue
            }


            // ====================================================
            // TOTAL
            // ====================================================

            if (
                line.equals(
                    "Total",
                    ignoreCase = true
                )
            ) {

                if (
                    i + 1 < lines.size &&
                    lines[i + 1].startsWith(
                        "Rp",
                        ignoreCase = true
                    )
                ) {

                    leftRight(
                        "Total",
                        lines[i + 1]
                    )

                    i += 2

                    continue
                }


                left(line)

                i++

                continue
            }


            // ====================================================
            // BAYAR
            // ====================================================

            if (
                line.startsWith(
                    "Bayar",
                    ignoreCase = true
                )
            ) {

                if (
                    i + 1 < lines.size &&
                    lines[i + 1].startsWith(
                        "Rp",
                        ignoreCase = true
                    )
                ) {

                    leftRight(
                        line,
                        lines[i + 1]
                    )

                    i += 2

                    continue
                }


                left(line)

                i++

                continue
            }


            // ====================================================
            // KEMBALIAN
            // ====================================================

            if (
                line.startsWith(
                    "Kembalian",
                    ignoreCase = true
                )
            ) {

                if (
                    i + 1 < lines.size &&
                    lines[i + 1].startsWith(
                        "Rp",
                        ignoreCase = true
                    )
                ) {

                    leftRight(
                        line,
                        lines[i + 1]
                    )

                    i += 2

                    continue
                }


                left(line)

                i++

                continue
            }


            // ====================================================
            // PRODUK + HARGA
            // ====================================================

            if (
                i + 1 < lines.size &&
                lines[i + 1].startsWith(
                    "Rp",
                    ignoreCase = true
                ) &&
                !line.equals(
                    "Total",
                    ignoreCase = true
                ) &&
                !line.startsWith(
                    "Bayar",
                    ignoreCase = true
                ) &&
                !line.startsWith(
                    "Kembalian",
                    ignoreCase = true
                )
            ) {

                leftRight(
                    line,
                    lines[i + 1]
                )

                i += 2

                continue
            }


            // ====================================================
            // TERIMA KASIH
            // ====================================================

            if (
                line.contains(
                    "Terima kasih",
                    ignoreCase = true
                )
            ) {

                result.append(
                    "\r\n"
                )

                center(line)

                i++

                continue
            }


            // ====================================================
            // ALAMAT
            // ====================================================

            if (
                line.startsWith(
                    "Jl.",
                    ignoreCase = true
                ) ||
                line.startsWith(
                    "Jl ",
                    ignoreCase = true
                )
            ) {

                center(line)

                i++

                continue
            }


            // ====================================================
            // PITSTOP
            // ====================================================

            if (
                line.contains(
                    "~ Pitstop ~",
                    ignoreCase = true
                )
            ) {

                result.append(
                    "\r\n"
                )

                center(line)

                i++

                continue
            }


            // ====================================================
            // DEFAULT
            // ====================================================

            left(line)

            i++
        }


        // ========================================================
        // FEED
        // ========================================================

        result.append(
            "\r\n"
        )

        result.append(
            "\r\n"
        )


        return result.toString()
    }


    // ============================================================
    // HTML -> TEXT
    // ============================================================

    private fun htmlToText(
        html: String
    ): String {

        var result =
            html


        /*
         * Hapus CSS.
         * Ini penting supaya kode seperti:
         *
         * body {
         *   font-size...
         * }
         *
         * tidak ikut tercetak.
         */

        result =
            result.replace(
                Regex(
                    "<style[^>]*>.*?</style>",
                    setOf(
                        RegexOption.IGNORE_CASE,
                        RegexOption.DOT_MATCHES_ALL
                    )
                ),
                ""
            )


        /*
         * Hapus JavaScript.
         */

        result =
            result.replace(
                Regex(
                    "<script[^>]*>.*?</script>",
                    setOf(
                        RegexOption.IGNORE_CASE,
                        RegexOption.DOT_MATCHES_ALL
                    )
                ),
                ""
            )


        /*
         * BR -> ENTER
         */

        result =
            result.replace(
                Regex(
                    "<br\\s*/?>",
                    RegexOption.IGNORE_CASE
                ),
                "\n"
            )


        /*
         * Tag penutup block -> ENTER
         */

        result =
            result.replace(
                Regex(
                    "</(p|div|tr|h1|h2|h3|section)>",
                    RegexOption.IGNORE_CASE
                ),
                "\n"
            )


        /*
         * TD -> spasi
         */

        result =
            result.replace(
                Regex(
                    "</td>",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )


        /*
         * Hapus semua HTML tag.
         */

        result =
            result.replace(
                Regex(
                    "<[^>]*>"
                ),
                ""
            )


        /*
         * HTML entities.
         */

        result =
            result.replace(
                "&nbsp;",
                " "
            )

        result =
            result.replace(
                "&amp;",
                "&"
            )

        result =
            result.replace(
                "&lt;",
                "<"
            )

        result =
            result.replace(
                "&gt;",
                ">"
            )

        result =
            result.replace(
                "&quot;",
                "\""
            )

        result =
            result.replace(
                "&#39;",
                "'"
            )


        /*
         * Normalisasi newline.
         */

        result =
            result.replace(
                "\r\n",
                "\n"
            )

        result =
            result.replace(
                "\r",
                "\n"
            )


        /*
         * Hapus spasi/tab berlebihan.
         */

        result =
            result.replace(
                Regex("[ \\t]+"),
                " "
            )


        /*
         * Hapus spasi di awal baris.
         */

        result =
            result.replace(
                Regex("\n[ \\t]+"),
                "\n"
            )


        /*
         * Maksimal dua baris kosong.
         */

        result =
            result.replace(
                Regex("\n{3,}"),
                "\n\n"
            )


        return result.trim()
    }
}