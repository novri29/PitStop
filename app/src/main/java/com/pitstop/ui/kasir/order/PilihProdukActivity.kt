package com.pitstop.ui.kasir.order

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.pitstop.adapter.ProdukGridAdapter
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ActivityPilihProdukBinding
import com.pitstop.save.entity.KATEGORI_COFFEE
import com.pitstop.save.entity.KATEGORI_NON_COFFEE
import com.pitstop.save.entity.KATEGORI_SNACK
import com.pitstop.save.entity.MenuKopi
import com.pitstop.save.entity.TIPE_CAFE
import com.pitstop.ui.admin.MenuKopiViewModel
import com.pitstop.util.CartManager
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory

class PilihProdukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPilihProdukBinding
    private lateinit var viewModel: MenuKopiViewModel
    private lateinit var adapter: ProdukGridAdapter
    private var semuaMenu: List<MenuKopi> = emptyList()
    private var kategoriTerpilih: String? = null
    private var keyword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPilihProdukBinding.inflate(layoutInflater)
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

        // Keranjang boleh sudah berisi item dari layanan lain (Cuci Motor/Mobil) - sengaja TIDAK direset,
        // supaya kasir bisa menggabungkan semuanya menjadi satu transaksi/struk.

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[MenuKopiViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }

        adapter = ProdukGridAdapter(onTambah = { menu ->
            CartManager.tambahItem(menu.nama, menu.hargaJual, TIPE_CAFE, menu.id)
            updateBottomBar()
            Toast.makeText(this, "${menu.nama} ditambahkan", Toast.LENGTH_SHORT).show()
        })
        binding.rvProduk.layoutManager = GridLayoutManager(this, 3)
        binding.rvProduk.adapter = adapter

        viewModel.menuList.observe(this) { list ->
            semuaMenu = list
            terapkanFilter()
        }

        binding.tabKategori.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                kategoriTerpilih = when (tab?.position) {
                    1 -> KATEGORI_COFFEE
                    2 -> KATEGORI_NON_COFFEE
                    3 -> KATEGORI_SNACK
                    else -> null
                }
                terapkanFilter()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                keyword = s.toString()
                terapkanFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnLihatKeranjang.setOnClickListener {
            startActivity(Intent(this, KeranjangActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomBar()
    }

    private fun terapkanFilter() {
        var hasil = semuaMenu
        kategoriTerpilih?.let { kat -> hasil = hasil.filter { it.kategori == kat } }
        if (keyword.isNotBlank()) hasil = hasil.filter { it.nama.contains(keyword, ignoreCase = true) }
        adapter.submitList(hasil)
    }

    private fun updateBottomBar() {
        val totalItem = CartManager.totalItem()
        if (totalItem > 0) {
            binding.bottomBar.visibility = View.VISIBLE
            binding.tvRingkasanCart.text = "$totalItem item - ${Formatter.rupiah(CartManager.total())}"
        } else {
            binding.bottomBar.visibility = View.GONE
        }
    }
}