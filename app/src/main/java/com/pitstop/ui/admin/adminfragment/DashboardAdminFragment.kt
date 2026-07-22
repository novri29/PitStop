package com.pitstop.ui.admin.adminfragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.FragmentDashboardAdminBinding
import com.pitstop.save.entity.TIPE_CAFE
import com.pitstop.save.entity.TIPE_MOBIL
import com.pitstop.save.entity.TIPE_MOTOR
import com.pitstop.ui.admin.AdminMainActivity
import com.pitstop.ui.admin.KategoriActivity
import com.pitstop.ui.admin.ProdukMinumanActivity
import com.pitstop.ui.admin.ResepMinumanActivity
import com.pitstop.ui.admin.RingkasanViewModel
import com.pitstop.ui.admin.StockBahanActivity
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory

class DashboardAdminFragment : Fragment() {

    private var _binding: FragmentDashboardAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RingkasanViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[RingkasanViewModel::class.java]

        pilihUnit(TIPE_CAFE)

        binding.unitMobil.setOnClickListener { pilihUnit(TIPE_MOBIL) }
        binding.unitMotor.setOnClickListener { pilihUnit(TIPE_MOTOR) }
        binding.unitCafe.setOnClickListener { pilihUnit(TIPE_CAFE) }

        viewModel.jumlahTransaksiHariIni.observe(viewLifecycleOwner) {
            binding.tvTotalTransaksi.text = it.toString()
        }
        viewModel.omzetHariIni.observe(viewLifecycleOwner) {
            binding.tvTotalOmzet.text = Formatter.rupiah(it ?: 0.0)
        }
        viewModel.produkTerjualHariIni.observe(viewLifecycleOwner) {
            binding.tvTotalProdukTerjual.text = (it ?: 0).toString()
        }
        viewModel.stokBahanHabis.observe(viewLifecycleOwner) {
            binding.tvStokHabis.text = it.toString()
        }

        binding.menuStockBahan.setOnClickListener { startActivity(Intent(requireContext(), StockBahanActivity::class.java)) }
        binding.menuResep.setOnClickListener { startActivity(Intent(requireContext(), ResepMinumanActivity::class.java)) }
        binding.menuProduk.setOnClickListener { startActivity(Intent(requireContext(), ProdukMinumanActivity::class.java)) }
        binding.menuKategori.setOnClickListener { startActivity(Intent(requireContext(), KategoriActivity::class.java)) }
        binding.menuLaporan.setOnClickListener { (activity as? AdminMainActivity)?.pindahKeTab(R.id.nav_laporan) }
        binding.menuPengaturan.setOnClickListener { (activity as? AdminMainActivity)?.pindahKeTab(R.id.nav_pengaturan) }
    }

    private fun pilihUnit(tipe: String) {
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
