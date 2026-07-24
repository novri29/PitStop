package com.pitstop.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ItemProdukBinding
import com.pitstop.save.entity.MenuKopi
import com.pitstop.util.Formatter
import com.pitstop.util.ImageUtil

class ProdukAdapter(
    private var items: List<MenuKopi> = emptyList(),
    private var ketersediaanMap: Map<Int, Boolean> = emptyMap(),
    private val onEdit: (MenuKopi) -> Unit,
    private val onHapus: (MenuKopi) -> Unit
) : RecyclerView.Adapter<ProdukAdapter.VH>() {

    fun submitData(list: List<MenuKopi>, ketersediaan: Map<Int, Boolean>) {
        items = list
        ketersediaanMap = ketersediaan
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val menu = items[position]
        val binding = holder.binding
        binding.tvNama.text = menu.nama
        binding.tvHarga.text = "Rp ${Formatter.rupiah(menu.hargaJual).removePrefix("Rp").trim()}"
        binding.tvHpp.text = "HPP: ${Formatter.rupiah(menu.hargaModal)}"

        val thumbnail = ImageUtil.loadThumbnail(menu.gambarPath, 150, 150)
        if (thumbnail != null) {
            binding.imgFoto.setPadding(0, 0, 0, 0)
            binding.imgFoto.imageTintList = null
            binding.imgFoto.setImageBitmap(thumbnail)
        } else {
            val paddingPx = (10 * binding.root.resources.displayMetrics.density).toInt()
            binding.imgFoto.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            binding.imgFoto.setImageResource(R.drawable.ic_cafe_cup)
            binding.imgFoto.imageTintList = ColorStateList.valueOf(
                binding.root.context.getColor(R.color.primary)
            )
        }

        val tersedia = ketersediaanMap[menu.id] ?: true
        if (tersedia) {
            binding.tvBadge.text = "Tersedia"
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_green)
            binding.tvBadge.setTextColor(binding.root.context.getColor(R.color.green))
        } else {
            binding.tvBadge.text = "Stok Habis"
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_red)
            binding.tvBadge.setTextColor(binding.root.context.getColor(R.color.red))
        }

        binding.btnEdit.setOnClickListener { onEdit(menu) }
        binding.btnMore.setOnClickListener { onHapus(menu) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemProdukBinding) : RecyclerView.ViewHolder(binding.root)
}
