// app/src/main/java/com/Groupe15/SocialApp/viewmodel/FeedViewModel.kt

package com.Groupe15.SocialApp.viewmodel

import com.Groupe15.SocialApp.repository.CommentRepository
import com.Groupe15.SocialApp.repository.PostRepository

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Comment  // ← models
import com.Groupe15.SocialApp.models.Post     // ← models
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository   : PostRepository,
    private val commentRepository: CommentRepository
) : ViewModel() {

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
       /* viewModelScope.launch {
            postRepository.toggleLike(postId)
        }*/
        viewModelScope.launch {
            val result = postRepository.toggleLike(postId)
            result.onFailure { e ->
                android.util.Log.e("LIKE", "Erreur toggleLike: ${e.message}")
            }
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            commentRepository.addComment(postId, text)
        }
    }
    fun createPost(content: String) {
        viewModelScope.launch {
            postRepository.createPost(content)
        }
    }
}