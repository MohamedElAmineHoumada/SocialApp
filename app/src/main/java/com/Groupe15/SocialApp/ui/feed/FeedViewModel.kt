package com.Groupe15.SocialApp.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.data.model.Post
import com.Groupe15.SocialApp.ui.auth.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    // On transforme le Flow du Repository en LiveData pour le Fragment
    val posts: LiveData<List<Post>> = postRepository.getLivePostsFlux().asLiveData()

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.toggleLike(postId)
        }
    }
}
