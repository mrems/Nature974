package com.pastaga.geronimo

import android.content.Context
import android.content.SharedPreferences

class ModelPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "model_preferences"
        private const val KEY_SELECTED_MODEL = "selected_model"
        const val DEFAULT_MODEL = "gemini-2.5-flash-lite-preview-09-2025"
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setSelectedModel(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
    }

    fun getModelDisplayName(modelId: String): String {
        return when (modelId) {
            "gemini-2.5-flash-lite-preview-09-2025" -> "Rapide"
            "gemini-2.5-flash" -> "Équilibré"
            "gemini-3-pro-preview" -> "Ultra-précis"
            else -> "Inconnu"
        }
    }

    fun getModelCredits(modelId: String): Int {
        return when (modelId) {
            "gemini-2.5-flash-lite-preview-09-2025" -> 1
            "gemini-2.5-flash" -> 2
            "gemini-3-pro-preview" -> 5
            else -> 2 // Valeur par défaut
        }
    }
}

