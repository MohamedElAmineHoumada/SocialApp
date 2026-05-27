package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Comment
import com.Groupe15.SocialApp.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.Groupe15.SocialApp.repository.CommentRepository
import com.Groupe15.SocialApp.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val postRepository   : PostRepository,
    private val commentRepository: CommentRepository
) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    //val posts: LiveData<List<Post>> = _posts
    val posts: LiveData<List<Post>> = postRepository.getLivePosts().asLiveData()

    private val _selectedPostId = MutableStateFlow<String?>(null)

    val comments: LiveData<List<Comment>> = _selectedPostId
        .flatMapLatest { postId ->
            if (postId != null) commentRepository.getComments(postId)
            else flowOf(emptyList())
        }
        .asLiveData()

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
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                feedRepository.getFeedPosts().collect { posts ->
                    _posts.value = posts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }



    fun sharePost(post: Post) { }
}