package com.example.cafesteam.ui.kasir.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.FragmentDashboardKasirBinding
import com.pitstop.save.entity.JENIS_MOBIL
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.save.entity.TIPE_CAFE
import com.pitstop.save.entity.TIPE_MOBIL
import com.pitstop.save.entity.TIPE_MOTOR
import com.pitstop.ui.admin.RingkasanViewModel
import com.pitstop.ui.admin.StockSteamViewModel
import com.pitstop.ui.kasir.order.KonfirmasiLayananDialog
import com.pitstop.ui.kasir.order.PilihProdukActivity
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory


class DashboardKasirFragment : Fragment() {

    private var _binding: FragmentDashboardKasirBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RingkasanViewModel
    private lateinit var steamViewModel: StockSteamViewModel

    private var hargaMotor = 0.0
    private var hargaMobil = 0.0
    private var unitTerpilih = TIPE_CAFE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardKasirBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[RingkasanViewModel::class.java]
        steamViewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[StockSteamViewModel::class.java]

        pilihUnit(TIPE_CAFE)

        binding.unitMobil.setOnClickListener { pilihUnit(TIPE_MOBIL) }
        binding.unitMotor.setOnClickListener { pilihUnit(TIPE_MOTOR) }
        binding.unitCafe.setOnClickListener { pilihUnit(TIPE_CAFE) }

        steamViewModel.layananList.observe(viewLifecycleOwner) { list ->
            list.find { it.jenis == JENIS_MOTOR }?.let { hargaMotor = it.harga }
            list.find { it.jenis == JENIS_MOBIL }?.let { hargaMobil = it.harga }
        }

        var jumlahHariIni = 0
        var omzetHariIni = 0.0

        viewModel.jumlahTransaksiHariIni.observe(viewLifecycleOwner) {
            jumlahHariIni = it
            binding.tvTotalTransaksi.text = it.toString()
            updateRataRata(jumlahHariIni, omzetHariIni)
        }
        viewModel.omzetHariIni.observe(viewLifecycleOwner) {
            omzetHariIni = it ?: 0.0
            binding.tvTotalOmzet.text = Formatter.rupiah(omzetHariIni)
            updateRataRata(jumlahHariIni, omzetHariIni)
        }
        viewModel.produkTerjualHariIni.observe(viewLifecycleOwner) {
            binding.tvTotalPesanan.text = (it ?: 0).toString()
        }

        binding.btnPesanBaru.setOnClickListener { mulaiPesanan() }
    }

    private fun updateRataRata(jumlah: Int, omzet: Double) {
        val rataRata = if (jumlah > 0) omzet / jumlah else 0.0
        binding.tvRataRata.text = Formatter.rupiah(rataRata)
    }

    private fun mulaiPesanan() {
        when (unitTerpilih) {
            TIPE_CAFE -> startActivity(Intent(requireContext(), PilihProdukActivity::class.java))
            TIPE_MOTOR -> {
                if (hargaMotor <= 0.0) {
                    Toast.makeText(requireContext(), "Harga Cuci Motor belum diatur Admin", Toast.LENGTH_SHORT).show()
                    return
                }
                KonfirmasiLayananDialog.tampilkan(requireContext(), TIPE_MOTOR, "Cuci Motor", hargaMotor)
            }
            TIPE_MOBIL -> {
                if (hargaMobil <= 0.0) {
                    Toast.makeText(requireContext(), "Harga Cuci Mobil belum diatur Admin", Toast.LENGTH_SHORT).show()
                    return
                }
                KonfirmasiLayananDialog.tampilkan(requireContext(), TIPE_MOBIL, "Cuci Mobil", hargaMobil)
            }
        }
    }

    private fun pilihUnit(tipe: String) {
        unitTerpilih = tipe
        viewModel.pilihUnit(tipe)

        resetChip(binding.unitMobil, binding.iconMobil, binding.labelMobil)
        resetChip(binding.unitMotor, binding.iconMotor, binding.labelMotor)
        resetChip(binding.unitCafe, binding.iconCafe, binding.labelCafe)

        when (tipe) {
            TIPE_MOBIL -> selectChip(binding.unitMobil, binding.iconMobil, binding.labelMobil)
            TIPE_MOTOR -> selectChip(binding.unitMotor, binding.iconMotor, binding.labelMotor)
            TIPE_CAFE -> selectChip(binding.unitCafe, binding.iconCafe, binding.labelCafe)
        }
    }

    private fun selectChip(container: View, icon: ImageView, label: TextView) {
        container.setBackgroundResource(R.drawable.bg_pill_selected)
        label.setTextColor(resources.getColor(R.color.white, null))
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(resources.getColor(R.color.white, null)))
    }

    private fun resetChip(container: View, icon: ImageView, label: TextView) {
        container.setBackgroundResource(R.drawable.bg_pill_outline)
        label.setTextColor(resources.getColor(R.color.black, null))
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(resources.getColor(R.color.black, null)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
