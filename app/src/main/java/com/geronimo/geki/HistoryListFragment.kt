package com.geronimo.geki

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.os.Parcelable
import java.io.IOException
import java.util.Locale

class HistoryListFragment : Fragment(), ModelSelectionDialog.ModelSelectionListener {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var analysisHistoryManager: AnalysisHistoryManager
    private lateinit var emptyStateLayout: View
    private lateinit var imageAnalyzer: ImageAnalyzer
    private lateinit var modelPreferences: ModelPreferences
    private lateinit var settingsPreferences: SettingsPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var sharedPrefs: SharedPreferences? = null
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == AnalysisHistoryManager.KEY_HISTORY_LIST) {
            // Charger l'historique quand il change
            loadHistory()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageAnalyzer = ImageAnalyzer(requireContext())
        modelPreferences = ModelPreferences(requireContext())
        settingsPreferences = SettingsPreferences(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
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

    /**
     * RÃ©cupÃ¨re (au mieux) la localisation courante (pays/rÃ©gion) pour une rÃ©-analyse.
     * - Ne demande pas de permission.
     * - Retourne null/null si indisponible.
     */
    private suspend fun getCurrentCountryRegionForReanalysis(): Pair<String?, String?> {
        if (!settingsPreferences.isLocationEnabled()) return null to null
        if (!checkLocationPermissions()) return null to null

        return try {
            val location = fusedLocationClient.lastLocation.await() ?: return null to null
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                } catch (_: IOException) {
                    null
                } catch (_: Exception) {
                    null
                }
            }
            val addr = addresses?.firstOrNull()
            (addr?.countryName) to (addr?.adminArea)
        } catch (_: Exception) {
            null to null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyRecyclerView = view.findViewById(R.id.history_recycler_view)
        analysisHistoryManager = AnalysisHistoryManager(requireContext())
        emptyStateLayout = view.findViewById(R.id.history_empty_state)
        sharedPrefs = requireContext().getSharedPreferences(AnalysisHistoryManager.PREFS_NAME, android.content.Context.MODE_PRIVATE)

        // Initialiser et attacher l'adaptateur ici avec une liste vide
        historyRecyclerView.adapter = HistoryAdapter(mutableListOf(), { entry ->
            // GÃ©rer le clic sur un Ã©lÃ©ment de l'historique
            // Sauvegarder cette fiche comme derniÃ¨re consultÃ©e
            lifecycleScope.launch(Dispatchers.IO) {
                analysisHistoryManager.saveLastViewedCard(entry)
            }
            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_IMAGE_URI, entry.imageUri)
                putExtra(ResultActivity.EXTRA_LOCAL_NAME, entry.localName)
                putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, entry.scientificName)
                putExtra(ResultActivity.EXTRA_TYPE, entry.type)
                putExtra(ResultActivity.EXTRA_HABITAT, entry.habitat)
                putExtra(ResultActivity.EXTRA_CHARACTERISTICS, entry.characteristics)
                putExtra(ResultActivity.EXTRA_LOCAL_CONTEXT, entry.localContext)
                putExtra(ResultActivity.EXTRA_DESCRIPTION, entry.description)
                putExtra(ResultActivity.EXTRA_REPRESENTATIVE_COLOR_HEX, entry.representativeColorHex) // Ajout pour un clic normal
                putExtra(ResultActivity.EXTRA_PECULIARITIES, entry.Peculiarities)
                putExtra(ResultActivity.EXTRA_DANGER, entry.danger) // Assurez-vous de passer le champ danger
                putExtra(ResultActivity.EXTRA_CONFIDENCE_SCORE, entry.confidenceScore ?: -1) // Passer le score de confiance
                putParcelableArrayListExtra(ResultActivity.EXTRA_ALTERNATIVE_IDENTIFICATIONS, entry.alternativeIdentifications?.let { ArrayList(it) }) // Passer les alternatives
                putExtra(ResultActivity.EXTRA_JUSTIFICATION_TEXT, entry.justificationText) // Passer le texte de justification
            }
            startActivity(intent)
        }) { entry, itemView ->
            // CrÃ©er un BottomSheetDialog personnalisÃ©
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_history_options, null)
            bottomSheetDialog.setContentView(dialogView)

            // Option RÃ©-analyser
            dialogView.findViewById<View>(R.id.option_reanalyze).setOnClickListener {
                bottomSheetDialog.dismiss()
                showModelSelectionDialog(entry)
            }

            // Option Supprimer avec confirmation
            dialogView.findViewById<View>(R.id.option_delete).setOnClickListener {
                bottomSheetDialog.dismiss()
                
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Geki_Dialog)
                    .setTitle(getString(R.string.dialog_delete_title))
                    .setMessage(getString(R.string.dialog_delete_message))
                    .setPositiveButton(getString(R.string.dialog_delete_button)) { dialog, which ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val lastViewedCard = analysisHistoryManager.getLastViewedCard()
                            val isLastViewed = lastViewedCard != null && lastViewedCard.imageUri == entry.imageUri
                            analysisHistoryManager.deleteAnalysisEntry(entry)
                            if (isLastViewed) {
                                // Si la fiche supprimÃ©e est la derniÃ¨re consultÃ©e, trouver la suivante dans l'historique
                                val updatedHistory = analysisHistoryManager.getAnalysisHistory()
                                val newLastViewed = updatedHistory.firstOrNull { !it.isTutorial } ?: updatedHistory.firstOrNull { it.isTutorial }
                                if (newLastViewed != null) {
                                    analysisHistoryManager.saveLastViewedCard(newLastViewed)
                                } else {
                                    // Si l'historique est vide, effacer la derniÃ¨re consultÃ©e
                                    sharedPrefs?.edit()?.remove(AnalysisHistoryManager.KEY_LAST_VIEWED_CARD)?.commit()
                                }
                            }
                            withContext(Dispatchers.Main) {
                                loadHistory()
                            }
                        }
                    }
                    .setNegativeButton(getString(R.string.dialog_cancel_button)) { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
            }

            bottomSheetDialog.show()
        }

        loadHistory()
    }

    override fun onStart() {
        super.onStart()
        // Ã‰coute des changements de l'historique
        sharedPrefs?.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onStop() {
        sharedPrefs?.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onStop()
    }

    // Recharger au retour sur l'Ã©cran pour prendre en compte les ajouts effectuÃ©s pendant l'absence
    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val historyList = analysisHistoryManager.getAnalysisHistory()
            // Filtrer les fiches tutorielles pour ne pas les afficher dans l'historique
            val filteredHistoryList = historyList.filter { !it.isTutorial }

            withContext(Dispatchers.Main) {
                if (filteredHistoryList.isNotEmpty()) {
                    historyRecyclerView.visibility = View.VISIBLE
                    emptyStateLayout.visibility = View.GONE
                    // Sauvegarder l'Ã©tat de scroll avant mise Ã  jour
                    val layoutManager = historyRecyclerView.layoutManager
                    val savedState = layoutManager?.onSaveInstanceState()

                    val adapter = historyRecyclerView.adapter as HistoryAdapter

                    // Mettre Ã  jour les Ã©lÃ©ments et restaurer la position
                    adapter.updateItems(filteredHistoryList)
                    layoutManager?.onRestoreInstanceState(savedState)
                } else {
                    historyRecyclerView.visibility = View.GONE
                    emptyStateLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    // MÃ©thode pour afficher la boÃ®te de dialogue de sÃ©lection du modÃ¨le
    private fun showModelSelectionDialog(entry: AnalysisEntry) {
        val dialog = ModelSelectionDialog.newInstance()
        // Stocker temporairement l'entrÃ©e pour la rÃ©-analyse
        (dialog as ModelSelectionDialog).entryToReanalyze = entry
        dialog.show(childFragmentManager, "ModelSelectionDialog")
    }

    // MÃ©thode pour rÃ©-analyser avec le modÃ¨le sÃ©lectionnÃ©
    private suspend fun reanalyzeWithModel(entry: AnalysisEntry, modelId: String) {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

        lifecycleScope.launch(Dispatchers.IO) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Utilisateur non connectÃ©. Veuillez vous connecter.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // --- AJOUT DE LA VÃ‰RIFICATION CÃ”TÃ‰ CLIENT (sans Toast) ---
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
            val creditsSnapshot = userDocRef.get().await()
            val currentCredits = creditsSnapshot.getLong("credits")?.toInt() ?: 0

            if (currentCredits <= 0) { // VÃ©rifier si les crÃ©dits sont Ã  zÃ©ro ou nÃ©gatifs
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    val intent = Intent(requireContext(), PurchaseActivity::class.java)
                    startActivity(intent)
                    // Toast supprimÃ© ici
                }
                return@launch
            }
            // --- FIN DE L'AJOUT ---

            val newResponse = try {
                val locationEnabled = settingsPreferences.isLocationEnabled()
                val (currentCountry, currentRegion) =
                    if (locationEnabled) getCurrentCountryRegionForReanalysis() else (null to null)
                // Si on a une localisation courante, on la privilÃ©gie; sinon on retombe sur celle stockÃ©e dans la fiche
                val countryToSend = if (locationEnabled) (currentCountry ?: entry.country) else null
                val regionToSend = if (locationEnabled) (currentRegion ?: entry.region) else null
                imageAnalyzer.analyzeImage(
                    Uri.parse(entry.imageUri),
                    modelId,
                    currentUser.uid,
                    countryToSend,
                    regionToSend,
                    Locale.getDefault().language // Passer le paramÃ¨tre de langue
                )
            } catch (e: InsufficientCreditsException) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    // Rediriger vers la page d'abonnement quand les crÃ©dits sont Ã©puisÃ©s
                    val intent = Intent(requireContext(), PurchaseActivity::class.java)
                    startActivity(intent)
                    // Toast supprimÃ© ici
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                if (newResponse != null) {
                    val updatedEntry = entry.copy(
                        localName = newResponse.localName,
                        scientificName = newResponse.scientificName,
                        description = "N/C", // Description n'est plus fournie par l'API
                        type = newResponse.type,
                        habitat = newResponse.habitat,
                        characteristics = newResponse.characteristics,
                        localContext = newResponse.localContext,
                        Peculiarities = newResponse.Peculiarities, // Assigner le nouveau champ
                        representativeColorHex = newResponse.representativeColorHex,
                        danger = newResponse.danger,
                        confidenceScore = newResponse.confidenceScore, // Assigner le score de confiance
                        alternativeIdentifications = newResponse.alternativeIdentifications, // Assigner les alternatives
                        justificationText = newResponse.justificationText // Assigner le texte de justification
                    )

                    // Mettre Ã  jour l'historique de maniÃ¨re synchrone dans un thread IO
                    withContext(Dispatchers.IO) {
                        analysisHistoryManager.updateAnalysisEntry(updatedEntry)
                        analysisHistoryManager.saveLastViewedCard(updatedEntry)
                    }

                    // Ouvrir ResultActivity avec les nouveaux rÃ©sultats
                    val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                        putExtra(ResultActivity.EXTRA_IMAGE_URI, entry.imageUri)
                        putExtra(ResultActivity.EXTRA_LOCAL_NAME, newResponse.localName)
                        putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, newResponse.scientificName)
                        putExtra(ResultActivity.EXTRA_TYPE, newResponse.type)
                        putExtra(ResultActivity.EXTRA_HABITAT, newResponse.habitat)
                        putExtra(ResultActivity.EXTRA_CHARACTERISTICS, newResponse.characteristics)
                        putExtra(ResultActivity.EXTRA_LOCAL_CONTEXT, newResponse.localContext)
                        putExtra(ResultActivity.EXTRA_DESCRIPTION, "N/C")
                        putExtra(ResultActivity.EXTRA_REPRESENTATIVE_COLOR_HEX, newResponse.representativeColorHex) // Ajout pour la rÃ©-analyse
                        putExtra(ResultActivity.EXTRA_PECULIARITIES, updatedEntry.Peculiarities)
                        putExtra(ResultActivity.EXTRA_DANGER, newResponse.danger) // Assurez-vous de passer le champ danger
                        putExtra(ResultActivity.EXTRA_CONFIDENCE_SCORE, newResponse.confidenceScore ?: -1) // Passer le score de confiance
                        putParcelableArrayListExtra(ResultActivity.EXTRA_ALTERNATIVE_IDENTIFICATIONS, newResponse.alternativeIdentifications?.let { ArrayList(it) }) // Passer les alternatives
                        putExtra(ResultActivity.EXTRA_JUSTIFICATION_TEXT, newResponse.justificationText) // Passer le texte de justification
                    }
                    startActivity(intent)

                    // L'historique sera rechargÃ© automatiquement par onResume() au retour
                } else {
                    Toast.makeText(requireContext(), "Ã‰chec de la rÃ©-analyse.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ImplÃ©mentation de ModelSelectionListener
    override fun onModelSelected(modelId: String) {
        // RÃ©cupÃ©rer l'entrÃ©e stockÃ©e temporairement et lancer la rÃ©-analyse
        val dialog = childFragmentManager.findFragmentByTag("ModelSelectionDialog") as? ModelSelectionDialog
        val entry = dialog?.entryToReanalyze
        if (entry != null) {
            lifecycleScope.launch {
                reanalyzeWithModel(entry, modelId)
            }
        }
    }
}

