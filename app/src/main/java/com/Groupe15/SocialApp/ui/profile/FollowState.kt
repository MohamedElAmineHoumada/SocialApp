package com.Groupe15.SocialApp.ui.profile

sealed class FollowState {
    object Idle : FollowState()
    object Loading : FollowState()
    data class IsFollowing(val isFollowing: Boolean) : FollowState()
    object FollowSuccess : FollowState()
    object UnfollowSuccess : FollowState()
    data class Error(val message: String) : FollowState()
}