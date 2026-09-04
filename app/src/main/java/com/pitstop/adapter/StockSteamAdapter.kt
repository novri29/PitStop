package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.pitstop.databinding.ItemStockSteamBinding
import com.pitstop.save.entity.StockSteam
import com.pitstop.util.Formatter

class StockSteamAdapter(
    private val onDelete: (StockSteam) -> Unit,
    private val onTambahStock: (StockSteam) -> Unit
) : ListAdapter<StockSteam, StockSteamAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val binding = ItemStockSteamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return VH(binding)
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        val item = getItem(position)

        holder.binding.tvNama.text =
            "${item.nama} (${item.jenis})"

        holder.binding.tvStock.text =
            "Stock: ${formatJumlah(item.stock)} ${item.satuan}"

        holder.binding.tvHpp.text =
            "HPP: ${Formatter.rupiah(item.hargaPerSatuan)} / ${item.satuan}"

        holder.binding.btnDelete.setOnClickListener {
            onDelete(item)
        }

        holder.binding.btnEditStock.setOnClickListener {
            onTambahStock(item)
        }
    }

    private fun formatJumlah(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    class VH(
        val binding: ItemStockSteamBinding
    ) : RecyclerView.ViewHolder(binding.root)

    companion object {

        val DIFF = object : DiffUtil.ItemCallback<StockSteam>() {

            override fun areItemsTheSame(
                oldItem: StockSteam,
                newItem: StockSteam
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: StockSteam,
                newItem: StockSteam
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}