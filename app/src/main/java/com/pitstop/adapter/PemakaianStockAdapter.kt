package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.StockSteam
import com.pitstop.pitstop.databinding.ItemPemakaianBahanBinding

data class PemakaianStockItem(val stockSteam: StockSteam, val jumlah: Double)

/**
 * Menampilkan daftar bahan (StockSteam) yang dipakai untuk 1 layanan steam (ukuran motor),
 * sedang disusun sebelum disimpan. Pola sama dengan PemakaianBahanAdapter di sisi Cafe.
 */
class PemakaianStockAdapter(
    private val items: MutableList<PemakaianStockItem> = mutableListOf(),
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<PemakaianStockAdapter.VH>() {

    fun setItems(newItems: List<PemakaianStockItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<PemakaianStockItem> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPemakaianBahanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvItem.text = "${item.stockSteam.nama} - ${item.jumlah} ${item.stockSteam.satuan}"
        holder.binding.btnRemove.setOnClickListener { onDelete(position) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemPemakaianBahanBinding) : RecyclerView.ViewHolder(binding.root)
}
