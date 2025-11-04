package com.pastaga.geronimo

import android.content.Intent
import android.content.SharedPreferences
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
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryListFragment : Fragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var analysisHistoryManager: AnalysisHistoryManager
    private lateinit var emptyStateLayout: View
    private lateinit var imageAnalyzer: ImageAnalyzer
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
            // Gérer le clic sur un élément de l'historique
            // Sauvegarder cette fiche comme dernière consultée
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
            }
            startActivity(intent)
        }) { entry, itemView ->
            // Créer un BottomSheetDialog personnalisé
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_history_options, null)
            bottomSheetDialog.setContentView(dialogView)

            // Option Ré-analyser
            dialogView.findViewById<View>(R.id.option_reanalyze).setOnClickListener {
                bottomSheetDialog.dismiss()
                
                val loadingDialog = LoadingDialogFragment()
                loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

                lifecycleScope.launch(Dispatchers.IO) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "Utilisateur non connecté. Veuillez vous connecter.", Toast.LENGTH_LONG).show()
                        }
                        Log.e("HistoryListFragment", "Erreur: Ré-analyse appelée sans utilisateur connecté.")
                        return@launch
                    }
                    Log.d("HistoryListFragment", "Utilisateur connecté: ${currentUser.uid}")
                    // Gating: décrémenter 1 crédit avant ré-analyse
                    try {
                        CreditsManager.decrementOneCredit()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "Crédits insuffisants ou non connecté.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val newResponse = imageAnalyzer.analyzeImage(
                        Uri.parse(entry.imageUri),
                        entry.country,
                        entry.region
                    )

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
                                localContext = newResponse.localContext
                            )
                            
                            // Mettre à jour l'historique de manière synchrone dans un thread IO
                            withContext(Dispatchers.IO) {
                                analysisHistoryManager.updateAnalysisEntry(updatedEntry)
                                analysisHistoryManager.saveLastViewedCard(updatedEntry)
                            }
                            
                            // Ouvrir ResultActivity avec les nouveaux résultats
                            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                                putExtra(ResultActivity.EXTRA_IMAGE_URI, entry.imageUri)
                                putExtra(ResultActivity.EXTRA_LOCAL_NAME, newResponse.localName)
                                putExtra(ResultActivity.EXTRA_SCIENTIFIC_NAME, newResponse.scientificName)
                                putExtra(ResultActivity.EXTRA_TYPE, newResponse.type)
                                putExtra(ResultActivity.EXTRA_HABITAT, newResponse.habitat)
                                putExtra(ResultActivity.EXTRA_CHARACTERISTICS, newResponse.characteristics)
                                putExtra(ResultActivity.EXTRA_LOCAL_CONTEXT, newResponse.localContext)
                                putExtra(ResultActivity.EXTRA_DESCRIPTION, "N/C")
                            }
                            startActivity(intent)
                            
                            // L'historique sera rechargé automatiquement par onResume() au retour
                        } else {
                            Toast.makeText(requireContext(), "Échec de la ré-analyse.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            // Option Supprimer avec confirmation
            dialogView.findViewById<View>(R.id.option_delete).setOnClickListener {
                bottomSheetDialog.dismiss()
                
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Geronimo_Dialog)
                    .setTitle("Confirmer la suppression")
                    .setMessage("Êtes-vous sûr de vouloir supprimer cette fiche d'analyse ?")
                    .setPositiveButton("Supprimer") { dialog, which ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val lastViewedCard = analysisHistoryManager.getLastViewedCard()
                            val isLastViewed = lastViewedCard != null && lastViewedCard.imageUri == entry.imageUri
                            analysisHistoryManager.deleteAnalysisEntry(entry)
                            if (isLastViewed) {
                                // Si la fiche supprimée est la dernière consultée, trouver la suivante dans l'historique
                                val updatedHistory = analysisHistoryManager.getAnalysisHistory()
                                val newLastViewed = updatedHistory.firstOrNull { !it.isTutorial } ?: updatedHistory.firstOrNull { it.isTutorial }
                                if (newLastViewed != null) {
                                    analysisHistoryManager.saveLastViewedCard(newLastViewed)
                                } else {
                                    // Si l'historique est vide, effacer la dernière consultée
                                    sharedPrefs?.edit()?.remove(AnalysisHistoryManager.KEY_LAST_VIEWED_CARD)?.commit()
                                }
                            }
                            withContext(Dispatchers.Main) {
                                loadHistory()
                            }
                        }
                    }
                    .setNegativeButton("Annuler") { dialog, which ->
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
        // Écoute des changements de l'historique
        sharedPrefs?.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onStop() {
        sharedPrefs?.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onStop()
    }

    // Recharger au retour sur l'écran pour prendre en compte les ajouts effectués pendant l'absence
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
                    // Sauvegarder l'état de scroll avant mise à jour
                    val layoutManager = historyRecyclerView.layoutManager
                    val savedState = layoutManager?.onSaveInstanceState()

                    val adapter = historyRecyclerView.adapter as HistoryAdapter

                    // Mettre à jour les éléments et restaurer la position
                    adapter.updateItems(filteredHistoryList)
                    layoutManager?.onRestoreInstanceState(savedState)
                } else {
                    historyRecyclerView.visibility = View.GONE
                    emptyStateLayout.visibility = View.VISIBLE
                }
            }
        }
    }
}

