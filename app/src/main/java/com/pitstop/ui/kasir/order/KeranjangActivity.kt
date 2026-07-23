package com.pitstop.ui.kasir.order

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.CartAdapter
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ActivityKeranjangBinding
import com.pitstop.save.entity.TIPE_MOBIL
import com.pitstop.save.entity.TIPE_MOTOR
import com.pitstop.ui.admin.StockSteamViewModel
import com.pitstop.util.CartManager
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory

class KeranjangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangBinding
    private lateinit var adapter: CartAdapter
    private lateinit var steamViewModel: StockSteamViewModel
    private var hargaMotor = 0.0
    private var hargaMobil = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityKeranjangBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        adapter = CartAdapter(CartManager.items) { updateRingkasan() }
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = adapter

        steamViewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]
        steamViewModel.layananList.observe(this) { list ->
            list.find { it.jenis == com.pitstop.save.entity.JENIS_MOTOR }?.let { hargaMotor = it.harga }
            list.find { it.jenis == com.pitstop.save.entity.JENIS_MOBIL }?.let { hargaMobil = it.harga }
        }

        binding.btnTambahMotor.setOnClickListener {
            if (hargaMotor <= 0.0) {
                Toast.makeText(this, "Harga Cuci Motor belum diatur Admin", Toast.LENGTH_SHORT).show()
            } else {
                CartManager.tambahItem("Cuci Motor", hargaMotor, TIPE_MOTOR)
                adapter.notifyDataSetChanged()
                updateRingkasan()
                Toast.makeText(this, "Cuci Motor ditambahkan", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnTambahMobil.setOnClickListener {
            if (hargaMobil <= 0.0) {
                Toast.makeText(this, "Harga Cuci Mobil belum diatur Admin", Toast.LENGTH_SHORT).show()
            } else {
                CartManager.tambahItem("Cuci Mobil", hargaMobil, TIPE_MOBIL)
                adapter.notifyDataSetChanged()
                updateRingkasan()
                Toast.makeText(this, "Cuci Mobil ditambahkan", Toast.LENGTH_SHORT).show()
            }
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

    private fun updateRingkasan() {
        binding.tvEmpty.visibility = if (CartManager.items.isEmpty()) View.VISIBLE else View.GONE
        binding.tvTotalItem.text = "Total Item: ${CartManager.totalItem()}"
        binding.tvTotal.text = "Total: ${Formatter.rupiah(CartManager.total())}"
    }
}
