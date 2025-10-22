package com.example.naturepei

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AnalysisHistoryManager(context: Context) {

    private val PREFS_NAME = "naturepei_analysis_history"
    private val KEY_HISTORY_LIST = "history_list"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAnalysisEntry(entry: AnalysisEntry) {
        val historyList = getAnalysisHistory().toMutableList()
        historyList.add(0, entry) // Ajouter la nouvelle entrée en haut de la liste
        val json = gson.toJson(historyList)
        sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).apply()
    }

    fun getAnalysisHistory(): List<AnalysisEntry> {
        val json = sharedPreferences.getString(KEY_HISTORY_LIST, null)
        return if (json != null) {
            val type = object : TypeToken<List<AnalysisEntry>>() {}.type
            gson.fromJson(json, type)
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
            sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).apply()
        }
    }
}

