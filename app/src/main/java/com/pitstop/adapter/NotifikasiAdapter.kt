package com.pitstop.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ItemNotifikasiBinding
import com.pitstop.util.Formatter
import com.pitstop.util.NotifikasiItem

class NotifikasiAdapter(
    private val items: MutableList<NotifikasiItem>,
    private val onItemClick: (NotifikasiItem) -> Unit
) : RecyclerView.Adapter<NotifikasiAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotifikasiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.binding

        b.tvJudul.text = item.judul
        b.tvIsi.text = item.isi
        b.tvWaktu.text = Formatter.tanggalWaktu(item.waktu)

        val context = b.root.context

        // Ikon & warna bubble mengikuti jenis notifikasi
        when (item.jenis) {
            "REFUND" -> {
                b.imgIcon.setImageResource(R.drawable.ic_warning)
                b.imgIcon.background = context.getDrawable(R.drawable.bg_icon_bubble_red)
                b.imgIcon.setColorFilter(context.getColor(R.color.red))
            }
            "LOW_STOCK" -> {
                b.imgIcon.setImageResource(R.drawable.ic_stock_bahan)
                b.imgIcon.background = context.getDrawable(R.drawable.bg_icon_bubble_orange)
                b.imgIcon.setColorFilter(context.getColor(R.color.orange))
            }
            else -> {
                b.imgIcon.setImageResource(R.drawable.ic_check_circle)
                b.imgIcon.background = context.getDrawable(R.drawable.bg_icon_bubble_green)
                b.imgIcon.setColorFilter(context.getColor(R.color.green))
            }
        }

        // Kartu belum dibaca: latar lebih terang + titik penanda.
        // Kartu sudah dibaca: latar abu-abu redup + teks lebih pudar, seperti aplikasi modern.
        if (item.dibaca) {
            b.root.background = context.getDrawable(R.drawable.bg_item_notif_read)
            b.dotUnread.visibility = View.GONE
            b.tvJudul.alpha = 0.55f
            b.tvIsi.alpha = 0.55f
            b.imgIcon.alpha = 0.55f
        } else {
            b.root.background = context.getDrawable(R.drawable.bg_item_notif_unread)
            b.dotUnread.visibility = View.VISIBLE
            b.tvJudul.alpha = 1f
            b.tvIsi.alpha = 1f
            b.imgIcon.alpha = 1f
        }

        // Semua logika klik ditangani LANGSUNG di sini (tidak lewat pemanggilan
        // balik/casting ke adapter dari luar), dan posisi diambil ulang saat
        // diklik (bindingAdapterPosition) — bukan dari variabel "position" hasil
        // capture saat bind. Ini mencegah listener "nyangkut" ke data lama kalau
        // urutan/isi list berubah, sehingga kartu SELALU bisa dibuka detailnya
        // kapan pun diklik, baik yang belum maupun yang sudah dibaca.
        b.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            val current = items[pos]

            if (!current.dibaca) {
                items[pos] = current.copy(dibaca = true)
                notifyItemChanged(pos)
            }

            onItemClick(items[pos])
        }
    }

    override fun getItemCount(): Int = items.size

    fun markAllRead() {
        for (i in items.indices) {
            items[i] = items[i].copy(dibaca = true)
        }
        notifyDataSetChanged()
    }

    class VH(val binding: ItemNotifikasiBinding) : RecyclerView.ViewHolder(binding.root)
}