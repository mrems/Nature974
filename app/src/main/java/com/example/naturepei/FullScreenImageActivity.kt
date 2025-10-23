package com.example.naturepei

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val fullScreenImageView: ImageView = findViewById(R.id.full_screen_image_view)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        imageUriString?.let { uriString ->
            fullScreenImageView.setImageURI(Uri.parse(uriString))
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "fullScreenImageUri"
    }
}



