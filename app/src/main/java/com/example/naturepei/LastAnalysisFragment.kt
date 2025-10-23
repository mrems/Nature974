package com.example.naturepei

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
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
    private lateinit var lastAnalysisOptionsButton: ImageButton
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
        lastAnalysisOptionsButton = view.findViewById(R.id.last_analysis_options_button)
        lastAnalysisContentLayout = view.findViewById(R.id.last_analysis_content)
        lastAnalysisInfoCardsContainer = view.findViewById(R.id.last_analysis_info_cards_container)

        lastAnalysisOptionsButton.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            val deleteItem = popup.menu.add(0, 0, 0, "Effacer")
            deleteItem.isEnabled = false // grisé / désactivé
            popup.menu.add(0, 1, 1, "Re-analyser")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        val entry = lastEntry ?: return@setOnMenuItemClickListener true
                        val loadingDialog = LoadingDialogFragment()
                        loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
                        lifecycleScope.launch(Dispatchers.IO) {
                            val response = imageAnalyzer.analyzeImage(Uri.parse(entry.imageUri))
                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()
                                if (response != null) {
                                    val updated = entry.copy(
                                        localName = response.localName,
                                        scientificName = response.scientificName,
                                        description = "N/C", // Description n'est plus fournie par l'API
                                        type = response.type,
                                        habitat = response.habitat,
                                        characteristics = response.characteristics,
                                        reunionContext = response.reunionContext
                                    )
                                    analysisHistoryManager.updateAnalysisEntry(updated)
                                    // mettre à jour l'UI locale
                                    lastEntry = updated
                                    loadLastAnalysis() // Recharger l'UI après la ré-analyse
                                    Toast.makeText(requireContext(), "Ré-analyse terminée !", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Échec de la ré-analyse.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

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
                    setupInfoCard(cardReunionContext, R.drawable.ic_reunion_context, "Contexte local", entry.reunionContext)

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
