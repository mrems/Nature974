package com.pastaga.geronimo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pastaga.geronimo.R
import android.widget.ImageView

class PeculiaritiesFragment : Fragment() {

    private var Peculiarities: String? = null
    private var danger: Boolean = false // Nouveau champ
    private var tutorialExplanation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            Peculiarities = it.getString(ARG_PECULIARITIES)
            danger = it.getBoolean(ARG_DANGER, false) // Récupérer la valeur du danger
            tutorialExplanation = it.getString(ARG_TUTORIAL_EXPLANATION)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_peculiarities, container, false)
        
        val peculiaritiesCard = view.findViewById<View>(R.id.info_card_peculiarities_dangers)
        val peculiaritiesTitle = peculiaritiesCard.findViewById<TextView>(R.id.title_info)
        val peculiaritiesContent = peculiaritiesCard.findViewById<TextView>(R.id.content_info)
        val dangerTitleIcon = peculiaritiesCard.findViewById<ImageView>(R.id.icon_danger_title)

        peculiaritiesTitle.text = getString(R.string.tutorial_info_card_title_peculiarities)
        // Afficher le texte d'explication du tutoriel dans le contenu de la carte si disponible, sinon les particularités normales.
        peculiaritiesContent.text = tutorialExplanation ?: Peculiarities

        if (danger) {
            dangerTitleIcon.visibility = View.VISIBLE
        } else {
            dangerTitleIcon.visibility = View.GONE
        }

        peculiaritiesCard.visibility = if ((Peculiarities != null && Peculiarities != "N/C") || (tutorialExplanation != null && tutorialExplanation != "N/C")) View.VISIBLE else View.GONE

        return view
    }

    companion object {
        private const val ARG_PECULIARITIES = "peculiarities"
        private const val ARG_DANGER = "danger"
        private const val ARG_TUTORIAL_EXPLANATION = "tutorial_explanation"

        @JvmStatic
        fun newInstance(Peculiarities: String, danger: Boolean, tutorialExplanation: String? = null) = PeculiaritiesFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PECULIARITIES, Peculiarities)
                putBoolean(ARG_DANGER, danger) // Passer la valeur du danger
                putString(ARG_TUTORIAL_EXPLANATION, tutorialExplanation)
            }
        }
    }
}
