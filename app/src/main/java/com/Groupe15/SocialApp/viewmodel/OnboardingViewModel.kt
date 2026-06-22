package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun saveBirthDate(birthDate: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.updateBirthDate(birthDate)
            onComplete()
        }
    }

    fun saveGender(gender: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.updateGender(gender)
            onComplete()
        }
    }

    fun saveInterests(interests: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.updateInterests(interests)
            onComplete()
        }
    }
}
