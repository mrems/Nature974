package com.example.naturepei

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment

class ThemeSelectionDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = android.app.AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_theme_selection, null)

            val themeRadioGroup: RadioGroup = view.findViewById(R.id.theme_radio_group)
            val radioSystemTheme: RadioButton = view.findViewById(R.id.radio_system_theme)
            val radioLightTheme: RadioButton = view.findViewById(R.id.radio_light_theme)
            val radioDarkTheme: RadioButton = view.findViewById(R.id.radio_dark_theme)

            val sharedPrefs = it.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
            val currentTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO) // Thème par défaut: Clair

            // Récupérer le thème actuel et cocher le bon RadioButton
            when (currentTheme) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> radioSystemTheme.isChecked = true
                AppCompatDelegate.MODE_NIGHT_NO -> radioLightTheme.isChecked = true
                AppCompatDelegate.MODE_NIGHT_YES -> radioDarkTheme.isChecked = true
            }

            themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val newThemeMode = when (checkedId) {
                    R.id.radio_system_theme -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    R.id.radio_light_theme -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.radio_dark_theme -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                sharedPrefs.edit().putInt("theme_mode", newThemeMode).apply()
                AppCompatDelegate.setDefaultNightMode(newThemeMode)

                // Redémarrer l'application pour appliquer le thème immédiatement
                val intent = Intent(activity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                activity?.startActivity(intent)
                activity?.finish()
            }

            builder.setView(view)
                .setTitle("Sélectionner le thème")
                .setNegativeButton("Annuler") { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
