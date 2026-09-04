package com.pitstop.ui.kasir.order

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.pitstop.pitstop.databinding.ActivityPilihLayananSteamBinding
import com.pitstop.pitstop.R
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.TIPE_MOTOR
import com.pitstop.save.entity.UKURAN_MOTOR_BESAR
import com.pitstop.save.entity.UKURAN_MOTOR_KECIL
import com.pitstop.save.entity.UKURAN_MOTOR_SEDANG
import com.pitstop.save.entity.UKURAN_PREMIUM_BESAR
import com.pitstop.save.entity.UKURAN_PREMIUM_KECIL
import com.pitstop.save.entity.UKURAN_PREMIUM_SEDANG
import com.pitstop.ui.admin.StockSteamViewModel
import com.pitstop.util.CartManager
import com.pitstop.util.Formatter
import com.pitstop.util.ViewModelFactory

/**
 * Layar Kasir untuk memilih ukuran Cuci Motor (Motor Kecil/Sedang/Besar atau
 * Premium Kecil/Sedang/Besar) beserta plat nomor kendaraan sebelum ditambahkan ke
 * keranjang. Menggantikan dialog konfirmasi lama yang cuma punya 1 harga tunggal.
 */
class PilihLayananSteamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPilihLayananSteamBinding
    private lateinit var viewModel: StockSteamViewModel
    private var layananMap: Map<String, Layanan> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPilihLayananSteamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fix: dorong toolbar agar tidak ketutupan status bar / icon baterai di SDK 35+
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarHeader) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val originalHeight = resources.getDimensionPixelSize(
                androidx.appcompat.R.dimen.abc_action_bar_default_height_material
            )
            view.layoutParams.height = originalHeight + statusBarInsets.top
            view.requestLayout()
            view.setPadding(view.paddingLeft, statusBarInsets.top, view.paddingRight, view.paddingBottom)
            insets
        }

        viewModel = ViewModelProvider(this, ViewModelFactory(this))[StockSteamViewModel::class.java]
        binding.btnBack.setOnClickListener { finish() }
        binding.etPlatNomor.setText(CartManager.platNomor)

        viewModel.layananList.observe(this) { list ->
            layananMap = list.associateBy { it.ukuran }
            layananMap[UKURAN_MOTOR_KECIL]?.let {
                binding.radioMotorKecil.text = "${UKURAN_MOTOR_KECIL} - ${Formatter.rupiah(it.harga)}"
            }
            layananMap[UKURAN_MOTOR_SEDANG]?.let {
                binding.radioMotorSedang.text = "${UKURAN_MOTOR_SEDANG} - ${Formatter.rupiah(it.harga)}"
            }
            layananMap[UKURAN_MOTOR_BESAR]?.let {
                binding.radioMotorBesar.text = "${UKURAN_MOTOR_BESAR} - ${Formatter.rupiah(it.harga)}"
            }
            layananMap[UKURAN_PREMIUM_KECIL]?.let {
                binding.radioPremiumKecil.text = "${UKURAN_PREMIUM_KECIL} - ${Formatter.rupiah(it.harga)}"
            }
            layananMap[UKURAN_PREMIUM_SEDANG]?.let {
                binding.radioPremiumSedang.text = "${UKURAN_PREMIUM_SEDANG} - ${Formatter.rupiah(it.harga)}"
            }
            layananMap[UKURAN_PREMIUM_BESAR]?.let {
                binding.radioPremiumBesar.text = "${UKURAN_PREMIUM_BESAR} - ${Formatter.rupiah(it.harga)}"
            }
        }

        binding.btnTambahKeranjang.setOnClickListener { tambahKeKeranjang() }
    }

    private fun ukuranTerpilih(): String = when (binding.rgUkuran.checkedRadioButtonId) {
        R.id.radioMotorSedang -> UKURAN_MOTOR_SEDANG
        R.id.radioMotorBesar -> UKURAN_MOTOR_BESAR
        R.id.radioPremiumKecil -> UKURAN_PREMIUM_KECIL
        R.id.radioPremiumSedang -> UKURAN_PREMIUM_SEDANG
        R.id.radioPremiumBesar -> UKURAN_PREMIUM_BESAR
        else -> UKURAN_MOTOR_KECIL
    }

    private fun tambahKeKeranjang() {
        val platNomor = binding.etPlatNomor.text.toString().trim()
        if (platNomor.isEmpty()) {
            Toast.makeText(this, "Isi plat nomor kendaraan dulu", Toast.LENGTH_SHORT).show()
            return
        }

        val ukuran = ukuranTerpilih()
        val layanan = layananMap[ukuran]
        if (layanan == null || layanan.harga <= 0.0) {
            Toast.makeText(this, "Harga untuk ukuran ini belum diatur Admin", Toast.LENGTH_SHORT).show()
            return
        }

        CartManager.platNomor = platNomor.uppercase()
        CartManager.tambahItem(layanan.nama, layanan.harga, TIPE_MOTOR, layananId = layanan.id)

        Toast.makeText(this, "${layanan.nama} ditambahkan", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, KeranjangActivity::class.java))
        finish()
    }
}