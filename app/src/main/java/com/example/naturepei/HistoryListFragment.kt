package com.example.naturepei

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryListFragment : Fragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var analysisHistoryManager: AnalysisHistoryManager
    private lateinit var noDataTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyRecyclerView = view.findViewById(R.id.history_recycler_view)
        analysisHistoryManager = AnalysisHistoryManager(requireContext())
        noDataTextView = view.findViewById(R.id.history_no_data_text)

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory() // Recharger l'historique au retour sur le fragment
    }

    private fun loadHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val historyList = analysisHistoryManager.getAnalysisHistory()
            withContext(Dispatchers.Main) {
                if (historyList.isNotEmpty()) {
                    historyRecyclerView.visibility = View.VISIBLE
                    noDataTextView.visibility = View.GONE
                    val adapter = HistoryAdapter(historyList) { entry ->
                        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                            putExtra(ResultActivity.EXTRA_IMAGE_URI, entry.imageUri)
                            putExtra(ResultActivity.EXTRA_LOCAL_NAME, entry.localName)
                            putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, entry.scientificName)
                            putExtra(ResultActivity.EXTRA_DESCRIPTION, entry.description)
                        }
                        startActivity(intent)
                    }
                    historyRecyclerView.adapter = adapter
                } else {
                    historyRecyclerView.visibility = View.GONE
                    noDataTextView.visibility = View.VISIBLE
                }
            }
        }
    }
}
