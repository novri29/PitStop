package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.STATUS_REFUND
import com.pitstop.save.entity.Transaksi
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.ItemLaporanBinding

class LaporanAdapter(
    private val onClick: (Transaksi) -> Unit = {}
) : ListAdapter<Transaksi, LaporanAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)
        holder.binding.tvTipe.text = "${t.tipe} - ${t.kasirUsername}"
        holder.binding.tvNoTransaksi.text = "PIT-${t.id.toString().padStart(6, '0')}"
        holder.binding.tvTanggal.text = Formatter.tanggalWaktu(t.tanggal)
        holder.binding.tvTotal.text = Formatter.rupiah(t.total)
        holder.binding.tvBadgeRefund.visibility = if (t.status == STATUS_REFUND) View.VISIBLE else View.GONE

        if (t.platNomor.isNotBlank()) {
            holder.binding.tvPlatNomor.visibility = View.VISIBLE
            holder.binding.tvPlatNomor.text = "Plat: ${t.platNomor}"
        } else {
            holder.binding.tvPlatNomor.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener { onClick(t) }
    }

    class VH(val binding: ItemLaporanBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Transaksi>() {
            override fun areItemsTheSame(oldItem: Transaksi, newItem: Transaksi) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Transaksi, newItem: Transaksi) = oldItem == newItem
        }
    }
}