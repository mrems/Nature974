package com.example.naturepei

import android.content.Intent
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

class HistoryListFragment : Fragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var analysisHistoryManager: AnalysisHistoryManager
    private lateinit var noDataTextView: TextView
    private lateinit var imageAnalyzer: ImageAnalyzer

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

        loadHistory()
    }

    // Ne pas recharger automatiquement l'historique au retour pour préserver la position de scroll
    // override fun onResume() {
    //     super.onResume()
    //     loadHistory()
    // }

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
                            putExtra(ResultActivity.EXTRA_REUNION_CONTEXT, entry.reunionContext)
                            putExtra(ResultActivity.EXTRA_DESCRIPTION, entry.description)
                        }
                        startActivity(intent)
                    }) { entry, itemView ->
                        // Gérer les options via le nouveau bouton
                        val popupMenu = PopupMenu(requireContext(), itemView) // 'itemView' est la vue du bouton
                        popupMenu.menu.apply {
                            add(Menu.NONE, 0, 0, "Effacer")
                            add(Menu.NONE, 1, 1, "Re-analyser")
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
                                        val newResponse = imageAnalyzer.analyzeImage(Uri.parse(entry.imageUri))

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
                                                    reunionContext = newResponse.reunionContext
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
