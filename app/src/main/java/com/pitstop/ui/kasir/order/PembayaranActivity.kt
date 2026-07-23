package com.pitstop.ui.kasir.order

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.pitstop.pitstop.databinding.ActivityPembayaranBinding
import com.pitstop.repository.TransaksiItemInput
import com.pitstop.save.entity.METODE_CASH
import com.pitstop.save.entity.METODE_QRIS
import com.pitstop.save.entity.METODE_TRANSFER
import com.pitstop.ui.kasir.TransaksiViewModel
import com.pitstop.util.CartManager
import com.pitstop.util.Formatter
import com.pitstop.util.SessionManager
import com.pitstop.util.ViewModelFactory

class PembayaranActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPembayaranBinding
    private lateinit var viewModel: TransaksiViewModel
    private lateinit var session: SessionManager
    private var metodeTerpilih = METODE_CASH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPembayaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        session = SessionManager(this)
        viewModel = ViewModelProvider(this, ViewModelFactory(this))[TransaksiViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }
        binding.tvTotal.text = Formatter.rupiah(CartManager.total())

        pilihMetode(METODE_CASH)
        binding.rowCash.setOnClickListener { pilihMetode(METODE_CASH) }
        binding.rowQris.setOnClickListener { pilihMetode(METODE_QRIS) }
        binding.rowTransfer.setOnClickListener { pilihMetode(METODE_TRANSFER) }

        binding.etJumlahDibayar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                hitungKembalian()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnBayar.setOnClickListener { prosesPembayaran() }
    }

    private fun pilihMetode(metode: String) {
        metodeTerpilih = metode
        binding.checkCash.visibility = if (metode == METODE_CASH) View.VISIBLE else View.INVISIBLE
        binding.checkQris.visibility = if (metode == METODE_QRIS) View.VISIBLE else View.INVISIBLE
        binding.checkTransfer.visibility = if (metode == METODE_TRANSFER) View.VISIBLE else View.INVISIBLE

        binding.areaCash.visibility = if (metode == METODE_CASH) View.VISIBLE else View.GONE
        binding.areaQris.visibility = if (metode == METODE_QRIS) View.VISIBLE else View.GONE
        binding.areaTransfer.visibility = if (metode == METODE_TRANSFER) View.VISIBLE else View.GONE
    }

    private fun hitungKembalian() {
        val dibayar = binding.etJumlahDibayar.text.toString().toDoubleOrNull() ?: 0.0
        val kembalian = dibayar - CartManager.total()
        binding.tvKembalian.text = Formatter.rupiah(if (kembalian > 0) kembalian else 0.0)
        binding.tvKembalian.setTextColor(
            getColor(if (kembalian < 0) com.pitstop.pitstop.R.color.red else com.pitstop.pitstop.R.color.green)
        )
    }

    private fun prosesPembayaran() {
        val total = CartManager.total()
        var jumlahDibayar = total
        var kembalian = 0.0

        if (metodeTerpilih == METODE_CASH) {
            jumlahDibayar = binding.etJumlahDibayar.text.toString().toDoubleOrNull() ?: 0.0
            if (jumlahDibayar < total) {
                Toast.makeText(this, "Jumlah dibayar kurang dari total pembayaran", Toast.LENGTH_SHORT).show()
                return
            }
            kembalian = jumlahDibayar - total
        }

        val items = CartManager.items.map {
            TransaksiItemInput(nama = it.nama, qty = it.qty, hargaSatuan = it.harga, menuKopiId = it.menuKopiId)
        }

        viewModel.simpanTransaksi(
            tipe = CartManager.tipeGabungan(),
            kasirUsername = session.getUsername(),
            items = items,
            catatan = CartManager.catatan,
            metodePembayaran = metodeTerpilih,
            jumlahDibayar = jumlahDibayar,
            kembalian = kembalian
        ) { transaksiId ->
            runOnUiThread {
                val intent = Intent(this, NotaStrukActivity::class.java)
                intent.putExtra(NotaStrukActivity.EXTRA_TRANSAKSI_ID, transaksiId.toInt())
                startActivity(intent)
                CartManager.reset()
                finish()
            }
        }
    }
}
