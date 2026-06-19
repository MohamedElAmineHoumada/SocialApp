package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authEvents = MutableSharedFlow<AuthEvent>()
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    val currentUser: StateFlow<User?> = authRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun isLoggedIn() = authRepository.isLoggedIn()

    fun getCurrentUserUid() = authRepository.getCurrentUserUid()

    fun signOut() = authRepository.logout()

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            if (result.isSuccess) {
                _authEvents.emit(AuthEvent.PasswordResetSent)
            } else {
                _authEvents.emit(AuthEvent.Error(result.exceptionOrNull()?.message ?: "Erreur inconnue"))
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                _authEvents.emit(AuthEvent.AccountDeleted)
            } else {
                _authEvents.emit(AuthEvent.Error(result.exceptionOrNull()?.message ?: "Erreur inconnue"))
            }
        }
    }

    fun updatePrivacyStatus(isPrivate: Boolean) {
        viewModelScope.launch {
            val result = authRepository.updatePrivacyStatus(isPrivate)
            if (result.isFailure) {
                _authEvents.emit(AuthEvent.Error(result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour"))
            }
        }
    }

    sealed class AuthEvent {
        object PasswordResetSent : AuthEvent()
        object AccountDeleted : AuthEvent()
        data class Error(val message: String) : AuthEvent()
    }
}
