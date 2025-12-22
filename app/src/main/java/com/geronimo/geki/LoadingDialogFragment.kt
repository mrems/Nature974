package com.geronimo.geki

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
        // DÃ©sactiver le comportement de fermeture par dÃ©faut
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
        
        // Ajouter une animation d'entrÃ©e au conteneur principal
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
            // Rendre le fond semi-transparent et flouter l'arriÃ¨re-plan
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
        // ArrÃªter l'animation pour libÃ©rer les ressources
        lottieAnimation?.cancelAnimation()
        lottieAnimation = null
        super.onDestroyView()
    }
}


