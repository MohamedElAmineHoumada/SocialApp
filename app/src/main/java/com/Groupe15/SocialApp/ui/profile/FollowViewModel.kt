package com.Groupe15.SocialApp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.repository.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowViewModel @Inject constructor(
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _followState = MutableStateFlow<FollowState>(FollowState.Idle)
    val followState: StateFlow<FollowState> = _followState

    fun checkIsFollowing(targetUid: String) {
        viewModelScope.launch {
            _followState.value = FollowState.Loading
            val isFollowing = followRepository.isFollowing(targetUid)
            _followState.value = FollowState.IsFollowing(isFollowing)
        }
    }

    fun followUser(targetUid: String) {
        viewModelScope.launch {
            _followState.value = FollowState.Loading
            val result = followRepository.followUser(targetUid)
            _followState.value = if (result.isSuccess) FollowState.FollowSuccess
            else FollowState.Error(result.exceptionOrNull()?.message ?: "Erreur")
        }
    }

    fun unfollowUser(targetUid: String) {
        viewModelScope.launch {
            _followState.value = FollowState.Loading
            val result = followRepository.unfollowUser(targetUid)
            _followState.value = if (result.isSuccess) FollowState.UnfollowSuccess
            else FollowState.Error(result.exceptionOrNull()?.message ?: "Erreur")
        }
    }
}