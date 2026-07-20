package com.pitstop.pitstop

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        // Toggle Pilihan Role
        binding.btnRoleAdmin.setOnClickListener {
            selectedRole = "ADMIN"
            binding.btnRoleAdmin.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
            binding.btnRoleAdmin.setTextColor(getColor(android.R.color.white))
            binding.btnRoleKasir.setBackgroundColor(getColor(android.R.color.transparent))
            binding.btnRoleKasir.setTextColor(getColor(android.R.color.darker_gray))
        }

        binding.btnRoleKasir.setOnClickListener {
            selectedRole = "KASIR"
            binding.btnRoleKasir.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
            binding.btnRoleKasir.setTextColor(getColor(android.R.color.white))
            binding.btnRoleAdmin.setBackgroundColor(getColor(android.R.color.transparent))
            binding.btnRoleAdmin.setTextColor(getColor(android.R.color.darker_gray))
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