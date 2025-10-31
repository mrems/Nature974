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
 
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageButton
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
import okhttp3.MediaType
import okhttp3.RequestBody
import android.hardware.camera2.CaptureFailure
 
 
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
 
 
// Importations pour la localisation
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.ImageView
 
class CameraFragment : Fragment() {

    private lateinit var cameraPreviewTextureView: TextureView

    private var currentPhotoUri: Uri? = null
    private var croppedImageUri: Uri? = null
    private var imageFile: File? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private lateinit var previewSize: Size
    private lateinit var imageReader: ImageReader
    private lateinit var cameraManager: CameraManager

    private lateinit var optionsButton: ImageButton
    private lateinit var captureButton: ImageButton
    private lateinit var overlayLayout: FrameLayout
    private lateinit var galleryBottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var galleryHandle: FrameLayout
    private lateinit var appTitleImageView: ImageView

    private var swipeStartX: Float? = null
    private var swipeStartY: Float? = null
    private var lockedDirection: Direction? = null
    private val touchSlop: Int by lazy { ViewConfiguration.get(requireContext()).scaledTouchSlop }

    private enum class Direction { VERTICAL, HORIZONTAL }

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private lateinit var imageAnalyzer: ImageAnalyzer
    private lateinit var phoneOrientationSensor: PhoneOrientationSensor
    private var sensorOrientation: Int = 0
    private var deviceRotation: Int = 0
    
    

    private var analysisJob: Job? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private lateinit var creditsTextView: TextView
    
    

    // Variables pour la localisation
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userCountry: String? = null
    private var userRegion: String? = null
    private var creditsListener: ListenerRegistration? = null
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageAnalyzer = ImageAnalyzer(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100 // Ce n'est plus nécessaire avec ActivityResultContracts
        private const val REQUEST_STORAGE_PERMISSION = 101 // Ce n'est plus nécessaire avec ActivityResultContracts
        private const val BACKEND_API_URL = "https://super-abu.com/api/nature-pei/"
        private const val UCROP_REQUEST_CODE = 69
        private const val ARG_IMAGE_URI = "image_uri"
        private const val SWIPE_THRESHOLD_PX = 80
        private const val SWIPE_VELOCITY_THRESHOLD = 900

        fun newInstance(imageUri: String? = null): CameraFragment {
            val fragment = CameraFragment()
            imageUri?.let { uri ->
                val args = Bundle()
                args.putString(ARG_IMAGE_URI, uri)
                fragment.arguments = args
            }
            return fragment
        }
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
        captureButton = view.findViewById(R.id.capture_button)
        optionsButton = view.findViewById(R.id.options_icon)
        overlayLayout = view.findViewById(R.id.overlay_layout)
        galleryBottomSheet = view.findViewById(R.id.gallery_bottom_sheet)
        galleryHandle = view.findViewById(R.id.gallery_sheet_handle)
        bottomSheetBehavior = BottomSheetBehavior.from(galleryBottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isDraggable = true

        appTitleImageView = view.findViewById(R.id.app_title_image_view)

        creditsTextView = view.findViewById(R.id.credits_text_view)

        // Calculer la hauteur du titre + une petite marge en dp
        val density = resources.displayMetrics.density
        val extraOffset = (34 * density).toInt() // Marge supplémentaire pour ne pas coller au titre, augmentée de 4dp (total 34dp)
        view.post { // S'assurer que le layout est mesuré avant de calculer la hauteur
            val screenHeight = resources.displayMetrics.heightPixels
            // Adapter le calcul pour la ImageView, ou utiliser une valeur fixe si l'ImageView n'a pas de bottom défini immédiatement
            // Pour simplifier, nous allons utiliser une valeur fixe pour la hauteur du titre + marge.
            // Environ 30dp pour la hauteur du titre + 8dp de marginTop = 38dp, plus l'extraOffset
            val titleHeightDp = 30 // Hauteur estimée du titre en dp
            val titleHeightPx = (titleHeightDp * density).toInt()
            val desiredHeight = screenHeight - titleHeightPx - extraOffset
            // Définir la hauteur maximale du bottom sheet pour qu'il ne dépasse pas le titre
            bottomSheetBehavior.maxHeight = desiredHeight
        }

        // Désactiver le swipe latéral du ViewPager quand la galerie est ouverte
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.view_pager)
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val disable = newState == BottomSheetBehavior.STATE_EXPANDED || newState == BottomSheetBehavior.STATE_HALF_EXPANDED || newState == BottomSheetBehavior.STATE_DRAGGING
                viewPager?.isUserInputEnabled = !disable
                galleryHandle.visibility = if (disable || newState == BottomSheetBehavior.STATE_COLLAPSED) View.VISIBLE else View.GONE
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val disable = slideOffset > 0.05f
                viewPager?.isUserInputEnabled = !disable
                galleryHandle.visibility = if (slideOffset > 0f) View.VISIBLE else View.GONE
            }
        })

        // Injecter le contenu de galerie dans le container du bottom sheet
        childFragmentManager.beginTransaction()
            .replace(R.id.gallery_sheet_container, GalleryFragment())
            .commitNowAllowingStateLoss()

        // Recevoir l'image choisie depuis GalleryFragment (résultat posté sur parentFragmentManager du child)
        childFragmentManager.setFragmentResultListener("gallery_result", viewLifecycleOwner) { _, bundle ->
            val uriString = bundle.getString("uri")
            if (uriString != null) {
                // Laisser la galerie visible le temps que le recadrage s'ouvre, elle sera fermée discrètement ensuite
                startCrop(Uri.parse(uriString))
            }
        }

        // Affichage temps réel du solde de crédits
        setupCreditsListener()

        // Swipe sur l'écran caméra: distinguer vertical vs horizontal et coopérer avec ViewPager
        val onTouchListener = View.OnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.x
                    swipeStartY = event.y
                    lockedDirection = null
                    // Empêcher temporairement l'interception par le ViewPager; on la réautorise si geste horizontal
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val startX = swipeStartX
                    val startY = swipeStartY
                    if (startX == null || startY == null) return@OnTouchListener false
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (lockedDirection == null) {
                        if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                            if (kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.2f) {
                                lockedDirection = Direction.VERTICAL
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                true
                            } else if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                lockedDirection = Direction.HORIZONTAL
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                                return@OnTouchListener false
                            } else {
                                false
                            }
                        } else false
                    } else if (lockedDirection == Direction.VERTICAL) {
                        // Seuil dynamique en fonction de la densité et du touch slop
                        val density = resources.displayMetrics.density
                        val verticalTrigger = kotlin.math.max(touchSlop * 2, (48 * density).toInt())
                        if (dy < -verticalTrigger) {
                            openBottomSheet()
                            true
                        } else if (dy > verticalTrigger && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                            closeBottomSheet()
                            true
                        } else true
                    } else false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    swipeStartX = null
                    swipeStartY = null
                    lockedDirection = null
                    false
                }
                else -> false
            }
        }

        overlayLayout.setOnTouchListener(onTouchListener)
        cameraPreviewTextureView.setOnTouchListener(onTouchListener)

        // S'assurer que la permission lecture images est accordée (Android 13+)
        ensureGalleryReadPermission()

        phoneOrientationSensor = PhoneOrientationSensor(requireContext()) { /* plus de rotation UI */ }

        // Initialisation normale de la caméra si aucune URI n'est fournie
        cameraPreviewTextureView.surfaceTextureListener = textureListener

        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        captureButton.setOnClickListener { takePicture() }
        // Supprime l'ouverture du sélecteur de thème
        optionsButton.visibility = View.GONE

        // Suppression du bouton galerie et de son chargement

        // Retirer les gestuelles custom: le bottom sheet est glissé directement par l'utilisateur

        // Si une URI est passée pour la ré-analyse, nous devons lancer l'analyse dans une coroutine.
        arguments?.getString(ARG_IMAGE_URI)?.let { uriString ->
            val imageUri = Uri.parse(uriString)
            // Désactiver la capture et analyser directement l'image fournie
            cameraPreviewTextureView.visibility = View.GONE
            captureButton.visibility = View.GONE
            optionsButton.visibility = View.GONE
            lifecycleScope.launch(Dispatchers.IO) { // Lancer l'analyse dans une coroutine
                analyzeImageWithGemini(imageUri)
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    analysisJob?.isActive == true -> {
                        analysisJob?.cancel()
                        Toast.makeText(requireContext(), "Analyse annulée.", Toast.LENGTH_SHORT).show()
                        Log.d("CameraFragment", "Analyse annulée par le bouton retour.")
                        // Revenir à l'état initial ou à l'écran de recadrage
                        croppedImageUri = null // Annuler l'URI recadrée
                        closeCamera() // Fermer la caméra proprement
                        // openCamera() // Ne pas appeler openCamera() ici, laisser onResume le faire.
                        cameraPreviewTextureView.visibility = View.VISIBLE
                        captureButton.visibility = View.VISIBLE
                        // optionsButton.visibility = View.VISIBLE // Si vous voulez le re-montrer
                    }
                    // Gérer si UCrop est visible (l'ActivityResultLauncher d'UCrop gère déjà le retour)
                    // Pour UCrop, le simple fait d'appeler super.handleOnBackPressed() ou de laisser la gestion par défaut
                    // devrait revenir à l'activité appelante (CameraFragment) avec RESULT_CANCELED.
                    // Il n'y a pas de moyen direct de savoir si UCrop est "actif" de l'extérieur.
                    // La meilleure approche est de simplement ne pas intercepter si on sait qu'on a lancé UCrop.
                    // Pour l'instant, on laisse UCrop gérer son propre retour.
                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun setupCreditsListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        creditsListener?.remove()
        creditsListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val credits = snapshot?.getLong("credits")?.toInt() ?: return@addSnapshotListener
            Log.d("CameraFragment", "Mise à jour crédits temps réel: $credits")
            creditsTextView.text = "$credits"
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        phoneOrientationSensor.startListening()
        if (cameraPreviewTextureView.isAvailable) {
            openCamera()
        } else {
            cameraPreviewTextureView.surfaceTextureListener = textureListener
        }
        // plus de rechargement de la dernière image
        requestLocationPermissions()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        phoneOrientationSensor.stopListening()
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
        captureSession?.close()
        captureSession = null // Explicitly nullify
        cameraDevice?.close()
        cameraDevice = null
        imageReader.close()
    }

    // Sélecteur de thème supprimé

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

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            lifecycleScope.launch(Dispatchers.IO) { // Appeler suspend fun dans une coroutine
                analyzeImageWithGemini() // Démarrer l'analyse une fois l'image recadrée
            }
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            super.onCaptureFailed(session, request, failure)
            Log.e("CameraFragment", "Photo capturée échouée: ${failure.reason}")
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cameraId = cameraManager.cameraIdList[0]
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                val displayRotation = requireActivity().windowManager.defaultDisplay.rotation
                deviceRotation = getOrientation(displayRotation)

                val map: StreamConfigurationMap? = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                // Optimisation : Choisir une résolution de preview adaptée (720p ou 1080p)
                previewSize = chooseOptimalSize(map!!.getOutputSizes(SurfaceTexture::class.java))

                // ImageReader pour la capture photo en haute résolution
                val captureSize = map.getOutputSizes(android.graphics.ImageFormat.JPEG)[0]
                imageReader = ImageReader.newInstance(captureSize.width, captureSize.height, android.graphics.ImageFormat.JPEG, 2)

                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return@launch
                }
                withContext(Dispatchers.Main) { // Revenir sur le thread principal pour ouvrir la caméra
                    cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                Log.e("CameraFragment", "Cannot open camera: Fragment not attached to activity", e)
            }
        }
    }
    
    /**
     * Choisit une taille de preview optimale pour éviter le lag
     * Cible 1080p max pour la fluidité
     */
    private fun chooseOptimalSize(sizes: Array<Size>): Size {
        val preferredWidth = 1920 // 1080p
        val preferredHeight = 1080
        
        // Trier par aire pour avoir les tailles de la plus petite à la plus grande
        val sortedSizes = sizes.sortedBy { it.width * it.height }
        
        // Chercher la taille la plus proche de 1080p sans dépasser
        val optimalSize = sortedSizes.lastOrNull { 
            it.width <= preferredWidth && it.height <= preferredHeight 
        } ?: sortedSizes.firstOrNull { 
            it.width <= 1280 && it.height <= 720 // Fallback à 720p
        } ?: sortedSizes[0] // Dernier recours
        
        Log.d("CameraFragment", "Taille de preview sélectionnée: ${optimalSize.width}x${optimalSize.height}")
        return optimalSize
    }

    private fun getOrientation(rotation: Int): Int {
        return when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    @Suppress("DEPRECATION")
    private fun createCameraPreviewSession() {
        try {
            val texture = cameraPreviewTextureView.surfaceTexture
            texture!!.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(surface)

            cameraDevice!!.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = cameraCaptureSession
                    try {
                        // Optimisations pour la fluidité
                        previewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        
                        // Limiter le FPS pour réduire la charge CPU (optionnel)
                        // previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, android.util.Range(30, 30))
                        
                        captureSession!!.setRepeatingRequest(previewRequestBuilder!!.build(), null, backgroundHandler)
                        Log.d("CameraFragment", "Session de preview configurée avec succès")
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(requireContext(), "Échec de la configuration de la session de capture.", Toast.LENGTH_SHORT).show()
                }
            }, null)
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

            // Obtenir l'orientation actuelle du téléphone depuis le capteur
            val currentOrientation = phoneOrientationSensor.getDeviceOrientation()
            
            // Calculer l'orientation JPEG finale (orientation capteur - orientation téléphone inversée)
            // Inversion car les axes sont inversés par rapport à l'orientation EXIF
            val rotation = (sensorOrientation - currentOrientation + 360) % 360
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation)
            Log.d("CameraFragment", "Device orientation: $currentOrientation°, Sensor orientation: $sensorOrientation°, Final JPEG_ORIENTATION: $rotation°")

            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

            captureSession?.stopRepeating()
            captureSession?.abortCaptures()

            captureSession?.capture(captureBuilder.build(), captureCallback, backgroundHandler)

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

    

    private fun openBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun closeBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun ensureGalleryReadPermission() {
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
            requestInAppStoragePermissionLauncher.launch(permission)
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

    

    // Lanceur d'activité pour la permission de stockage (galerie interne)
    private val requestInAppStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Permission de stockage refusée", Toast.LENGTH_SHORT).show()
        }
    }

    

    // Lanceur d'activité pour les permissions de localisation
    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Au moins une permission de localisation est accordée
            getLastLocation()
        } else {
            Toast.makeText(requireContext(), "Permission de localisation refusée. Certaines fonctionnalités pourraient être limitées.", Toast.LENGTH_LONG).show()
            userCountry = null
            userRegion = null
        }
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocationGranted || coarseLocationGranted
    }

    private fun requestLocationPermissions() {
        requestLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun getLastLocation() {
        if (checkLocationPermissions()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        getCountryAndRegionFromLocation(it)
                    } ?: run {
                        Log.d("CameraFragment", "Aucune localisation connue.")
                        userCountry = null
                        userRegion = null
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CameraFragment", "Erreur lors de la récupération de la localisation: ${e.message}")
                    userCountry = null
                    userRegion = null
                }
        } else {
            // Les permissions ne sont pas accordées, on ne devrait pas appeler getLastLocation sans vérification
            // Cette branche ne devrait pas être atteinte si checkLocationPermissions est bien gérée avant.
            userCountry = null
            userRegion = null
        }
    }

    private fun getCountryAndRegionFromLocation(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    userCountry = addresses[0].countryName
                    userRegion = addresses[0].adminArea
                    Log.d("CameraFragment", "Localisation obtenue: Pays = $userCountry, Région = $userRegion")
                } else {
                    Log.d("CameraFragment", "Aucune adresse trouvée pour la localisation.")
                    userCountry = null
                    userRegion = null
                }
            } catch (e: IOException) {
                Log.e("CameraFragment", "Erreur de géocodage: ${e.message}")
                userCountry = null
                userRegion = null
            }
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
            uCropOptions.setDimmedLayerColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            uCropOptions.setRootViewBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))

            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withOptions(uCropOptions)
                .withAspectRatio(1F, 1F)
                .withMaxResultSize(800, 800)
                .getIntent(requireContext())
            uCropActivityResultLauncher.launch(uCropIntent)
            // Fermer la galerie discrètement en arrière-plan juste après l'ouverture du recadrage
            view?.postDelayed({ closeBottomSheet() }, 150)
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
                // Fermer la galerie discrètement en arrière-plan pendant l'analyse
                closeBottomSheet()
                lifecycleScope.launch(Dispatchers.IO) {
                analyzeImageWithGemini() // Démarrer l'analyse une fois l'image recadrée
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val data = result.data
            if (data != null) {
                val cropError = UCrop.getError(data)
                Log.e("CameraFragment", "Erreur de recadrage: ${cropError?.message}")
                Toast.makeText(requireContext(), "Erreur de recadrage: ${cropError?.message}", Toast.LENGTH_LONG).show()
            }
        } else { // Annulation du recadrage (Activity.RESULT_CANCELED)
            Log.d("CameraFragment", "Recadrage annulé.")
            croppedImageUri = null // Nettoyer l'URI de l'image recadrée
            // Ne pas appeler openCamera() ici, laisser onResume le faire.
            cameraPreviewTextureView.visibility = View.VISIBLE
            captureButton.visibility = View.VISIBLE
            // optionsButton.visibility = View.VISIBLE
        }
    }

    // Méthode pour démarrer l'analyse de l'image (déjà existante)
    private suspend fun analyzeImageWithGemini(imageUri: Uri? = null) {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

        val finalImageUri: Uri? = imageUri ?: croppedImageUri ?: imageFile?.let { Uri.fromFile(it) }

        finalImageUri?.let { uri ->
            analysisJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "Utilisateur non connecté. Veuillez vous connecter.", Toast.LENGTH_LONG).show()
                        }
                        Log.e("CameraFragment", "Erreur: analyzeImageWithGemini appelé sans utilisateur connecté.")
                        return@launch
                    }
                    Log.d("CameraFragment", "Utilisateur connecté: ${currentUser.uid}")
                    // Gating: décrémenter 1 crédit côté serveur avant l'analyse
                    try {
                        CreditsManager.decrementOneCredit()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "Crédits insuffisants ou non connecté.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    // Analyser directement via l'URI; le backend gère le prompt et le traitement
                    val response = imageAnalyzer.analyzeImage(uri, userCountry, userRegion)

                    if (!currentCoroutineContext().isActive) {
                        Log.d("CameraFragment", "Analyse annulée avant la mise à jour UI.")
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        if (response != null) {
                            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                                putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString())
                                putExtra(ResultActivity.EXTRA_LOCAL_NAME, response.localName)
                                putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, response.scientificName)
                                putExtra(ResultActivity.EXTRA_TYPE, response.type)
                                putExtra(ResultActivity.EXTRA_HABITAT, response.habitat)
                                putExtra(ResultActivity.EXTRA_CHARACTERISTICS, response.characteristics)
                                putExtra(ResultActivity.EXTRA_LOCAL_CONTEXT, response.localContext)
                                putExtra(ResultActivity.EXTRA_DESCRIPTION, "N/C") // Description n'est plus fournie par l'API, utiliser N/C
                            }
                            startActivity(intent)

                            val analysisEntry = AnalysisEntry(
                                imageUri = uri.toString(),
                                localName = response.localName,
                                scientificName = response.scientificName,
                                type = response.type,
                                habitat = response.habitat,
                                characteristics = response.characteristics,
                                localContext = response.localContext,
                                country = userCountry,
                                region = userRegion,
                                description = "N/C" // Description n'est plus fournie par l'API, utiliser N/C
                            )
                            AnalysisHistoryManager(requireContext()).saveAnalysisEntry(analysisEntry)

                            Log.d("CameraFragment", "Réponse Gemini: ${response.localName}, ${response.scientificName}, ${response.type}") // Ajusté pour les nouveaux champs
                            Toast.makeText(requireContext(), "Analyse terminée !", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Aucune réponse de l\'API.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    if (!currentCoroutineContext().isActive) {
                        Log.d("CameraFragment", "Exception attrapée mais coroutine déjà annulée.")
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Erreur lors de l\'analyse: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    // S'assurer que la boîte de dialogue est fermée même si annulée ou erreur
                    withContext(Dispatchers.Main) {
                        if (loadingDialog.isAdded) {
                            loadingDialog.dismiss()
                        }
                        analysisJob = null
                    }
                }
            }
        } ?: withContext(Dispatchers.Main) { // Utiliser withContext pour les opérations UI sur le thread principal
            loadingDialog.dismiss()
            Toast.makeText(requireContext(), "Aucune image à analyser. Veuillez en capturer une ou en sélectionner une dans la galerie.", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertImageUriToBase64(imageUri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        return if (bytes != null) Base64.encodeToString(bytes, Base64.NO_WRAP) else ""
    }
    
}



