package com.pastaga.geronimo

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.DialogFragment

class LoadingDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Désactiver le comportement de fermeture par défaut
        isCancelable = false
        val view = inflater.inflate(R.layout.dialog_loading, container, false)
        
        // Ajouter une animation de pulsation à l'icône de feuille
        val leafIcon = view.findViewById<ImageView>(R.id.loading_leaf_icon)
        val pulseAnimation = AnimationUtils.loadAnimation(context, R.anim.pulse_animation)
        leafIcon?.startAnimation(pulseAnimation)
        
        // Ajouter une animation d'entrée au conteneur principal
        val contentLayout = view.findViewById<ViewGroup>(R.id.loading_content_layout)
        val scaleInAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_fade_in)
        contentLayout?.startAnimation(scaleInAnimation)
        
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Rendre le fond semi-transparent et flouter l'arrière-plan
            setDimAmount(0.7f)
        }
        return dialog
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}


