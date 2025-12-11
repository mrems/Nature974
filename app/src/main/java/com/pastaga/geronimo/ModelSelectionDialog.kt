package com.pastaga.geronimo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ModelSelectionDialog : DialogFragment() {

    interface ModelSelectionListener {
        fun onModelSelected(modelId: String)
    }

    private var listener: ModelSelectionListener? = null
    private var selectedModelId: String = "gemini-2.5-flash" // Valeur par défaut
    var entryToReanalyze: AnalysisEntry? = null // Pour la ré-analyse dans l'historique

    companion object {
        private const val ARG_SELECTED_MODEL = "selected_model"

        fun newInstance(selectedModelId: String): ModelSelectionDialog {
            val fragment = ModelSelectionDialog()
            val args = Bundle()
            args.putString(ARG_SELECTED_MODEL, selectedModelId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            parentFragment is ModelSelectionListener -> parentFragment as ModelSelectionListener
            context is ModelSelectionListener -> context as ModelSelectionListener
            else -> throw RuntimeException("Parent must implement ModelSelectionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // L'initialisation de `selectedModelId` à "gemini-2.5-flash" à la déclaration est suffisante.
        // La logique pour récupérer un modèle précédemment sélectionné via les arguments est supprimée
        // pour garantir que "gemini-2.5-flash" est toujours le modèle par défaut présélectionné.
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Geronimo_Dialog)
        val view = layoutInflater.inflate(R.layout.dialog_model_selection, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.model_radio_group)

        val linearLayoutFlashLite = view.findViewById<View>(R.id.linear_layout_flash_lite)
        val linearLayoutFlash = view.findViewById<View>(R.id.linear_layout_flash)
        val linearLayoutGemini3Pro = view.findViewById<View>(R.id.linear_layout_gemini_3_pro)

        // Configurer les listeners de clic pour les LinearLayouts
        linearLayoutFlashLite.setOnClickListener { radioGroup.check(R.id.radio_flash_lite) }
        linearLayoutFlash.setOnClickListener { radioGroup.check(R.id.radio_flash) }
        linearLayoutGemini3Pro.setOnClickListener { radioGroup.check(R.id.radio_gemini_3_pro) }

        // Sélectionner le modèle par défaut
        when (selectedModelId) {
            "gemini-2.5-flash-lite-preview-09-2025" -> radioGroup.check(R.id.radio_flash_lite)
            "gemini-2.5-flash" -> radioGroup.check(R.id.radio_flash)
            "gemini-3-pro-preview" -> radioGroup.check(R.id.radio_gemini_3_pro)
            else -> radioGroup.check(R.id.radio_flash) // Valeur par défaut
        }

        builder.setView(view)
            .setTitle(getString(R.string.model_selection_title))

        // Configurer le bouton Analyser
        val btnAnalyze = view.findViewById<View>(R.id.btn_analyze)

        btnAnalyze.setOnClickListener {
            val checkedId = radioGroup.checkedRadioButtonId
            val selectedModel = when (checkedId) {
                R.id.radio_flash_lite -> "gemini-2.5-flash-lite-preview-09-2025"
                R.id.radio_flash -> "gemini-2.5-flash"
                R.id.radio_gemini_3_pro -> "gemini-3-pro-preview"
                else -> "gemini-2.5-flash" // Valeur par défaut
            }
            listener?.onModelSelected(selectedModel)
            dismiss()
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
