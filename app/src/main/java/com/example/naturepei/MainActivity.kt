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
import androidx.appcompat.app.AppCompatDelegate
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
import android.media.ImageReader
import android.view.TextureView
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.Surface
import java.util.Arrays
import android.content.Context
import android.widget.ImageButton
import android.hardware.camera2.TotalCaptureResult

data class AnalysisEntry(
    val imageUri: String,
    val localName: String,
    val scientificName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : AppCompatActivity() {

    private lateinit var cameraPreviewTextureView: TextureView

    private var currentPhotoUri: Uri? = null
    private var croppedImageUri: Uri? = null

    private var cameraDevice: CameraDevice? = null
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewSize: Size
    private lateinit var imageReader: ImageReader // Ajout de ImageReader
    private lateinit var cameraManager: CameraManager // Ajout de CameraManager

    private lateinit var galleryButton: ImageButton
    private lateinit var historyButton: ImageButton
    private lateinit var optionsButton: ImageButton
    private lateinit var captureButton: ImageButton

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_STORAGE_PERMISSION = 101
        private const val BACKEND_API_URL = "https://super-abu.com/api/nature-pei/"
        private const val UCROP_REQUEST_CODE = 69 // Code de requête pour uCrop
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val themeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO) // Thème par défaut: Clair
        AppCompatDelegate.setDefaultNightMode(themeMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraPreviewTextureView = findViewById(R.id.camera_preview_texture_view)
        cameraPreviewTextureView.surfaceTextureListener = textureListener

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager // Initialisation de cameraManager

        galleryButton = findViewById(R.id.gallery_icon)
        historyButton = findViewById(R.id.history_icon)
        optionsButton = findViewById(R.id.options_icon)
        captureButton = findViewById(R.id.capture_button)

        galleryButton.setOnClickListener { checkStoragePermissionAndOpenGallery() }
        historyButton.setOnClickListener { 
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
         }
        optionsButton.setOnClickListener { showThemeSelectionDialog() }
        captureButton.setOnClickListener { takePicture() }
    }

    private fun showThemeSelectionDialog() {
        val dialog = ThemeSelectionDialogFragment()
        dialog.show(supportFragmentManager, "ThemeSelectionDialogFragment")
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            checkCameraPermission()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Gérer les changements de taille ici si nécessaire
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Appelé à chaque mise à jour de l'aperçu
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
            finish()
        }
    }

    private fun openCamera() {
        try {
            val cameraId = cameraManager.cameraIdList[0] // Utilise la première caméra disponible (arrière par défaut)
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // Forcément, on utilise la taille de l'aperçu pour la capture aussi, à adapter si nécessaire
            previewSize = map!!.getOutputSizes(SurfaceTexture::class.java)[0]
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, android.graphics.ImageFormat.JPEG, 2)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraManager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun createCameraPreviewSession() {
        try {
            val texture = cameraPreviewTextureView.surfaceTexture
            texture!!.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            // Ajouter la surface de imageReader à la session de capture
            cameraDevice!!.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = cameraCaptureSession
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(this@MainActivity, "Configuration de la caméra échouée", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
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
            openCamera()
        }
    }

    private fun takePicture() {
        if (cameraDevice == null) {
            Log.e("MainActivity", "CameraDevice est null")
            return
        }
        try {
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

            // Ajouter un listener pour la capture, pour gérer l'image une fois prise
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null)

            captureSession.stopRepeating()
            captureSession.abortCaptures()
            captureSession.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    Log.d("MainActivity", "Photo capturée !")
                    // Redémarrer l'aperçu après la capture
                    createCameraPreviewSession()
                }
            }, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        val image = it.acquireLatestImage()
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Sauvegarder l'image dans un fichier et obtenir son Uri
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "pic_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output ->
            output.write(bytes)
        }
        val savedUri = Uri.fromFile(file)
        image.close()
        Log.d("MainActivity", "Image enregistrée sous: $savedUri")
        // Démarrer le recadrage avec l'image capturée
        startCrop(savedUri)
    }

    private fun checkStoragePermissionAndOpenGallery() {
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

    private fun openGallery() {
        Log.d("MainActivity", "openGallery: Ouverture de la galerie")
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    Log.d("MainActivity", "pickImageLauncher: Image sélectionnée, URI: $imageUri")
                    startCrop(imageUri)
                } else {
                    Log.e("MainActivity", "pickImageLauncher: URI d'image nulle après sélection.")
                    Toast.makeText(this, "Erreur: URI d'image nulle.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("MainActivity", "pickImageLauncher: Sélection d'image annulée ou échouée.")
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
                openCamera()
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "onRequestPermissionsResult: Permission de stockage accordée, ouverture de la galerie.")
                openGallery()
            } else {
                Toast.makeText(this, "Permission de stockage refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCrop(sourceUri: Uri) {
        Log.d("MainActivity", "startCrop: Démarrage du recadrage pour URI: $sourceUri")
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("MainActivity", "startCrop: Erreur lors de la création du fichier image pour le recadrage.", ex)
            null
        }
        photoFile?.also { file ->
            val destinationUri = Uri.fromFile(file)
            Log.d("MainActivity", "startCrop: URI de destination pour le recadrage: $destinationUri")
            val uCropOptions = UCrop.Options()
            uCropOptions.setHideBottomControls(true) // Cacher les contrôles du bas (rotation, etc.)
            uCropOptions.setFreeStyleCropEnabled(false) // Désactiver le recadrage libre
            uCropOptions.setCropGridColumnCount(0)
            uCropOptions.setCropGridRowCount(0)

            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withOptions(uCropOptions)
                .withAspectRatio(1F, 1F) // Appliquer le ratio 1:1 directement ici
                .withMaxResultSize(800, 800) // Exemple: taille max 800x800
                .getIntent(this)
            uCropActivityResultLauncher.launch(uCropIntent)
        } ?: run {
            Log.e("MainActivity", "startCrop: Impossible de créer un fichier pour l'URI de destination.")
            Toast.makeText(this, "Erreur: Impossible de préparer le recadrage.", Toast.LENGTH_LONG).show()
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // Assurez-vous que le répertoire existe
        if (storageDir == null) {
            Log.e("MainActivity", "createImageFile: Le répertoire de stockage externe est null.")
            throw IOException("Impossible d'accéder au répertoire de stockage externe.")
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private val uCropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val resultUri = UCrop.getOutput(data)
                Log.d("MainActivity", "Result URI: $resultUri") // Ajout du log (gardé pour le debug)
                croppedImageUri = resultUri
                analyzeImageWithGemini() // Démarrer l'analyse une fois l'image recadrée
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val data = result.data
            if (data != null) {
                val cropError = UCrop.getError(data)
                Log.e("MainActivity", "Erreur de recadrage: ${cropError?.message}")
                Toast.makeText(this, "Erreur de recadrage: ${cropError?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun analyzeImageWithGemini() {
        val imageUri = croppedImageUri
        if (imageUri == null) {
            Toast.makeText(this, "Aucune image recadrée à analyser.", Toast.LENGTH_SHORT).show()
            return
        }

        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(supportFragmentManager, "LoadingDialogFragment")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                val base64Image = bitmapToBase64(bitmap, mimeType)
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
                            val delayTime = (Math.pow(2.0, attempt.toDouble()).toLong() * 1000) + (0..1000).random()
                            Log.e("MainActivity", "Tentative $attempt échouée. Nouvelle tentative dans ${delayTime / 1000}s...", e)
                            delay(delayTime)
                        } else {
                            throw e // Re-throw after max retries
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response != null) {
                        // Afficher les résultats de l'analyse dans une nouvelle interface
                        val intent = Intent(this@MainActivity, ResultActivity::class.java).apply {
                            putExtra(ResultActivity.EXTRA_IMAGE_URI, imageUri.toString())
                            putExtra(ResultActivity.EXTRA_LOCAL_NAME, response.localName)
                            putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, response.scientificName)
                            putExtra(ResultActivity.EXTRA_DESCRIPTION, response.description)
                        }
                        startActivity(intent)

                        // Sauvegarder l'entrée dans l'historique
                        val analysisEntry = AnalysisEntry(
                            imageUri = imageUri.toString(),
                            localName = response.localName,
                            scientificName = response.scientificName,
                            description = response.description
                        )
                        AnalysisHistoryManager(this@MainActivity).saveAnalysisEntry(analysisEntry)

                        Log.d("MainActivity", "Réponse Gemini: ${response.localName}, ${response.scientificName}, ${response.description}")
                        Toast.makeText(this@MainActivity, "Analyse terminée !", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Aucune réponse de l\'API.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de l\'analyse: ", e)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Erreur lors de l\'analyse: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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

    // La logique d'analyse sera refactorisée plus tard
    private fun analyzeBird() {
        // TODO: Implémenter l'écran de chargement et la nouvelle logique d'analyse
    }
}
