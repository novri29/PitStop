package com.pitstop.ui.admin.adminfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.LaporanAdapter
import com.pitstop.ui.admin.DetailTransaksiDialog
import com.pitstop.pitstop.databinding.FragmentTransaksiAdminBinding
import com.pitstop.ui.admin.RingkasanViewModel
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class TransaksiAdminFragment : Fragment() {

    private var _binding: FragmentTransaksiAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransaksiAdminBinding.inflate(inflater, container, false)
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
        binding.rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransaksi.adapter = adapter

        viewModel.getAllTransaksi().observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
