package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.MenuKopi
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.ItemResepBinding

class ResepAdapter(
    private var items: List<MenuKopi> = emptyList(),
    private var komposisiMap: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ResepAdapter.VH>() {

    fun submitData(list: List<MenuKopi>, komposisi: Map<Int, String>) {
        items = list
        komposisiMap = komposisi
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemResepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val menu = items[position]
        holder.binding.tvNama.text = menu.nama
        holder.binding.tvKomposisi.text = komposisiMap[menu.id] ?: "Belum ada komposisi bahan"
        holder.binding.tvHpp.text = "HPP: ${Formatter.rupiah(menu.hargaModal)}"
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemResepBinding) : RecyclerView.ViewHolder(binding.root)
}
