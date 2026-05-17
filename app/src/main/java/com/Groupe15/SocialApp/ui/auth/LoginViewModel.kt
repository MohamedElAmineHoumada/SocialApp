package com.Groupe15.SocialApp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableLiveData(AuthState())
    val state: LiveData<AuthState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)

            val result = repository.login(email, password)

            _state.value = if (result.isSuccess) {
                AuthState(success = true)
            } else {
                AuthState(
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}