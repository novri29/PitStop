package com.pitstop.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pitstop.save.entity.ROLE_ADMIN
import com.pitstop.save.entity.ROLE_KASIR
import com.pitstop.ui.admin.AdminMainActivity
import com.pitstop.util.SessionManager
import com.pitstop.pitstop.R
import com.pitstop.pitstop.databinding.ActivityLoginBinding
import com.pitstop.ui.kasir.KasirMainActivity
import com.pitstop.util.ViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var session: SessionManager
    private var peranTerpilih = ROLE_ADMIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        viewModel = ViewModelProvider(this, ViewModelFactory(this))[LoginViewModel::class.java]

        if (session.getUsername().isNotEmpty()) {
            goToDashboard(session.getRole())
            return
        }

        binding.tvHint.text = "Akun default:\nAdmin -> admin / admin123\nKasir -> kasir / kasir123"

        pilihPeran(ROLE_ADMIN)
        binding.roleAdmin.setOnClickListener { pilihPeran(ROLE_ADMIN) }
        binding.roleKasir.setOnClickListener { pilihPeran(ROLE_KASIR) }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(username, password) { user ->
                when {
                    user == null -> Toast.makeText(this, "Username atau password salah", Toast.LENGTH_SHORT).show()
                    user.role != peranTerpilih -> Toast.makeText(
                        this, "Akun ini bukan akun ${if (peranTerpilih == ROLE_ADMIN) "Admin" else "Kasir"}", Toast.LENGTH_SHORT
                    ).show()
                    else -> {
                        session.saveSession(user.username, user.role)
                        goToDashboard(user.role)
                    }
                }
            }
        }
    }

    private fun pilihPeran(role: String) {
        peranTerpilih = role
        if (role == ROLE_ADMIN) {
            binding.roleAdmin.setBackgroundResource(R.drawable.bg_pill_selected)
            binding.labelAdmin.setTextColor(getColor(R.color.white))
            binding.roleKasir.setBackgroundResource(R.drawable.bg_pill_outline)
            binding.labelKasir.setTextColor(getColor(R.color.black))
        } else {
            binding.roleKasir.setBackgroundResource(R.drawable.bg_pill_selected)
            binding.labelKasir.setTextColor(getColor(R.color.white))
            binding.roleAdmin.setBackgroundResource(R.drawable.bg_pill_outline)
            binding.labelAdmin.setTextColor(getColor(R.color.black))
        }
    }

    private fun goToDashboard(role: String) {
        val intent = if (role == ROLE_ADMIN) {
            Intent(this, AdminMainActivity::class.java)
        } else {
            Intent(this, KasirMainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
