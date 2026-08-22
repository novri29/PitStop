package com.pitstop.ui.kasir.kasirfragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.FragmentDashboardKasirBinding
import com.pitstop.save.entity.TIPE_CAFE
import com.pitstop.save.entity.TIPE_MOTOR
import com.pitstop.ui.admin.RingkasanViewModel
import com.pitstop.ui.component.BarChartEntry
import com.pitstop.ui.kasir.order.PilihLayananSteamActivity
import com.pitstop.ui.kasir.order.PilihProdukActivity
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory
import kotlinx.coroutines.launch
import com.pitstop.util.NotificationHelper

class DashboardKasirFragment : Fragment() {

    private var _binding: FragmentDashboardKasirBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RingkasanViewModel

    private var unitTerpilih = TIPE_CAFE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardKasirBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelFactory(requireContext()))[RingkasanViewModel::class.java]

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

        pilihUnit(TIPE_CAFE)

        binding.unitMotor.setOnClickListener { pilihUnit(TIPE_MOTOR) }
        binding.unitCafe.setOnClickListener { pilihUnit(TIPE_CAFE) }

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

        NotificationHelper.createChannel(requireContext())

        binding.notificationContainer.setOnClickListener {

            NotificationHelper.showNotifications(
                requireContext()
            )

            NotificationHelper.markAllAsRead(
                requireContext()
            )

            updateNotificationBadge()
        }

        muatGrafikOmzet()
    }

    private fun muatGrafikOmzet() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = viewModel.getOmzet7HariTerakhir()
            val entries = data.map { (label, omzet) -> BarChartEntry(label, omzet.toFloat()) }
            binding.chartOmzet.setData(entries, formatRupiah = true)
        }
    }

    private fun updateRataRata(jumlah: Int, omzet: Double) {
        val rataRata = if (jumlah > 0) omzet / jumlah else 0.0
        binding.tvRataRata.text = Formatter.rupiah(rataRata)
    }

    private fun mulaiPesanan() {
        when (unitTerpilih) {
            TIPE_CAFE -> startActivity(Intent(requireContext(), PilihProdukActivity::class.java))
            TIPE_MOTOR -> startActivity(Intent(requireContext(), PilihLayananSteamActivity::class.java))
        }
    }

    private fun pilihUnit(tipe: String) {
        unitTerpilih = tipe
        viewModel.pilihUnit(tipe)

        resetChip(binding.unitMotor, binding.iconMotor, binding.labelMotor)
        resetChip(binding.unitCafe, binding.iconCafe, binding.labelCafe)

        when (tipe) {
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

    private fun updateNotificationBadge() {

        val count = NotificationHelper.getUnreadCount(
            requireContext()
        )

        if (count > 0) {

            binding.notificationBadge.visibility = View.VISIBLE

            binding.notificationBadge.text =
                if (count > 99) "99+" else count.toString()

        } else {

            binding.notificationBadge.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()

        if (_binding != null) {
            updateNotificationBadge()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
