package com.example.naturepei.auth

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.naturepei.MainActivity
import com.example.naturepei.R
import com.example.naturepei.databinding.ActivityAuthBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Configuration du texte avec les images inline
        setupDescriptionWithIcons()

        // Configuration de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.example.naturepei.R.string.default_web_client_id))
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
                Toast.makeText(this, "Échec de la connexion Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.googleSignInButton.setOnClickListener { signInWithGoogle() }
    }

    private fun setupDescriptionWithIcons() {
        // On utilise un caractère spécial pour marquer où placer les images
        val text = "Geronimo c'est l'application qui peut reconnaître toutes les plantes\uFFFC et tous les animaux\uFFFC !!!"
        val spannableString = SpannableString(text)

        // Trouver la position du premier marqueur (après "plantes")
        val firstMarkerIndex = text.indexOf('\uFFFC')
        if (firstMarkerIndex != -1) {
            val plantDrawable = ContextCompat.getDrawable(this, R.drawable.plant)
            plantDrawable?.let {
                val size = (binding.authDescription.textSize * 1.1).toInt()
                val paddingLeft = (size * 0.3).toInt()
                it.setBounds(0, 0, size, size)
                val insetDrawable = InsetDrawable(it, paddingLeft, 0, 0, 0)
                insetDrawable.setBounds(0, 0, size + paddingLeft, size)
                val imageSpan = ImageSpan(insetDrawable, ImageSpan.ALIGN_BASELINE)
                spannableString.setSpan(imageSpan, firstMarkerIndex, firstMarkerIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Trouver la position du deuxième marqueur (après "animaux")
        val secondMarkerIndex = text.indexOf('\uFFFC', firstMarkerIndex + 1)
        if (secondMarkerIndex != -1) {
            val animalDrawable = ContextCompat.getDrawable(this, R.drawable.animal)
            animalDrawable?.let {
                val size = (binding.authDescription.textSize * 1.1).toInt()
                val paddingLeft = (size * 0.3).toInt()
                it.setBounds(0, 0, size, size)
                val insetDrawable = InsetDrawable(it, paddingLeft, 0, 0, 0)
                insetDrawable.setBounds(0, 0, size + paddingLeft, size)
                val imageSpan = ImageSpan(insetDrawable, ImageSpan.ALIGN_BASELINE)
                spannableString.setSpan(imageSpan, secondMarkerIndex, secondMarkerIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        binding.authDescription.text = spannableString
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
                    Toast.makeText(this, "Connexion Google réussie.", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this, "Échec de l'authentification Firebase avec Google: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
