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
    private lateinit var lastAnalysisDescriptionTextView: TextView
    private lateinit var lastAnalysisContentLayout: LinearLayout
    private lateinit var lastAnalysisOptionsButton: ImageButton

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
        lastAnalysisDescriptionTextView = view.findViewById(R.id.last_analysis_description)
        lastAnalysisContentLayout = view.findViewById(R.id.last_analysis_content)
        lastAnalysisOptionsButton = view.findViewById(R.id.last_analysis_options_button)

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
                                        description = response.description
                                    )
                                    analysisHistoryManager.updateAnalysisEntry(updated)
                                    // mettre à jour l'UI locale
                                    lastEntry = updated
                                    lastAnalysisLocalNameTextView.text = updated.localName
                                    lastAnalysisScientificNameTextView.text = updated.scientificName
                                    lastAnalysisDescriptionTextView.text = updated.description
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
                    lastAnalysisDescriptionTextView.text = "${entry.description}"

                } else {
                    lastAnalysisContentLayout.visibility = View.GONE
                    lastEntry = null
                }
            }
        }
    }
}
