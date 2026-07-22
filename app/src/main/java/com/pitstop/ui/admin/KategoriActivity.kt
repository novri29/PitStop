package com.pitstop.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pitstop.save.entity.KATEGORI_COFFEE
import com.pitstop.save.entity.KATEGORI_NON_COFFEE
import com.pitstop.save.entity.KATEGORI_SNACK
import com.pitstop.pitstop.databinding.ActivityKategoriBinding
import com.pitstop.util.ViewModelFactory

class KategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKategoriBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val viewModel = ViewModelProvider(this, ViewModelFactory(this))[MenuKopiViewModel::class.java]
        viewModel.menuList.observe(this) { list ->
            binding.tvCountCoffee.text = "${list.count { it.kategori == KATEGORI_COFFEE }} produk"
            binding.tvCountNonCoffee.text = "${list.count { it.kategori == KATEGORI_NON_COFFEE }} produk"
            binding.tvCountSnack.text = "${list.count { it.kategori == KATEGORI_SNACK }} produk"
        }
    }
}
