package com.pastaga.geronimo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var imageUriForCamera: String? = null
    private var isOnboardingComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.view_pager)
        
        // Vérifier si l'onboarding des permissions est nécessaire
        if (savedInstanceState != null) {
            isOnboardingComplete = savedInstanceState.getBoolean("onboarding_complete", false)
        }
        
        if (!isOnboardingComplete && needsOnboarding()) {
            // Afficher l'écran d'onboarding
            showOnboarding()
        } else {
            // Toutes les permissions sont accordées, afficher l'app normale
            isOnboardingComplete = true
            setupMainViewPager()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("onboarding_complete", isOnboardingComplete)
    }
    
    private fun needsOnboarding(): Boolean {
        // Vérifier si la permission caméra (obligatoire) est accordée
        val cameraGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        return !cameraGranted
    }
    
    private fun showOnboarding() {
        // Cacher le ViewPager et afficher l'OnboardingFragment
        viewPager.visibility = android.view.View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, OnboardingFragment.newInstance())
            .commitNowAllowingStateLoss()
    }
    
    fun onOnboardingCompleted() {
        isOnboardingComplete = true
        // Recréer la vue avec le ViewPager normal
        setupMainViewPager()
    }
    
    private fun setupMainViewPager() {
        // Retirer le OnboardingFragment s'il existe
        supportFragmentManager.findFragmentById(R.id.main_container)?.let { fragment ->
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitNowAllowingStateLoss()
        }
        
        // Créer la fiche d'exemple au premier lancement
        createTutorialEntryIfFirstLaunch()
        
        // Afficher le ViewPager
        viewPager.visibility = android.view.View.VISIBLE
        
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.setCurrentItem(1, false) // Définir l'écran central (CameraFragment) comme écran de démarrage
        viewPager.setOffscreenPageLimit(1) // Garder les fragments adjacents en mémoire pour des transitions plus fluides
    }
    
    private fun createTutorialEntryIfFirstLaunch() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        
        if (isFirstLaunch) {
            // Créer une fiche d'exemple éducative
            val tutorialEntry = AnalysisEntry(
                imageUri = "android.resource://$packageName/${R.drawable.illustration}",
                localName = "Nom commun",
                scientificName = "Nom scientifique",
                type = "Origine",
                habitat = "Cette section vous indique où l'espèce vit naturellement. Vous y trouverez des informations sur son environnement préféré.",
                characteristics = "Ici vous découvrirez les traits distinctifs qui vous aident à identifier l'espèce. Les détails visuels importants y sont décrits.",
                localContext = "Des informations spécifiques à votre région apparaissent ici. Vous saurez si l'espèce est protégée ou si elle présente des particularités locales.",
                country = null,
                region = null,
                description = "Ceci est un exemple de fiche d'analyse",
                timestamp = System.currentTimeMillis(),
                isTutorial = true,
                confidenceScore = 100, // Exemple de score de confiance
                tutorialExplanationFirstTab = "Découvrez ici le score de certitude de l'identification ainsi que des propositions d'alternatives avec les différences clés pour vous aider à affiner votre analyse.",
                tutorialExplanationPeculiarities = "Explorez ici les traits uniques, comportements et informations de sécurité pour chaque espèce. Ces détails vous aideront à mieux comprendre et interagir avec la nature."
            )
            
            val historyManager = AnalysisHistoryManager(this)
            historyManager.saveAnalysisEntry(tutorialEntry)
            
            // Marquer que ce n'est plus le premier lancement
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
    }

    fun navigateToCameraWithImage(imageUri: String) {
        imageUriForCamera = imageUri
        viewPager.setCurrentItem(1, true) // Naviguer vers le CameraFragment avec animation
    }

    fun navigateToGallery() {
        viewPager.setCurrentItem(3, true)
    }

    // Adaptateur pour le ViewPager
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3 // Trois écrans : Last Analysis, Camera, History

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LastAnalysisFragment()
                1 -> {
                    val fragment = CameraFragment.newInstance(imageUriForCamera)
                    imageUriForCamera = null // Réinitialiser après utilisation
                    fragment
                }
                2 -> HistoryListFragment()
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}

