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
        setContentView(R.layout.activity_splash)

        // Permettre au contenu de s'afficher sur les barres système
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Rendre les barres d'état et de navigation transparentes
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Définir l'apparence des icônes des barres système (clair ou foncé)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (controller != null) {
            controller.isAppearanceLightStatusBars = true // Icônes claires ou sombres pour la barre d'état
            controller.isAppearanceLightNavigationBars = true // Icônes claires ou sombres pour la barre de navigation
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




