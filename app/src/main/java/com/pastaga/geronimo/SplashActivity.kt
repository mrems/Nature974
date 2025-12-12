package com.pastaga.geronimo

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

        // Masquer complètement les barres d'état et de navigation
        hideSystemBars()

        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({ 
            val intent: Intent
            if (firebaseAuth.currentUser != null) {
                // Utilisateur déjà connecté, aller à l'écran principal
                intent = Intent(this, MainActivity::class.java)
            } else {
                // Pas d'utilisateur connecté
                // Vérifier si l'intro a déjà été vue
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val introCompleted = prefs.getBoolean("intro_completed", false)
                
                if (introCompleted) {
                    // Intro déjà vue, aller directement à la connexion Google
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
        // Edge-to-edge : permet au contenu (et au background) de s'étendre sous les barres système
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Sécurise l'absence de bande (couleurs transparentes) même si les barres réapparaissent brièvement
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Masquer complètement les barres d'état et de navigation (API-safe via compat)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Fallback legacy : certains appareils pré-R réagissent mieux avec ce flag en plus
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}




