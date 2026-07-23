package com.pitstop.ui.kasir.order

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.pitstop.util.CartManager
import com.pitstop.util.Formatter

object KonfirmasiLayananDialog {

    fun tampilkan(context: Context, tipeLayanan: String, namaLayanan: String, harga: Double) {
        AlertDialog.Builder(context)
            .setTitle("Tambah ke Keranjang")
            .setMessage("$namaLayanan\nHarga: ${Formatter.rupiah(harga)}\n\nItem ini akan ditambahkan ke keranjang pesanan (bisa digabung dengan pesanan lain).")
            .setPositiveButton("Tambahkan") { _, _ ->
                CartManager.tambahItem(namaLayanan, harga, tipeLayanan)
                context.startActivity(Intent(context, KeranjangActivity::class.java))
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
