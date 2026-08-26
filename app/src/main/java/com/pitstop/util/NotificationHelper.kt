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
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.pitstop.R
import com.pitstop.ui.admin.AdminMainActivity
import com.pitstop.adapter.NotifikasiAdapter
import com.pitstop.pitstop.databinding.DialogNotifikasiBinding
import com.pitstop.pitstop.databinding.DialogDetailNotifikasiBinding
import org.json.JSONArray
import org.json.JSONObject
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.save.entity.Bahan
import com.pitstop.util.Formatter

/**
 * Ringkasan satu item transaksi di dalam notifikasi refund.
 */
data class ItemRingkasNotif(
    val nama: String,
    val qty: Int,
    val subtotal: Double
)

/**
 * Model notifikasi yang sudah diparsing dari JSON, dipakai oleh NotifikasiAdapter
 * dan dialog detail.
 */
data class NotifikasiItem(
    val waktu: Long,
    val judul: String,
    val isi: String,
    val isiLengkap: String,
    val jenis: String,
    val dibaca: Boolean,
    val items: List<ItemRingkasNotif> = emptyList(),
    val alasan: String = ""
)

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

    fun checkAndNotifyLowStock(
        context: Context,
        bahanList: List<Bahan>
    ) {

        val lowStock = bahanList.filter { bahan ->

            bahan.initialStock > 0 &&
                    bahan.stock > 0 &&
                    bahan.stock <= bahan.initialStock * 0.30
        }

        if (lowStock.isEmpty()) {
            return
        }

        val prefs = prefs(context)

        val notified =
            prefs.getStringSet(
                "low_stock_notified",
                emptySet()
            )?.toMutableSet()
                ?: mutableSetOf()

        val itemBaruMenipis = lowStock.filter { bahan ->

            val key =
                "${bahan.id}_${bahan.initialStock}"

            !notified.contains(key)
        }

        if (itemBaruMenipis.isEmpty()) {
            return
        }

        itemBaruMenipis.forEach { bahan ->

            notified.add(
                "${bahan.id}_${bahan.initialStock}"
            )
        }

        prefs.edit()
            .putStringSet(
                "low_stock_notified",
                notified
            )
            .apply()

        val daftarItem =
            itemBaruMenipis.joinToString("\n") { bahan ->

                val persen =
                    ((bahan.stock / bahan.initialStock) * 100)
                        .toInt()

                "• ${bahan.nama}: " +
                        "${bahan.stock.toInt()} ${bahan.satuan} " +
                        "($persen%)"
            }

        simpanNotifikasiStock(
            context = context,
            isi = daftarItem
        )

        tampilkanNotifikasiSistem(
            context = context,
            judul = "Stock Bahan Menipis",
            isi = daftarItem
        )
    }

    private fun simpanNotifikasiStock(
        context: Context,
        isi: String
    ) {

        val oldJson =
            prefs(context)
                .getString(KEY_NOTIFICATIONS, "[]")
                ?: "[]"

        val oldArray = JSONArray(oldJson)

        val newObject = JSONObject().apply {
            put(
                "judul",
                "Stock Bahan Menipis"
            )

            put("isi", isi)

            put(
                "jenis",
                "LOW_STOCK"
            )

            put(
                "waktu",
                System.currentTimeMillis()
            )

            put(
                "dibaca",
                false
            )
        }

        val newArray = JSONArray()

        newArray.put(newObject)

        for (
        i in 0 until minOf(oldArray.length(), 49)
        ) {
            val old = oldArray.getJSONObject(i)

            // Jangan masukkan notifikasi transaksi berhasil lama
            if (
                old.optString("jenis") != "TRANSAKSI"
            ) {
                newArray.put(old)
            }
        }

        prefs(context)
            .edit()
            .putString(
                KEY_NOTIFICATIONS,
                newArray.toString()
            )
            .apply()
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
     * Tandai SATU notifikasi (berdasarkan waktu/id-nya) sebagai sudah dibaca.
     * Dipanggil saat pengguna mengetuk salah satu notifikasi di daftar.
     */
    fun markAsRead(context: Context, waktu: Long) {

        val json = prefs(context)
            .getString(KEY_NOTIFICATIONS, "[]")
            ?: "[]"

        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.optLong("waktu") == waktu) {
                item.put("dibaca", true)
                break
            }
        }

        prefs(context)
            .edit()
            .putString(KEY_NOTIFICATIONS, array.toString())
            .apply()
    }

    /**
     * Ambil seluruh notifikasi yang tersimpan, sudah diparsing ke model NotifikasiItem,
     * terbaru di paling atas.
     */
    fun getNotifications(context: Context): List<NotifikasiItem> {

        val json = prefs(context)
            .getString(KEY_NOTIFICATIONS, "[]")
            ?: "[]"

        val array = JSONArray(json)
        val result = ArrayList<NotifikasiItem>()

        for (i in 0 until array.length()) {

            val item = array.getJSONObject(i)
            val jenis = item.optString("jenis")
            val alasan = item.optString("alasan")

            // Ringkasan untuk kartu (hanya baris pertama, tanpa alasan berulang).
            val isiMentah = item.optString("isi")
            val baris = isiMentah.split("\n").filter { it.isNotBlank() }

            val ringkasan = when {
                jenis == "LOW_STOCK" && baris.size > 1 ->
                    "${baris.size} bahan stoknya menipis, ketuk untuk lihat detail"
                jenis == "LOW_STOCK" && baris.size == 1 ->
                    baris.first().removePrefix("• ").trim()
                else ->
                    isiMentah.substringBefore("\n").trim()
            }

            val itemsJson = item.optJSONArray("items")
            val daftarItem = ArrayList<ItemRingkasNotif>()

            if (itemsJson != null) {
                for (j in 0 until itemsJson.length()) {
                    val detail = itemsJson.getJSONObject(j)
                    daftarItem.add(
                        ItemRingkasNotif(
                            nama = detail.optString("namaItem"),
                            qty = detail.optInt("qty"),
                            subtotal = detail.optDouble("subtotal")
                        )
                    )
                }
            }

            result.add(
                NotifikasiItem(
                    waktu = item.optLong("waktu"),
                    judul = item.optString("judul"),
                    isi = ringkasan,
                    isiLengkap = isiMentah,
                    jenis = jenis,
                    dibaca = item.optBoolean("dibaca"),
                    items = daftarItem,
                    alasan = alasan
                )
            )
        }

        return result
    }

    /**
     * Menampilkan daftar notifikasi saat lonceng ditekan.
     * Tiap notifikasi tampil sebagai kartu sendiri: yang belum dibaca berlatar
     * terang dengan titik penanda, begitu diketuk langsung berubah abu-abu
     * (sudah dibaca) DAN membuka dialog detail berisi rincian item + alasan —
     * persis seperti pola notifikasi di aplikasi modern.
     */
    fun showNotifications(context: Context) {

        val notifications = getNotifications(context).toMutableList()

        val binding = DialogNotifikasiBinding.inflate(
            android.view.LayoutInflater.from(context)
        )

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        if (notifications.isEmpty()) {
            binding.rvNotifikasi.visibility = android.view.View.GONE
            binding.btnTandaiSemua.visibility = android.view.View.GONE
            binding.tvEmpty.visibility = android.view.View.VISIBLE
        } else {
            val adapter = NotifikasiAdapter(notifications) { item ->
                markAsRead(context, item.waktu)
                showDetailNotifikasi(context, item)
            }
            binding.rvNotifikasi.layoutManager = LinearLayoutManager(context)
            binding.rvNotifikasi.adapter = adapter

            binding.btnTandaiSemua.setOnClickListener {
                markAllAsRead(context)
                adapter.markAllRead()
            }
        }

        binding.btnTutup.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /**
     * Dialog rincian satu notifikasi: daftar item yang di-refund + alasannya
     * (untuk jenis REFUND), atau isi lengkap apa adanya (untuk jenis lain
     * seperti stok menipis).
     */
    private fun showDetailNotifikasi(context: Context, item: NotifikasiItem) {

        val binding = DialogDetailNotifikasiBinding.inflate(
            android.view.LayoutInflater.from(context)
        )

        val detailDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        detailDialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        binding.tvJudulDetail.text = item.judul
        binding.tvWaktuDetail.text = Formatter.tanggalWaktu(item.waktu)

        when (item.jenis) {
            "REFUND" -> {
                binding.imgIconDetail.setImageResource(R.drawable.ic_warning)
                binding.imgIconDetail.background = context.getDrawable(R.drawable.bg_icon_bubble_red)
                binding.imgIconDetail.setColorFilter(context.getColor(R.color.red))

                binding.sectionItem.visibility = android.view.View.VISIBLE
                binding.containerItem.removeAllViews()

                item.items.forEach { detail ->
                    val row = android.widget.TextView(context).apply {
                        text = "${detail.nama} x${detail.qty} — ${Formatter.rupiah(detail.subtotal)}"
                        setTextColor(context.getColor(R.color.text_primary))
                        textSize = 13f
                        setPadding(0, 6, 0, 6)
                    }
                    binding.containerItem.addView(row)
                }

                if (item.alasan.isNotBlank()) {
                    binding.sectionAlasan.visibility = android.view.View.VISIBLE
                    binding.tvAlasanDetail.text = item.alasan
                } else {
                    binding.sectionAlasan.visibility = android.view.View.GONE
                }

                binding.tvIsiUmum.visibility = android.view.View.GONE
            }
            "LOW_STOCK" -> {
                binding.imgIconDetail.setImageResource(R.drawable.ic_stock_bahan)
                binding.imgIconDetail.background = context.getDrawable(R.drawable.bg_icon_bubble_orange)
                binding.imgIconDetail.setColorFilter(context.getColor(R.color.orange))

                binding.sectionItem.visibility = android.view.View.GONE
                binding.sectionAlasan.visibility = android.view.View.GONE

                binding.tvIsiUmum.visibility = android.view.View.VISIBLE
                binding.tvIsiUmum.text = item.isiLengkap
            }
            else -> {
                binding.imgIconDetail.setImageResource(R.drawable.ic_check_circle)
                binding.imgIconDetail.background = context.getDrawable(R.drawable.bg_icon_bubble_green)
                binding.imgIconDetail.setColorFilter(context.getColor(R.color.green))

                binding.sectionItem.visibility = android.view.View.GONE
                binding.sectionAlasan.visibility = android.view.View.GONE

                binding.tvIsiUmum.visibility = android.view.View.VISIBLE
                binding.tvIsiUmum.text = item.isiLengkap
            }
        }

        binding.btnTutupDetail.setOnClickListener { detailDialog.dismiss() }

        detailDialog.show()
    }
}