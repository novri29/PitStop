package com.pitstop.ui.admin.adminfragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pitstop.ui.login.LoginActivity
import com.pitstop.util.SessionManager
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.FragmentPengaturanAdminBinding
import com.pitstop.save.AppDatabase
import com.pitstop.save.entity.ROLE_ADMIN
import com.pitstop.save.entity.ROLE_KASIR
import com.pitstop.save.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengaturanAdminFragment : Fragment() {

    private var _binding: FragmentPengaturanAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private val userDao by lazy { AppDatabase.getInstance(requireContext()).userDao() }

    // Role akun yang sedang dipilih di toggle (Admin/Kasir) untuk form Ganti Password
    private var roleTerpilih: String = ROLE_ADMIN
    private var adminUser: User? = null
    private var kasirUser: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPengaturanAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        binding.tvUsername.text = session.getUsername()

        // Kelola Layanan Steam & Buka Stock Steam sudah dipindahkan ke Dashboard Admin

        setupGantiPassword()

        binding.btnLogout.setOnClickListener {
            session.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    private fun setupGantiPassword() {
        if (session.getRole() != ROLE_ADMIN) {
            // Safety net: nonaktifkan section kalau bukan admin yang login
            binding.pilihAkunAdmin.isEnabled = false
            binding.pilihAkunKasir.isEnabled = false
            binding.etUsernameAkun.isEnabled = false
            binding.etPasswordAkun.isEnabled = false
            binding.btnSimpanAkun.setOnClickListener {
                Toast.makeText(requireContext(), "Hanya admin yang bisa mengubah akun", Toast.LENGTH_SHORT).show()
            }
            return
        }

        muatDataAkun()

        binding.pilihAkunAdmin.setOnClickListener { pilihToggleAkun(ROLE_ADMIN) }
        binding.pilihAkunKasir.setOnClickListener { pilihToggleAkun(ROLE_KASIR) }

        binding.btnSimpanAkun.setOnClickListener { simpanPerubahanAkun() }
    }

    private fun muatDataAkun() {
        viewLifecycleOwner.lifecycleScope.launch {
            adminUser = withContext(Dispatchers.IO) { userDao.getUserByRole(ROLE_ADMIN) }
            kasirUser = withContext(Dispatchers.IO) { userDao.getUserByRole(ROLE_KASIR) }
            tampilkanFormUntukRole(roleTerpilih)
        }
    }

    /** Ganti toggle Admin/Kasir: tampilan chip berubah, dan form terisi data akun yang dipilih. */
    private fun pilihToggleAkun(role: String) {
        roleTerpilih = role
        tampilkanFormUntukRole(role)
    }

    private fun tampilkanFormUntukRole(role: String) {
        val isAdmin = role == ROLE_ADMIN

        binding.pilihAkunAdmin.setBackgroundResource(if (isAdmin) R.drawable.bg_pill_selected else R.drawable.bg_pill_outline)
        binding.labelAkunAdmin.setTextColor(resources.getColor(if (isAdmin) R.color.white else R.color.black, null))
        binding.iconAkunAdmin.setColorFilter(resources.getColor(if (isAdmin) R.color.white else R.color.black, null))

        binding.pilihAkunKasir.setBackgroundResource(if (!isAdmin) R.drawable.bg_pill_selected else R.drawable.bg_pill_outline)
        binding.labelAkunKasir.setTextColor(resources.getColor(if (!isAdmin) R.color.white else R.color.black, null))
        binding.iconAkunKasir.setColorFilter(resources.getColor(if (!isAdmin) R.color.white else R.color.black, null))

        val user = if (isAdmin) adminUser else kasirUser
        binding.etUsernameAkun.setText(user?.username ?: "")
        binding.etPasswordAkun.setText("")
    }

    private fun simpanPerubahanAkun() {
        // Guard tambahan di level fungsi, bukan cuma di UI
        if (session.getRole() != ROLE_ADMIN) {
            Toast.makeText(requireContext(), "Hanya admin yang bisa mengubah akun", Toast.LENGTH_SHORT).show()
            return
        }

        val role = roleTerpilih
        val usernameBaru = binding.etUsernameAkun.text.toString().trim()
        val passwordBaru = binding.etPasswordAkun.text.toString().trim()

        if (usernameBaru.isEmpty()) {
            Toast.makeText(requireContext(), "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { userDao.getUserByRole(role) }
            if (user == null) {
                Toast.makeText(requireContext(), "Akun tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val existing = withContext(Dispatchers.IO) { userDao.getUserByUsername(usernameBaru) }
            if (existing != null && existing.id != user.id) {
                Toast.makeText(requireContext(), "Username sudah dipakai akun lain", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val updatedUser = user.copy(
                username = usernameBaru,
                password = if (passwordBaru.isEmpty()) user.password else passwordBaru
            )
            withContext(Dispatchers.IO) { userDao.update(updatedUser) }

            if (role == ROLE_ADMIN) {
                adminUser = updatedUser
            } else {
                kasirUser = updatedUser
            }

            // Refresh tampilan seketika (langsung terupdate otomatis)
            binding.etPasswordAkun.setText("")
            binding.etUsernameAkun.setText(usernameBaru)

            if (role == ROLE_ADMIN) {
                session.saveSession(usernameBaru, role)
                binding.tvUsername.text = usernameBaru
                Toast.makeText(requireContext(), "Akun admin berhasil diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Akun kasir berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}