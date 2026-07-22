package com.pitstop.util

import com.pitstop.save.entity.METODE_CASH

data class CartLineItem(
    val nama: String,
    var qty: Int,
    val harga: Double,
    val tipeLayanan: String,       // "Cuci Motor" / "Cuci Mobil" / "Cafe"
    val menuKopiId: Int? = null
)

/**
 * Menyimpan state keranjang pesanan sementara selama alur:
 * Pilih Produk / Layanan -> Keranjang -> Pembayaran -> Struk.
 *
 * Keranjang ini BOLEH berisi campuran Cuci Motor + Cuci Mobil + item Cafe sekaligus,
 * sehingga bisa dibayar dan dicetak dalam SATU struk. Hanya di-reset saat pembayaran
 * berhasil disimpan, atau saat kasir menekan "Kosongkan Keranjang" secara manual.
 */
object CartManager {
    val items: MutableList<CartLineItem> = mutableListOf()
    var catatan: String = ""
    var metodePembayaran: String = METODE_CASH
    var jumlahDibayar: Double = 0.0
    var kembalian: Double = 0.0

    fun total(): Double = items.sumOf { it.harga * it.qty }

    fun totalItem(): Int = items.sumOf { it.qty }

    fun tambahItem(nama: String, harga: Double, tipeLayanan: String, menuKopiId: Int? = null) {
        val existing = items.find { it.menuKopiId == menuKopiId && it.nama == nama && it.tipeLayanan == tipeLayanan }
        if (existing != null) {
            existing.qty += 1
        } else {
            items.add(CartLineItem(nama, 1, harga, tipeLayanan, menuKopiId))
        }
    }

    /** Gabungan jenis transaksi untuk disimpan sebagai header Transaksi, contoh: "Cuci Motor + Cafe" */
    fun tipeGabungan(): String {
        val tipeSet = items.map { it.tipeLayanan }.distinct()
        return when {
            tipeSet.isEmpty() -> "Cafe"
            tipeSet.size == 1 -> tipeSet.first()
            else -> tipeSet.joinToString(" + ")
        }
    }

    fun reset() {
        items.clear()
        catatan = ""
        metodePembayaran = METODE_CASH
        jumlahDibayar = 0.0
        kembalian = 0.0
    }
}
