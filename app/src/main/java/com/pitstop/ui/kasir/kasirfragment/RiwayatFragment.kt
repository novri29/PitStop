package com.pitstop.ui.kasir.kasirfragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.LaporanAdapter
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.FragmentRiwayatBinding
import com.pitstop.ui.admin.DetailTransaksiDialog
import com.pitstop.ui.admin.RingkasanViewModel
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[RingkasanViewModel::class.java]

        val adapter = LaporanAdapter(onClick = { transaksi ->
            viewLifecycleOwner.lifecycleScope.launch {
                val detail = viewModel.getDetail(transaksi.id)
                DetailTransaksiDialog.tampilkan(requireContext(), transaksi, detail)
            }
        })
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = adapter

        viewModel.getAllTransaksi().observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.omzetHariIni.observe(viewLifecycleOwner) {
            binding.tvTotalHariIni.text = "Omzet Hari Ini: ${Formatter.rupiah(it ?: 0.0)}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}