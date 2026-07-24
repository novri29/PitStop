package com.pitstop.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ItemProdukGridBinding
import com.pitstop.save.entity.MenuKopi
import com.pitstop.util.Formatter
import com.pitstop.util.ImageUtil

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

        val thumbnail = ImageUtil.loadThumbnail(menu.gambarPath, 200, 200)
        if (thumbnail != null) {
            holder.binding.imgFoto.setPadding(0, 0, 0, 0)
            holder.binding.imgFoto.imageTintList = null
            holder.binding.imgFoto.setImageBitmap(thumbnail)
        } else {
            val paddingPx = (26 * holder.binding.root.resources.displayMetrics.density).toInt()
            holder.binding.imgFoto.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            holder.binding.imgFoto.setImageResource(R.drawable.ic_cafe_cup)
            holder.binding.imgFoto.imageTintList = ColorStateList.valueOf(
                holder.binding.root.context.getColor(R.color.primary)
            )
        }

        holder.binding.btnTambah.setOnClickListener { onTambah(menu) }
        holder.binding.root.setOnClickListener { onTambah(menu) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemProdukGridBinding) : RecyclerView.ViewHolder(binding.root)
}
