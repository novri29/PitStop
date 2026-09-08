package com.pitstop.ui.kasir.order

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.CartAdapter
import com.pitstop.pitstop.databinding.ActivityKeranjangBinding
import com.pitstop.ui.admin.MenuKopiViewModel
import com.pitstop.ui.admin.StockSteamViewModel
import com.pitstop.util.CartLineItem
import com.pitstop.util.CartManager
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class KeranjangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangBinding
    private lateinit var adapter: CartAdapter
    private lateinit var menuKopiViewModel: MenuKopiViewModel
    private lateinit var stockSteamViewModel: StockSteamViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityKeranjangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        menuKopiViewModel = ViewModelProvider(this, ViewModelFactory(this))[MenuKopiViewModel::class.java]
        stockSteamViewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]

        // Fix: dorong toolbar agar tidak ketutupan status bar / icon baterai di SDK 35+
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarHeader) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            // Simpan tinggi asli toolbar sekali saja (sebelum ditambah padding)
            val originalHeight = resources.getDimensionPixelSize(
                androidx.appcompat.R.dimen.abc_action_bar_default_height_material
            )

            view.layoutParams.height = originalHeight + statusBarInsets.top
            view.requestLayout()

            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.etCatatan.setText(CartManager.catatan)

        adapter = CartAdapter(
            items = CartManager.items,
            onTambahQty = { item -> tambahQtyItem(item) },
            onChanged = { updateRingkasan() }
        )
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = adapter

        binding.btnTambahMotor.setOnClickListener {
            startActivity(Intent(this, PilihLayananSteamActivity::class.java))
        }
        binding.btnTambahCafe.setOnClickListener {
            startActivity(Intent(this, PilihProdukActivity::class.java))
        }

        binding.btnHapusSemua.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Kosongkan Keranjang")
                .setMessage("Hapus semua item di keranjang?")
                .setPositiveButton("Ya, Hapus") { _, _ ->
                    CartManager.items.clear()
                    adapter.notifyDataSetChanged()
                    updateRingkasan()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnLanjut.setOnClickListener {
            if (CartManager.items.isEmpty()) {
                Toast.makeText(this, "Keranjang masih kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CartManager.catatan = binding.etCatatan.text.toString().trim()
            startActivity(Intent(this, PembayaranActivity::class.java))
        }

        updateRingkasan()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateRingkasan()
    }

    /**
     * Dipanggil saat tombol '+' di satu baris keranjang ditekan. Qty TIDAK langsung ditambah --
     * dicek dulu ke repository apakah stok bahan masih cukup untuk qty+1 (dijumlahkan dengan
     * baris lain untuk menu/layanan yang sama, mis. baris promo & normal untuk kopi yang sama),
     * sama seperti pengecekan yang dilakukan di PilihProdukActivity / PilihLayananSteamActivity
     * sebelum item pertama kali masuk keranjang. Ini menutup celah "+" bisa menambah qty
     * melewati stok yang tersedia tanpa peringatan ke kasir.
     */
    private fun tambahQtyItem(item: CartLineItem) {
        lifecycleScope.launch {
            val stokCukup = when {
                item.menuKopiId != null -> {
                    val qtyDiKeranjang = CartManager.items
                        .filter { it.menuKopiId == item.menuKopiId }
                        .sumOf { it.qty }
                    menuKopiViewModel.cekStokCukup(item.menuKopiId, qtyDiKeranjang + 1)
                }
                item.layananId != null -> {
                    val qtyDiKeranjang = CartManager.items
                        .filter { it.layananId == item.layananId }
                        .sumOf { it.qty }
                    stockSteamViewModel.cekStokCukup(item.layananId, qtyDiKeranjang + 1)
                }
                else -> true
            }

            if (stokCukup) {
                item.qty += 1
                // notifyDataSetChanged (bukan notifyItemChanged(position)) supaya tetap aman
                // walau posisi baris ini di list sempat berubah selama proses cek stok tadi
                // (mis. baris lain sempat dihapus kasir).
                adapter.notifyDataSetChanged()
                updateRingkasan()
            } else {
                Toast.makeText(
                    this@KeranjangActivity,
                    "Stok bahan untuk \"${item.nama}\" tidak cukup",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateRingkasan() {
        binding.tvEmpty.visibility = if (CartManager.items.isEmpty()) View.VISIBLE else View.GONE
        binding.tvTotalItem.text = "Total Item: ${CartManager.totalItem()}"
        binding.tvTotal.text = "Total: ${Formatter.rupiah(CartManager.total())}"

        if (CartManager.platNomor.isNotBlank()) {
            binding.tvPlatNomorCart.visibility = View.VISIBLE
            binding.tvPlatNomorCart.text = "Plat Nomor: ${CartManager.platNomor}"
        } else {
            binding.tvPlatNomorCart.visibility = View.GONE
        }
    }
}