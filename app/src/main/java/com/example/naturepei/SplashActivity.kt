package com.example.naturepei

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.naturepei.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 1000 // 1 seconde
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({ 
            val intent: Intent
            if (firebaseAuth.currentUser != null) {
                // Utilisateur déjà connecté, aller à l'écran principal
                intent = Intent(this, MainActivity::class.java)
            } else {
                // Pas d'utilisateur connecté, aller à l'écran d'authentification
                intent = Intent(this, AuthActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, SPLASH_DELAY)
    }
}



