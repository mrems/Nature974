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

    private var peculiaritiesAndDangers: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            peculiaritiesAndDangers = it.getString(ARG_PECULIARITIES_AND_DANGERS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_peculiarities, container, false)
        
        val peculiaritiesCard = view.findViewById<View>(R.id.info_card_peculiarities_dangers)
        val peculiaritiesTitle = peculiaritiesCard.findViewById<TextView>(R.id.title_info)
        val peculiaritiesContent = peculiaritiesCard.findViewById<TextView>(R.id.content_info)
        val peculiaritiesIcon = peculiaritiesCard.findViewById<ImageView>(R.id.icon_info)

        peculiaritiesTitle.text = "Particularités et Dangers"
        peculiaritiesContent.text = peculiaritiesAndDangers
        peculiaritiesIcon.setImageResource(R.drawable.ic_new_options) // Utilisation d'une icône générique pour l'instant
        peculiaritiesIcon.visibility = if (peculiaritiesAndDangers != null && peculiaritiesAndDangers != "N/C") View.VISIBLE else View.GONE
        peculiaritiesCard.visibility = if (peculiaritiesAndDangers != null && peculiaritiesAndDangers != "N/C") View.VISIBLE else View.GONE

        return view
    }

    companion object {
        private const val ARG_PECULIARITIES_AND_DANGERS = "peculiarities_and_dangers"

        @JvmStatic
        fun newInstance(peculiaritiesAndDangers: String) = PeculiaritiesFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PECULIARITIES_AND_DANGERS, peculiaritiesAndDangers)
            }
        }
    }
}
