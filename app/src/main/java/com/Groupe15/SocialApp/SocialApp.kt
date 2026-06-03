package com.Groupe15.SocialApp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context.MODE_PRIVATE
@HiltAndroidApp
class SocialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Lire la préférence sauvegardée
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
