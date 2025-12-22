package com.geronimo.geki

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.geronimo.geki.IntroPage1Fragment
import com.geronimo.geki.IntroPage2Fragment
import com.geronimo.geki.IntroPage3Fragment
import com.geronimo.geki.IntroPage4Fragment
import com.geronimo.geki.IntroPage5Fragment

class IntroOnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forcer la suppression de l'ActionBar
        supportActionBar?.hide()
        
        setContentView(R.layout.activity_intro_onboarding)
        
        viewPager = findViewById(R.id.intro_view_pager)
        
        val adapter = IntroScreenSlidePagerAdapter(this)
        viewPager.adapter = adapter
        
        // DÃ©sactiver le swipe pour contrÃ´ler manuellement la navigation
        viewPager.isUserInputEnabled = false
    }
    
    fun goToNextPage() {
        val currentItem = viewPager.currentItem
        if (currentItem < 4) {
            viewPager.setCurrentItem(currentItem + 1, false)
        }
    }
    
    fun goToGoogleSignIn() {
        // Lancer directement la connexion Google
        // Ne PAS appeler finish() pour permettre le retour en arriÃ¨re si l'utilisateur annule
        // Ne PAS marquer intro_completed ici - on le fera aprÃ¨s connexion rÃ©ussie
        val intent = Intent(this, GoogleSignInActivity::class.java)
        startActivity(intent)
    }
    
    private inner class IntroScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 5
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> IntroPage1Fragment()
                1 -> IntroPage2Fragment()
                2 -> IntroPage3Fragment()
                3 -> IntroPage4Fragment()
                4 -> IntroPage5Fragment()
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}

