package com.example.mobile_appafn.ui.auth

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableLiveData(AuthState())
    val state: LiveData<AuthState> = _state

    fun login(email: String, password: String) {

        viewModelScope.launch {

            val result = repository.login(email, password)

            _state.value =
                if (result.isSuccess) {
                    AuthState(success = true)
                } else {
                    AuthState(
                        error = result.exceptionOrNull()?.message
                    )
                }
        }
    }
}