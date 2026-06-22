package com.Groupe15.SocialApp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.AuthRepository
import com.Groupe15.SocialApp.repository.FeedRepository
import com.Groupe15.SocialApp.repository.FollowRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val feedRepository: FeedRepository,
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

    private val _userStories = MutableStateFlow<List<Story>>(emptyList())
    val userStories: StateFlow<List<Story>> = _userStories.asStateFlow()

    //  posts sauvegardés (onglet "Saved")
    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts.asStateFlow()

    private val _isLoadingSaved = MutableStateFlow(false)
    val isLoadingSaved: StateFlow<Boolean> = _isLoadingSaved.asStateFlow()

    private var savedPostsJob: Job? = null

    private var postsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadProfile(targetUid: String) {
        val currentUid = auth.currentUser?.uid ?: ""
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

        startListeningToUserPosts(resolvedUid)
        loadUserStories(resolvedUid)
        resyncPostsCount(resolvedUid)
    }

    private fun startListeningToUserPosts(uid: String) {
        postsListener?.remove()
        _isLoadingPosts.value = true
        
        postsListener = firestore.collection("posts")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                _isLoadingPosts.value = false
                if (error != null) {
                    _userPosts.value = emptyList()
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.also { post ->
                            if (post.postId.isBlank()) post.postId = doc.id
                        }
                    }.sortedByDescending { it.getCreatedAtMillis() }
                    _userPosts.value = posts
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
        savedPostsJob?.cancel()
    }

    private fun loadUserStories(uid: String) {
        viewModelScope.launch {
            _userStories.value = feedRepository.getStoriesByUserId(uid)
        }
    }

    private fun resyncPostsCount(uid: String) {
        viewModelScope.launch {
            try {
                val countQuery = firestore.collection("posts")
                    .whereEqualTo("userId", uid)
                    .count()
                    .get(AggregateSource.SERVER)
                    .await()
                
                val realCount = countQuery.count.toInt()
                
                // Mettre à jour Firestore pour que le compteur soit cohérent partout
                firestore.collection("users").document(uid)
                    .update("postsCount", realCount)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //  charge les posts sauvegardés en temps réel (uniquement pour son propre profil)
    fun loadSavedPosts() {
        if (savedPostsJob != null) return // déjà en écoute
        savedPostsJob = viewModelScope.launch {
            _isLoadingSaved.value = true
            feedRepository.getSavedPostIdsFlow().collect { ids ->
                val posts = feedRepository.getPostsByIds(ids)
                _savedPosts.value = posts.sortedByDescending { it.getCreatedAtMillis() }
                _isLoadingSaved.value = false
            }
        }
    }

    fun refreshProfile() {
        val uid = _profileUser.value?.id ?: return
        loadProfile(uid)
    }
}