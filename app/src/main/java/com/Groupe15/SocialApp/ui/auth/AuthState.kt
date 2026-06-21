package com.Groupe15.SocialApp.ui.auth

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    /** Connexion réussie mais email non encore vérifié */
    object EmailNotVerified : AuthState()
    /** Email de vérification renvoyé avec succès */
    object VerificationEmailSent : AuthState()
    data class Error(val message: String) : AuthState()
}