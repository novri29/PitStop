package com.pitstop.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.pitstop.ui.admin.adminfragment.DashboardAdminFragment
import com.pitstop.ui.admin.adminfragment.LaporanFragment
import com.pitstop.ui.admin.adminfragment.PengaturanAdminFragment
import com.pitstop.ui.admin.adminfragment.TransaksiAdminFragment
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ActivityAdminMainBinding

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            tampilkanFragment(DashboardAdminFragment())
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardAdminFragment()
                R.id.nav_transaksi -> TransaksiAdminFragment()
                R.id.nav_laporan -> LaporanFragment()
                R.id.nav_pengaturan -> PengaturanAdminFragment()
                else -> DashboardAdminFragment()
            }
            tampilkanFragment(fragment)
            true
        }
    }

    private fun tampilkanFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /** Dipanggil dari dalam fragment untuk pindah ke tab lain (mis. tombol "Lihat Laporan"). */
    fun pindahKeTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }
}
