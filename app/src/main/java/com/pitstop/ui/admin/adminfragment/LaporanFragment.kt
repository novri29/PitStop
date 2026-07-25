package com.pitstop.ui.admin.adminfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pitstop.adapter.LaporanAdapter
import com.pitstop.adapter.ProdukTerlarisAdapter
import com.pitstop.pitstop.R
import com.pitstop.ui.admin.DetailTransaksiDialog
import com.pitstop.util.ExcelExporter
import com.pitstop.util.Formatter
import com.pitstop.pitstop.databinding.FragmentLaporanBinding
import com.pitstop.save.entity.Transaksi
import com.pitstop.ui.admin.RingkasanViewModel
import com.pitstop.ui.component.BarChartEntry
import com.pitstop.util.TipePeriode
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RingkasanViewModel
    private lateinit var adapter: LaporanAdapter
    private lateinit var produkTerlarisAdapter: ProdukTerlarisAdapter

    /** true = tampilkan seluruh riwayat tanpa filter tanggal (mode lama) */
    private var modeSemua = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[RingkasanViewModel::class.java]

        adapter = LaporanAdapter(onClick = { transaksi ->
            viewLifecycleOwner.lifecycleScope.launch {
                val detail = viewModel.getDetail(transaksi.id)
                DetailTransaksiDialog.tampilkan(requireContext(), transaksi, detail)
            }
        })
        binding.rvLaporan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLaporan.adapter = adapter

        produkTerlarisAdapter = ProdukTerlarisAdapter()
        binding.rvProdukTerlaris.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProdukTerlaris.adapter = produkTerlarisAdapter

        binding.btnHarian.setOnClickListener { pilihPeriode(TipePeriode.HARIAN) }
        binding.btnBulanan.setOnClickListener { pilihPeriode(TipePeriode.BULANAN) }
        binding.btnTahunan.setOnClickListener { pilihPeriode(TipePeriode.TAHUNAN) }
        binding.btnSemua.setOnClickListener { pilihSemua() }

        binding.btnPeriodeSebelumnya.setOnClickListener {
            viewModel.geserPeriode(-1)
            muatGrafikDanProdukTerlaris()
        }
        binding.btnPeriodeBerikutnya.setOnClickListener {
            viewModel.geserPeriode(1)
            muatGrafikDanProdukTerlaris()
        }

        viewModel.labelPeriode.observe(viewLifecycleOwner) { binding.tvLabelPeriode.text = it }

        // Mode periode (Harian/Bulanan/Tahunan) - list & total ikut rentang tanggal terpilih
        viewModel.transaksiPeriode.observe(viewLifecycleOwner) { list ->
            if (!modeSemua) tampilkanList(list)
        }
        viewModel.omzetPeriode.observe(viewLifecycleOwner) {
            if (!modeSemua) binding.tvTotalOmzet.text = "Total Omzet: ${Formatter.rupiah(it ?: 0.0)}"
        }
        viewModel.jumlahTransaksiPeriode.observe(viewLifecycleOwner) {
            if (!modeSemua) binding.tvJumlahTransaksi.text = "$it transaksi"
        }

        // Mode Semua - seluruh riwayat tanpa filter tanggal
        viewModel.getAllTransaksi().observe(viewLifecycleOwner) { list ->
            if (modeSemua) tampilkanList(list)
        }
        viewModel.totalOmzetKeseluruhan.observe(viewLifecycleOwner) {
            if (modeSemua) binding.tvTotalOmzet.text = "Total Omzet: ${Formatter.rupiah(it ?: 0.0)}"
        }

        pilihPeriode(TipePeriode.HARIAN)
        binding.btnExport.setOnClickListener { exportExcel() }
    }

    private fun tampilkanList(list: List<Transaksi>) {
        adapter.submitList(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        if (modeSemua) binding.tvJumlahTransaksi.text = "${list.size} transaksi"
    }

    private fun pilihPeriode(tipe: TipePeriode) {
        modeSemua = false
        viewModel.pilihPeriode(tipe)
        binding.rowNavigasiPeriode.visibility = View.VISIBLE

        binding.tvLabelProdukTerlaris.text = when (tipe) {
            TipePeriode.HARIAN -> "Produk Terlaris (Hari Ini)"
            TipePeriode.BULANAN -> "Produk Terlaris (Bulan Ini)"
            TipePeriode.TAHUNAN -> "Produk Terlaris (Tahun Ini)"
        }

        resetToggle(binding.btnHarian); resetToggle(binding.btnBulanan)
        resetToggle(binding.btnTahunan); resetToggle(binding.btnSemua)

        val terpilih = when (tipe) {
            TipePeriode.HARIAN -> binding.btnHarian
            TipePeriode.BULANAN -> binding.btnBulanan
            TipePeriode.TAHUNAN -> binding.btnTahunan
        }
        setToggleAktif(terpilih)
        muatGrafikDanProdukTerlaris()
    }

    private fun pilihSemua() {
        modeSemua = true
        binding.rowNavigasiPeriode.visibility = View.GONE
        binding.tvLabelProdukTerlaris.text = "Produk Terlaris (Semua)"
        binding.tvLabelGrafik.text = "Tren Omzet 7 Hari Terakhir"

        resetToggle(binding.btnHarian); resetToggle(binding.btnBulanan)
        resetToggle(binding.btnTahunan); resetToggle(binding.btnSemua)
        setToggleAktif(binding.btnSemua)

        viewLifecycleOwner.lifecycleScope.launch {
            val produk = viewModel.getProdukTerlarisSemua()
            produkTerlarisAdapter.submitList(produk)
            binding.tvProdukTerlarisKosong.visibility = if (produk.isEmpty()) View.VISIBLE else View.GONE

            // Mode "Semua" tidak punya satu rentang tanggal spesifik, jadi grafik tetap tampilkan tren 7 hari terakhir
            val data = viewModel.getOmzet7HariTerakhir()
            val entries = data.map { (label, omzet) -> BarChartEntry(label, omzet.toFloat()) }
            binding.chartOmzet.setData(entries, formatRupiah = true)
        }
    }

    /** Muat ulang grafik omzet & daftar Produk Terlaris sesuai filter periode+unit yang sedang aktif. */
    private fun muatGrafikDanProdukTerlaris() {
        if (modeSemua) return
        binding.tvLabelGrafik.text = viewModel.getLabelGrafik()
        viewLifecycleOwner.lifecycleScope.launch {
            val produk = viewModel.getProdukTerlarisPeriode()
            produkTerlarisAdapter.submitList(produk)
            binding.tvProdukTerlarisKosong.visibility = if (produk.isEmpty()) View.VISIBLE else View.GONE

            val data = viewModel.getGrafikSesuaiPeriode()
            val entries = data.map { (label, omzet) -> BarChartEntry(label, omzet.toFloat()) }
            binding.chartOmzet.setData(entries, formatRupiah = true)
        }
    }

    private fun setToggleAktif(tv: TextView) {
        tv.setBackgroundResource(R.drawable.bg_pill_selected)
        tv.setTextColor(resources.getColor(R.color.white, null))
    }

    private fun resetToggle(tv: TextView) {
        tv.setBackgroundResource(R.drawable.bg_pill_outline)
        tv.setTextColor(resources.getColor(R.color.black, null))
    }

    private fun exportExcel() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = if (modeSemua) viewModel.getSemuaTransaksi() else viewModel.getTransaksiPeriodeSuspend()
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
