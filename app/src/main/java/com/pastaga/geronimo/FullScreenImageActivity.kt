package com.pastaga.geronimo

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val fullScreenImageView: PhotoView = findViewById(R.id.full_screen_image_view)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val isTutorial = intent.getBooleanExtra(EXTRA_IS_TUTORIAL, false)
        
        // Détecter aussi les anciennes URIs de ressources Android (fallback pour fiches tutoriel existantes)
        if (isTutorial || imageUriString?.startsWith("android.resource://") == true) {
            // Pour les fiches tutoriel, charger directement depuis les ressources drawable
            fullScreenImageView.load(R.drawable.illustration)
        } else {
            imageUriString?.let { uriString ->
                fullScreenImageView.load(Uri.parse(uriString)) {
                    crossfade(true)
                }
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "fullScreenImageUri"
        const val EXTRA_IS_TUTORIAL = "isTutorial"
    }
}





