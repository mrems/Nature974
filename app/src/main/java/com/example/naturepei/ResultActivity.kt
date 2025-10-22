package com.example.naturepei

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

class ResultActivity : AppCompatActivity() {

    private lateinit var imageAnalyzer: ImageAnalyzer
    private lateinit var analysisHistoryManager: AnalysisHistoryManager

    private lateinit var resultImageView: ImageView
    private lateinit var resultLocalNameTextView: TextView
    private lateinit var resultScientificNameTextView: TextView
    private lateinit var resultDescriptionTextView: TextView
    private lateinit var optionsButton: ImageButton

    private var currentEntry: AnalysisEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        imageAnalyzer = ImageAnalyzer(this)
        analysisHistoryManager = AnalysisHistoryManager(this)

        resultImageView = findViewById(R.id.result_image_view)
        resultLocalNameTextView = findViewById(R.id.result_local_name)
        resultScientificNameTextView = findViewById(R.id.result_scientific_name)
        resultDescriptionTextView = findViewById(R.id.result_description)
        optionsButton = findViewById(R.id.result_options_button)

        // Récupérer les données de l'Intent
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val localName = intent.getStringExtra(EXTRA_LOCAL_NAME)
        val scientificName = intent.getStringExtra(EXTRA_SCIENTIFIC_NAME)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)

        if (imageUriString != null && localName != null && scientificName != null && description != null) {
            currentEntry = AnalysisEntry(imageUriString, localName, scientificName, description)
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
        entry.imageUri.let { uriString ->
            resultImageView.setImageURI(Uri.parse(uriString))
        }
        resultLocalNameTextView.text = entry.localName
        resultScientificNameTextView.text = entry.scientificName
        resultDescriptionTextView.text = "Description : ${entry.description}"
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
                                        description = newResponse.description
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
        const val EXTRA_DESCRIPTION = "description"
    }
}

