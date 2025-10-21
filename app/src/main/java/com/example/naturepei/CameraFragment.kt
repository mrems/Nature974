package com.example.naturepei

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.hardware.camera2.CameraCharacteristics
import android.os.HandlerThread
import android.os.Handler

class CameraFragment : Fragment() {

    private lateinit var cameraPreviewTextureView: TextureView

    private var currentPhotoUri: Uri? = null
    private var croppedImageUri: Uri? = null

    private var cameraDevice: CameraDevice? = null
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewSize: Size
    private lateinit var imageReader: ImageReader
    private lateinit var cameraManager: CameraManager

    private lateinit var galleryButton: ImageButton
    private lateinit var optionsButton: ImageButton
    private lateinit var captureButton: ImageButton

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100 // Ce n'est plus nécessaire avec ActivityResultContracts
        private const val REQUEST_STORAGE_PERMISSION = 101 // Ce n'est plus nécessaire avec ActivityResultContracts
        private const val BACKEND_API_URL = "https://super-abu.com/api/nature-pei/"
        private const val UCROP_REQUEST_CODE = 69
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraPreviewTextureView = view.findViewById(R.id.camera_preview_texture_view)
        cameraPreviewTextureView.surfaceTextureListener = textureListener

        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        galleryButton = view.findViewById(R.id.gallery_icon)
        optionsButton = view.findViewById(R.id.options_icon)
        captureButton = view.findViewById(R.id.capture_button)

        galleryButton.setOnClickListener { checkStoragePermissionAndOpenGallery() }
        optionsButton.setOnClickListener { showThemeSelectionDialog() }
        captureButton.setOnClickListener { takePicture() }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (cameraPreviewTextureView.isAvailable) {
            openCamera()
        } else {
            cameraPreviewTextureView.surfaceTextureListener = textureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        captureSession.close()
        cameraDevice?.close()
        cameraDevice = null
        imageReader.close()
    }

    private fun showThemeSelectionDialog() {
        val dialog = ThemeSelectionDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, "ThemeSelectionDialogFragment")
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
            activity?.finish()
        }
    }

    private fun openCamera() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map: StreamConfigurationMap? = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            previewSize = map!!.getOutputSizes(SurfaceTexture::class.java)[0]
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, android.graphics.ImageFormat.JPEG, 2)

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
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

            cameraDevice!!.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = cameraCaptureSession
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(requireContext(), "Configuration de la caméra échouée", Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            openCamera()
        }
    }

    private fun takePicture() {
        if (cameraDevice == null) {
            Log.e("CameraFragment", "CameraDevice est null")
            return
        }
        try {
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

            captureSession.stopRepeating()
            captureSession.abortCaptures()

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    Log.d("CameraFragment", "Photo capturée !")
                    createCameraPreviewSession()
                }
            }
            captureSession.capture(captureBuilder.build(), captureCallback, backgroundHandler)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        val image = it.acquireLatestImage()
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val file = File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "pic_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output ->
            output.write(bytes)
        }
        val savedUri = Uri.fromFile(file)
        image.close()
        Log.d("CameraFragment", "Image enregistrée sous: $savedUri")
        startCrop(savedUri)
    }

    private fun checkStoragePermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestStoragePermissionLauncher.launch(permission)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        Log.d("CameraFragment", "openGallery: Ouverture de la galerie")
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    Log.d("CameraFragment", "pickImageLauncher: Image sélectionnée, URI: $imageUri")
                    startCrop(imageUri)
                } else {
                    Log.e("CameraFragment", "pickImageLauncher: URI d'image nulle après sélection.")
                    Toast.makeText(requireContext(), "Erreur: URI d'image nulle.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("CameraFragment", "pickImageLauncher: Sélection d'image annulée ou échouée.")
            }
        }

    // Lanceur d'activité pour la permission de la caméra
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            activity?.finish()
        }
    }

    // Lanceur d'activité pour la permission de stockage
    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission de stockage refusée", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(sourceUri: Uri) {
        Log.d("CameraFragment", "startCrop: Démarrage du recadrage pour URI: $sourceUri")
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("CameraFragment", "startCrop: Erreur lors de la création du fichier image pour le recadrage.", ex)
            null
        }
        photoFile?.also { file ->
            val destinationUri = Uri.fromFile(file)
            Log.d("CameraFragment", "startCrop: URI de destination pour le recadrage: $destinationUri")
            val uCropOptions = UCrop.Options()
            uCropOptions.setHideBottomControls(true)
            uCropOptions.setFreeStyleCropEnabled(false)
            uCropOptions.setCropGridColumnCount(0)
            uCropOptions.setCropGridRowCount(0)

            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withOptions(uCropOptions)
                .withAspectRatio(1F, 1F)
                .withMaxResultSize(800, 800)
                .getIntent(requireContext())
            uCropActivityResultLauncher.launch(uCropIntent)
        } ?: run {
            Log.e("CameraFragment", "startCrop: Impossible de créer un fichier pour l'URI de destination.")
            Toast.makeText(requireContext(), "Erreur: Impossible de préparer le recadrage.", Toast.LENGTH_LONG).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir == null) {
            Log.e("CameraFragment", "createImageFile: Le répertoire de stockage externe est null.")
            throw IOException("Impossible d'accéder au répertoire de stockage externe.")
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private val uCropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val resultUri = UCrop.getOutput(data)
                Log.d("CameraFragment", "Result URI: $resultUri")
                croppedImageUri = resultUri
                analyzeImageWithGemini() // Démarrer l'analyse une fois l'image recadrée
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val data = result.data
            if (data != null) {
                val cropError = UCrop.getError(data)
                Log.e("CameraFragment", "Erreur de recadrage: ${cropError?.message}")
                Toast.makeText(requireContext(), "Erreur de recadrage: ${cropError?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun analyzeImageWithGemini() {
        val imageUri = croppedImageUri
        if (imageUri == null) {
            Toast.makeText(requireContext(), "Aucune image recadrée à analyser.", Toast.LENGTH_SHORT).show()
            return
        }

        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                val mimeType = requireActivity().contentResolver.getType(imageUri) ?: "image/jpeg"
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
                            Log.e("CameraFragment", "Tentative $attempt échouée. Nouvelle tentative dans ${delayTime / 1000}s...", e)
                            delay(delayTime)
                        } else {
                            throw e // Re-throw after max retries
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response != null) {
                        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                            putExtra(ResultActivity.EXTRA_IMAGE_URI, imageUri.toString())
                            putExtra(ResultActivity.EXTRA_LOCAL_NAME, response.localName)
                            putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, response.scientificName)
                            putExtra(ResultActivity.EXTRA_DESCRIPTION, response.description)
                        }
                        startActivity(intent)

                        val analysisEntry = AnalysisEntry(
                            imageUri = imageUri.toString(),
                            localName = response.localName,
                            scientificName = response.scientificName,
                            description = response.description
                        )
                        AnalysisHistoryManager(requireContext()).saveAnalysisEntry(analysisEntry)

                        Log.d("CameraFragment", "Réponse Gemini: ${response.localName}, ${response.scientificName}, ${response.description}")
                        Toast.makeText(requireContext(), "Analyse terminée !", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Aucune réponse de l\'API.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraFragment", "Erreur lors de l\'analyse: ", e)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Erreur lors de l\'analyse: ${e.message}", Toast.LENGTH_LONG).show()
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BACKEND_API_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val naturePeiService = retrofit.create(NaturePeiApiService::class.java)

    private fun getCombinedPrompt(): String {
        val systemPrompt = "Vous êtes un expert en biodiversité (faune et flore) spécialisé dans les espèces de l\'Île de la Réunion. Identifiez l\'espèce et fournissez sa description."
        val userQuery = "Identifiez cette espèce (animale ou végétale) trouvée à l\'Île de la Réunion. Fournissez son nom commun en français, son nom scientifique (si applicable) et une description de ses caractéristiques (taille, forme, couleur, habitat, comportements, floraison/fructification, etc.) sur l\'île. Indiquez également où elle peut être trouvée sur l\'île. Si l\'espèce ne peut pas être identifiée, dites simplement 'Identification impossible' sans description."
        return "${systemPrompt}\n\n${userQuery}"
    }
}
