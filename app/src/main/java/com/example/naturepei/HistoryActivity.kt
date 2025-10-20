package com.example.naturepei

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var analysisHistoryManager: AnalysisHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyRecyclerView = findViewById(R.id.history_recycler_view)
        analysisHistoryManager = AnalysisHistoryManager(this)

        loadHistory()
    }

    private fun loadHistory() {
        val historyList = analysisHistoryManager.getAnalysisHistory()
        val adapter = HistoryAdapter(historyList) { entry ->
            // Quand un élément de l'historique est cliqué, afficher les détails dans ResultActivity
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_IMAGE_URI, entry.imageUri)
                putExtra(ResultActivity.EXTRA_LOCAL_NAME, entry.localName)
                putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, entry.scientificName)
                putExtra(ResultActivity.EXTRA_DESCRIPTION, entry.description)
            }
            startActivity(intent)
        }
        historyRecyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadHistory() // Recharger l'historique au retour sur l'activité
    }
}

