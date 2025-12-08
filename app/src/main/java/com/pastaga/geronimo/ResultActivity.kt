package com.pastaga.geronimo

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.view.ViewGroup
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import coil.load

class ResultActivity : AppCompatActivity() {

    private lateinit var imageAnalyzer: ImageAnalyzer
    private lateinit var analysisHistoryManager: AnalysisHistoryManager

    private lateinit var resultImageView: ImageView
    private lateinit var resultLocalNameTextView: TextView
    private lateinit var resultScientificNameTextView: TextView
    private lateinit var resultTypeBadge: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var dangerImageOverlay: ImageView

    private var currentEntry: AnalysisEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        imageAnalyzer = ImageAnalyzer(this)
        analysisHistoryManager = AnalysisHistoryManager(this)

        resultImageView = findViewById(R.id.result_image_view)
        resultLocalNameTextView = findViewById(R.id.result_local_name)
        resultScientificNameTextView = findViewById(R.id.result_scientific_name)
        resultTypeBadge = findViewById(R.id.result_type_badge)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        dangerImageOverlay = findViewById(R.id.danger_image_overlay)

        // Récupérer les données de l'Intent
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val localName = intent.getStringExtra(EXTRA_LOCAL_NAME)
        val scientificName = intent.getStringExtra(EXTRA_SCIENTIFIC_NAME)
        val type = intent.getStringExtra(EXTRA_TYPE)
        val habitat = intent.getStringExtra(EXTRA_HABITAT)
        val characteristics = intent.getStringExtra(EXTRA_CHARACTERISTICS)
        val localContext = intent.getStringExtra(EXTRA_LOCAL_CONTEXT)
        val Peculiarities = intent.getStringExtra(EXTRA_PECULIARITIES) // Nouveau champ
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) // Pour rétrocompatibilité
        val representativeColorHex = intent.getStringExtra(EXTRA_REPRESENTATIVE_COLOR_HEX) // Rétablir l'extraction de la couleur
        val danger = intent.getBooleanExtra(EXTRA_DANGER, false) // Nouveau champ danger
        val confidenceScore = intent.getIntExtra(EXTRA_CONFIDENCE_SCORE, -1) // Nouveau champ confidenceScore
        val alternativeIdentifications: List<AlternativeIdentification>? = intent.getParcelableArrayListExtra(EXTRA_ALTERNATIVE_IDENTIFICATIONS)
        val justificationText = intent.getStringExtra(EXTRA_JUSTIFICATION_TEXT) // Nouveau champ justificationText
        val isTutorial = intent.getBooleanExtra(EXTRA_IS_TUTORIAL, false) // Flag pour les fiches tutoriel

        if (imageUriString != null && localName != null && scientificName != null) {
            currentEntry = AnalysisEntry(
                imageUri = imageUriString,
                localName = localName,
                scientificName = scientificName,
                type = type,
                habitat = habitat,
                characteristics = characteristics,
                localContext = localContext,
                Peculiarities = Peculiarities, // Assigner le nouveau champ
                description = description ?: "N/C", // Assurer une valeur par défaut
                representativeColorHex = representativeColorHex,
                danger = danger, // Assigner le nouveau champ danger
                confidenceScore = if (confidenceScore != -1) confidenceScore else null, // Assigner le nouveau champ
                alternativeIdentifications = alternativeIdentifications, // Assigner le nouveau champ
                justificationText = justificationText, // Assigner le nouveau champ
                isTutorial = isTutorial // Flag pour les fiches tutoriel
            )
            displayResult(currentEntry!!)
        } else {
            Toast.makeText(this, "Erreur: Données de résultat manquantes.", Toast.LENGTH_LONG).show()
            finish()
        }

        resultImageView.setOnClickListener { // Ajout du listener pour le clic sur l'image
            currentEntry?.let { entry ->
                val intent = Intent(this, FullScreenImageActivity::class.java).apply {
                    putExtra(FullScreenImageActivity.EXTRA_IMAGE_URI, entry.imageUri)
                    putExtra(FullScreenImageActivity.EXTRA_IS_TUTORIAL, entry.isTutorial)
                }
                startActivity(intent)
            }
        }
    }

    private fun displayResult(entry: AnalysisEntry) {
        // Utiliser Coil pour charger l'image de manière fiable
        // Détecter aussi les anciennes URIs de ressources Android (fallback pour fiches tutoriel existantes)
        if (entry.isTutorial || entry.imageUri.startsWith("android.resource://")) {
            // Pour les fiches tutoriel, charger directement depuis les ressources drawable
            resultImageView.load(R.drawable.illustration) {
                crossfade(true)
            }
        } else {
            resultImageView.load(Uri.parse(entry.imageUri)) {
                crossfade(true)
            }
        }
        resultLocalNameTextView.text = entry.localName
        resultScientificNameTextView.text = entry.scientificName

        // Afficher le badge de type et appliquer la couleur représentative
        entry.type?.let { typeValue ->
            if (typeValue != "N/C") {
                resultTypeBadge.text = typeValue
                entry.representativeColorHex?.let { colorHex ->
                    if (colorHex.startsWith("#") && colorHex.length == 7) {
                        try {
                            val roundedDrawable = ContextCompat.getDrawable(this, R.drawable.badge_rounded_dynamic_color) as GradientDrawable
                            roundedDrawable.setColor(android.graphics.Color.parseColor(colorHex))
                            resultTypeBadge.background = roundedDrawable
                        } catch (e: IllegalArgumentException) {
                            resultTypeBadge.setBackgroundResource(R.drawable.badge_nc) // Couleur par défaut en cas d'erreur
                        }
                    } else {
                        resultTypeBadge.setBackgroundResource(R.drawable.badge_nc) // Couleur par défaut si le format est incorrect
                    }
                } ?: run { resultTypeBadge.setBackgroundResource(R.drawable.badge_nc) }
                resultTypeBadge.visibility = View.VISIBLE
            } else {
                resultTypeBadge.visibility = View.GONE
            }
        } ?: run { resultTypeBadge.visibility = View.GONE }

        // Afficher l'icône danger sur la photo si l'espèce est dangereuse
        dangerImageOverlay.visibility = if (entry.danger) View.VISIBLE else View.GONE

        // Configurer le ViewPager2 et le TabLayout
        val pagerAdapter = ResultFragmentPagerAdapter(this, entry)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val customView = LayoutInflater.from(this).inflate(R.layout.custom_tab_with_icon, null)
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

    }

    // Adapter pour le ViewPager2
    private class ResultFragmentPagerAdapter(activity: AppCompatActivity, private val entry: AnalysisEntry) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4 // Nombre d'onglets (un de plus pour le nouvel onglet)

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ConfidenceAndAlternativesFragment.newInstance(entry)
                1 -> GeneralInfoFragment.newInstance(entry.habitat ?: "N/C", entry.characteristics ?: "N/C")
                2 -> PeculiaritiesFragment.newInstance(entry.Peculiarities ?: "N/C", entry.danger)
                3 -> LocalContextFragment.newInstance(entry.localContext ?: "N/C")
                else -> throw IllegalArgumentException("Invalid tab position")
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_LOCAL_NAME = "localName"
        const val EXTRA_SCIENTIFIC_NAME = "scientificName"
        const val EXTRA_TYPE = "type"
        const val EXTRA_HABITAT = "habitat"
        const val EXTRA_CHARACTERISTICS = "characteristics"
        const val EXTRA_LOCAL_CONTEXT = "localContext"
        const val EXTRA_PECULIARITIES = "Peculiarities" // Nouveau extra
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_REPRESENTATIVE_COLOR_HEX = "representativeColorHex"
        const val EXTRA_DANGER = "danger" // Nouveau extra pour le danger
        const val EXTRA_CONFIDENCE_SCORE = "confidenceScore"
        const val EXTRA_ALTERNATIVE_IDENTIFICATIONS = "alternativeIdentifications"
        const val EXTRA_JUSTIFICATION_TEXT = "justificationText"
        const val EXTRA_IS_TUTORIAL = "isTutorial"
    }
}


