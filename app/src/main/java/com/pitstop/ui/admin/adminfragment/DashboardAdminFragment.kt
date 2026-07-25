package com.pitstop.ui.admin.adminfragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.pitstop.ui.component.BarChartEntry
import com.pitstop.util.Formatter
import com.pitstop.util.TipePeriode
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

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

        // ---------- Filter Periode: Harian / Bulanan / Tahunan ----------
        pilihPeriode(TipePeriode.HARIAN)
        binding.btnHarian.setOnClickListener { pilihPeriode(TipePeriode.HARIAN) }
        binding.btnBulanan.setOnClickListener { pilihPeriode(TipePeriode.BULANAN) }
        binding.btnTahunan.setOnClickListener { pilihPeriode(TipePeriode.TAHUNAN) }

        binding.btnPeriodeSebelumnya.setOnClickListener {
            viewModel.geserPeriode(-1)
            muatGrafikOmzet()
        }
        binding.btnPeriodeBerikutnya.setOnClickListener {
            viewModel.geserPeriode(1)
            muatGrafikOmzet()
        }

        viewModel.labelPeriode.observe(viewLifecycleOwner) { binding.tvLabelPeriode.text = it }

        viewModel.jumlahTransaksiPeriode.observe(viewLifecycleOwner) {
            binding.tvTotalTransaksi.text = it.toString()
        }
        viewModel.omzetPeriode.observe(viewLifecycleOwner) {
            binding.tvTotalOmzet.text = Formatter.rupiah(it ?: 0.0)
        }
        viewModel.produkTerjualPeriode.observe(viewLifecycleOwner) {
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

    private fun muatGrafikOmzet() {
        binding.tvLabelGrafik.text = viewModel.getLabelGrafik()
        viewLifecycleOwner.lifecycleScope.launch {
            val data = viewModel.getGrafikSesuaiPeriode()
            val entries = data.map { (label, omzet) -> BarChartEntry(label, omzet.toFloat()) }
            binding.chartOmzet.setData(entries, formatRupiah = true)
        }
    }

    private fun pilihPeriode(tipe: TipePeriode) {
        viewModel.pilihPeriode(tipe)
        binding.tvLabelRingkasan.text = when (tipe) {
            TipePeriode.HARIAN -> "Ringkasan Harian"
            TipePeriode.BULANAN -> "Ringkasan Bulanan"
            TipePeriode.TAHUNAN -> "Ringkasan Tahunan"
        }

        resetTogglePeriode(binding.btnHarian)
        resetTogglePeriode(binding.btnBulanan)
        resetTogglePeriode(binding.btnTahunan)

        val terpilih = when (tipe) {
            TipePeriode.HARIAN -> binding.btnHarian
            TipePeriode.BULANAN -> binding.btnBulanan
            TipePeriode.TAHUNAN -> binding.btnTahunan
        }
        terpilih.setBackgroundResource(R.drawable.bg_pill_selected)
        terpilih.setTextColor(resources.getColor(R.color.white, null))

        muatGrafikOmzet()
    }

    private fun resetTogglePeriode(tv: android.widget.TextView) {
        tv.setBackgroundResource(R.drawable.bg_pill_outline)
        tv.setTextColor(resources.getColor(R.color.black, null))
    }

    private fun pilihUnit(tipe: String) {
        viewModel.pilihUnitPeriode(tipe)

        resetChip(binding.unitMobil, binding.iconMobil, binding.labelMobil)
        resetChip(binding.unitMotor, binding.iconMotor, binding.labelMotor)
        resetChip(binding.unitCafe, binding.iconCafe, binding.labelCafe)

        when (tipe) {
            TIPE_MOBIL -> selectChip(binding.unitMobil, binding.iconMobil, binding.labelMobil)
            TIPE_MOTOR -> selectChip(binding.unitMotor, binding.iconMotor, binding.labelMotor)
            TIPE_CAFE -> selectChip(binding.unitCafe, binding.iconCafe, binding.labelCafe)
        }
    }

    private fun selectChip(container: View, icon: android.widget.ImageView, label: android.widget.TextView) {
        container.setBackgroundResource(R.drawable.bg_pill_selected)
        label.setTextColor(resources.getColor(R.color.white, null))
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(resources.getColor(R.color.white, null)))
    }

    private fun resetChip(container: View, icon: android.widget.ImageView, label: android.widget.TextView) {
        container.setBackgroundResource(R.drawable.bg_pill_outline)
        label.setTextColor(resources.getColor(R.color.black, null))
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(resources.getColor(R.color.black, null)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
