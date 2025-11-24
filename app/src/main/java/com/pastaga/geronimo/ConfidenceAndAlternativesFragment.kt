package com.pastaga.geronimo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ConfidenceAndAlternativesFragment : Fragment() {

    private var analysisEntry: AnalysisEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            analysisEntry = it.getParcelable(ARG_ANALYSIS_ENTRY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_confidence_and_alternatives, container, false)

        val confidenceCard = view.findViewById<View>(R.id.confidence_card)
        val confidenceProgressBar: ProgressBar = view.findViewById(R.id.confidence_score_progress)
        val confidenceTextView: TextView = view.findViewById(R.id.confidence_score_text)
        val confidenceDescription: TextView = view.findViewById(R.id.confidence_description)
        val alternativesSection = view.findViewById<View>(R.id.alternatives_section)
        val alternativesRecyclerView: RecyclerView = view.findViewById(R.id.alternatives_recycler_view)
        val alternativesSubtitle: TextView = view.findViewById(R.id.alternatives_subtitle)

        analysisEntry?.let { entry ->
            // Gestion du confidence score
            entry.confidenceScore?.let { score ->
                confidenceProgressBar.progress = score
                confidenceTextView.text = "${score}%"
                
                // Changer la description selon le score
                confidenceDescription.text = when {
                    score >= 90 -> "Identification très fiable"
                    score >= 70 -> "Identification fiable"
                    score >= 50 -> "Identification probable"
                    else -> "Identification incertaine"
                }
            } ?: run { // Si confidenceScore est null
                confidenceCard.visibility = View.GONE
            }

            // Gestion des alternatives
            entry.alternativeIdentifications?.let { alternatives ->
                if (alternatives.isNotEmpty()) {
                    alternativesRecyclerView.layoutManager = NonScrollingLinearLayoutManager(requireContext())
                    alternativesRecyclerView.adapter = AlternativeAdapter(alternatives)
                    
                    // Adapter le texte selon le nombre d'alternatives
                    alternativesSubtitle.text = when (alternatives.size) {
                        1 -> "L'IA a identifié 1 autre possibilité"
                        else -> "L'IA a identifié ${alternatives.size} autres possibilités"
                    }
                } else {
                    alternativesSection.visibility = View.GONE
                }
            } ?: run { // Si alternativeIdentifications est null
                alternativesSection.visibility = View.GONE
            }
        } ?: run {
            // Gérer le cas où analysisEntry est null
            confidenceCard.visibility = View.GONE
            alternativesSection.visibility = View.GONE
        }

        return view
    }

    companion object {
        private const val ARG_ANALYSIS_ENTRY = "analysis_entry"

        fun newInstance(analysisEntry: AnalysisEntry): ConfidenceAndAlternativesFragment {
            val fragment = ConfidenceAndAlternativesFragment()
            val args = Bundle()
            args.putParcelable(ARG_ANALYSIS_ENTRY, analysisEntry)
            fragment.arguments = args
            return fragment
        }
    }
}
