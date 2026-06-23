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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    fun resetState() {
        _state.value = AuthState.Idle
    }


    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.register(email, password, displayName)
            _state.value = if (result.isSuccess) AuthState.Success
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Erreur lors de l'inscription")
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(idToken)
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
}