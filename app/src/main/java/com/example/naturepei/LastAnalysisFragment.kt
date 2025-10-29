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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LastAnalysisFragment : Fragment() {

    private lateinit var analysisHistoryManager: AnalysisHistoryManager
    private lateinit var imageAnalyzer: ImageAnalyzer

    private lateinit var lastAnalysisImageView: ImageView
    private lateinit var lastAnalysisLocalNameTextView: TextView
    private lateinit var lastAnalysisScientificNameTextView: TextView
    private lateinit var lastAnalysisTypeBadge: TextView
    private lateinit var lastAnalysisContentLayout: LinearLayout
    private lateinit var lastAnalysisInfoCardsContainer: LinearLayout

    private var lastEntry: AnalysisEntry? = null

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
        imageAnalyzer = ImageAnalyzer(requireContext())

        lastAnalysisImageView = view.findViewById(R.id.last_analysis_image)
        lastAnalysisLocalNameTextView = view.findViewById(R.id.last_analysis_local_name)
        lastAnalysisScientificNameTextView = view.findViewById(R.id.last_analysis_scientific_name)
        lastAnalysisTypeBadge = view.findViewById(R.id.last_analysis_type_badge)
        lastAnalysisContentLayout = view.findViewById(R.id.last_analysis_content)
        lastAnalysisInfoCardsContainer = view.findViewById(R.id.last_analysis_info_cards_container)


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
                    val entry = history[0]
                    lastEntry = entry
                    lastAnalysisContentLayout.visibility = View.VISIBLE

                    entry.imageUri.let { uriString ->
                        lastAnalysisImageView.setImageURI(Uri.parse(uriString))
                    }
                    lastAnalysisLocalNameTextView.text = entry.localName
                    lastAnalysisScientificNameTextView.text = entry.scientificName

                    lastAnalysisTypeBadge.text = entry.type ?: "N/C"
                    lastAnalysisTypeBadge.visibility = if (entry.type != null && entry.type != "N/C") View.VISIBLE else View.GONE
                    entry.type?.let { typeValue ->
                        when {
                            typeValue.contains("endémique", ignoreCase = true) -> lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_endemique)
                            typeValue.contains("introduit", ignoreCase = true) -> lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_introduit)
                            else -> lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_nc)
                        }
                    }

                    val cardHabitat = lastAnalysisInfoCardsContainer.getChildAt(0)
                    val cardCharacteristics = lastAnalysisInfoCardsContainer.getChildAt(1)
                    val cardReunionContext = lastAnalysisInfoCardsContainer.getChildAt(2)

                    setupInfoCard(cardHabitat, R.drawable.ic_habitat, "Habitat", entry.habitat)
                    setupInfoCard(cardCharacteristics, R.drawable.ic_characteristics, "Caractéristiques", entry.characteristics)
                    setupInfoCard(cardReunionContext, R.drawable.ic_reunion_context, "Contexte local", entry.localContext)

                } else {
                    lastAnalysisContentLayout.visibility = View.GONE
                    lastEntry = null
                }
            }
        }
    }

    private fun setupInfoCard(cardView: View, iconResId: Int, title: String, content: String?) {
        val icon = cardView.findViewById<ImageView>(R.id.icon_info)
        val titleTextView = cardView.findViewById<TextView>(R.id.title_info)
        val contentTextView = cardView.findViewById<TextView>(R.id.content_info)

        if (content != null && content != "N/C") {
            icon.setImageResource(iconResId)
            titleTextView.text = title
            contentTextView.text = content
            cardView.visibility = View.VISIBLE
        } else {
            cardView.visibility = View.GONE
        }
    }
}
