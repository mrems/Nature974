package com.pastaga.geronimo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class IntroPage4Fragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro_page4, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Bouton offre Premium (100 crédits - 9.99$)
        val btnPremium = view.findViewById<Button>(R.id.btn_premium)
        btnPremium.setOnClickListener {
            // TODO: Intégrer le système d'achat
            Toast.makeText(context, "Starter Pack - 100 crédits pour 9.99$", Toast.LENGTH_SHORT).show()
            // Pour l'instant, passer à la page suivante
            (activity as? IntroOnboardingActivity)?.goToNextPage()
        }
        
        // Bouton offre Test (20 crédits - 0.52$)
        val btnTest = view.findViewById<Button>(R.id.btn_test)
        btnTest.setOnClickListener {
            // TODO: Intégrer le système d'achat
            Toast.makeText(context, "Try it out - 20 crédits pour 0.52$", Toast.LENGTH_SHORT).show()
            // Pour l'instant, passer à la page suivante
            (activity as? IntroOnboardingActivity)?.goToNextPage()
        }
        
        // Bouton Skip
        val skipButton = view.findViewById<TextView>(R.id.page3_button)
        skipButton.setOnClickListener {
            (activity as? IntroOnboardingActivity)?.goToNextPage()
        }
    }
}
