package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.repository.AuthRepository
import com.Groupe15.SocialApp.ui.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                // Vérifier si l'email est vérifié
                if (!authRepository.isEmailVerified()) {
                    _state.value = AuthState.EmailNotVerified
                } else {
                    _state.value = AuthState.Success
                }
            } else {
                _state.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Erreur de connexion")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            // Google = email toujours vérifié, aller directement au feed
            _state.value = if (result.isSuccess) AuthState.Success
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Erreur Google")
        }
    }

    fun signInWithFacebook(accessToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.signInWithFacebook(accessToken)
            _state.value = if (result.isSuccess) AuthState.Success
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Erreur Facebook")
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            authRepository.resendVerificationEmail()
            _state.value = AuthState.VerificationEmailSent
        }
    }

    fun checkEmailVerified() {
        viewModelScope.launch {
            val result = authRepository.reloadUser()
            if (result.getOrDefault(false)) {
                _state.value = AuthState.Success
            }
            // sinon : pas encore vérifié, on ne change pas le state
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}