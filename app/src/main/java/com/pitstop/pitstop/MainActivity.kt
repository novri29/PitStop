package com.pitstop.pitstop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.pitstop.fragments.AdminDashboardFragment
import com.pitstop.fragments.KasirDashboardFragment
import com.pitstop.pitstop.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil data role yang dikirim dari LoginActivity
        val role = intent.getStringExtra("EXTRA_ROLE") ?: "KASIR"

        // Tampilkan Fragment sesuai Role
        if (savedInstanceState == null) {
            if (role == "ADMIN") {
                loadFragment(AdminDashboardFragment())
            } else {
                loadFragment(KasirDashboardFragment())
            }
        }
    }

    // Fungsi untuk mengganti Fragment di dalam MainActivity
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // R.id.fragment_container adalah ID container di layout XML
            .commit()
    }
}