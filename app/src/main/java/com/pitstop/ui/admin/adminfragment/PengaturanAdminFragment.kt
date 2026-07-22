package com.pitstop.ui.admin.adminfragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pitstop.save.entity.JENIS_MOBIL
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.ui.admin.StockSteamActivity
import com.pitstop.ui.admin.StockSteamViewModel
import com.pitstop.ui.login.LoginActivity
import com.pitstop.util.SessionManager
import com.pitstop.pitstop.databinding.FragmentPengaturanAdminBinding
import com.pitstop.util.ViewModelFactory

class PengaturanAdminFragment : Fragment() {

    private var _binding: FragmentPengaturanAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StockSteamViewModel
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPengaturanAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        viewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[StockSteamViewModel::class.java]

        binding.tvUsername.text = session.getUsername()

        viewModel.layananList.observe(viewLifecycleOwner) { list ->
            list.find { it.jenis == JENIS_MOTOR }?.let { binding.etHargaMotor.setText(it.harga.toInt().toString()) }
            list.find { it.jenis == JENIS_MOBIL }?.let { binding.etHargaMobil.setText(it.harga.toInt().toString()) }
        }

        binding.btnSimpanHarga.setOnClickListener {
            val hargaMotor = binding.etHargaMotor.text.toString().toDoubleOrNull()
            val hargaMobil = binding.etHargaMobil.text.toString().toDoubleOrNull()
            if (hargaMotor == null || hargaMobil == null) {
                Toast.makeText(requireContext(), "Isi harga layanan dengan benar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.simpanHargaLayanan("Cuci Motor", JENIS_MOTOR, hargaMotor)
            viewModel.simpanHargaLayanan("Cuci Mobil", JENIS_MOBIL, hargaMobil)
            Toast.makeText(requireContext(), "Harga layanan tersimpan", Toast.LENGTH_SHORT).show()
        }

        binding.btnStockSteam.setOnClickListener {
            startActivity(Intent(requireContext(), StockSteamActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            session.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
