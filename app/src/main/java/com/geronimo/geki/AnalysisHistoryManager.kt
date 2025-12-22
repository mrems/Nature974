package com.geronimo.geki

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AnalysisEntry(
    val imageUri: String,
    val localName: String,
    val scientificName: String,
    val type: String? = null, // Nouveau champ, peut Ãªtre nul pour la rÃ©trocompatibilitÃ©
    val habitat: String? = null, // Nouveau champ, peut Ãªtre nul pour la rÃ©trocompatibilitÃ©
    val characteristics: String? = null, // Nouveau champ, peut Ãªtre nul pour la rÃ©trocompatibilitÃ©
    val localContext: String? = null, // Nouveau champ, peut Ãªtre nul pour la rÃ©trocompatibilitÃ©
    val Peculiarities: String? = null, // Nouveau champ pour les particularitÃ©s
    val country: String? = null, // Localisation: pays
    val region: String? = null, // Localisation: rÃ©gion/adminArea
    val description: String, // Ancien champ, pour la rÃ©trocompatibilitÃ©
    val timestamp: Long? = System.currentTimeMillis(), // Nouveau champ pour l'ordre chronologique
    val isTutorial: Boolean = false, // Flag pour identifier les fiches d'exemple/tutoriel
    val representativeColorHex: String? = null,
    val danger: Boolean = false, // Nouveau champ pour indiquer le danger
    val confidenceScore: Int? = null, // Score de confiance de l'IA (0-100)
    val alternativeIdentifications: List<AlternativeIdentification>? = null, // Autres possibilitÃ©s identifiÃ©es
    val justificationText: String? = null, // Texte de justification quand aucune alternative n'est proposÃ©e
    val tutorialExplanationFirstTab: String? = null, // Nouveau champ pour le texte explicatif du premier onglet du tutoriel
    val tutorialExplanationPeculiarities: String? = null // Nouveau champ pour le texte explicatif de l'onglet particularitÃ©s
) : Parcelable {
    // Le constructeur secondaire a Ã©tÃ© supprimÃ© pour Ã©viter les ambiguÃ¯tÃ©s et simplifier la gestion des champs. 
    // Toutes les instanciations de AnalysisEntry doivent maintenant utiliser le constructeur principal avec tous les arguments.
}

@Parcelize
data class AlternativeIdentification(
    val scientificName: String,
    val localName: String? = null,
    val difference: String // Comment la diffÃ©rencier de l'identification principale
) : Parcelable

class AnalysisHistoryManager(context: Context) {

    companion object {
        const val PREFS_NAME: String = "naturepei_analysis_history"
        const val KEY_HISTORY_LIST: String = "history_list"
        const val KEY_LAST_VIEWED_CARD: String = "last_viewed_card"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val sharedPreferencesPublic: SharedPreferences
        get() = sharedPreferences
    private val gson = Gson()

    fun saveAnalysisEntry(entry: AnalysisEntry) {
        val historyList = getAnalysisHistory().toMutableList()
        historyList.add(0, entry) // Ajouter la nouvelle entrÃ©e en haut de la liste
        val json = gson.toJson(historyList)
        sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).apply()
    }

    fun getAnalysisHistory(): List<AnalysisEntry> {
        val json = sharedPreferences.getString(KEY_HISTORY_LIST, null)
        return if (json != null) {
            val type = object : TypeToken<List<AnalysisEntry>>() {}.type
            val history = gson.fromJson<List<AnalysisEntry>>(json, type)
            history
        } else {
            emptyList()
        }
    }

    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_HISTORY_LIST).apply()
    }

    fun deleteAnalysisEntry(entry: AnalysisEntry) {
        val historyList = getAnalysisHistory().toMutableList()
        historyList.remove(entry)
        val json = gson.toJson(historyList)
        sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).apply()
    }

    fun updateAnalysisEntry(updatedEntry: AnalysisEntry) {
        val historyList = getAnalysisHistory().toMutableList()
        val index = historyList.indexOfFirst { it.imageUri == updatedEntry.imageUri }
        if (index != -1) {
            historyList[index] = updatedEntry
            val json = gson.toJson(historyList)
            sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).commit() // Utiliser commit() pour une sauvegarde synchrone
        }
    }

    // Sauvegarder la derniÃ¨re fiche consultÃ©e
    fun saveLastViewedCard(entry: AnalysisEntry) {
        val json = gson.toJson(entry)
        sharedPreferences.edit().putString(KEY_LAST_VIEWED_CARD, json).commit() // Utiliser commit() pour une sauvegarde synchrone
    }

    // RÃ©cupÃ©rer la derniÃ¨re fiche consultÃ©e
    fun getLastViewedCard(): AnalysisEntry? {
        val json = sharedPreferences.getString(KEY_LAST_VIEWED_CARD, null)
        return if (json != null) {
            val entry = gson.fromJson(json, AnalysisEntry::class.java)
            entry
        } else {
            null
        }
    }
}


