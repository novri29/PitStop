package com.pitstop.pitstop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.pitstop.fragments.AdminDashboardFragment
import com.pitstop.fragments.KasirDashboardFragment
import com.pitstop.fragments.admin.LaporanFragment
import com.pitstop.fragments.admin.PengaturanFragment
import com.pitstop.fragments.admin.TransaksiFragment
import com.pitstop.fragments.kasir.PesananFragment
import com.pitstop.fragments.kasir.ProfilFragment
import com.pitstop.fragments.kasir.RiwayatFragment
import com.pitstop.pitstop.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role = intent.getStringExtra("EXTRA_ROLE") ?: "KASIR"

        if (role == "ADMIN") {
            setupAdminNavigation()
        } else {
            setupKasirNavigation()
        }

        // Tampilkan default fragment saat pertama kali dibuka
        if (savedInstanceState == null) {
            val defaultFragment = if (role == "ADMIN") AdminDashboardFragment() else KasirDashboardFragment()
            loadFragment(defaultFragment)
        }
    }

    // Logic khusus Admin
    private fun setupAdminNavigation() {
        binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_admin)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> AdminDashboardFragment()
                R.id.nav_transaksi -> TransaksiFragment()
                R.id.nav_laporan -> LaporanFragment()
                R.id.nav_pengaturan -> PengaturanFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(selectedFragment)
            true
        }
    }

    // Logic khusus Kasir
    private fun setupKasirNavigation() {
        binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_kasir)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> KasirDashboardFragment()
                R.id.nav_pesanan -> PesananFragment()
                R.id.nav_riwayat -> RiwayatFragment()
                R.id.nav_profil -> ProfilFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(selectedFragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}