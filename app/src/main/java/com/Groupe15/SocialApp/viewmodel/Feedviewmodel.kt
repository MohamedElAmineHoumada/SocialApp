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

/**
 * Onglets disponibles dans le Feed (Home).
 */
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
    private val followRepository: FollowRepository
) : ViewModel() {

    // ---- Onglet sélectionné (Following / For You) ----
    private val _selectedTab = MutableStateFlow(FeedTab.FOLLOWING)
    val selectedTab: LiveData<FeedTab> = _selectedTab.asLiveData()

    fun selectTab(tab: FeedTab) {
        _selectedTab.value = tab
    }

    // Posts affichés à l'écran selon l'onglet actif.
    // NOTE: followingPosts/forYouPosts séparés ont été retirés car ils créaient
    // 2 listeners Firestore actifs en permanence même non utilisés par l'UI.
    // On garde uniquement le flow réactif à l'onglet sélectionné.
    val posts: LiveData<List<Post>> = _selectedTab
        .flatMapLatest { tab ->
            android.util.Log.d("FeedDebug", "Onglet sélectionné: $tab")
            when (tab) {
                FeedTab.FOLLOWING -> postRepository.getLivePosts()
                FeedTab.FOR_YOU -> recommendationRepository.getForYouPosts(excludeAlreadyLiked = false)
            }
        }
        .map { list ->
            android.util.Log.d("FeedDebug", "Posts reçus: ${list.size} -> ${list.map { it.authorUsername }}")
            list
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

    // Likes de l'utilisateur courant pour les posts visibles actuellement (pour l'état du coeur)
    private val _likedPostIds = MutableLiveData<Set<String>>(emptySet())
    val likedPostIds: LiveData<Set<String>> = _likedPostIds

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
            postRepository.toggleLike(postId)
            // Met à jour l'état local immédiatement (optimistic UI)
            val current = _likedPostIds.value.orEmpty().toMutableSet()
            if (postId in current) current.remove(postId) else current.add(postId)
            _likedPostIds.value = current
        }
    }

    /**
     * Charge l'état "liké ou non" pour une liste de posts visibles.
     * À appeler quand la liste de posts change (ex: changement d'onglet).
     */
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
        // Les Flow callbackFlow écoutent en temps réel en permanence.
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