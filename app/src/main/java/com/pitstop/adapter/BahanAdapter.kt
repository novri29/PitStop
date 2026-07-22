package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.save.entity.Bahan
import com.pitstop.util.Formatter
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ItemBahanBinding

class BahanAdapter(
    private val onDelete: (Bahan) -> Unit
) : ListAdapter<Bahan, BahanAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBahanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemBahanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bahan: Bahan) {
            binding.tvNama.text = bahan.nama
            binding.tvStock.text = "Stock: ${bahan.stock.toInt()} ${bahan.satuan}"
            binding.tvHarga.text = "Harga: ${Formatter.rupiah(bahan.hargaPerSatuan)} / ${bahan.satuan}"

            when {
                bahan.stock <= 0 -> {
                    binding.tvBadge.text = "Habis"
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_red)
                    binding.tvBadge.setTextColor(binding.root.context.getColor(R.color.red))
                }
                bahan.stock < 200 -> {
                    binding.tvBadge.text = "Menipis"
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_orange)
                    binding.tvBadge.setTextColor(binding.root.context.getColor(R.color.orange))
                }
                else -> {
                    binding.tvBadge.text = "Tersedia"
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_green)
                    binding.tvBadge.setTextColor(binding.root.context.getColor(R.color.green))
                }
            }
            binding.btnDelete.setOnClickListener { onDelete(bahan) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Bahan>() {
            override fun areItemsTheSame(oldItem: Bahan, newItem: Bahan) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Bahan, newItem: Bahan) = oldItem == newItem
        }
    }
}
