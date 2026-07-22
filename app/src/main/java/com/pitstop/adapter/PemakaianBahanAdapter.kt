package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.Bahan
import com.pitstop.pitstop.databinding.ItemPemakaianBahanBinding

data class PemakaianItem(val bahan: Bahan, val jumlah: Double)

class PemakaianBahanAdapter(
    private val items: MutableList<PemakaianItem> = mutableListOf(),
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<PemakaianBahanAdapter.VH>() {

    fun setItems(newItems: List<PemakaianItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<PemakaianItem> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPemakaianBahanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvItem.text = "${item.bahan.nama} - ${item.jumlah} ${item.bahan.satuan}"
        holder.binding.btnRemove.setOnClickListener { onDelete(position) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemPemakaianBahanBinding) : RecyclerView.ViewHolder(binding.root)
}
