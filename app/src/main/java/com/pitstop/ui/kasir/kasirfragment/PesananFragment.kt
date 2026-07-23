package com.pitstop.ui.kasir.kasirfragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pitstop.pitstop.databinding.FragmentPesananBinding
import com.pitstop.save.entity.JENIS_MOBIL
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.save.entity.TIPE_MOBIL
import com.pitstop.save.entity.TIPE_MOTOR
import com.pitstop.ui.admin.StockSteamViewModel
import com.pitstop.ui.kasir.order.KonfirmasiLayananDialog
import com.pitstop.ui.kasir.order.PilihProdukActivity
import com.pitstop.util.ViewModelFactory


class PesananFragment : Fragment() {

    private var _binding: FragmentPesananBinding? = null
    private val binding get() = _binding!!
    private var hargaMotor = 0.0
    private var hargaMobil = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPesananBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fix: dorong toolbar agar tidak ketutupan status bar / icon baterai di SDK 35+
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarHeader) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            // Simpan tinggi asli toolbar sekali saja (sebelum ditambah padding)
            val originalHeight = resources.getDimensionPixelSize(
                androidx.appcompat.R.dimen.abc_action_bar_default_height_material
            )

            view.layoutParams.height = originalHeight + statusBarInsets.top
            view.requestLayout()

            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        val steamViewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[StockSteamViewModel::class.java]
        steamViewModel.layananList.observe(viewLifecycleOwner) { list ->
            list.find { it.jenis == JENIS_MOTOR }?.let { hargaMotor = it.harga }
            list.find { it.jenis == JENIS_MOBIL }?.let { hargaMobil = it.harga }
        }

        binding.btnMotor.setOnClickListener {
            if (hargaMotor <= 0.0) {
                Toast.makeText(requireContext(), "Harga Cuci Motor belum diatur Admin", Toast.LENGTH_SHORT).show()
            } else {
                KonfirmasiLayananDialog.tampilkan(requireContext(), TIPE_MOTOR, "Cuci Motor", hargaMotor)
            }
        }
        binding.btnMobil.setOnClickListener {
            if (hargaMobil <= 0.0) {
                Toast.makeText(requireContext(), "Harga Cuci Mobil belum diatur Admin", Toast.LENGTH_SHORT).show()
            } else {
                KonfirmasiLayananDialog.tampilkan(requireContext(), TIPE_MOBIL, "Cuci Mobil", hargaMobil)
            }
        }
        binding.btnCafe.setOnClickListener {
            startActivity(Intent(requireContext(), PilihProdukActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
