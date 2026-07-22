package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.StockSteam
import com.pitstop.pitstop.databinding.ItemStockSteamBinding

class StockSteamAdapter(
    private val onDelete: (StockSteam) -> Unit
) : ListAdapter<StockSteam, StockSteamAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStockSteamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.tvNama.text = "${item.nama} (${item.jenis})"
        holder.binding.tvStock.text = "Stock: ${item.stock} ${item.satuan}"
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    class VH(val binding: ItemStockSteamBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<StockSteam>() {
            override fun areItemsTheSame(oldItem: StockSteam, newItem: StockSteam) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: StockSteam, newItem: StockSteam) = oldItem == newItem
        }
    }
}
