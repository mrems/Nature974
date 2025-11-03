package com.pastaga.geronimo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Fragment d'onboarding qui gère la demande de permissions
 * avant de permettre l'accès à l'application principale
 */
class OnboardingFragment : Fragment() {

    private lateinit var iconImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var requestButton: Button
    
    private var currentPermissionIndex = 0
    
    // Liste des permissions à demander avec leurs descriptions
    private data class PermissionInfo(
        val permission: String,
        val title: String,
        val description: String,
        val iconRes: Int,
        val required: Boolean = true
    )
    
    private val permissionsToRequest = mutableListOf<PermissionInfo>()
    
    // Launcher pour une permission simple
    private val singlePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(isGranted)
    }
    
    // Launcher pour permissions multiples (localisation)
    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        handlePermissionResult(anyGranted)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Construire la liste des permissions à demander
        buildPermissionsList()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        iconImageView = view.findViewById(R.id.onboarding_icon)
        titleTextView = view.findViewById(R.id.onboarding_title)
        descriptionTextView = view.findViewById(R.id.onboarding_description)
        requestButton = view.findViewById(R.id.request_permission_button)
        
        requestButton.setOnClickListener {
            requestCurrentPermission()
        }
        
        // Afficher la première permission ou terminer si toutes sont accordées
        checkAndShowNextPermission()
    }
    
    private fun buildPermissionsList() {
        permissionsToRequest.clear()
        
        // Permission caméra
        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            permissionsToRequest.add(
                PermissionInfo(
                    permission = Manifest.permission.CAMERA,
                    title = "Permission Caméra",
                    description = "Cette application a besoin d'accéder à votre caméra pour capturer et analyser des photos de plantes et d'animaux.",
                    iconRes = R.drawable.camera,
                    required = true
                )
            )
        }
        
        // Permissions de localisation
        val locationGranted = isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                              isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!locationGranted) {
            permissionsToRequest.add(
                PermissionInfo(
                    permission = "LOCATION", // Identifiant spécial pour gérer les deux permissions
                    title = "Permission Localisation",
                    description = "L'accès à votre localisation permet d'améliorer la précision de l'identification des espèces en fonction de votre région.",
                    iconRes = R.drawable.localisation,
                    required = true
                )
            )
        }
        
        // Permission de stockage/médias
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (!isPermissionGranted(storagePermission)) {
            permissionsToRequest.add(
                PermissionInfo(
                    permission = storagePermission,
                    title = "Permission Galerie",
                    description = "L'accès à vos photos vous permet d'analyser des images existantes depuis votre galerie.",
                    iconRes = R.drawable.stockage,
                    required = true
                )
            )
        }
    }
    
    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun checkAndShowNextPermission() {
        if (currentPermissionIndex >= permissionsToRequest.size) {
            // Toutes les permissions ont été traitées
            onOnboardingComplete()
            return
        }
        
        val permissionInfo = permissionsToRequest[currentPermissionIndex]
        
        // Mettre à jour l'UI
        iconImageView.setImageResource(permissionInfo.iconRes)
        titleTextView.text = permissionInfo.title
        descriptionTextView.text = permissionInfo.description
        requestButton.text = "Accepter"
    }
    
    private fun requestCurrentPermission() {
        if (currentPermissionIndex >= permissionsToRequest.size) {
            onOnboardingComplete()
            return
        }
        
        val permissionInfo = permissionsToRequest[currentPermissionIndex]
        
        when (permissionInfo.permission) {
            "LOCATION" -> {
                // Demander les deux permissions de localisation
                multiplePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            else -> {
                // Demander une permission simple
                singlePermissionLauncher.launch(permissionInfo.permission)
            }
        }
    }
    
    private fun handlePermissionResult(isGranted: Boolean) {
        val permissionInfo = permissionsToRequest[currentPermissionIndex]
        
        if (!isGranted && permissionInfo.required) {
            // Permission requise refusée
            Toast.makeText(
                requireContext(),
                "Cette permission est nécessaire pour utiliser l'application.",
                Toast.LENGTH_LONG
            ).show()
            // On peut choisir de redemander ou de bloquer l'accès
            // Pour l'instant, on avance quand même
        }
        
        // Passer à la permission suivante
        currentPermissionIndex++
        checkAndShowNextPermission()
    }
    
    private fun onOnboardingComplete() {
        // Vérifier que toutes les permissions obligatoires sont accordées
        val missingPermissions = mutableListOf<String>()
        
        // Vérifier la caméra
        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            missingPermissions.add("caméra")
        }
        
        // Vérifier la localisation
        val locationGranted = isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                              isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!locationGranted) {
            missingPermissions.add("localisation")
        }
        
        // Vérifier le stockage
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (!isPermissionGranted(storagePermission)) {
            missingPermissions.add("galerie")
        }
        
        // Si des permissions manquent, recommencer l'onboarding
        if (missingPermissions.isNotEmpty()) {
            val message = if (missingPermissions.size == 1) {
                "La permission ${missingPermissions[0]} est obligatoire pour utiliser cette application."
            } else {
                "Les permissions suivantes sont obligatoires : ${missingPermissions.joinToString(", ")}."
            }
            
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_LONG
            ).show()
            
            // Retourner au début
            currentPermissionIndex = 0
            buildPermissionsList()
            checkAndShowNextPermission()
            return
        }
        
        // Toutes les permissions sont traitées, notifier l'activité
        (activity as? MainActivity)?.onOnboardingCompleted()
    }
    
    companion object {
        fun newInstance() = OnboardingFragment()
    }
}


