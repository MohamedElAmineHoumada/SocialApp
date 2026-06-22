package com.Groupe15.SocialApp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.PostRepository
import com.Groupe15.SocialApp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _selectedVideo = MutableStateFlow<Uri?>(null)
    val selectedVideo: StateFlow<Uri?> = _selectedVideo.asStateFlow()

    private val _visibility = MutableStateFlow("Public")
    val visibility: StateFlow<String> = _visibility.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _postSuccess = MutableStateFlow(false)
    val postSuccess: StateFlow<Boolean> = _postSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        _selectedImages.value = _selectedImages.value + uris
        _selectedVideo.value = null // Only one type of media for now or handle both? User asked for text, image or video.
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value.filter { it != uri }
    }

    fun setVideo(uri: Uri?) {
        _selectedVideo.value = uri
        _selectedImages.value = emptyList()
    }

    fun setVisibility(value: String) {
        _visibility.value = value
    }

    fun createPost(caption: String) {
        if (_isPosting.value) return

        // ✅ Validation : il faut AU MOINS du texte OU AU MOINS un média
        if (caption.isBlank() && _selectedImages.value.isEmpty() && _selectedVideo.value == null) {
            _error.value = "Ajoute du texte, une photo ou une vidéo pour publier"
            return
        }

        viewModelScope.launch {
            _isPosting.value = true
            _error.value = null
            try {
                postRepository.createPost(
                    caption = caption,
                    imageUris = _selectedImages.value,
                    videoUri = _selectedVideo.value,
                    visibility = _visibility.value
                )
                _postSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Une erreur est survenue"
            } finally {
                _isPosting.value = false
            }
        }
    }
}