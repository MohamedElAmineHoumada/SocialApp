package com.Groupe15.SocialApp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.Groupe15.SocialApp.data.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableLiveData(AuthState())
    val state: LiveData<AuthState> = _state

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)

            val result = repository.register(email, password, username)

            _state.value = if (result.isSuccess) {
                AuthState(success = true)
            } else {
                AuthState(
                    error = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                )
            }
        }
    }
}
