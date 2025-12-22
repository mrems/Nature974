package com.geronimo.geki

import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import coil.load
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_settings) // Charge directement le layout complet de la page de prÃ©fÃ©rences

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val settingsPreferences = SettingsPreferences(this)
        val currentUser = FirebaseAuth.getInstance().currentUser

        // RÃ©cupÃ©rer les vues du layout
        val profileImageView: ImageView = findViewById(R.id.profile_image)
        val userNameTextView: TextView = findViewById(R.id.user_profile_name)
        val userEmailTextView: TextView = findViewById(R.id.user_profile_email)
        val systemLanguageTextView: TextView = findViewById(R.id.system_language_value)
        val apiLanguageTextView: TextView = findViewById(R.id.api_language_value)
        val saveToGallerySwitch: Switch = findViewById(R.id.save_to_gallery_switch)
        val disableCameraIndicatorsSwitch: Switch = findViewById(R.id.disable_camera_indicators_switch)
        val locationSwitch: Switch = findViewById(R.id.location_switch)
        val darkThemeSwitch: Switch = findViewById(R.id.dark_theme_switch)

        // Mettre Ã  jour les informations de l'utilisateur
        if (currentUser != null) {
            userNameTextView.text = currentUser.displayName ?: "Non disponible"
            userEmailTextView.text = currentUser.email ?: "Non disponible"
            currentUser.photoUrl?.let { uri ->
                profileImageView.load(uri) {
                    crossfade(true)
                    placeholder(R.drawable.geki_icon)
                    error(R.drawable.geki_icon)
                }
            }
        } else {
            userNameTextView.text = "Non connectÃ©"
            userEmailTextView.text = ""
            profileImageView.load(R.drawable.geki_icon)
        }

        // Mettre Ã  jour les informations de langue
        val currentSystemLanguage = Locale.getDefault().displayLanguage
        systemLanguageTextView.text = currentSystemLanguage

        val apiLanguage = Locale.getDefault().language
        apiLanguageTextView.text = apiLanguage

        // Switch: Enregistrer dans la galerie (ne concerne QUE MediaStore)
        saveToGallerySwitch.isEnabled = true
        saveToGallerySwitch.isChecked = settingsPreferences.shouldSaveToGallery()
        saveToGallerySwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPreferences.setSaveToGallery(isChecked)
        }

        // Switch: Indicateurs (blancs) de la camÃ©ra
        // ON = indicateurs affichÃ©s, OFF = indicateurs cachÃ©s
        disableCameraIndicatorsSwitch.isEnabled = true
        disableCameraIndicatorsSwitch.isChecked = settingsPreferences.areCameraIndicatorsEnabled()
        disableCameraIndicatorsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPreferences.setCameraIndicatorsEnabled(isChecked)
        }

        // Switch: Localisation
        // ON = localisation activÃ©e (country/region envoyÃ©s au backend), OFF = aucune info de localisation envoyÃ©e
        locationSwitch.isEnabled = true
        locationSwitch.isChecked = settingsPreferences.isLocationEnabled()
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPreferences.setLocationEnabled(isChecked)
        }

        // Switch: ThÃ¨me sombre
        // ON = mode sombre forcÃ©, OFF = mode clair forcÃ©
        darkThemeSwitch.isEnabled = true
        darkThemeSwitch.setOnCheckedChangeListener(null)
        darkThemeSwitch.isChecked = settingsPreferences.isDarkThemeEnabled()
        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPreferences.setDarkThemeEnabled(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
