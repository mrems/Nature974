package com.pastaga.geronimo

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 1000 // 1 seconde
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Force des barres blanches avec icônes sombres, au cas où le thème serait surchargé par l'OEM
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            var flags = window.decorView.systemUiVisibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                flags = flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                flags = flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = flags
        }

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
}




