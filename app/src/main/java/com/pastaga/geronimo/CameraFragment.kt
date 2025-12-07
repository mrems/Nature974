package com.pastaga.geronimo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
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
import android.widget.Button
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
 
 
class CameraFragment : Fragment(), ModelSelectionDialog.ModelSelectionListener, TitleMenuDialogFragment.TitleMenuListener {

    private lateinit var cameraPreviewTextureView: TextureView

    private var cameraOpening: Boolean = false
    private var cameraStarted: Boolean = false
    private var cameraOpenRetryCount: Int = 0
    private val maxCameraOpenRetries: Int = 1

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
    private lateinit var modelPreferences: ModelPreferences
    private var sensorOrientation: Int = 0
    private var deviceRotation: Int = 0

    private var pendingImageUri: Uri? = null // Pour stocker l'URI temporairement pendant la sélection du modèle

    private var analysisJob: Job? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private lateinit var creditsTextView: TextView
    private lateinit var creditsContainer: LinearLayout
    
    // Variables pour la localisation
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userCountry: String? = null
    private var userRegion: String? = null
    private var creditsListener: ListenerRegistration? = null
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageAnalyzer = ImageAnalyzer(requireContext())
        modelPreferences = ModelPreferences(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100 // Ce n'est plus nÃ©cessaire avec ActivityResultContracts
        private const val REQUEST_STORAGE_PERMISSION = 101 // Ce n'est plus nÃ©cessaire avec ActivityResultContracts
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
        appTitleImageView.setOnClickListener { showTitleMenu() }

        creditsTextView = view.findViewById(R.id.credits_text_view)
        creditsContainer = view.findViewById(R.id.credits_container)
        setupCreditsListener()
        val hintTitleContainer: LinearLayout = view.findViewById(R.id.hint_title_container)
        val hintCreditsContainer: LinearLayout = view.findViewById(R.id.hint_credits_container)
        val hintSlideLeftContainer: LinearLayout = view.findViewById(R.id.hint_slide_left_container)
        val hintSlideRightContainer: LinearLayout = view.findViewById(R.id.hint_slide_right_container)
        val hintSlideBottomContainer: LinearLayout = view.findViewById(R.id.hint_slide_bottom_container)
        val hintClickMessageCenter: TextView = view.findViewById(R.id.hint_click_message_center)


        // Clic sur le compteur de crédits pour ouvrir l'écran d'achat
        val creditsContainer = view.findViewById<LinearLayout>(R.id.credits_container)
        creditsContainer.setOnClickListener {
            val intent = Intent(requireContext(), PurchaseActivity::class.java)
            startActivity(intent)
        }

        // Affichage systématique de l'aide générale + des indicateurs (titre, crédits, slide)
        fun showWithFadeThenHide(viewToShow: View, delayBeforeHideMs: Long = 3000L) {
            viewToShow.visibility = View.VISIBLE
            viewToShow.alpha = 0f
            viewToShow.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction {
                    viewToShow.postDelayed({
                        viewToShow.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                viewToShow.visibility = View.GONE
                            }
                            .start()
                    }, delayBeforeHideMs)
                }
                .start()
        }

        showWithFadeThenHide(hintTitleContainer, 2000L)
        showWithFadeThenHide(hintCreditsContainer, 2000L)
        showWithFadeThenHide(hintSlideLeftContainer, 2000L)
        showWithFadeThenHide(hintSlideRightContainer, 2000L)
        showWithFadeThenHide(hintSlideBottomContainer, 2000L)
        showWithFadeThenHide(hintClickMessageCenter, 2000L)

        // Calculer la hauteur du titre + une petite marge en dp
        val density = resources.displayMetrics.density
        val extraOffset = (34 * density).toInt() // Marge supplÃ©mentaire pour ne pas coller au titre, augmentÃ©e de 4dp (total 34dp)
        view.post { // S'assurer que le layout est mesurÃ© avant de calculer la hauteur
            val screenHeight = resources.displayMetrics.heightPixels
            // Adapter le calcul pour la ImageView, ou utiliser une valeur fixe si l'ImageView n'a pas de bottom dÃ©fini immÃ©diatement
            // Pour simplifier, nous allons utiliser une valeur fixe pour la hauteur du titre + marge.
            // Environ 30dp pour la hauteur du titre + 8dp de marginTop = 38dp, plus l'extraOffset
            val titleHeightDp = 30 // Hauteur estimÃ©e du titre en dp
            val titleHeightPx = (titleHeightDp * density).toInt()
            val desiredHeight = (screenHeight - titleHeightPx - extraOffset) * 2 / 3 // Modifier ici pour 2/3 de la hauteur
            // DÃ©finir la hauteur maximale du bottom sheet pour qu'il ne dÃ©passe pas le titre
            bottomSheetBehavior.maxHeight = desiredHeight
        }

        // DÃ©sactiver le swipe latÃ©ral du ViewPager quand la galerie est ouverte
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

        // Recevoir l'image choisie depuis GalleryFragment (rÃ©sultat postÃ© sur parentFragmentManager du child)
        childFragmentManager.setFragmentResultListener("gallery_result", viewLifecycleOwner) { _, bundle ->
            val uriString = bundle.getString("uri")
            if (uriString != null) {
                // Laisser la galerie visible le temps que le recadrage s'ouvre, elle sera fermÃ©e discrÃ¨tement ensuite
                startCrop(Uri.parse(uriString))
            }
        }

        // Affichage temps rÃ©el du solde de crÃ©dits
        setupCreditsListener()

        // Swipe sur l'Ã©cran camÃ©ra: distinguer vertical vs horizontal et coopÃ©rer avec ViewPager
        val onTouchListener = View.OnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.x
                    swipeStartY = event.y
                    lockedDirection = null
                    // EmpÃªcher temporairement l'interception par le ViewPager; on la rÃ©autorise si geste horizontal
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
                        // Seuil dynamique en fonction de la densitÃ© et du touch slop
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

        // S'assurer que la permission lecture images est accordÃ©e (Android 13+)
        ensureGalleryReadPermission()

        phoneOrientationSensor = PhoneOrientationSensor(requireContext()) { /* plus de rotation UI */ }

        // Initialisation normale de la camÃ©ra si aucune URI n'est fournie
        cameraPreviewTextureView.surfaceTextureListener = textureListener

        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        captureButton.setOnClickListener { takePicture() }
        // Supprime l'ouverture du sÃ©lecteur de thÃ¨me
        optionsButton.visibility = View.GONE

        // Suppression du bouton galerie et de son chargement

        // Retirer les gestuelles custom: le bottom sheet est glissÃ© directement par l'utilisateur

        // Si une URI est passÃ©e pour la rÃ©-analyse, nous devons lancer l'analyse dans une coroutine.
        arguments?.getString(ARG_IMAGE_URI)?.let { uriString ->
            val imageUri = Uri.parse(uriString)
            // DÃ©sactiver la capture et analyser directement l'image fournie
            cameraPreviewTextureView.visibility = View.GONE
            captureButton.visibility = View.GONE
            optionsButton.visibility = View.GONE
            showModelSelectionDialog(imageUri)
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    analysisJob?.isActive == true -> {
                        analysisJob?.cancel()
                        Toast.makeText(requireContext(), "Analyse annulÃ©e.", Toast.LENGTH_SHORT).show()
                        Log.d("CameraFragment", "Analyse annulÃ©e par le bouton retour.")
                        croppedImageUri = null
                        closeCamera()
                        cameraPreviewTextureView.visibility = View.VISIBLE
                        captureButton.visibility = View.VISIBLE
                    }
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
            creditsContainer.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        phoneOrientationSensor.startListening()
        
        // VÃ©rifier que les permissions sont accordÃ©es avant d'ouvrir la camÃ©ra
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (cameraPreviewTextureView.isAvailable) {
                openCamera()
            } else {
                cameraPreviewTextureView.surfaceTextureListener = textureListener
            }
        } else {
            // Si la permission n'est pas accordÃ©e, afficher un message et retourner Ã  l'authentification
            Toast.makeText(requireContext(), "Permission camÃ©ra requise", Toast.LENGTH_SHORT).show()
        }
        
        // Obtenir la localisation si la permission est dÃ©jÃ  accordÃ©e (sans redemander)
        if (checkLocationPermissions()) {
            getLastLocation()
        }
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
        if (this::imageReader.isInitialized) {
            try {
                imageReader.close()
            } catch (_: Throwable) {}
        }
        cameraOpening = false
        cameraStarted = false
        cameraOpenRetryCount = 0
    }

    // SÃ©lecteur de thÃ¨me supprimÃ©

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            // Les permissions ont dÃ©jÃ  Ã©tÃ© vÃ©rifiÃ©es lors de l'onboarding
            // On peut directement ouvrir la camÃ©ra
            if (backgroundHandler == null) {
                startBackgroundThread()
            }
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            super.onCaptureFailed(session, request, failure)
            Log.e("CameraFragment", "Photo capturÃ©e Ã©chouÃ©e: ${failure.reason}")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            cameraOpening = false
            cameraStarted = true
            cameraOpenRetryCount = 0
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
            cameraOpening = false
            cameraStarted = false
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
            cameraOpening = false
            cameraStarted = false
            if (cameraOpenRetryCount < maxCameraOpenRetries && isAdded && view != null) {
                cameraOpenRetryCount++
                view?.postDelayed({ openCamera() }, 300)
            } else {
                Toast.makeText(requireContext(), "Erreur caméra. Réessayez.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        // VÃ©rifier d'abord que la permission est accordÃ©e
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CameraFragment", "Permission camÃ©ra non accordÃ©e, impossible d'ouvrir la camÃ©ra")
            return
        }
        if (cameraOpening || cameraStarted) {
            Log.d("CameraFragment", "Ouverture camÃ©ra déjà en cours ou déjà démarrée.")
            return
        }
        cameraOpening = true
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cameraId = cameraManager.cameraIdList[0]
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                val displayRotation = requireActivity().windowManager.defaultDisplay.rotation
                deviceRotation = getOrientation(displayRotation)

                val map: StreamConfigurationMap? = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                // Optimisation : Choisir une rÃ©solution de preview adaptÃ©e (720p ou 1080p)
                previewSize = chooseOptimalSize(map!!.getOutputSizes(SurfaceTexture::class.java))

                // ImageReader pour la capture photo avec taille modÃ©rÃ©e (~2048px bord long)
                val captureSize = chooseOptimalCaptureSize(map.getOutputSizes(android.graphics.ImageFormat.JPEG))
                imageReader = ImageReader.newInstance(captureSize.width, captureSize.height, android.graphics.ImageFormat.JPEG, 2)
                Log.d("CameraFragment", "Taille de capture sÃ©lectionnÃ©e: ${captureSize.width}x${captureSize.height}")

                withContext(Dispatchers.Main) { // Revenir sur le thread principal pour ouvrir la camÃ©ra
                    cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
                }
            } catch (e: CameraAccessException) {
                Log.e("CameraFragment", "Erreur d'accÃ¨s Ã  la camÃ©ra", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur d'accÃ¨s Ã  la camÃ©ra", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IllegalStateException) {
                Log.e("CameraFragment", "Cannot open camera: Fragment not attached to activity", e)
            } catch (e: Exception) {
                Log.e("CameraFragment", "Erreur inattendue lors de l'ouverture de la camÃ©ra", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur lors de l'ouverture de la camÃ©ra", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (!cameraStarted) {
                    // Si l'ouverture n'a pas abouti jusqu'ici, relÃ¢cher le flag pour permettre un retry
                    cameraOpening = false
                }
            }
        }
    }
    
    /**
     * Choisit une taille de preview optimale pour Ã©viter le lag
     * Cible 1080p max pour la fluiditÃ©
     */
    private fun chooseOptimalSize(sizes: Array<Size>): Size {
        val preferredLongEdge = 1920 // cible ~1080p
        val ratio16by9 = 16.0 / 9.0
        val tolerance = 0.06 // ~\u00b13.3%

        fun aspectRatio(s: Size): Double {
            val longEdge = maxOf(s.width, s.height).toDouble()
            val shortEdge = minOf(s.width, s.height).toDouble()
            return longEdge / shortEdge
        }

        // 1) Pr\u00e9f\u00e9rer 16:9 <= preferredLongEdge
        val candidates169 = sizes.filter { s ->
            val r = aspectRatio(s)
            val longEdge = maxOf(s.width, s.height)
            Math.abs(r - ratio16by9) <= tolerance && longEdge <= preferredLongEdge
        }.sortedBy { it.width * it.height }

        if (candidates169.isNotEmpty()) {
            val pick = candidates169.last()
            Log.d("CameraFragment", "Preview 16:9 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
            return pick
        }

        // 2) Fallback 4:3 <= preferredLongEdge
        val ratio4by3 = 4.0 / 3.0
        val candidates43 = sizes.filter { s ->
            val r = aspectRatio(s)
            val longEdge = maxOf(s.width, s.height)
            Math.abs(r - ratio4by3) <= tolerance && longEdge <= preferredLongEdge
        }.sortedBy { it.width * it.height }

        if (candidates43.isNotEmpty()) {
            val pick = candidates43.last()
            Log.d("CameraFragment", "Preview 4:3 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
            return pick
        }

        // 3) Exclure les formats carr\u00e9s si possible, sinon prendre la plus grande <= preferredLongEdge
        val nonSquare = sizes.filter { s ->
            val r = aspectRatio(s)
            val longEdge = maxOf(s.width, s.height)
            Math.abs(r - 1.0) > 0.05 && longEdge <= preferredLongEdge
        }.sortedBy { it.width * it.height }
        if (nonSquare.isNotEmpty()) {
            val pick = nonSquare.last()
            Log.d("CameraFragment", "Preview non-carr\u00e9e s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
            return pick
        }

        // 4) Dernier recours: plus grande r\u00e9solution disponible
        val fallback = sizes.maxByOrNull { it.width * it.height } ?: sizes[0]
        Log.d("CameraFragment", "Preview fallback s\u00e9lectionn\u00e9e: ${fallback.width}x${fallback.height}")
        return fallback
    }
    
    /**
     * Choisit une taille de capture modÃ©rÃ©e pour Ã©conomiser mÃ©moire et bande passante
     * Cible ~2048px pour le bord long
     */
    private fun chooseOptimalCaptureSize(sizes: Array<Size>): Size {
        val maxLongEdge = 4096
        val ratio16by9 = 16.0 / 9.0
        val ratio4by3 = 4.0 / 3.0
        val tolerance = 0.06

        fun aspectRatio(s: Size): Double {
            val longEdge = maxOf(s.width, s.height).toDouble()
            val shortEdge = minOf(s.width, s.height).toDouble()
            return longEdge / shortEdge
        }

        // 1) Pr\u00e9f\u00e9rer 16:9 avec bord long <= 2048
        val candidates169 = sizes.filter { s ->
            val r = aspectRatio(s)
            val longEdge = maxOf(s.width, s.height)
            Math.abs(r - ratio16by9) <= tolerance && longEdge <= maxLongEdge
        }.sortedBy { it.width * it.height }

        if (candidates169.isNotEmpty()) {
            val pick = candidates169.last()
            Log.d("CameraFragment", "Capture 16:9 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
            return pick
        }

        // 2) Fallback 4:3 avec bord long <= 2048 (mais PAS carr\u00e9)
        val candidates43 = sizes.filter { s ->
            val r = aspectRatio(s)
            val longEdge = maxOf(s.width, s.height)
            Math.abs(r - ratio4by3) <= tolerance && longEdge <= maxLongEdge
        }.sortedBy { it.width * it.height }

        if (candidates43.isNotEmpty()) {
            val pick = candidates43.last()
            Log.d("CameraFragment", "Capture 4:3 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
            return pick
        }

        // 3) Exclure autant que possible les sorties carr\u00e9es
        val nonSquare = sizes.filter { s ->
            val r = aspectRatio(s)
            val longEdge = maxOf(s.width, s.height)
            Math.abs(r - 1.0) > 0.05 && longEdge <= maxLongEdge
        }.sortedBy { it.width * it.height }
        if (nonSquare.isNotEmpty()) {
            val pick = nonSquare.last()
            Log.d("CameraFragment", "Capture non-carr\u00e9e s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
            return pick
        }

        // 4) Dernier recours: plus grande r\u00e9solution non-carr\u00e9e, sinon n'importe laquelle
        val anyNonSquare = sizes.filter { s -> Math.abs(aspectRatio(s) - 1.0) > 0.05 }
        val fallback = (anyNonSquare.maxByOrNull { it.width * it.height }
            ?: sizes.maxByOrNull { it.width * it.height }) ?: sizes[0]
        Log.d("CameraFragment", "Capture fallback s\u00e9lectionn\u00e9e: ${fallback.width}x${fallback.height}")
        return fallback
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

    /**
     * Configure la transformation du TextureView pour un affichage "center crop".
     * L'image de la caméra remplit tout l'écran en maintenant son ratio d'aspect,
     * et les parties qui dépassent sont coupées sur les côtés.
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (!::previewSize.isInitialized) return
        if (viewWidth == 0 || viewHeight == 0) return
        
        val matrix = Matrix()
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f
        
        // Le buffer de la caméra a ces dimensions (généralement en paysage, ex: 1920x1080)
        val bufferWidth = previewSize.width.toFloat()
        val bufferHeight = previewSize.height.toFloat()
        
        // En mode portrait (rotation 0 ou 180), le buffer est affiché tourné de 90°
        // donc on échange width et height pour le calcul du ratio affiché
        val displayRotation = requireActivity().windowManager.defaultDisplay.rotation
        val isPortrait = displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180
        
        val effectiveBufferWidth = if (isPortrait) bufferHeight else bufferWidth
        val effectiveBufferHeight = if (isPortrait) bufferWidth else bufferHeight
        
        // Calculer le scale pour center crop
        // Le TextureView étire par défaut le buffer pour remplir la vue
        val scaleX = viewWidth / effectiveBufferWidth
        val scaleY = viewHeight / effectiveBufferHeight
        
        // Pour center crop, on utilise le scale maximum pour que l'image remplisse tout l'écran
        val scale = maxOf(scaleX, scaleY)
        
        // Calculer la correction à appliquer par rapport au comportement par défaut
        val correctionScaleX = scale / scaleX
        val correctionScaleY = scale / scaleY
        
        matrix.setScale(correctionScaleX, correctionScaleY, centerX, centerY)
        
        cameraPreviewTextureView.setTransform(matrix)
        Log.d("CameraFragment", "configureTransform: view=${viewWidth}x${viewHeight}, buffer=${bufferWidth}x${bufferHeight}, correction=${correctionScaleX}x${correctionScaleY}")
    }

    @Suppress("DEPRECATION")
    private fun createCameraPreviewSession() {
        try {
            val texture = cameraPreviewTextureView.surfaceTexture
            texture!!.setDefaultBufferSize(previewSize.width, previewSize.height)
            
            // Appliquer la transformation center crop pour éviter la déformation de l'image
            configureTransform(cameraPreviewTextureView.width, cameraPreviewTextureView.height)
            
            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(surface)

            cameraDevice!!.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = cameraCaptureSession
                    try {
                        // Optimisations pour la fluiditÃ©
                        previewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                        captureSession!!.setRepeatingRequest(previewRequestBuilder!!.build(), null, backgroundHandler)
                        Log.d("CameraFragment", "Session de preview configurÃ©e avec succÃ¨s")
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(requireContext(), "Ã‰chec de la configuration de la caméra.", Toast.LENGTH_SHORT).show()
                    // Petit retry contrÃ´lÃ© pour certaines devices donnant un "Ã©cran vert" ou un Ã©chec sporadique
                    if (cameraOpenRetryCount < maxCameraOpenRetries && isAdded && view != null) {
                        cameraOpenRetryCount++
                        closeCamera()
                        view?.postDelayed({ openCamera() }, 300)
                    }
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
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

            // Obtenir l'orientation actuelle du tÃ©lÃ©phone depuis le capteur
            val currentOrientation = phoneOrientationSensor.getDeviceOrientation()
            
            // Calculer l'orientation JPEG finale (orientation capteur - orientation tÃ©lÃ©phone inversÃ©e)
            // Inversion car les axes sont inversÃ©s par rapport Ã  l'orientation EXIF
            val rotation = (sensorOrientation - currentOrientation + 360) % 360
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation)
            Log.d("CameraFragment", "Device orientation: ${currentOrientation}°, Sensor orientation: ${sensorOrientation}°, Final JPEG_ORIENTATION: ${rotation}°")

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
        Log.d("CameraFragment", "Image enregistrÃ©e sous: $savedUri")
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
            // Permission non accordÃ©e - informer l'utilisateur gentiment
            Toast.makeText(
                requireContext(),
                "Permission de lecture de la galerie requise pour cette fonctionnalitÃ©",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Les launchers de permissions ne sont plus nÃ©cessaires
    // Les permissions sont gÃ©rÃ©es par l'OnboardingFragment avant d'arriver ici

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

    // Cette fonction n'est plus nÃ©cessaire car les permissions sont gÃ©rÃ©es par l'onboarding
    private fun requestLocationPermissions() {
        // Ne rien faire - les permissions sont dÃ©jÃ  gÃ©rÃ©es
        // Si nÃ©cessaire, on peut simplement obtenir la localisation si la permission est accordÃ©e
        if (checkLocationPermissions()) {
            getLastLocation()
        }
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
                    Log.e("CameraFragment", "Erreur lors de la rÃ©cupÃ©ration de la localisation: ${e.message}")
                    userCountry = null
                    userRegion = null
                }
        } else {
            // Les permissions ne sont pas accordÃ©es, on ne devrait pas appeler getLastLocation sans vÃ©rification
            // Cette branche ne devrait pas Ãªtre atteinte si checkLocationPermissions est bien gÃ©rÃ©e avant.
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
                    Log.d("CameraFragment", "Localisation obtenue: Pays = $userCountry, RÃ©gion = $userRegion")
                } else {
                    Log.d("CameraFragment", "Aucune adresse trouvÃ©e pour la localisation.")
                    userCountry = null
                    userRegion = null
                }
            } catch (e: IOException) {
                Log.e("CameraFragment", "Erreur de gÃ©ocodage: ${e.message}")
                userCountry = null
                userRegion = null
            }
        }
    }

    private fun startCrop(sourceUri: Uri) {
        Log.d("CameraFragment", "startCrop: DÃ©marrage du recadrage pour URI: $sourceUri")
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("CameraFragment", "startCrop: Erreur lors de la crÃ©ation du fichier image pour le recadrage.", ex)
            null
        }
        photoFile?.also { file ->
            val destinationUri = Uri.fromFile(file)
            Log.d("CameraFragment", "startCrop: URI de destination pour le recadrage: $destinationUri")
            val uCropOptions = UCrop.Options()
            uCropOptions.setHideBottomControls(true) // Cacher les contrôles inférieurs (rotation et scale)
            uCropOptions.setFreeStyleCropEnabled(false) // Désactiver le recadrage libre pour assurer un ratio fixe
            uCropOptions.setCropGridColumnCount(3)
            uCropOptions.setCropGridRowCount(3)
            uCropOptions.setShowCropGrid(false) // Désactiver le quadrillage
            uCropOptions.setShowCropFrame(true)
            uCropOptions.setCropGridStrokeWidth(2)
            uCropOptions.setCropFrameStrokeWidth(4)
            uCropOptions.setDimmedLayerColor(ContextCompat.getColor(requireContext(), R.color.dimmed_background)) // Utilise le noir semi-transparent
            uCropOptions.setRootViewBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black)) // Fond noir pour la zone de recadrage
            uCropOptions.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))
            uCropOptions.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.white))
            uCropOptions.setToolbarWidgetColor(ContextCompat.getColor(requireContext(), R.color.black)) // Couleur des boutons de la barre d'outils en noir
            uCropOptions.setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.black)) // Couleur des contrôles actifs en noir
            uCropOptions.setCropFrameColor(ContextCompat.getColor(requireContext(), R.color.white))
            uCropOptions.setCropGridColor(ContextCompat.getColor(requireContext(), R.color.white))
            uCropOptions.setToolbarTitle(getString(R.string.crop_image_title))

            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withOptions(uCropOptions)
                .withAspectRatio(1F, 1F)
                .getIntent(requireContext())

            uCropActivityResultLauncher.launch(uCropIntent)
            // Fermer la galerie discrÃ¨tement en arriÃ¨re-plan juste aprÃ¨s l'ouverture du recadrage
            view?.postDelayed({ closeBottomSheet() }, 150)
        } ?: run {
            Log.e("CameraFragment", "startCrop: Impossible de crÃ©er un fichier pour l'URI de destination.")
            Toast.makeText(requireContext(), "Erreur: Impossible de prÃ©parer le recadrage.", Toast.LENGTH_LONG).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir == null) {
            Log.e("CameraFragment", "createImageFile: Le rÃ©pertoire de stockage externe est null.")
            throw IOException("Impossible d'accÃ©der au rÃ©pertoire de stockage externe.")
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
                // Fermer la galerie discrÃ¨tement en arriÃ¨re-plan pendant l'analyse
                closeBottomSheet()
                showModelSelectionDialog(resultUri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val data = result.data
            if (data != null) {
                val cropError = UCrop.getError(data)
                Log.e("CameraFragment", "Erreur de recadrage: ${cropError?.message}")
                Toast.makeText(requireContext(), "Erreur de recadrage: ${cropError?.message}", Toast.LENGTH_LONG).show()
            }
        } else { // Annulation du recadrage (Activity.RESULT_CANCELED)
            Log.d("CameraFragment", "Recadrage annulÃ©.")
            croppedImageUri = null // Nettoyer l'URI de l'image recadrÃ©e
            // Ne pas appeler openCamera() ici, laisser onResume le faire.
            cameraPreviewTextureView.visibility = View.VISIBLE
            captureButton.visibility = View.VISIBLE
            // optionsButton.visibility = View.VISIBLE
        }
    }

    // Méthode pour afficher la boîte de dialogue de sélection du modèle
    private fun showModelSelectionDialog(imageUri: Uri? = null) {
        pendingImageUri = imageUri // Stocker l'URI pour l'utiliser après la sélection
        val selectedModel = modelPreferences.getSelectedModel()
        val dialog = ModelSelectionDialog.newInstance(selectedModel)
        dialog.show(childFragmentManager, "ModelSelectionDialog")
    }

    // MÃ©thode pour démarrer l'analyse de l'image (déjà existante)
    private suspend fun analyzeImageWithGemini(imageUri: Uri? = null, modelId: String) {
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

                    // --- AJOUT DE LA VÉRIFICATION CÔTÉ CLIENT (sans Toast) ---
                    val currentCreditsText = creditsTextView.text.toString()
                    val currentCredits = currentCreditsText.toIntOrNull() ?: 0

                    if (currentCredits <= 0) { // Vérifier si les crédits sont à zéro ou négatifs
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            val intent = Intent(requireContext(), PurchaseActivity::class.java)
                            startActivity(intent)
                            // Toast supprimé ici
                        }
                        Log.d("CameraFragment", "Redirection vers PurchaseActivity: crédits épuisés (vérification client).")
                        return@launch
                    }
                    // --- FIN DE L'AJOUT ---

                    val response = try {
                        imageAnalyzer.analyzeImage(uri, modelId, currentUser.uid, userCountry, userRegion)
                    } catch (e: InsufficientCreditsException) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            // Rediriger vers la page d'abonnement quand les crédits sont épuisés
                            val intent = Intent(requireContext(), PurchaseActivity::class.java)
                            startActivity(intent)
                            // Toast supprimé ici
                        }
                        Log.d("CameraFragment", "Redirection vers PurchaseActivity: InsufficientCreditsException.")
                        return@launch
                    }

                    if (!currentCoroutineContext().isActive) {
                        Log.d("CameraFragment", "Analyse annulÃ©e avant la mise Ã  jour UI.")
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        if (response != null) {
                            Log.d("NaturePei_Debug", "[CameraFragment] Danger de la réponse de l'API: ${response.danger}")
                            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                                putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString())
                                putExtra(ResultActivity.EXTRA_LOCAL_NAME, response.localName)
                                putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, response.scientificName)
                                putExtra(ResultActivity.EXTRA_TYPE, response.type)
                                putExtra(ResultActivity.EXTRA_HABITAT, response.habitat)
                                putExtra(ResultActivity.EXTRA_CHARACTERISTICS, response.characteristics)
                                putExtra(ResultActivity.EXTRA_LOCAL_CONTEXT, response.localContext)
                                putExtra(ResultActivity.EXTRA_PECULIARITIES, response.Peculiarities) // Passer le nouveau champ
                                putExtra(ResultActivity.EXTRA_DESCRIPTION, "N/C") // Description n'est plus fournie par l'API, utiliser N/C
                                putExtra(ResultActivity.EXTRA_REPRESENTATIVE_COLOR_HEX, response.representativeColorHex) // Passer la couleur
                                putExtra(ResultActivity.EXTRA_DANGER, response.danger) // Passer le champ danger
                                putExtra(ResultActivity.EXTRA_CONFIDENCE_SCORE, response.confidenceScore ?: -1) // Passer le score de confiance
                                putParcelableArrayListExtra(ResultActivity.EXTRA_ALTERNATIVE_IDENTIFICATIONS, response.alternativeIdentifications?.let { ArrayList(it) }) // Passer les alternatives
                                putExtra(ResultActivity.EXTRA_JUSTIFICATION_TEXT, response.justificationText) // Passer le texte de justification
                            }
                            startActivity(intent)

                            val analysisEntry = AnalysisEntry(
                                imageUri = uri.toString(), // L'URI de l'image est maintenant requis pour le constructeur
                                localName = response.localName,
                                scientificName = response.scientificName,
                                type = response.type,
                                habitat = response.habitat,
                                characteristics = response.characteristics,
                                localContext = response.localContext,
                                Peculiarities = response.Peculiarities, // Assigner le nouveau champ
                                country = userCountry,
                                region = userRegion,
                                description = "N/C", // Description n'est plus fournie par l'API, utiliser N/C
                                representativeColorHex = response.representativeColorHex, // Assigner la couleur à l'AnalysisEntry
                                danger = response.danger, // Assigner le champ danger
                                confidenceScore = response.confidenceScore, // Assigner le score de confiance
                                alternativeIdentifications = response.alternativeIdentifications, // Assigner les alternatives
                                justificationText = response.justificationText // Assigner le texte de justification
                            )
                            val historyManager = AnalysisHistoryManager(requireContext())
                            historyManager.saveAnalysisEntry(analysisEntry)
                            // Sauvegarder cette fiche comme dernière consultée
                            historyManager.saveLastViewedCard(analysisEntry)

                        } else {
                            Toast.makeText(requireContext(), "Aucune rÃ©ponse de l\'API.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    if (!currentCoroutineContext().isActive) {
                        Log.d("CameraFragment", "Exception attrapÃ©e mais coroutine dÃ©jÃ  annulÃ©e.")
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Erreur lors de l\'analyse: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    // S'assurer que la boÃ®te de dialogue est fermÃ©e mÃªme si annulÃ©e ou erreur
                    withContext(Dispatchers.Main) {
                        if (loadingDialog.isAdded) {
                            loadingDialog.dismiss()
                        }
                        analysisJob = null
                    }
                }
            }
        } ?: withContext(Dispatchers.Main) { // Utiliser withContext pour les opÃ©rations UI sur le thread principal
            loadingDialog.dismiss()
            Toast.makeText(requireContext(), "Aucune image Ã  analyser. Veuillez en capturer une ou en sÃ©lectionner une dans la galerie.", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertImageUriToBase64(imageUri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        return if (bytes != null) Base64.encodeToString(bytes, Base64.NO_WRAP) else ""
    }

    // Implémentation de ModelSelectionListener
    override fun onModelSelected(modelId: String) {
        // Sauvegarder la préférence de l'utilisateur
        modelPreferences.setSelectedModel(modelId)

        // Démarrer l'analyse avec le modèle sélectionné et l'URI stockée
        val imageUri = pendingImageUri
        pendingImageUri = null // Nettoyer la variable

        if (imageUri != null) {
            lifecycleScope.launch {
                analyzeImageWithGemini(imageUri, modelId)
            }
        } else {
            // Fallback: utiliser l'URI croppée actuelle
            val fallbackUri = croppedImageUri ?: imageFile?.let { Uri.fromFile(it) }
            if (fallbackUri != null) {
                lifecycleScope.launch {
                    analyzeImageWithGemini(fallbackUri, modelId)
                }
            } else {
                Toast.makeText(requireContext(), "Erreur: aucune image à analyser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTitleMenu() {
        val titleMenuDialog = TitleMenuDialogFragment()
        titleMenuDialog.listener = this
        titleMenuDialog.show(childFragmentManager, "TitleMenuDialog")
    }

    override fun onMenuItemClick(itemId: Int) {
        when (itemId) {
            R.id.menu_preferences -> {
                // TODO: Ouvrir l'écran des préférences
                Toast.makeText(requireContext(), "Préférences - Fonctionnalité à implémenter", Toast.LENGTH_SHORT).show()
            }
            R.id.menu_manual -> {
                val manualUrl = "https://mrems.github.io/Nature974/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(manualUrl))
                startActivity(intent)
            }
            R.id.menu_feedback -> {
                // Appeler la nouvelle fonction pour montrer la boÃ®te de dialogue d'Ã©valuation in-app
                showInAppReview()
            }
            R.id.menu_subscription -> {
                val intent = Intent(requireContext(), PurchaseActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showInAppReview() {
        val manager = ReviewManagerFactory.create(requireContext())
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result
                manager.launchReviewFlow(requireActivity(), reviewInfo).addOnCompleteListener { _ ->
                    // Le flux est terminé. L'API n'indique pas si l'utilisateur a laissé un avis.
                    // On continue le flux normal de l'application.
                }
            } else {
                // Il y a eu un problème. Vous pouvez logguer l'erreur ou
                // avoir un fallback, par exemple, ouvrir le Play Store directement.
                Log.e("CameraFragment", "Erreur lors de la demande de l'API d'évaluation in-app: ${request.exception?.message}")
                // Optionnel : Fallback vers l'ouverture directe du Play Store
                val packageName = requireContext().packageName
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
            }
        }
    }

}
