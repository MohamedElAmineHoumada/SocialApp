package com.Groupe15.SocialApp.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var newImageUri: Uri? = null

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun setNewProfileImage(uri: Uri) {
        newImageUri = uri
    }

    fun saveProfile(displayName: String, username: String, bio: String, website: String) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")

                // Upload nouvelle photo si sélectionnée
                var profileImageUrl = _currentUser.value?.profileImageUrl ?: ""
                newImageUri?.let { uri ->
                    val ref = storage.reference.child("avatars/$uid/${UUID.randomUUID()}.jpg")
                    ref.putFile(uri).await()
                    profileImageUrl = ref.downloadUrl.await().toString()
                }

                // Mettre à jour Firestore
                val updates = mapOf(
                    "displayName"     to displayName,
                    "username"        to username,
                    "bio"             to bio,
                    "website"         to website,
                    "profileImageUrl" to profileImageUrl
                )
                firestore.collection("users").document(uid).update(updates).await()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur lors de la sauvegarde"
            } finally {
                _isSaving.value = false
            }
        }
    }
}