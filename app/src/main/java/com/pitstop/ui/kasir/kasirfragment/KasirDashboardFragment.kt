package com.pitstop.ui.kasir.kasirfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pitstop.pitstop.databinding.FragmentKasirDashboardBinding

class KasirDashboardFragment : Fragment() {

    private var _binding: FragmentKasirDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKasirDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPesanBaru.setOnClickListener {
            // Action menuju Fragment Transaksi / Pilih Produk
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}