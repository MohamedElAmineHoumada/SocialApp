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

    // ✅ NOUVEAU : nombre réel de publications (texte, image, vidéo confondus),
    // calculé directement depuis Firestore au lieu de se fier au champ statique
    // postsCount du document User (qui peut se désynchroniser, comme observé
    // précédemment pour followersCount/followingCount).
    private val _realPostsCount = MutableStateFlow(0)
    val realPostsCount: StateFlow<Int> = _realPostsCount.asStateFlow()

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
        loadRealPostsCount(resolvedUid)
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

    // ✅ NOUVEAU : compte le nombre RÉEL de posts de l'utilisateur (tous types confondus :
    // texte, image, vidéo — un seul modèle Post couvre tout, distingué seulement par le
    // contenu de imageUrls/content). Utilise l'agrégation count() de Firestore, qui est
    // rapide et ne télécharge pas tous les documents (contrairement à .get().size()).
    private fun loadRealPostsCount(uid: String) {
        viewModelScope.launch {
            try {
                val countSnapshot = firestore.collection("posts")
                    .whereEqualTo("userId", uid)
                    .count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER)
                    .await()
                _realPostsCount.value = countSnapshot.count.toInt()
            } catch (e: Exception) {
                // En cas d'échec (ex: ancienne version de Firestore SDK sans count()),
                // on retombe sur la taille de la liste déjà chargée (limitée à 30)
                _realPostsCount.value = _userPosts.value.size
            }
        }
    }

    // Appelé par FollowViewModel après follow/unfollow pour rafraîchir les compteurs
    fun refreshProfile() {
        val uid = _profileUser.value?.id ?: return
        loadProfile(uid)
    }
}