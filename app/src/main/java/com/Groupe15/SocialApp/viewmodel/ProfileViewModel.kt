package com.Groupe15.SocialApp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.AuthRepository
import com.Groupe15.SocialApp.repository.FollowRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val followRepository: FollowRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _profileUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _profileUser.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(false)
    val isOwnProfile: StateFlow<Boolean> = _isOwnProfile.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    private val _isLoadingPosts = MutableStateFlow(false)
    val isLoadingPosts: StateFlow<Boolean> = _isLoadingPosts.asStateFlow()

    fun loadProfile(targetUid: String) {
        val currentUid = auth.currentUser?.uid ?: ""

        // Résoudre le vrai UID : si vide ou "me", charger le profil connecté
        val resolvedUid = if (targetUid.isEmpty() || targetUid == "me") currentUid else targetUid

        _isOwnProfile.value = resolvedUid == currentUid

        viewModelScope.launch {
            if (_isOwnProfile.value) {
                authRepository.getCurrentUser().collect { user ->
                    _profileUser.value = user
                }
            } else {
                authRepository.getUserById(resolvedUid).collect { user ->
                    _profileUser.value = user
                }
            }
        }

        loadUserPosts(resolvedUid)
    }

    private fun loadUserPosts(uid: String) {
        viewModelScope.launch {
            _isLoadingPosts.value = true
            try {
                val snapshot = firestore.collection("posts")
                    .whereEqualTo("userId", uid)
                    .limit(30)
                    .get()
                    .await()
                _userPosts.value = snapshot.toObjects(Post::class.java)
                    .sortedByDescending { it.getCreatedAtMillis() }
            } catch (e: Exception) {
                _userPosts.value = emptyList()
            } finally {
                _isLoadingPosts.value = false
            }
        }
    }

    // Appelé par FollowViewModel après follow/unfollow pour rafraîchir les compteurs
    fun refreshProfile() {
        val uid = _profileUser.value?.id ?: return
        loadProfile(uid)
    }
}