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
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieAnimationView

class LoadingDialogFragment : DialogFragment() {

    private var lottieAnimation: LottieAnimationView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Désactiver le comportement de fermeture par défaut
        isCancelable = false
        val view = inflater.inflate(R.layout.dialog_loading, container, false)
        
        // Charger l'animation Lottie depuis res/raw/loading.json
        lottieAnimation = view.findViewById<LottieAnimationView>(R.id.loading_lottie_animation)
        lottieAnimation?.apply {
            try {
                // Charger le fichier JSON depuis res/raw/
                setAnimation(R.raw.loading)
                repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
                playAnimation()
            } catch (e: Exception) {
            }
        }
        
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
    
    override fun onDestroyView() {
        // Arrêter l'animation pour libérer les ressources
        lottieAnimation?.cancelAnimation()
        lottieAnimation = null
        super.onDestroyView()
    }
}


