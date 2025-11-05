package com.pastaga.geronimo

import android.net.Uri
import android.widget.VideoView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.util.Log
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment

class IntroPage1Fragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro_page1, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val button = view.findViewById<Button>(R.id.page1_button)
        button.setOnClickListener {
            (activity as? IntroOnboardingActivity)?.goToNextPage()
        }

        val videoView = view.findViewById<VideoView>(R.id.page1_video_illustration)
        val placeholder = view.findViewById<ImageView>(R.id.page1_video_placeholder)
        
        try {
            val videoPath = "android.resource://" + requireContext().packageName + "/" + R.raw.bienvenue
            Log.d("IntroPage1", "Chargement de la vidéo: $videoPath")
            
            val uri = Uri.parse(videoPath)
            videoView.setVideoURI(uri)
            
            videoView.setOnPreparedListener { mp ->
                Log.d("IntroPage1", "Vidéo préparée, démarrage...")
                // Utiliser le mode de mise à l'échelle vidéo pour un comportement similaire à fitCenter
                mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                mp.isLooping = true
                videoView.start()
                
                // Attendre un court instant que la première frame soit rendue avant de cacher le placeholder
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("IntroPage1", "Cachant le placeholder...")
                    placeholder.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            placeholder.visibility = View.GONE
                        }
                        .start()
                }, 200)
            }
            
            videoView.setOnErrorListener { mp, what, extra ->
                Log.e("IntroPage1", "Erreur vidéo: what=$what, extra=$extra")
                // Garder le placeholder visible en cas d'erreur
                true
            }
            
            videoView.setOnInfoListener { mp, what, extra ->
                Log.d("IntroPage1", "Info vidéo: what=$what, extra=$extra")
                false
            }
        } catch (e: Exception) {
            Log.e("IntroPage1", "Exception lors du chargement de la vidéo", e)
        }
    }
}

