package com.pastaga.geronimo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ConfidenceAndAlternativesFragment : Fragment() {

    private var analysisEntry: AnalysisEntry? = null

    // Couleurs du dégradé pour la barre de progression (doivent correspondre à custom_progress_bar.xml)
    private val startColor = Color.parseColor("#8BC34A") // Vert clair
    private val endColor = Color.parseColor("#2196F3") // Bleu clair

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
        val justificationText: TextView = view.findViewById(R.id.justification_text)
        val tutorialExplanationTextView: TextView = view.findViewById(R.id.tutorial_explanation_text)

        analysisEntry?.let { entry ->
            // Gestion commune du confidence score
            entry.confidenceScore?.let { score ->
                confidenceProgressBar.progress = score
                confidenceTextView.text = "${score}%"

                // Adapter la couleur du texte du score en fonction de la progression
                val interpolatedColor = interpolateColor(score)
                confidenceTextView.setTextColor(interpolatedColor)

                // Changer la description selon le score
                confidenceDescription.text = when {
                    score >= 90 -> "Identification très fiable"
                    score >= 70 -> "Identification fiable"
                    score >= 50 -> "Identification probable"
                    else -> "Identification incertaine"
                }
                confidenceCard.visibility = View.VISIBLE
            } ?: run { // Si confidenceScore est null
                confidenceCard.visibility = View.GONE
            }

            if (entry.isTutorial) {
                tutorialExplanationTextView.text = entry.tutorialExplanationFirstTab
                tutorialExplanationTextView.visibility = View.VISIBLE
                alternativesSection.visibility = View.GONE
            } else {
                tutorialExplanationTextView.visibility = View.GONE
                alternativesSection.visibility = View.VISIBLE

                // Gestion des alternatives
                entry.alternativeIdentifications?.let { alternatives ->
                    if (alternatives.isNotEmpty()) {
                        // Afficher les alternatives
                        alternativesRecyclerView.layoutManager = NonScrollingLinearLayoutManager(requireContext())
                        alternativesRecyclerView.adapter = AlternativeAdapter(alternatives, entry.confidenceScore) // Passer le confidenceScore ici
                        alternativesRecyclerView.visibility = View.VISIBLE

                        // Afficher le subtitle adapté au nombre d'alternatives
                        alternativesSubtitle.text = when (alternatives.size) {
                            1 -> "L'IA a identifié 1 autre possibilité"
                            else -> "L'IA a identifié ${alternatives.size} autres possibilités"
                        }
                        alternativesSubtitle.visibility = View.VISIBLE

                        // Cacher le texte de justification
                        justificationText.visibility = View.GONE
                    } else {
                        // Pas d'alternatives : afficher le texte de justification
                        alternativesRecyclerView.visibility = View.GONE
                        alternativesSubtitle.visibility = View.GONE

                        // Afficher le texte de justification si disponible
                        entry.justificationText?.let { justification ->
                            justificationText.text = justification
                            justificationText.visibility = View.VISIBLE
                        } ?: run {
                            // Si pas de justification, cacher toute la section
                            alternativesSection.visibility = View.GONE
                        }
                    }
                } ?: run { // Si alternativeIdentifications est null
                    alternativesSection.visibility = View.GONE
                }
            }
        } ?: run {
            // Gérer le cas où analysisEntry est null
            confidenceCard.visibility = View.GONE
            alternativesSection.visibility = View.GONE
            tutorialExplanationTextView.visibility = View.GONE
        }

        return view
    }

    /**
     * Interpole une couleur entre la couleur de dÃ©but et la couleur de fin en fonction d'une progression (0-100).
     */
    private fun interpolateColor(progress: Int): Int {
        val fraction = progress / 100f
        val inverseFraction = 1 - fraction

        val a = (Color.alpha(startColor) * inverseFraction + Color.alpha(endColor) * fraction).toInt()
        val r = (Color.red(startColor) * inverseFraction + Color.red(endColor) * fraction).toInt()
        val g = (Color.green(startColor) * inverseFraction + Color.green(endColor) * fraction).toInt()
        val b = (Color.blue(startColor) * inverseFraction + Color.blue(endColor) * fraction).toInt()

        return Color.argb(a, r, g, b)
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
