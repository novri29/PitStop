package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.ItemNotaBinding

class NotaAdapter(private val items: List<TransaksiDetail>) : RecyclerView.Adapter<NotaAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvNamaQty.text = "${item.namaItem} x${item.qty}"
        holder.binding.tvSubtotal.text = Formatter.rupiah(item.subtotal)
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemNotaBinding) : RecyclerView.ViewHolder(binding.root)
}
