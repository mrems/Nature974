package com.geronimo.geki

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSignInActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forcer la suppression de l'ActionBar
        supportActionBar?.hide()
        
        // Pas de layout, on lance directement la connexion
        firebaseAuth = FirebaseAuth.getInstance()

        // Configuration de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialisation de l'ActivityResultLauncher pour Google Sign-In
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Ã‰chec de la connexion Google: ${e.message}", Toast.LENGTH_LONG).show()
                // Retourner Ã  l'intro en cas d'Ã©chec
                finish()
            }
        }

        // Lancer automatiquement la connexion Google
        signInWithGoogle()
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Connexion rÃ©ussie, transition vers MainActivity
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this, "Ã‰chec de l'authentification Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
    }

    private fun navigateToMainActivity() {
        // Marquer que l'intro a Ã©tÃ© complÃ©tÃ©e (seulement aprÃ¨s connexion rÃ©ussie)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("intro_completed", true).apply()
        
        val intent = Intent(this, MainActivity::class.java)
        // Nettoyer tout le back stack pour Ã©viter de revenir Ã  l'onboarding
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        // Si l'utilisateur appuie sur retour, revenir Ã  l'intro onboarding
        super.onBackPressed()
        finish()
    }
}

