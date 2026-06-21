package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.FollowRequest
import com.Groupe15.SocialApp.models.SuggestionUser
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.repository.FollowRepository
import com.Groupe15.SocialApp.repository.NetworkRepository
import com.Groupe15.SocialApp.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val followRepository: FollowRepository,
    private val postRepository: PostRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _followRequests = MutableStateFlow<List<FollowRequest>>(emptyList())
    val followRequests: StateFlow<List<FollowRequest>> = _followRequests.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SuggestionUser>>(emptyList())
    val suggestions: StateFlow<List<SuggestionUser>> = _suggestions.asStateFlow()

    private val _trendingPosts = MutableStateFlow<List<Post>>(emptyList())
    val trendingPosts: StateFlow<List<Post>> = _trendingPosts.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private var lastDocument: DocumentSnapshot? = null
    private var isLastPage = false

    val categories = listOf("All", "AI", "Photography", "Tech", "Lifestyle", "Design")

    init {
        loadFollowRequests()
        loadSuggestions()
        loadTrendingPosts()
    }

    fun loadTrendingPosts(isRefresh: Boolean = false) {
        if (isRefresh) {
            lastDocument = null
            isLastPage = false
        }
        
        if (isLastPage || _isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val (posts, lastDoc) = networkRepository.getTrendingPosts(pageSize = 10, lastDocument = lastDocument)
                if (isRefresh) {
                    _trendingPosts.value = posts
                } else {
                    _trendingPosts.value = _trendingPosts.value + posts
                }
                lastDocument = lastDoc
                if (posts.size < 10) {
                    isLastPage = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadSuggestions()
        } else {
            viewModelScope.launch {
                try {
                    val results = networkRepository.searchUsers(query)
                    _suggestions.value = results
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
        // Reset and reload if category filtering is server-side
        // loadTrendingPosts(isRefresh = true)
    }

    private fun loadSuggestions() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _suggestions.value = networkRepository.getSuggestedUsers(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFollowRequests() {
        viewModelScope.launch {
            try {
                _followRequests.value = followRepository.getPendingFollowRequests()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onFollowUser(targetUid: String) {
        viewModelScope.launch {
            val result = followRepository.followUser(targetUid)
            if (result.isSuccess) {
                _suggestions.value = _suggestions.value.filterNot { it.id == targetUid }
            }
        }
    }

    fun onAcceptRequest(requestId: String) {
        viewModelScope.launch {
            followRepository.acceptFollowRequest(requestId)
            _followRequests.value = _followRequests.value.filterNot { it.id == requestId }
        }
    }

    fun onDeclineRequest(requestId: String) {
        viewModelScope.launch {
            followRepository.rejectFollowRequest(requestId)
            _followRequests.value = _followRequests.value.filterNot { it.id == requestId }
        }
    }
}
