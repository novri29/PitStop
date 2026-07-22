package com.pitstop.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.pitstop.fragments.admin.TambahBahanFragment
import com.pitstop.pitstop.MainActivity
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.FragmentAdminDashboardBinding

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.headerBar) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            // Konversi 16dp ke pixel secara presisi berdasarkan densitas layar HP
            val padding16dp = (16 * resources.displayMetrics.density).toInt()

            // Terapkan padding atas = tinggi status bar + 16dp
            v.setPadding(
                v.paddingLeft,
                statusBar.top + padding16dp,
                v.paddingRight,
                padding16dp
            )

            insets
        }

        // Binding event klik menu Admin
        binding.menuStockBahan.setOnClickListener {
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    TambahBahanFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.menuLaporan.setOnClickListener {
            // Action navigasi ke fragment/page Laporan (Export Excel)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}