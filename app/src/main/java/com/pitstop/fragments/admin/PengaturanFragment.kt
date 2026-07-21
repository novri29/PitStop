package com.pitstop.fragments.admin

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pitstop.helper.SessionManager // Import SessionManager
import com.pitstop.pitstop.LoginActivity
import com.pitstop.pitstop.databinding.FragmentPengaturanBinding

class PengaturanFragment : Fragment() {

    private var _binding: FragmentPengaturanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengaturanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Penanganan Zona Aman Status Bar HP
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val density = resources.displayMetrics.density
            val extraPaddingPx = (16 * density).toInt()

            v.setPadding(
                v.paddingLeft,
                statusBarInsets.top + extraPaddingPx,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }

        // Listener Tombol Logout
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        // 1. Hapus data sesi tersimpan di SharedPreferences
        val sessionManager = SessionManager(requireContext())
        sessionManager.clearSession()

        // 2. Feedback ke pengguna
        Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()

        // 3. Pindah ke LoginActivity & bersihkan backstack
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)

        // 4. Tutup host activity (MainActivity)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Mencegah memory leak
    }

    companion object {
        @JvmStatic
        fun newInstance() = PengaturanFragment()
    }
}