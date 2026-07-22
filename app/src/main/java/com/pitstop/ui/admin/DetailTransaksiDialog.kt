package com.pitstop.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.NotaAdapter
import com.pitstop.save.entity.METODE_CASH
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.DialogDetailTransaksiBinding

/**
 * Dialog ringkas untuk menampilkan rincian item dari sebuah transaksi
 * (dipanggil saat item pada daftar Laporan/Transaksi/Riwayat diklik).
 */
object DetailTransaksiDialog {

    fun tampilkan(context: Context, transaksi: Transaksi, detail: List<TransaksiDetail>) {
        val binding = DialogDetailTransaksiBinding.inflate(LayoutInflater.from(context))

        binding.tvTipe.text = transaksi.tipe
        binding.tvInfo.text = "${Formatter.tanggalWaktu(transaksi.tanggal)}  •  Kasir: ${transaksi.kasirUsername}"

        binding.rvItem.layoutManager = LinearLayoutManager(context)
        binding.rvItem.adapter = NotaAdapter(detail)

        binding.tvTotal.text = Formatter.rupiah(transaksi.total)
        binding.tvLabelBayar.text = "Bayar (${transaksi.metodePembayaran})"
        binding.tvBayar.text = Formatter.rupiah(transaksi.jumlahDibayar)

        if (transaksi.metodePembayaran == METODE_CASH && transaksi.kembalian > 0) {
            binding.rowKembalian.visibility = View.VISIBLE
            binding.tvKembalian.text = Formatter.rupiah(transaksi.kembalian)
        } else {
            binding.rowKembalian.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        binding.btnTutup.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
