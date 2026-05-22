// app/src/main/java/com/Groupe15/SocialApp/viewmodel/FeedViewModel.kt

package com.Groupe15.SocialApp.viewmodel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    // Flow Firestore converti en LiveData — le Fragment observe ça
    val posts: LiveData<List<Post>> = postRepository.getLivePosts().asLiveData()

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.toggleLike(postId)
        }
    }
}