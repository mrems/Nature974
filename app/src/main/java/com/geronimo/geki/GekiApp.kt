package com.geronimo.geki

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class GekiApp : Application() {
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


