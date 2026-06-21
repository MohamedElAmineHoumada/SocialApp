package com.Groupe15.SocialApp.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Comment
import com.Groupe15.SocialApp.models.MessageType
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.Groupe15.SocialApp.repository.CommentRepository
import com.Groupe15.SocialApp.repository.FeedRepository
import com.Groupe15.SocialApp.repository.FollowRepository
import com.Groupe15.SocialApp.repository.MessageRepository
import com.Groupe15.SocialApp.repository.PostRepository
import com.Groupe15.SocialApp.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FeedTab {
    FOLLOWING,
    FOR_YOU
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val recommendationRepository: RecommendationRepository,
    private val commentRepository: CommentRepository,
    private val followRepository: FollowRepository,
    private val messageRepository: MessageRepository // ✅ NOUVEAU
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FeedTab.FOLLOWING)
    val selectedTab: LiveData<FeedTab> = _selectedTab.asLiveData()

    fun selectTab(tab: FeedTab) {
        _selectedTab.value = tab
    }

    val posts: LiveData<List<Post>> = _selectedTab
        .flatMapLatest { tab ->
            when (tab) {
                FeedTab.FOLLOWING -> postRepository.getLivePosts()
                FeedTab.FOR_YOU -> recommendationRepository.getForYouPosts(excludeAlreadyLiked = false)
            }
        }
        .asLiveData()

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

    private val _likedPostIds = MutableLiveData<Set<String>>(emptySet())
    val likedPostIds: LiveData<Set<String>> = _likedPostIds

    //  IDs des posts sauvegardés, mis à jour en temps réel
    private val _savedPostIds = MutableLiveData<Set<String>>(emptySet())
    val savedPostIds: LiveData<Set<String>> = _savedPostIds

    val currentUserId: String? get() = feedRepository.currentUserId

    init {
        loadFeed()
        loadStories()
        loadRecentContacts()
        loadSavedPostIds() // ✅ NOUVEAU
    }

    fun selectPost(postId: String) {
        _selectedPostId.value = postId
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.toggleLike(postId)
            val current = _likedPostIds.value.orEmpty().toMutableSet()
            if (postId in current) current.remove(postId) else current.add(postId)
            _likedPostIds.value = current
        }
    }

    fun refreshLikedState(postIds: List<String>) {
        viewModelScope.launch {
            val liked = postRepository.getLikedPostIds(postIds)
            _likedPostIds.value = liked
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            commentRepository.addComment(postId, text)
        }
    }

    fun loadFeed() {
        _isLoading.value = false
    }

    private fun loadStories() {
        viewModelScope.launch {
            try {
                feedRepository.getStories().collect { stories ->
                    _stories.value = stories
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    suspend fun createStory(mediaUri: android.net.Uri, text: String, filter: String): Result<Unit> {
        return feedRepository.createStory(mediaUri, text, filter)
    }

    fun followUser(targetUid: String) {
        viewModelScope.launch {
            followRepository.followUser(targetUid)
        }
    }

    private fun loadRecentContacts() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                val users = followRepository.getFollowingUsers(uid)
                _recentContacts.value = users
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleSavePost(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleSavePost(postId)
        }
    }

    //  écoute réactive des posts sauvegardés
    private fun loadSavedPostIds() {
        viewModelScope.launch {
            feedRepository.getSavedPostIdsFlow().collect { ids ->
                _savedPostIds.value = ids.toSet()
            }
        }
    }

    // envoie le lien du post dans une conversation existante (ou la crée)
    fun sendPostToChat(otherUserId: String, post: Post) {
        val senderId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val chatId = messageRepository.getChatId(senderId, otherUserId)
                messageRepository.sendMessage(
                    chatId = chatId,
                    senderId = senderId,
                    receiverId = otherUserId,
                    text = "https://socialapp.com/post/${post.postId}",
                    type = MessageType.TEXT
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}