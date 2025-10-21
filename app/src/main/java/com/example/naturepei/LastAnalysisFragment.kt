package com.example.naturepei

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LastAnalysisFragment : Fragment() {

    private lateinit var analysisHistoryManager: AnalysisHistoryManager

    private lateinit var lastAnalysisImageView: ImageView
    private lateinit var lastAnalysisLocalNameTextView: TextView
    private lateinit var lastAnalysisScientificNameTextView: TextView
    private lateinit var lastAnalysisDescriptionTextView: TextView
    private lateinit var lastAnalysisContentLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_last_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analysisHistoryManager = AnalysisHistoryManager(requireContext())

        lastAnalysisImageView = view.findViewById(R.id.last_analysis_image)
        lastAnalysisLocalNameTextView = view.findViewById(R.id.last_analysis_local_name)
        lastAnalysisScientificNameTextView = view.findViewById(R.id.last_analysis_scientific_name)
        lastAnalysisDescriptionTextView = view.findViewById(R.id.last_analysis_description)
        lastAnalysisContentLayout = view.findViewById(R.id.last_analysis_content)

        loadLastAnalysis()
    }

    override fun onResume() {
        super.onResume()
        loadLastAnalysis()
    }

    private fun loadLastAnalysis() {
        lifecycleScope.launch(Dispatchers.IO) {
            val history = analysisHistoryManager.getAnalysisHistory()
            withContext(Dispatchers.Main) {
                if (history.isNotEmpty()) {
                    val lastEntry = history[0]
                    lastAnalysisContentLayout.visibility = View.VISIBLE

                    lastEntry.imageUri.let { uriString ->
                        lastAnalysisImageView.setImageURI(Uri.parse(uriString))
                    }
                    lastAnalysisLocalNameTextView.text = "Nom Local / Créole : ${lastEntry.localName}"
                    lastAnalysisScientificNameTextView.text = "Nom Scientifique : ${lastEntry.scientificName}"
                    lastAnalysisDescriptionTextView.text = "Description : ${lastEntry.description}"

                } else {
                    lastAnalysisContentLayout.visibility = View.GONE
                }
            }
        }
    }
}
