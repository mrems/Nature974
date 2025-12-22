package com.geronimo.geki

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
        
        // VÃ©rifier si l'onboarding des permissions est nÃ©cessaire
        if (savedInstanceState != null) {
            isOnboardingComplete = savedInstanceState.getBoolean("onboarding_complete", false)
        }
        
        if (!isOnboardingComplete && needsOnboarding()) {
            // Afficher l'Ã©cran d'onboarding
            showOnboarding()
        } else {
            // Toutes les permissions sont accordÃ©es, afficher l'app normale
            isOnboardingComplete = true
            setupMainViewPager()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("onboarding_complete", isOnboardingComplete)
    }
    
    private fun needsOnboarding(): Boolean {
        // VÃ©rifier si la permission camÃ©ra (obligatoire) est accordÃ©e
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
        // RecrÃ©er la vue avec le ViewPager normal
        setupMainViewPager()
    }
    
    private fun setupMainViewPager() {
        // Retirer le OnboardingFragment s'il existe
        supportFragmentManager.findFragmentById(R.id.main_container)?.let { fragment ->
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitNowAllowingStateLoss()
        }
        
        // CrÃ©er la fiche d'exemple au premier lancement
        createTutorialEntryIfFirstLaunch()
        
        // Afficher le ViewPager
        viewPager.visibility = android.view.View.VISIBLE
        
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.setCurrentItem(1, false) // DÃ©finir l'Ã©cran central (CameraFragment) comme Ã©cran de dÃ©marrage
        viewPager.setOffscreenPageLimit(1) // Garder les fragments adjacents en mÃ©moire pour des transitions plus fluides
    }
    
    private fun createTutorialEntryIfFirstLaunch() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        
        if (isFirstLaunch) {
            // CrÃ©er une fiche d'exemple Ã©ducative
            val tutorialEntry = AnalysisEntry(
                imageUri = "android.resource://$packageName/${R.drawable.illustration}",
                localName = getString(R.string.tutorial_entry_local_name),
                scientificName = getString(R.string.tutorial_entry_scientific_name),
                type = getString(R.string.tutorial_entry_type),
                habitat = getString(R.string.tutorial_entry_habitat),
                characteristics = getString(R.string.tutorial_entry_characteristics),
                localContext = getString(R.string.tutorial_entry_local_context),
                country = null,
                region = null,
                description = getString(R.string.tutorial_entry_description),
                timestamp = System.currentTimeMillis(),
                isTutorial = true,
                confidenceScore = 100, // Exemple de score de confiance
                tutorialExplanationFirstTab = getString(R.string.tutorial_explanation_first_tab),
                tutorialExplanationPeculiarities = getString(R.string.tutorial_explanation_peculiarities)
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
        override fun getItemCount(): Int = 3 // Trois Ã©crans : Last Analysis, Camera, History

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LastAnalysisFragment()
                1 -> {
                    val fragment = CameraFragment.newInstance(imageUriForCamera)
                    imageUriForCamera = null // RÃ©initialiser aprÃ¨s utilisation
                    fragment
                }
                2 -> HistoryListFragment()
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}

