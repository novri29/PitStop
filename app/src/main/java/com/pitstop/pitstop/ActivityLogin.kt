package com.pitstop.pitstop

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pitstop.save.AppDatabase
import com.pitstop.pitstop.MainActivity
import com.pitstop.pitstop.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var selectedRole = "ADMIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)

        // Toggle Pilihan Role - ADMIN
        binding.btnRoleAdmin.setOnClickListener {
            selectedRole = "ADMIN"

            // 1. Set Admin Aktif (Background Biru, Teks Putih, Icon Putih)
            binding.btnRoleAdmin.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_blue))
            binding.btnRoleAdmin.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            binding.btnRoleAdmin.iconTint = ContextCompat.getColorStateList(this, android.R.color.white)

            // 2. Set Kasir Non-aktif (Background Transparan, Teks Abu, Icon Abu)
            binding.btnRoleKasir.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            binding.btnRoleKasir.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            binding.btnRoleKasir.iconTint = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        }

        // Toggle Pilihan Role - KASIR
        binding.btnRoleKasir.setOnClickListener {
            selectedRole = "KASIR"

            // 1. Set Kasir Aktif (Background Biru, Teks Putih, Icon Putih)
            binding.btnRoleKasir.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_blue))
            binding.btnRoleKasir.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            binding.btnRoleKasir.iconTint = ContextCompat.getColorStateList(this, android.R.color.white)

            // 2. Set Admin Non-aktif (Background Transparan, Teks Abu, Icon Abu)
            binding.btnRoleAdmin.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            binding.btnRoleAdmin.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            binding.btnRoleAdmin.iconTint = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        }

        // Logic Process Login
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi username dan password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = db.userDao().login(username, password, selectedRole)
                if (user != null) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra("EXTRA_ROLE", user.role)
                        putExtra("EXTRA_USERNAME", user.username)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Username/Password atau Peran salah!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}