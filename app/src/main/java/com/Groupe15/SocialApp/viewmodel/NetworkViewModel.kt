package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.FollowRequest
import com.Groupe15.SocialApp.models.SuggestionUser
import com.Groupe15.SocialApp.repository.FollowRepository
import com.Groupe15.SocialApp.repository.NetworkRepository
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _followRequests = MutableStateFlow<List<FollowRequest>>(emptyList())
    val followRequests: StateFlow<List<FollowRequest>> = _followRequests.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SuggestionUser>>(emptyList())
    val suggestions: StateFlow<List<SuggestionUser>> = _suggestions.asStateFlow()

    init {
        loadFollowRequests()
        loadSuggestions()
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

    /** À appeler quand on clique sur "Follow" sur une carte de suggestion. */
    fun onFollowUser(targetUid: String) {
        viewModelScope.launch {
            val result = followRepository.followUser(targetUid)
            if (result.isSuccess) {
                // On retire la personne suivie de la liste affichée
                _suggestions.value = _suggestions.value.filterNot { it.id == targetUid }
            }
        }
    }

    /** À appeler quand on accepte une demande de suivi. */
    fun onAcceptRequest(requestId: String) {
        viewModelScope.launch {
            followRepository.acceptFollowRequest(requestId)
            _followRequests.value = _followRequests.value.filterNot { it.id == requestId }
        }
    }

    /** À appeler quand on refuse une demande de suivi. */
    fun onDeclineRequest(requestId: String) {
        viewModelScope.launch {
            followRepository.rejectFollowRequest(requestId)
            _followRequests.value = _followRequests.value.filterNot { it.id == requestId }
        }
    }
}