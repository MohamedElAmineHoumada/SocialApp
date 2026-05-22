package com.Groupe15.SocialApp.ui.auth

import androidx.lifecycle.ViewModel
import com.Groupe15.SocialApp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun isLoggedIn() = authRepository.isLoggedIn()

    fun signOut() = authRepository.logout()
}