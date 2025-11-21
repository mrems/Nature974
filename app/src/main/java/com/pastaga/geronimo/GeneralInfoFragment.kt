package com.pastaga.geronimo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pastaga.geronimo.R
import android.widget.ImageView

class GeneralInfoFragment : Fragment() {

    private var habitat: String? = null
    private var characteristics: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            habitat = it.getString(ARG_HABITAT)
            characteristics = it.getString(ARG_CHARACTERISTICS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_general_info, container, false)
        
        val habitatCard = view.findViewById<View>(R.id.info_card_habitat)
        val habitatTitle = habitatCard.findViewById<TextView>(R.id.title_info)
        val habitatContent = habitatCard.findViewById<TextView>(R.id.content_info)
        val habitatIcon = habitatCard.findViewById<ImageView>(R.id.icon_info)

        habitatTitle.text = "Habitat"
        habitatContent.text = habitat
        habitatIcon.setImageResource(R.drawable.tipi)
        habitatIcon.visibility = if (habitat != null && habitat != "N/C") View.VISIBLE else View.GONE
        habitatCard.visibility = if (habitat != null && habitat != "N/C") View.VISIBLE else View.GONE

        val characteristicsCard = view.findViewById<View>(R.id.info_card_characteristics)
        val characteristicsTitle = characteristicsCard.findViewById<TextView>(R.id.title_info)
        val characteristicsContent = characteristicsCard.findViewById<TextView>(R.id.content_info)
        val characteristicsIcon = characteristicsCard.findViewById<ImageView>(R.id.icon_info)

        characteristicsTitle.text = "Caractéristiques"
        characteristicsContent.text = characteristics
        characteristicsIcon.setImageResource(R.drawable.regle)
        characteristicsIcon.visibility = if (characteristics != null && characteristics != "N/C") View.VISIBLE else View.GONE
        characteristicsCard.visibility = if (characteristics != null && characteristics != "N/C") View.VISIBLE else View.GONE

        return view
    }

    companion object {
        private const val ARG_HABITAT = "habitat"
        private const val ARG_CHARACTERISTICS = "characteristics"

        @JvmStatic
        fun newInstance(habitat: String, characteristics: String) = GeneralInfoFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_HABITAT, habitat)
                putString(ARG_CHARACTERISTICS, characteristics)
            }
        }
    }
}
