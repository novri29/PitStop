package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.dao.ProdukTerlarisRow
import com.pitstop.pitstop.databinding.ItemProdukTerlarisBinding
import com.pitstop.util.Formatter

class ProdukTerlarisAdapter(
    private var items: List<ProdukTerlarisRow> = emptyList()
) : RecyclerView.Adapter<ProdukTerlarisAdapter.VH>() {

    fun submitList(list: List<ProdukTerlarisRow>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProdukTerlarisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvRank.text = (position + 1).toString()
        holder.binding.tvNama.text = item.namaItem
        holder.binding.tvOmzet.text = "Omzet: ${Formatter.rupiah(item.totalOmzet)}"
        holder.binding.tvQty.text = "${item.totalQty} terjual"
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemProdukTerlarisBinding) : RecyclerView.ViewHolder(binding.root)
}
