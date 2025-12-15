package com.pastaga.geronimo

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "settings_preferences"
        private const val KEY_SAVE_TO_GALLERY = "save_to_gallery"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
        private const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"
        // Nouveau comportement: true = indicateurs activés
        private const val KEY_CAMERA_INDICATORS_ENABLED = "camera_indicators_enabled"
        // Ancienne clé (compat rétro): true = indicateurs désactivés
        private const val KEY_DISABLE_CAMERA_INDICATORS_LEGACY = "disable_camera_indicators"
    }

    /**
     * Par défaut, on conserve le comportement actuel: enregistrer dans la galerie publique.
     */
    fun shouldSaveToGallery(): Boolean {
        return prefs.getBoolean(KEY_SAVE_TO_GALLERY, true)
    }

    fun setSaveToGallery(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_TO_GALLERY, enabled).apply()
    }

    /**
     * Par défaut, la localisation est activée (comportement actuel).
     * Quand désactivée, l'app ne doit pas envoyer country/region au backend.
     */
    fun isLocationEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCATION_ENABLED, true)
    }

    fun setLocationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply()
    }

    /**
     * Par défaut, les indicateurs sont activés (comportement actuel).
     * Compat rétro: si l'ancienne clé "disable_camera_indicators" existe, on l'inverse.
     */
    fun areCameraIndicatorsEnabled(): Boolean {
        return when {
            prefs.contains(KEY_CAMERA_INDICATORS_ENABLED) ->
                prefs.getBoolean(KEY_CAMERA_INDICATORS_ENABLED, true)
            prefs.contains(KEY_DISABLE_CAMERA_INDICATORS_LEGACY) ->
                !prefs.getBoolean(KEY_DISABLE_CAMERA_INDICATORS_LEGACY, false)
            else ->
                true
        }
    }

    fun setCameraIndicatorsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CAMERA_INDICATORS_ENABLED, enabled).apply()
    }

    /**
     * Par défaut: thème clair (comportement actuel).
     * Si true, l'app force le mode sombre via AppCompatDelegate.
     */
    fun isDarkThemeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME_ENABLED, false)
    }

    fun setDarkThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME_ENABLED, enabled).apply()
    }
}


