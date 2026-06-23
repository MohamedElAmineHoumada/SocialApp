package com.Groupe15.SocialApp.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat


object LanguageManager {

    private const val PREFS_NAME = "settings"
    private const val KEY_LANGUAGE = "app_language"

    /** Codes de langue supportés par l'application. */
    const val LANG_FRENCH = "fr"
    const val LANG_ENGLISH = "en"
    const val LANG_ARABIC = "ar"


    fun setLanguage(context: Context, languageCode: String) {
        // Persistance manuelle : nécessaire sur Android < 13 où le système ne retient
        // pas le choix de langue par application automatiquement.
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()

        applyLocale(languageCode)
    }


    fun applySavedLanguage(context: Context) {
        val saved = getSavedLanguage(context)
        if (saved != null) {
            applyLocale(saved)
        }

    }

    fun getSavedLanguage(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
    }

    private fun applyLocale(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}