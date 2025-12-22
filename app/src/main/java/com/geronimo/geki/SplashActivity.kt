package com.geronimo.geki

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 2000 // 2 secondes
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Masquer complÃ¨tement les barres d'Ã©tat et de navigation
        hideSystemBars()

        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({ 
            val intent: Intent
            if (firebaseAuth.currentUser != null) {
                // Utilisateur dÃ©jÃ  connectÃ©, aller Ã  l'Ã©cran principal
                intent = Intent(this, MainActivity::class.java)
            } else {
                // Pas d'utilisateur connectÃ©
                // VÃ©rifier si l'intro a dÃ©jÃ  Ã©tÃ© vue
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val introCompleted = prefs.getBoolean("intro_completed", false)
                
                if (introCompleted) {
                    // Intro dÃ©jÃ  vue, aller directement Ã  la connexion Google
                    intent = Intent(this, GoogleSignInActivity::class.java)
                } else {
                    // Premier lancement, montrer l'intro
                    intent = Intent(this, IntroOnboardingActivity::class.java)
                }
            }
            startActivity(intent)
            finish()
        }, SPLASH_DELAY)
    }

    private fun hideSystemBars() {
        // Edge-to-edge : permet au contenu (et au background) de s'Ã©tendre sous les barres systÃ¨me
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // SÃ©curise l'absence de bande (couleurs transparentes) mÃªme si les barres rÃ©apparaissent briÃ¨vement
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Masquer complÃ¨tement les barres d'Ã©tat et de navigation (API-safe via compat)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Fallback legacy : certains appareils prÃ©-R rÃ©agissent mieux avec ce flag en plus
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}




