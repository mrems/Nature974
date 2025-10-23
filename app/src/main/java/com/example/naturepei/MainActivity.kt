package com.example.naturepei

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var imageUriForCamera: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val themeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO) // Thème par défaut: Clair
        AppCompatDelegate.setDefaultNightMode(themeMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.view_pager)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.setCurrentItem(1, false) // Définir l'écran central (CameraFragment) comme écran de démarrage
        viewPager.setOffscreenPageLimit(1) // Garder les fragments adjacents en mémoire pour des transitions plus fluides
    }

    fun navigateToCameraWithImage(imageUri: String) {
        imageUriForCamera = imageUri
        viewPager.setCurrentItem(1, true) // Naviguer vers le CameraFragment avec animation
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
