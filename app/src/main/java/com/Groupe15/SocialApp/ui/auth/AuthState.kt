package com.Groupe15.SocialApp.ui.auth

data class AuthState(
    val success: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false
)