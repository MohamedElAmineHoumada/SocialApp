package com.Groupe15.SocialApp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context.MODE_PRIVATE
import com.Groupe15.SocialApp.util.LanguageManager

@HiltAndroidApp
class SocialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Lire la préférence sauvegardée (mode sombre)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // ✅ Appliquer la langue sauvegardée (français/anglais/arabe), si l'utilisateur
        // en a déjà choisi une explicitement. Doit être appelé avant l'affichage de
        // la première UI pour que l'app démarre directement dans la bonne langue.
        LanguageManager.applySavedLanguage(this)
    }
}