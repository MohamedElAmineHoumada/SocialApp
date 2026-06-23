package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.FollowRepository
import com.Groupe15.SocialApp.ui.profile.FollowState
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

    private val _followersList = MutableStateFlow<List<User>>(emptyList())
    val followersList: StateFlow<List<User>> = _followersList

    private val _followingList = MutableStateFlow<List<User>>(emptyList())
    val followingList: StateFlow<List<User>> = _followingList

    private val _isLoadingList = MutableStateFlow(false)
    val isLoadingList: StateFlow<Boolean> = _isLoadingList

    // uid du profil dont on a chargé la liste, pour pouvoir resync ses compteurs après une action
    private var lastLoadedProfileUid: String = ""

    fun checkIsFollowing(targetUid: String) {
        viewModelScope.launch {
            _followState.value = FollowState.Loading
            val isFollowing = followRepository.isFollowing(targetUid)
            val isFollowedBy = followRepository.isFollowedBy(targetUid)
            _followState.value = FollowState.IsFollowing(isFollowing, isFollowedBy)
        }
    }

    fun followUser(targetUid: String) {
        viewModelScope.launch {
            _followState.value = FollowState.Loading
            val result = followRepository.followUser(targetUid)
            if (result.isSuccess) {
                val nowFollowing = followRepository.isFollowing(targetUid)
                _followState.value = if (nowFollowing) FollowState.FollowSuccess
                else FollowState.Error("Erreur de synchronisation")
            } else {
                _followState.value = FollowState.Error(result.exceptionOrNull()?.message ?: "Erreur")
            }
        }
    }

    fun unfollowUser(targetUid: String) {
        viewModelScope.launch {
            _followState.value = FollowState.Loading
            val result = followRepository.unfollowUser(targetUid)
            if (result.isSuccess) {
                val stillFollowing = followRepository.isFollowing(targetUid)
                _followState.value = if (!stillFollowing) FollowState.UnfollowSuccess
                else FollowState.Error("Erreur de synchronisation")
            } else {
                _followState.value = FollowState.Error(result.exceptionOrNull()?.message ?: "Erreur")
            }
        }
    }

    fun loadFollowers(uid: String) {
        lastLoadedProfileUid = uid
        viewModelScope.launch {
            _isLoadingList.value = true
            val ids = followRepository.getFollowers(uid)
            _followersList.value = followRepository.getUsersByIds(ids)
            _isLoadingList.value = false
        }
    }

    fun loadFollowing(uid: String) {
        lastLoadedProfileUid = uid
        viewModelScope.launch {
            _isLoadingList.value = true
            val ids = followRepository.getFollowing(uid)
            _followingList.value = followRepository.getUsersByIds(ids)
            _isLoadingList.value = false
        }
    }

    // Appelé depuis la liste "Abonnements" pour se désabonner d'un utilisateur
    fun unfollowFromList(targetUid: String) {
        viewModelScope.launch {
            val result = followRepository.unfollowUser(targetUid)
            if (result.isSuccess) {
                _followingList.value = _followingList.value.filter { it.id != targetUid }
                if (lastLoadedProfileUid.isNotEmpty()) {
                    followRepository.resyncCounts(lastLoadedProfileUid)
                }
            }
        }
    }

    // Appelé depuis la liste "Abonnés" pour retirer un abonné
    fun removeFollowerFromList(targetUid: String) {
        viewModelScope.launch {
            val result = followRepository.removeFollower(targetUid)
            if (result.isSuccess) {
                _followersList.value = _followersList.value.filter { it.id != targetUid }
                if (lastLoadedProfileUid.isNotEmpty()) {
                    followRepository.resyncCounts(lastLoadedProfileUid)
                }
            }
        }
    }

    // Recalcule les compteurs depuis Firestore (corrige les écarts comme 10 vs 3)
    fun resyncCounts(uid: String) {
        viewModelScope.launch {
            followRepository.resyncCounts(uid)
        }
    }
}