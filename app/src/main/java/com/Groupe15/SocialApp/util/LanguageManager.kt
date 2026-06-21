package com.Groupe15.SocialApp.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Gère le changement de langue de l'application.
 *
 * Utilise AppCompatDelegate.setApplicationLocales(), l'API moderne d'AppCompat 1.6+
 * qui :
 *  - applique immédiatement la nouvelle langue à TOUTES les Activities/Composables
 *    affichant des stringResource(), sans avoir besoin de recréer manuellement l'Activity
 *  - persiste automatiquement le choix au niveau du système sur Android 13+ (per-app language)
 *  - sur Android < 13, on persiste nous-mêmes le choix dans SharedPreferences pour le
 *    réappliquer à chaque lancement de l'app (voir SocialApp.kt)
 */
object LanguageManager {

    private const val PREFS_NAME = "settings"
    private const val KEY_LANGUAGE = "app_language"

    /** Codes de langue supportés par l'application. */
    const val LANG_FRENCH = "fr"
    const val LANG_ENGLISH = "en"
    const val LANG_ARABIC = "ar"

    /**
     * Applique la langue donnée immédiatement (recompose l'UI visible) et la persiste
     * pour qu'elle soit réappliquée au prochain lancement de l'app.
     */
    fun setLanguage(context: Context, languageCode: String) {
        // Persistance manuelle : nécessaire sur Android < 13 où le système ne retient
        // pas le choix de langue par application automatiquement.
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()

        applyLocale(languageCode)
    }

    /**
     * Relit la langue sauvegardée et l'applique. À appeler une fois au démarrage
     * de l'application (SocialApp.onCreate), avant que la première UI ne s'affiche,
     * pour garantir que l'app démarre toujours dans la bonne langue.
     */
    fun applySavedLanguage(context: Context) {
        val saved = getSavedLanguage(context)
        if (saved != null) {
            applyLocale(saved)
        }
        // Si aucune langue n'a jamais été choisie, on ne force rien : AppCompat utilise
        // alors la langue du système par défaut (comportement standard attendu).
    }

    /** Renvoie le code de langue actuellement sauvegardé, ou null si jamais défini. */
    fun getSavedLanguage(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
    }

    private fun applyLocale(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}