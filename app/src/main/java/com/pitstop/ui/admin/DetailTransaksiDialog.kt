package com.pitstop.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.NotaAdapter
import com.pitstop.save.entity.METODE_CASH
import com.pitstop.save.entity.STATUS_REFUND
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.util.Formatter
import com.pitstop.util.SessionManager
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.DialogDetailTransaksiBinding
import com.pitstop.pitstop.databinding.DialogRefundTransaksiBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Dialog ringkas untuk menampilkan rincian item dari sebuah transaksi
 * (dipanggil saat item pada daftar Laporan/Transaksi/Riwayat diklik).
 * Dari sini kasir/admin juga bisa memproses REFUND transaksi (mis. pelanggan tidak jadi
 * melakukan pesanan), lengkap dengan keterangan/alasan yang wajib diisi.
 */
object DetailTransaksiDialog {

    fun tampilkan(
        context: Context,
        transaksi: Transaksi,
        detail: List<TransaksiDetail>,
        scope: CoroutineScope,
        viewModel: RingkasanViewModel,
        onRefundSukses: () -> Unit = {}
    ) {
        val binding = DialogDetailTransaksiBinding.inflate(LayoutInflater.from(context))

        binding.tvTipe.text = transaksi.tipe
        binding.tvInfo.text = if (transaksi.platNomor.isNotBlank()) {
            "${Formatter.tanggalWaktu(transaksi.tanggal)}  •  Kasir: ${transaksi.kasirUsername}  •  Plat: ${transaksi.platNomor}"
        } else {
            "${Formatter.tanggalWaktu(transaksi.tanggal)}  •  Kasir: ${transaksi.kasirUsername}"
        }

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

        val sudahRefund = transaksi.status == STATUS_REFUND
        if (sudahRefund) {
            binding.tvStatus.text = context.getString(R.string.status_refund)
            binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_red)
            binding.tvStatus.setTextColor(context.getColor(R.color.red))

            binding.boxRefundInfo.visibility = View.VISIBLE
            binding.tvAlasanRefund.text = "Alasan: ${transaksi.alasanRefund}"
            val waktu = transaksi.waktuRefund?.let { Formatter.tanggalWaktu(it) } ?: "-"
            binding.tvInfoRefund.text = "Direfund oleh ${transaksi.direfundOleh ?: "-"}  •  $waktu"

            binding.btnRefund.visibility = View.GONE
        } else {
            binding.boxRefundInfo.visibility = View.GONE
            binding.btnRefund.visibility = View.VISIBLE
        }

        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        binding.btnTutup.setOnClickListener { dialog.dismiss() }

        binding.btnRefund.setOnClickListener {
            tampilkanKonfirmasiRefund(context, transaksi, scope, viewModel) {
                dialog.dismiss()
                onRefundSukses()
            }
        }

        dialog.show()
    }

    private fun tampilkanKonfirmasiRefund(
        context: Context,
        transaksi: Transaksi,
        scope: CoroutineScope,
        viewModel: RingkasanViewModel,
        onSukses: () -> Unit
    ) {
        val refundBinding = DialogRefundTransaksiBinding.inflate(LayoutInflater.from(context))
        val refundDialog = AlertDialog.Builder(context).setView(refundBinding.root).create()

        refundBinding.btnBatalRefund.setOnClickListener { refundDialog.dismiss() }
        refundBinding.btnProsesRefund.setOnClickListener {
            val alasan = refundBinding.etAlasanRefund.text.toString().trim()
            if (alasan.isEmpty()) {
                Toast.makeText(context, "Keterangan/alasan refund wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val username = SessionManager(context).getUsername().ifBlank { "-" }

            refundBinding.btnProsesRefund.isEnabled = false
            scope.launch {
                val sukses = viewModel.refundTransaksi(transaksi.id, alasan, username)
                refundDialog.dismiss()
                if (sukses) {
                    Toast.makeText(context, "Transaksi berhasil di-refund", Toast.LENGTH_SHORT).show()
                    onSukses()
                } else {
                    Toast.makeText(context, "Gagal memproses refund (transaksi mungkin sudah direfund)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        refundDialog.show()
    }
}
