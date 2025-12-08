package com.pastaga.geronimo

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
import android.util.Log
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import coil.load

class LastAnalysisFragment : Fragment() {

    private lateinit var analysisHistoryManager: AnalysisHistoryManager
    private lateinit var imageAnalyzer: ImageAnalyzer

    private lateinit var lastAnalysisImageView: ImageView
    private lateinit var lastAnalysisLocalNameTextView: TextView
    private lateinit var lastAnalysisScientificNameTextView: TextView
    private lateinit var lastAnalysisTypeBadge: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var dangerImageOverlay: ImageView

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
        tabLayout = view.findViewById(R.id.tab_layout_last_analysis)
        viewPager = view.findViewById(R.id.view_pager_last_analysis)
        dangerImageOverlay = view.findViewById(R.id.danger_image_overlay)

        lastAnalysisImageView.setOnClickListener {
            lastEntry?.let { entry ->
                val intent = Intent(requireContext(), FullScreenImageActivity::class.java).apply {
                    putExtra(FullScreenImageActivity.EXTRA_IMAGE_URI, entry.imageUri)
                    putExtra(FullScreenImageActivity.EXTRA_IS_TUTORIAL, entry.isTutorial)
                }
                startActivity(intent)
            }
        }

        loadLastAnalysis()
    }

    override fun onResume() {
        super.onResume()
        loadLastAnalysis()
    }

    private fun loadLastAnalysis() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Charger la dernière fiche consultée (analyse, ré-analyse ou consultation historique)
            var entry = analysisHistoryManager.getLastViewedCard()
            val history = analysisHistoryManager.getAnalysisHistory()
            // Vérifier si la dernière fiche consultée existe toujours dans l'historique
            if (entry != null && history.none { it.imageUri == entry!!.imageUri }) {
                // Si elle n'existe plus, trouver la première fiche non-tutorielle dans l'historique
                entry = history.firstOrNull { !it.isTutorial }
                if (entry != null) {
                    analysisHistoryManager.saveLastViewedCard(entry)
                } else {
                    // Si aucune fiche n'existe, utiliser une fiche exemple par défaut
                    entry = history.firstOrNull { it.isTutorial }
                    if (entry == null) {
                        // Si aucune fiche tutorielle n'existe, on pourrait en créer une par défaut ici si nécessaire
                        // Pour l'instant, on laisse vide si aucune fiche tutorielle n'est présente
                    } else {
                        analysisHistoryManager.saveLastViewedCard(entry)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                if (entry != null) {
                    lastEntry = entry

                    // Utiliser Coil pour charger l'image de manière fiable
                    // Détecter aussi les anciennes URIs de ressources Android (fallback pour fiches tutoriel existantes)
                    if (entry.isTutorial || entry.imageUri.startsWith("android.resource://")) {
                        // Pour les fiches tutoriel, charger directement depuis les ressources drawable
                        lastAnalysisImageView.load(R.drawable.illustration) {
                            crossfade(true)
                        }
                    } else {
                        lastAnalysisImageView.load(Uri.parse(entry.imageUri)) {
                            crossfade(true)
                        }
                    }
                    lastAnalysisLocalNameTextView.text = entry.localName
                    lastAnalysisScientificNameTextView.text = entry.scientificName

                    
                    lastAnalysisTypeBadge.text = entry.type ?: "N/C"
                    lastAnalysisTypeBadge.visibility = if (entry.type != null && entry.type != "N/C") View.VISIBLE else View.GONE

                    // Afficher l'icône danger sur la photo si l'espèce est dangereuse
                    dangerImageOverlay.visibility = if (entry.danger) View.VISIBLE else View.GONE
                    
                    if (entry.isTutorial) {
                        lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_origine)
                    } else {
                        entry.representativeColorHex?.let { colorHex ->
                            if (colorHex.startsWith("#") && colorHex.length == 7) {
                                try {
                                    val roundedDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.badge_rounded_dynamic_color) as GradientDrawable
                                    roundedDrawable.setColor(android.graphics.Color.parseColor(colorHex))
                                    lastAnalysisTypeBadge.background = roundedDrawable
                                } catch (e: IllegalArgumentException) {
                                    lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_nc) // Couleur par défaut en cas d'erreur
                                }
                            } else {
                                lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_nc) // Couleur par défaut si le format est incorrect
                            }
                        } ?: run { lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_nc) } // Couleur par défaut si la couleur est nulle
                    }

                    // Configurer le ViewPager2 et le TabLayout
                    val pagerAdapter = LastAnalysisFragmentPagerAdapter(this@LastAnalysisFragment, entry)
                    viewPager.adapter = pagerAdapter

                    TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab_with_icon, null)
                        val mainIcon = customView.findViewById<ImageView>(R.id.tab_main_icon)

                        when (position) {
                            0 -> mainIcon.setImageResource(R.drawable.onglet_1)
                            1 -> mainIcon.setImageResource(R.drawable.onglet_2)
                            2 -> mainIcon.setImageResource(R.drawable.onglet_3)
                            3 -> mainIcon.setImageResource(R.drawable.onglet_4)
                            else -> {}
                        }

                        tab.setCustomView(customView)
                    }.attach()

                } else {
                    // Si aucune fiche n'est trouvée, chercher une fiche tutorielle par défaut
                    val tutorialEntry = history.firstOrNull { it.isTutorial }
                    if (tutorialEntry != null) {
                        lastEntry = tutorialEntry

                        // Pour les fiches tutoriel, charger directement depuis les ressources drawable
                        lastAnalysisImageView.load(R.drawable.illustration) {
                            crossfade(true)
                        }
                        lastAnalysisLocalNameTextView.text = tutorialEntry.localName
                        lastAnalysisScientificNameTextView.text = tutorialEntry.scientificName

                        
                        lastAnalysisTypeBadge.text = getString(R.string.tutorial_type_badge_text)
                        lastAnalysisTypeBadge.visibility = View.VISIBLE
                        
                        lastAnalysisTypeBadge.setBackgroundResource(R.drawable.badge_origine)

                        // Afficher l'icône danger sur la photo si l'espèce est dangereuse
                        dangerImageOverlay.visibility = if (tutorialEntry.danger) View.VISIBLE else View.GONE

                        // Configurer le ViewPager2 et le TabLayout pour la fiche tutorielle
                        val pagerAdapter = LastAnalysisFragmentPagerAdapter(this@LastAnalysisFragment, tutorialEntry)
                        viewPager.adapter = pagerAdapter

                        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab_with_icon, null)
                            val mainIcon = customView.findViewById<ImageView>(R.id.tab_main_icon)

                            when (position) {
                                0 -> mainIcon.setImageResource(R.drawable.onglet_1)
                                1 -> mainIcon.setImageResource(R.drawable.onglet_2)
                                2 -> mainIcon.setImageResource(R.drawable.onglet_3)
                                3 -> mainIcon.setImageResource(R.drawable.onglet_4)
                                else -> {}
                            }

                            tab.setCustomView(customView)
                        }.attach()

                    } else {
                      
                        lastEntry = null
                    }
                }
            }
        }
    }

    // Adapter pour le ViewPager2
    private class LastAnalysisFragmentPagerAdapter(fragment: Fragment, private val entry: AnalysisEntry) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 4 // Nombre d'onglets

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ConfidenceAndAlternativesFragment.newInstance(entry)
                1 -> GeneralInfoFragment.newInstance(entry.habitat ?: "N/C", entry.characteristics ?: "N/C")
                2 -> PeculiaritiesFragment.newInstance(entry.Peculiarities ?: "N/C", entry.danger, entry.tutorialExplanationPeculiarities)
                3 -> LocalContextFragment.newInstance(entry.localContext ?: "N/C")
                else -> throw IllegalArgumentException("Invalid tab position")
            }
        }
    }
}

