package com.example.naturepei

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Environment

class MainActivity : AppCompatActivity() {

    private lateinit var cameraButton: Button
    private lateinit var galleryButton: Button
    private lateinit var croppedImageView: ImageView
    private lateinit var analyzeButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var localBirdNameTextView: TextView
    private lateinit var scientificBirdNameTextView: TextView
    private lateinit var birdDescriptionTextView: TextView

    private var currentPhotoUri: Uri? = null
    private var croppedImageUri: Uri? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_STORAGE_PERMISSION = 101
        private const val BACKEND_API_URL = "https://super-abu.com/api/nature-pei/"
        private const val UCROP_REQUEST_CODE = 69 // Code de requête pour uCrop
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues
        cameraButton = findViewById(R.id.cameraButton)
        galleryButton = findViewById(R.id.galleryButton)
        croppedImageView = findViewById(R.id.croppedImageView) // Changer l'ID ici
        analyzeButton = findViewById(R.id.analyzeButton)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        localBirdNameTextView = findViewById(R.id.localBirdNameTextView)
        scientificBirdNameTextView = findViewById(R.id.scientificBirdNameTextView)
        birdDescriptionTextView = findViewById(R.id.birdDescriptionTextView)

        // Listeners
        cameraButton.setOnClickListener { checkCameraPermission() }
        galleryButton.setOnClickListener { checkStoragePermission() }
        analyzeButton.setOnClickListener { analyzeBird() }

        // Désactiver le bouton d'analyse par défaut
        analyzeButton.isEnabled = false
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            startCamera()
        }
    }

    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            openGallery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(this, "Permission de stockage refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            null
        }
        // Continue only if the File was successfully created
        photoFile?.also { file ->
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "com.example.naturepei.fileprovider",
                file
            )
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            takePictureLauncher.launch(cameraIntent)
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                currentPhotoUri?.let { uri ->
                    startCrop(uri)
                    analyzeButton.isEnabled = false // Désactiver jusqu'au recadrage
                } ?: run {
                    Toast.makeText(this@MainActivity, "Erreur: URI de la photo nulle.", Toast.LENGTH_LONG).show()
                    analyzeButton.isEnabled = false
                }
            } else {
                analyzeButton.isEnabled = false
            }
        }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    startCrop(imageUri)
                    analyzeButton.isEnabled = false // Désactiver jusqu'au recadrage
                }
            }
        }

    private fun startCrop(sourceUri: Uri) {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }
        photoFile?.also { file ->
            val destinationUri = Uri.fromFile(file)
            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1F, 1F) // Exemple: ratio 1:1
                .withMaxResultSize(800, 800) // Exemple: taille max 800x800
                .getIntent(this)
            uCropActivityResultLauncher.launch(uCropIntent)
        }
    }

    private val uCropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val resultUri = UCrop.getOutput(data)
                Log.d("UCrop", "Result URI: $resultUri") // Ajout du log (gardé pour le debug)
                croppedImageView.setImageURI(resultUri)
                croppedImageView.requestLayout() // Force le layout à se redessiner (gardé pour le debug)
                croppedImageView.invalidate() // Force le dessin de la vue (gardé pour le debug)
                croppedImageUri = resultUri
                analyzeButton.isEnabled = true
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) { // Revert du log du code de résultat
            val data = result.data
            if (data != null) {
                val cropError = UCrop.getError(data)
                Log.e("UCrop", "Erreur de recadrage: ${cropError?.message}") // Utiliser Log.e pour les erreurs
                Toast.makeText(this, "Erreur de recadrage: ${cropError?.message}", Toast.LENGTH_LONG).show()
            }
            analyzeButton.isEnabled = false
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap, mimeType: String): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val format = when (mimeType) {
            "image/jpeg" -> Bitmap.CompressFormat.JPEG
            "image/png" -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG // Default to JPEG
        }
        bitmap.compress(format, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Retrofit API Service
    interface NaturePeiApiService {
        @POST("analyze-image")
        suspend fun analyzeImage(@Body request: AnalyzeImageRequest): AnalyzeImageResponse
    }

    data class AnalyzeImageRequest(
        val image: String,
        val mimeType: String,
        val prompt: String
    )

    data class AnalyzeImageResponse(
        val localName: String,
        val scientificName: String,
        val description: String
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Augmenter le délai de connexion à 30 secondes
        .readTimeout(30, TimeUnit.SECONDS)    // Augmenter le délai de lecture à 30 secondes
        .writeTimeout(30, TimeUnit.SECONDS)   // Augmenter le délai d'écriture à 30 secondes
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BACKEND_API_URL)
        .client(okHttpClient) // Utiliser le client OkHttpClient personnalisé
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val naturePeiService = retrofit.create(NaturePeiApiService::class.java)

    private fun getCombinedPrompt(): String {
        val systemPrompt = "Vous êtes un expert en biodiversité (faune et flore) spécialisé dans les espèces de l\'Île de la Réunion. Identifiez l\'espèce et fournissez sa description."
        val userQuery = "Identifiez cette espèce (animale ou végétale) trouvée à l\'Île de la Réunion. Fournissez son nom commun en français, son nom scientifique (si applicable) et une description de ses caractéristiques (taille, forme, couleur, habitat, comportements, floraison/fructification, etc.) sur l\'île. Indiquez également où elle peut être trouvée sur l\'île. Si l\'espèce ne peut pas être identifiée, dites simplement 'Identification impossible' sans description."
        return "${systemPrompt}\n\n${userQuery}"
    }

    private fun analyzeBird() {
        // Récupérer le Bitmap à partir de l'ImageView
        val drawable = croppedImageView.drawable
        if (drawable == null || drawable !is android.graphics.drawable.BitmapDrawable) {
            Toast.makeText(this, "Veuillez sélectionner et recadrer une image d'abord.", Toast.LENGTH_SHORT).show()
            return
        }
        val croppedBitmap = (drawable as android.graphics.drawable.BitmapDrawable).bitmap

        if (croppedBitmap == null) {
            Toast.makeText(this, "Veuillez recadrer l'image d'abord.", Toast.LENGTH_SHORT).show()
            return
        }

        loadingProgressBar.visibility = View.VISIBLE
        analyzeButton.isEnabled = false
        localBirdNameTextView.text = ""
        scientificBirdNameTextView.text = ""
        birdDescriptionTextView.text = "Analyse en cours..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val mimeType = "image/jpeg" // Assuming JPEG for cropped image, can be improved
                val base64Image = bitmapToBase64(croppedBitmap, mimeType)
                val fullPrompt = getCombinedPrompt()

                val request = AnalyzeImageRequest(base64Image, mimeType, fullPrompt)

                var response: AnalyzeImageResponse? = null
                val maxRetries = 5
                var attempt = 0
                var success = false

                while (attempt < maxRetries && !success) {
                    try {
                        response = naturePeiService.analyzeImage(request)
                        success = true
                    } catch (e: Exception) {
                        attempt++
                        if (attempt < maxRetries) {
                            val delayTime = Math.pow(2.0, attempt.toDouble()).toLong() * 1000 + (0..1000).random()
                            Log.e("MainActivity", "Tentative $attempt échouée. Nouvelle tentative dans ${delayTime / 1000}s...", e)
                            delay(delayTime)
                        } else {
                            throw e // Re-throw after max retries
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    analyzeButton.isEnabled = true

                    if (response != null) {
                        if (response.localName.lowercase().contains("identification impossible")) {
                            localBirdNameTextView.text = "Identification Impossible"
                            scientificBirdNameTextView.text = "Non disponible"
                            birdDescriptionTextView.text = "Le modèle n\'a pas pu identifier l\'oiseau ou l\'image n\'est pas claire. Assurez-vous que l\'oiseau est bien visible et que l\'espèce est connue à la Réunion."
                        } else {
                            localBirdNameTextView.text = "Nom Local / Créole : ${response.localName}"
                            scientificBirdNameTextView.text = "Nom Scientifique : ${response.scientificName}"
                            birdDescriptionTextView.text = "Description : ${response.description}"
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Aucune réponse de l\'API.", Toast.LENGTH_LONG).show()
                        birdDescriptionTextView.text = "Erreur: Aucune réponse de l\'API."
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de l\'analyse: ", e)
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    analyzeButton.isEnabled = true
                    localBirdNameTextView.text = "Erreur du Système"
                    scientificBirdNameTextView.text = ""
                    birdDescriptionTextView.text = "Une erreur est survenue: ${e.message}"
                    Toast.makeText(this@MainActivity, "Erreur lors de l\'analyse: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
