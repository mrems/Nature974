package com.example.naturepei

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val imageView: ImageView = findViewById(R.id.result_image_view)
        val localNameTextView: TextView = findViewById(R.id.result_local_name)
        val scientificNameTextView: TextView = findViewById(R.id.result_scientific_name)
        val descriptionTextView: TextView = findViewById(R.id.result_description)

        // Récupérer les données de l'Intent
        val imageUriString = intent.getStringExtra("imageUri")
        val localName = intent.getStringExtra("localName")
        val scientificName = intent.getStringExtra("scientificName")
        val description = intent.getStringExtra("description")

        // Afficher l'image et les résultats
        imageUriString?.let { uriString ->
            imageView.setImageURI(Uri.parse(uriString))
        }
        localNameTextView.text = "Nom Local / Créole : ${localName ?: "N/A"}"
        scientificNameTextView.text = "Nom Scientifique : ${scientificName ?: "N/A"}"
        descriptionTextView.text = "Description : ${description ?: "N/A"}"
    }

    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_LOCAL_NAME = "localName"
        const val EXTRA_SCIENTIFIC_NAME = "scientificName"
        const val EXTRA_DESCRIPTION = "description"
    }
}

