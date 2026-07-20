package com.pitstop.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Binding event klik menu Admin
        binding.menuStockBahan.setOnClickListener {
            // Action navigasi ke fragment/page Stock Bahan
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