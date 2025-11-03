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

class ResultActivity : AppCompatActivity() {

    private lateinit var imageAnalyzer: ImageAnalyzer
    private lateinit var analysisHistoryManager: AnalysisHistoryManager

    private lateinit var resultImageView: ImageView
    private lateinit var resultLocalNameTextView: TextView
    private lateinit var resultScientificNameTextView: TextView
    private lateinit var resultTypeBadge: TextView

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


        // Récupérer les données de l'Intent
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val localName = intent.getStringExtra(EXTRA_LOCAL_NAME)
        val scientificName = intent.getStringExtra(EXTRA_SCIENTIFIC_NAME)
        val type = intent.getStringExtra(EXTRA_TYPE)
        val habitat = intent.getStringExtra(EXTRA_HABITAT)
        val characteristics = intent.getStringExtra(EXTRA_CHARACTERISTICS)
        val localContext = intent.getStringExtra(EXTRA_LOCAL_CONTEXT)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) // Pour rétrocompatibilité

        if (imageUriString != null && localName != null && scientificName != null) {
            currentEntry = AnalysisEntry(
                imageUri = imageUriString,
                localName = localName,
                scientificName = scientificName,
                type = type,
                habitat = habitat,
                characteristics = characteristics,
                localContext = localContext,
                description = description ?: "N/C" // Assurer une valeur par défaut
            )
            Log.d("ResultActivity", "Current Entry pour affichage: $currentEntry") // LOG AJOUTÉ ICI
            displayResult(currentEntry!!)
        } else {
            Toast.makeText(this, "Erreur: Données de résultat manquantes.", Toast.LENGTH_LONG).show()
            finish()
        }

        resultImageView.setOnClickListener { // Ajout du listener pour le clic sur l'image
            currentEntry?.imageUri?.let { uriString ->
                val intent = Intent(this, FullScreenImageActivity::class.java).apply {
                    putExtra(FullScreenImageActivity.EXTRA_IMAGE_URI, uriString)
                }
                startActivity(intent)
            }
        }
    }

    private fun displayResult(entry: AnalysisEntry) {
        entry.imageUri.let { uriString ->
            resultImageView.setImageURI(Uri.parse(uriString))
        }
        resultLocalNameTextView.text = entry.localName
        resultScientificNameTextView.text = entry.scientificName

        // Afficher le badge de type
        entry.type?.let { typeValue ->
            if (typeValue != "N/C") {
                resultTypeBadge.text = typeValue
                when {
                    typeValue.contains("endémique", ignoreCase = true) -> resultTypeBadge.setBackgroundResource(R.drawable.badge_endemique)
                    typeValue.contains("introduit", ignoreCase = true) -> resultTypeBadge.setBackgroundResource(R.drawable.badge_introduit)
                    else -> resultTypeBadge.setBackgroundResource(R.drawable.badge_nc)
                }
                resultTypeBadge.visibility = View.VISIBLE
            } else {
                resultTypeBadge.visibility = View.GONE
            }
        } ?: run { resultTypeBadge.visibility = View.GONE }

        // Récupérer le conteneur des cartes
        val infoCardsContainer = findViewById<LinearLayout>(R.id.info_cards_container)

        // Configurer chaque carte
        val cardHabitat = infoCardsContainer.getChildAt(0)
        val cardCharacteristics = infoCardsContainer.getChildAt(1)
        val cardReunionContext = infoCardsContainer.getChildAt(2)

        setupInfoCard(cardHabitat, R.drawable.tipi, "Habitat", entry.habitat)
        setupInfoCard(cardCharacteristics, R.drawable.regle, "Caractéristiques", entry.characteristics)
        setupInfoCard(cardReunionContext, R.drawable.local, "Contexte local", entry.localContext)
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


    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_LOCAL_NAME = "localName"
        const val EXTRA_SCIENTIFIC_NAME = "scientificName"
        const val EXTRA_TYPE = "type"
        const val EXTRA_HABITAT = "habitat"
        const val EXTRA_CHARACTERISTICS = "characteristics"
        const val EXTRA_LOCAL_CONTEXT = "localContext"
        const val EXTRA_DESCRIPTION = "description"
    }
}


