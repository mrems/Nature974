package com.example.naturepei

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.CollapsingToolbarLayout
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
    private lateinit var optionsButton: ImageButton

    private var currentEntry: AnalysisEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Activer le bouton retour
        toolbar.setNavigationOnClickListener { finish() } // Gérer le clic sur le bouton retour

        imageAnalyzer = ImageAnalyzer(this)
        analysisHistoryManager = AnalysisHistoryManager(this)

        resultImageView = findViewById(R.id.result_image_view)
        resultLocalNameTextView = findViewById(R.id.result_local_name)
        resultScientificNameTextView = findViewById(R.id.result_scientific_name)
        resultTypeBadge = findViewById(R.id.result_type_badge)
        optionsButton = findViewById(R.id.result_options_button)


        // Récupérer les données de l'Intent
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val localName = intent.getStringExtra(EXTRA_LOCAL_NAME)
        val scientificName = intent.getStringExtra(EXTRA_SCIENTIFIC_NAME)
        val type = intent.getStringExtra(EXTRA_TYPE)
        val habitat = intent.getStringExtra(EXTRA_HABITAT)
        val characteristics = intent.getStringExtra(EXTRA_CHARACTERISTICS)
        val reunionContext = intent.getStringExtra(EXTRA_REUNION_CONTEXT)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) // Pour rétrocompatibilité

        if (imageUriString != null && localName != null && scientificName != null) {
            currentEntry = AnalysisEntry(
                imageUri = imageUriString,
                localName = localName,
                scientificName = scientificName,
                type = type,
                habitat = habitat,
                characteristics = characteristics,
                reunionContext = reunionContext,
                description = description ?: "N/C" // Assurer une valeur par défaut
            )
            Log.d("ResultActivity", "Current Entry pour affichage: $currentEntry") // LOG AJOUTÉ ICI
            displayResult(currentEntry!!)
        } else {
            Toast.makeText(this, "Erreur: Données de résultat manquantes.", Toast.LENGTH_LONG).show()
            finish()
        }

        optionsButton.setOnClickListener { view ->
            showPopupMenu(view)
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
        val collapsingToolbarLayout: CollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar)
        collapsingToolbarLayout.title = entry.localName

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

        setupInfoCard(cardHabitat, R.drawable.ic_habitat, "Habitat", entry.habitat)
        setupInfoCard(cardCharacteristics, R.drawable.ic_characteristics, "Caractéristiques", entry.characteristics)
        setupInfoCard(cardReunionContext, R.drawable.ic_reunion_context, "Contexte Réunionnais", entry.reunionContext)
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

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.apply {
            add(Menu.NONE, 0, 0, "Effacer")
            add(Menu.NONE, 1, 1, "Re-analyser")
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> {
                    // Option Effacer avec confirmation
                    AlertDialog.Builder(this)
                        .setTitle("Confirmer la suppression")
                        .setMessage("Êtes-vous sûr de vouloir supprimer cette fiche d'analyse ?")
                        .setPositiveButton("Supprimer") { dialog, which ->
                            currentEntry?.let { entryToDelete ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    analysisHistoryManager.deleteAnalysisEntry(entryToDelete)
                                    withContext(Dispatchers.Main) { finish() }
                                }
                            }
                        }
                        .setNegativeButton("Annuler") { dialog, which ->
                            dialog.dismiss()
                        }
                        .show()
                    true
                }
                1 -> {
                    // Option Re-analyser
                    currentEntry?.let { entryToReanalyze ->
                        val loadingDialog = LoadingDialogFragment()
                        loadingDialog.show(supportFragmentManager, "LoadingDialogFragment")

                        lifecycleScope.launch(Dispatchers.IO) {
                            val newResponse = imageAnalyzer.analyzeImage(Uri.parse(entryToReanalyze.imageUri))

                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()
                                if (newResponse != null) {
                                    val updatedEntry = entryToReanalyze.copy(
                                        localName = newResponse.localName,
                                        scientificName = newResponse.scientificName,
                                        type = newResponse.type,
                                        habitat = newResponse.habitat,
                                        characteristics = newResponse.characteristics,
                                        reunionContext = newResponse.reunionContext,
                                        description = "N/C" // Description n'est plus fournie par l'API, utiliser N/C
                                    )
                                    analysisHistoryManager.updateAnalysisEntry(updatedEntry)
                                    currentEntry = updatedEntry // Mettre à jour l'entrée actuelle
                                    displayResult(updatedEntry) // Mettre à jour l'affichage
                                    Toast.makeText(this@ResultActivity, "Ré-analyse terminée !", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@ResultActivity, "Échec de la ré-analyse.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_LOCAL_NAME = "localName"
        const val EXTRA_SCIENTIFIC_NAME = "scientificName"
        const val EXTRA_TYPE = "type"
        const val EXTRA_HABITAT = "habitat"
        const val EXTRA_CHARACTERISTICS = "characteristics"
        const val EXTRA_REUNION_CONTEXT = "reunionContext"
        const val EXTRA_DESCRIPTION = "description"
    }
}

