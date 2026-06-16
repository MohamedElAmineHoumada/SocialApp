package com.Groupe15.SocialApp.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Comment
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.Groupe15.SocialApp.repository.CommentRepository
import com.Groupe15.SocialApp.repository.FeedRepository
import com.Groupe15.SocialApp.repository.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val commentRepository: CommentRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    // Posts filtrés : uniquement les utilisateurs suivis + soi-même
    val posts: LiveData<List<Post>> = feedRepository.getFeedPosts().asLiveData()

    private val _stories = MutableLiveData<List<Story>>(emptyList())
    val stories: LiveData<List<Story>> = _stories

    private val _recentContacts = MutableLiveData<List<com.Groupe15.SocialApp.models.User>>(emptyList())
    val recentContacts: LiveData<List<com.Groupe15.SocialApp.models.User>> = _recentContacts

    private val _selectedPostId = MutableStateFlow<String?>(null)
    val comments: LiveData<List<Comment>> = _selectedPostId
        .flatMapLatest { postId ->
            if (postId != null) commentRepository.getComments(postId)
            else flowOf(emptyList())
        }
        .asLiveData()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val currentUserId: String? get() = feedRepository.currentUserId

    init {
        loadFeed()
        loadStories()
        loadRecentContacts()
    }

    fun selectPost(postId: String) {
        _selectedPostId.value = postId
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleLike(postId)
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            commentRepository.addComment(postId, text)
        }
    }

    fun loadFeed() {
        // Le Flow callbackFlow dans getLivePosts() écoute en temps réel en permanence.
        // loadFeed() reste disponible pour le swipe-to-refresh côté UI.
        _isLoading.value = false
    }

    private fun loadStories() {
        viewModelScope.launch {
            feedRepository.getStories().collect { stories ->
                _stories.value = stories
            }
        }
    }

    fun sharePost(context: Context, post: Post) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${post.authorUsername} sur AFN : ${post.content}")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
    }

    fun shareToStory(post: Post) {
        viewModelScope.launch {
            feedRepository.shareToStory(post)
        }
    }

    fun followUser(targetUid: String) {
        viewModelScope.launch {
            followRepository.followUser(targetUid)
        }
    }

    private fun loadRecentContacts() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val users = followRepository.getFollowingUsers(uid)
            _recentContacts.value = users
        }
    }

    fun toggleSavePost(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleSavePost(postId)
        }
    }
}
