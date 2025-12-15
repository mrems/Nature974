package com.pastaga.geronimo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class GeronimoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val settingsPreferences = SettingsPreferences(this)
        AppCompatDelegate.setDefaultNightMode(
            if (settingsPreferences.isDarkThemeEnabled())
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}



