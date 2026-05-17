package com.Groupe15.SocialApp.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private var listenerRegistration = firestore
        .collection("posts")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(50)
        .addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val postList = snapshot?.toObjects(Post::class.java) ?: emptyList()
            _posts.postValue(postList)
        }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            // like avec Soukina
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration.remove()
    }
}