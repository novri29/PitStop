package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.MenuKopi
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.ItemProdukGridBinding

class ProdukGridAdapter(
    private var items: List<MenuKopi> = emptyList(),
    private val onTambah: (MenuKopi) -> Unit
) : RecyclerView.Adapter<ProdukGridAdapter.VH>() {

    fun submitList(list: List<MenuKopi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProdukGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val menu = items[position]
        holder.binding.tvNama.text = menu.nama
        holder.binding.tvHarga.text = Formatter.rupiah(menu.hargaJual)
        holder.binding.btnTambah.setOnClickListener { onTambah(menu) }
        holder.binding.root.setOnClickListener { onTambah(menu) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemProdukGridBinding) : RecyclerView.ViewHolder(binding.root)
}
