package com.pitstop.ui.admin.adminfragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pitstop.ui.login.LoginActivity
import com.pitstop.util.SessionManager
import com.pitstop.pitstop.databinding.FragmentPengaturanAdminBinding
import com.pitstop.save.AppDatabase
import com.pitstop.save.entity.ROLE_ADMIN
import com.pitstop.save.entity.ROLE_KASIR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengaturanAdminFragment : Fragment() {

    private var _binding: FragmentPengaturanAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private val userDao by lazy { AppDatabase.getInstance(requireContext()).userDao() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPengaturanAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        binding.tvUsername.text = session.getUsername()

        // Kelola Layanan Steam & Buka Stock Steam sudah dipindahkan ke Dashboard Admin

        setupUbahAkun()

        binding.btnLogout.setOnClickListener {
            session.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    private fun setupUbahAkun() {
        if (session.getRole() != ROLE_ADMIN) {
            // Safety net: sembunyikan/nonaktifkan section kalau bukan admin yang login
            binding.etAdminUsername.isEnabled = false
            binding.etAdminPasswordBaru.isEnabled = false
            binding.etKasirUsername.isEnabled = false
            binding.etKasirPasswordBaru.isEnabled = false
            binding.btnUpdateAdmin.setOnClickListener {
                Toast.makeText(requireContext(), "Hanya admin yang bisa mengubah akun", Toast.LENGTH_SHORT).show()
            }
            binding.btnUpdateKasir.setOnClickListener {
                Toast.makeText(requireContext(), "Hanya admin yang bisa mengubah akun", Toast.LENGTH_SHORT).show()
            }
            return
        }

        muatDataAkun()

        binding.btnUpdateAdmin.setOnClickListener {
            updateAkun(ROLE_ADMIN, binding.etAdminUsername, binding.etAdminPasswordBaru, isAkunSendiri = true)
        }

        binding.btnUpdateKasir.setOnClickListener {
            updateAkun(ROLE_KASIR, binding.etKasirUsername, binding.etKasirPasswordBaru, isAkunSendiri = false)
        }
    }

    private fun muatDataAkun() {
        viewLifecycleOwner.lifecycleScope.launch {
            val admin = withContext(Dispatchers.IO) { userDao.getUserByRole(ROLE_ADMIN) }
            val kasir = withContext(Dispatchers.IO) { userDao.getUserByRole(ROLE_KASIR) }

            admin?.let { binding.etAdminUsername.setText(it.username) }
            kasir?.let { binding.etKasirUsername.setText(it.username) }
        }
    }

    private fun updateAkun(role: String, etUsername: EditText, etPassword: EditText, isAkunSendiri: Boolean) {
        // Guard tambahan di level fungsi, bukan cuma di UI
        if (session.getRole() != ROLE_ADMIN) {
            Toast.makeText(requireContext(), "Hanya admin yang bisa mengubah akun", Toast.LENGTH_SHORT).show()
            return
        }

        val usernameBaru = etUsername.text.toString().trim()
        val passwordBaru = etPassword.text.toString().trim()

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

            // Refresh tampilan seketika (langsung terupdate otomatis)
            etPassword.setText("")
            etUsername.setText(usernameBaru)

            if (isAkunSendiri) {
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