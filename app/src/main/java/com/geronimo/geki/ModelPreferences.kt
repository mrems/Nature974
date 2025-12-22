package com.geronimo.geki

import android.content.Context
import android.content.SharedPreferences

class ModelPreferences(context: Context) {

    companion object {
        const val DEFAULT_MODEL = "2" // Niveau Équilibré par défaut
    }

    fun getModelDisplayName(modelId: String): String {
        return when (modelId) {
            "1" -> "Rapide"
            "2" -> "Ã‰quilibrÃ©"
            "3" -> "Ultra-prÃ©cis"
            else -> "Inconnu"
        }
    }

    fun getModelCredits(modelId: String): Int {
        return when (modelId) {
            "1" -> 1
            "2" -> 2
            "3" -> 5
            else -> 2 // Valeur par dÃ©faut
        }
    }
}

