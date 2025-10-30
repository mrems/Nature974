package com.example.naturepei

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
import android.view.Menu
import android.widget.PopupMenu
import android.app.AlertDialog
import android.net.Uri
import android.widget.Toast
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class HistoryListFragment : Fragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var analysisHistoryManager: AnalysisHistoryManager
    private lateinit var noDataTextView: TextView
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
        noDataTextView = view.findViewById(R.id.history_no_data_text)
        sharedPrefs = requireContext().getSharedPreferences(AnalysisHistoryManager.PREFS_NAME, android.content.Context.MODE_PRIVATE)

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
            withContext(Dispatchers.Main) {
                if (historyList.isNotEmpty()) {
                    historyRecyclerView.visibility = View.VISIBLE
                    noDataTextView.visibility = View.GONE
                    // Sauvegarder l'état de scroll avant mise à jour
                    val layoutManager = historyRecyclerView.layoutManager
                    val savedState = layoutManager?.onSaveInstanceState()

                    val currentAdapter = historyRecyclerView.adapter as? HistoryAdapter
                    val adapter = currentAdapter ?: HistoryAdapter(mutableListOf(), { entry ->
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
                        // Gérer les options via le nouveau bouton
                        val popupMenu = PopupMenu(requireContext(), itemView) // 'itemView' est la vue du bouton
                        popupMenu.menu.apply {
                            add(Menu.NONE, 0, 0, "Delete").setIcon(R.drawable.ic_delete)
                            add(Menu.NONE, 1, 1, "Re-analyze").setIcon(R.drawable.ic_reload)
                        }

                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                0 -> {
                                    // Option Effacer avec confirmation
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Confirmer la suppression")
                                        .setMessage("Êtes-vous sûr de vouloir supprimer cette fiche d'analyse ?")
                                        .setPositiveButton("Supprimer") { dialog, which ->
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                analysisHistoryManager.deleteAnalysisEntry(entry)
                                                withContext(Dispatchers.Main) {
                                                    loadHistory()
                                                    Toast.makeText(requireContext(), "Fiche supprimée.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .setNegativeButton("Annuler") { dialog, which ->
                                            dialog.dismiss()
                                        }
                                        .show()
                                    true
                                }
                                1 -> {
                                    // Option Re-analyser
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
                                                analysisHistoryManager.updateAnalysisEntry(updatedEntry)
                                                loadHistory()
                                                Toast.makeText(requireContext(), "Ré-analyse terminée !", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(requireContext(), "Échec de la ré-analyse.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        popupMenu.show()
                        true
                    }

                    if (currentAdapter == null) {
                        historyRecyclerView.adapter = adapter
                    }

                    // Mettre à jour les éléments et restaurer la position
                    adapter.updateItems(historyList)
                    layoutManager?.onRestoreInstanceState(savedState)
                } else {
                    historyRecyclerView.visibility = View.GONE
                    noDataTextView.visibility = View.VISIBLE
                }
            }
        }
    }
}
