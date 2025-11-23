package com.pastaga.geronimo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

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
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_model_selection, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.model_radio_group)

        // Configurer les boutons radio
        val models = arrayOf(
            "gemini-2.5-flash-lite-preview-09-2025" to R.id.radio_flash_lite,
            "gemini-2.5-flash" to R.id.radio_flash,
            "gemini-3-pro-preview" to R.id.radio_gemini_3_pro
        )

        // Sélectionner le modèle par défaut
        models.forEach { (modelId, radioId) ->
            if (modelId == selectedModelId) {
                view.findViewById<RadioButton>(radioId).isChecked = true
            }
        }

        builder.setView(view)
            .setTitle("Choisir le niveau d'analyse")

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
