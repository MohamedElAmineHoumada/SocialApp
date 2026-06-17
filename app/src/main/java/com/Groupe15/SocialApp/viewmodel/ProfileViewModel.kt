package com.Groupe15.SocialApp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.AuthRepository
import com.Groupe15.SocialApp.repository.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(true)
    val isOwnProfile: StateFlow<Boolean> = _isOwnProfile.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    fun loadProfile(targetUid: String, currentUid: String) {
        _isOwnProfile.value = targetUid == currentUid || targetUid.isEmpty()
        viewModelScope.launch {
            if (targetUid.isEmpty() || targetUid == currentUid) {
                authRepository.getCurrentUser().collect { user ->
                    _currentUser.value = user
                }
            } else {
                authRepository.getUserById(targetUid).collect { user ->
                    _currentUser.value = user
                }
            }
        }
    }

    fun toggleFollow(targetUid: String) {
        viewModelScope.launch {
            if (_isFollowing.value) {
                followRepository.unfollowUser(targetUid)
            } else {
                followRepository.followUser(targetUid)
            }
            _isFollowing.value = !_isFollowing.value
        }
    }
}