package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.util.CartLineItem
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.ItemCartBinding

/**
 * Menampilkan isi CartManager.items secara langsung (list dipegang oleh CartManager,
 * adapter ini hanya juru gambar + pemicu perubahan qty).
 */
class CartAdapter(
    private val items: MutableList<CartLineItem>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvNama.text = item.nama
        holder.binding.tvHargaSatuan.text = Formatter.rupiah(item.harga)
        holder.binding.tvQty.text = item.qty.toString()
        holder.binding.tvSubtotal.text = Formatter.rupiah(item.harga * item.qty)

        holder.binding.btnPlus.setOnClickListener {
            item.qty += 1
            notifyItemChanged(position)
            onChanged()
        }
        holder.binding.btnMinus.setOnClickListener {
            if (item.qty > 1) {
                item.qty -= 1
                notifyItemChanged(position)
            } else {
                items.removeAt(position)
                notifyItemRemoved(position)
            }
            onChanged()
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)
}
