package com.pitstop.ui.kasir

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.pitstop.ui.kasir.kasirfragment.DashboardKasirFragment
import com.pitstop.ui.kasir.kasirfragment.PesananFragment
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ActivityKasirMainBinding
import com.pitstop.ui.kasir.kasirfragment.ProfilFragment
import com.pitstop.ui.kasir.kasirfragment.RiwayatFragment

class KasirMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKasirMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKasirMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            tampilkanFragment(DashboardKasirFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardKasirFragment()
                R.id.nav_pesanan -> PesananFragment()
                R.id.nav_riwayat -> RiwayatFragment()
                R.id.nav_profil -> ProfilFragment()
                else -> DashboardKasirFragment()
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

    fun pindahKeTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }
}