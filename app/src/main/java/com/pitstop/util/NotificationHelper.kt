package com.pitstop.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.appcompat.app.AlertDialog
import com.pitstop.pitstop.R
import com.pitstop.ui.admin.AdminMainActivity
import org.json.JSONArray
import org.json.JSONObject
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.util.Formatter

object NotificationHelper {

    private const val CHANNEL_ID = "pitstop_transaksi"
    private const val PREF_NAME = "pitstop_notifications"
    private const val KEY_NOTIFICATIONS = "notifications"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Dipanggil saat aplikasi pertama kali membutuhkan notifikasi.
     */
    fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifikasi Transaksi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi transaksi berhasil dan refund PitStop"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Notifikasi transaksi berhasil.
     */
    fun transaksiBerhasil(
        context: Context,
        transaksiId: Long,
        tipe: String,
        total: Double,
        items: List<TransaksiDetail>
    ) {
        val judul = "Transaksi Berhasil"

        val ringkasanItem = items.joinToString(", ") {
            "${it.namaItem} x${it.qty}"
        }

        val isi = "$ringkasanItem • ${Formatter.rupiah(total)}"

        simpanNotifikasi(
            context = context,
            judul = judul,
            isi = isi,
            jenis = "TRANSAKSI",
            items = items
        )

        tampilkanNotifikasiSistem(
            context = context,
            judul = judul,
            isi = isi
        )
    }

    /**
     * Notifikasi refund.
     */
    fun transaksiRefund(
        context: Context,
        transaksiId: Int,
        total: Double,
        alasan: String,
        items: List<TransaksiDetail>
    ) {
        val judul = "Transaksi Di-Refund"

        val ringkasanItem = items.joinToString(", ") {
            "${it.namaItem} x${it.qty}"
        }

        val isi =
            "$ringkasanItem • ${Formatter.rupiah(total)}"

        val isiLengkap =
            "$isi\nAlasan: $alasan"

        simpanNotifikasi(
            context = context,
            judul = judul,
            isi = isiLengkap,
            jenis = "REFUND",
            items = items,
            alasan = alasan
        )

        tampilkanNotifikasiSistem(
            context = context,
            judul = judul,
            isi = isiLengkap
        )
    }

    private fun simpanNotifikasi(
        context: Context,
        judul: String,
        isi: String,
        jenis: String,
        items: List<TransaksiDetail> = emptyList(),
        alasan: String = ""
    ) {

        val oldJson = prefs(context)
            .getString(KEY_NOTIFICATIONS, "[]")
            ?: "[]"

        val oldArray = JSONArray(oldJson)

        val itemsJson = JSONArray()

        items.forEach { item ->
            itemsJson.put(
                JSONObject().apply {
                    put("namaItem", item.namaItem)
                    put("qty", item.qty)
                    put("hargaSatuan", item.hargaSatuan)
                    put("subtotal", item.subtotal)
                }
            )
        }

        val newObject = JSONObject().apply {
            put("judul", judul)
            put("isi", isi)
            put("jenis", jenis)
            put("waktu", System.currentTimeMillis())
            put("dibaca", false)
            put("items", itemsJson)
            put("alasan", alasan)
        }

        val newArray = JSONArray()

        newArray.put(newObject)

        // Simpan maksimal 50 notifikasi terbaru
        for (i in 0 until minOf(oldArray.length(), 49)) {
            newArray.put(oldArray.getJSONObject(i))
        }

        prefs(context)
            .edit()
            .putString(KEY_NOTIFICATIONS, newArray.toString())
            .apply()
    }

    private fun tampilkanNotifikasiSistem(
        context: Context,
        judul: String,
        isi: String
    ) {

        createChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(
            context,
            AdminMainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(judul)
            .setContentText(isi)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(isi)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notification
            )
    }

    /**
     * Jumlah notifikasi yang belum dibaca.
     */
    fun getUnreadCount(context: Context): Int {

        val json = prefs(context)
            .getString(KEY_NOTIFICATIONS, "[]")
            ?: "[]"

        val array = JSONArray(json)

        var count = 0

        for (i in 0 until array.length()) {
            if (!array.getJSONObject(i).getBoolean("dibaca")) {
                count++
            }
        }

        return count
    }

    /**
     * Tandai semua notifikasi sebagai sudah dibaca.
     */
    fun markAllAsRead(context: Context) {

        val json = prefs(context)
            .getString(KEY_NOTIFICATIONS, "[]")
            ?: "[]"

        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            array.getJSONObject(i)
                .put("dibaca", true)
        }

        prefs(context)
            .edit()
            .putString(KEY_NOTIFICATIONS, array.toString())
            .apply()
    }

    /**
     * Menampilkan daftar notifikasi saat lonceng ditekan.
     */
    fun showNotifications(context: Context) {

        val json = prefs(context)
            .getString(KEY_NOTIFICATIONS, "[]")
            ?: "[]"

        val array = JSONArray(json)

        if (array.length() == 0) {

            AlertDialog.Builder(context)
                .setTitle("Notifikasi")
                .setMessage("Belum ada notifikasi.")
                .setPositiveButton("Tutup", null)
                .show()

            return
        }

        val daftarNotifikasi = ArrayList<String>()

        for (i in 0 until array.length()) {

            val item = array.getJSONObject(i)

            val judul = item.optString("judul")
            val isi = item.optString("isi")
            val jenis = item.optString("jenis")

            val builder = StringBuilder()

            if (jenis == "REFUND") {
                builder.append("↩ $judul\n")
            } else {
                builder.append("✓ $judul\n")
            }

            builder.append("$isi\n")

            val itemsJson = item.optJSONArray("items")

            if (itemsJson != null && itemsJson.length() > 0) {

                builder.append("\nItem:\n")

                for (j in 0 until itemsJson.length()) {

                    val detail = itemsJson.getJSONObject(j)

                    val namaItem =
                        detail.optString("namaItem")

                    val qty =
                        detail.optInt("qty")

                    val subtotal =
                        detail.optDouble("subtotal")

                    builder.append(
                        "• $namaItem x$qty — ${Formatter.rupiah(subtotal)}\n"
                    )
                }
            }

            if (jenis == "REFUND") {

                val alasan =
                    item.optString("alasan")

                if (alasan.isNotBlank()) {
                    builder.append("\nAlasan: $alasan")
                }
            }

            daftarNotifikasi.add(
                builder.toString().trim()
            )
        }

        AlertDialog.Builder(context)
            .setTitle("Notifikasi")
            .setItems(
                daftarNotifikasi.toTypedArray(),
                null
            )
            .setPositiveButton("Tandai sudah dibaca") { _, _ ->
                markAllAsRead(context)
            }
            .setNegativeButton("Tutup", null)
            .show()
    }
}