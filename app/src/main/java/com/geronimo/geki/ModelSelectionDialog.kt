package com.geronimo.geki

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
    var entryToReanalyze: AnalysisEntry? = null // Pour la rÃ©-analyse dans l'historique

    companion object {
        fun newInstance(): ModelSelectionDialog {
            return ModelSelectionDialog()
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
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Geki_Dialog)
        val view = layoutInflater.inflate(R.layout.dialog_model_selection, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.model_radio_group)

        val linearLayoutFlashLite = view.findViewById<View>(R.id.linear_layout_flash_lite)
        val linearLayoutFlash = view.findViewById<View>(R.id.linear_layout_flash)
        val linearLayoutGemini3Pro = view.findViewById<View>(R.id.linear_layout_gemini_3_pro)

        // Configurer les listeners de clic pour les LinearLayouts
        linearLayoutFlashLite.setOnClickListener { radioGroup.check(R.id.radio_flash_lite) }
        linearLayoutFlash.setOnClickListener { radioGroup.check(R.id.radio_flash) }
        linearLayoutGemini3Pro.setOnClickListener { radioGroup.check(R.id.radio_gemini_3_pro) }

        // Toujours sÃ©lectionner le modÃ¨le Ã‰quilibrÃ© (2) par dÃ©faut
        radioGroup.check(R.id.radio_flash)

        builder.setView(view)
            .setTitle(getString(R.string.model_selection_title))

        // Configurer le bouton Analyser
        val btnAnalyze = view.findViewById<View>(R.id.btn_analyze)

        btnAnalyze.setOnClickListener {
            val checkedId = radioGroup.checkedRadioButtonId
            val selectedModel = when (checkedId) {
                R.id.radio_flash_lite -> "1"
                R.id.radio_flash -> "2"
                R.id.radio_gemini_3_pro -> "3"
                else -> "2" // Valeur par dÃ©faut
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
