package com.pitstop.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.pitstop.pitstop.databinding.ActivitySplashBinding
import com.pitstop.ui.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    // Berapa lama logo "tertahan" di layar setelah animasi selesai, sebelum pindah ke Login
    private val holdAfterAnimMs = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateLogo()
    }

    private fun animateLogo() {
        val logo = binding.imgLogoSplash
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f
        logo.alpha = 0f

        val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.6f, 1f)
        val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.6f, 1f)
        val alpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 800
            interpolator = OvershootInterpolator(2.2f)
            doOnEnd {
                Handler(Looper.getMainLooper()).postDelayed({ goToLogin() }, holdAfterAnimMs)
            }
            start()
        }
    }

    private fun goToLogin() {
        if (isFinishing) return
        startActivity(Intent(this, LoginActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
