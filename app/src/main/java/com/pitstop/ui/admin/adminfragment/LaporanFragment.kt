package com.pitstop.ui.admin.adminfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.LaporanAdapter
import com.pitstop.ui.admin.DetailTransaksiDialog
import com.pitstop.util.ExcelExporter
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.FragmentLaporanBinding
import com.pitstop.ui.admin.RingkasanViewModel
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RingkasanViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[RingkasanViewModel::class.java]

        val adapter = LaporanAdapter(onClick = { transaksi ->
            viewLifecycleOwner.lifecycleScope.launch {
                val detail = viewModel.getDetail(transaksi.id)
                DetailTransaksiDialog.tampilkan(requireContext(), transaksi, detail)
            }
        })
        binding.rvLaporan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLaporan.adapter = adapter

        viewModel.getAllTransaksi().observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.totalOmzetKeseluruhan.observe(viewLifecycleOwner) {
            binding.tvTotalOmzet.text = "Total Omzet: ${Formatter.rupiah(it ?: 0.0)}"
        }

        binding.btnExport.setOnClickListener { exportExcel() }
    }

    private fun exportExcel() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = viewModel.getSemuaTransaksi()
            if (data.isEmpty()) {
                Toast.makeText(requireContext(), "Belum ada data penjualan untuk diexport", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val file = ExcelExporter.exportLaporan(requireContext(), data)
            Toast.makeText(requireContext(), "Laporan tersimpan: ${file.name}", Toast.LENGTH_LONG).show()
            ExcelExporter.shareFile(requireContext(), file)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
