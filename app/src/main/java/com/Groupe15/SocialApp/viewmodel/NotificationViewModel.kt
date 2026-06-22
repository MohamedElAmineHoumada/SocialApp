package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Notification
import com.Groupe15.SocialApp.repository.FollowRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    private val _followingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followingMap = _followingMap.asStateFlow()

    private val _followedByMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followedByMap = _followedByMap.asStateFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        val uid = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val list = snapshot?.documents?.mapNotNull { it.toObject(Notification::class.java) } ?: emptyList()
                _notifications.value = list
                _unreadCount.value = list.count { !it.isRead }

                // Charger les relations follow pour les types concernés
                val userIds = list.filter { it.type == "follow_request" || it.type == "follow_accept" }
                    .map { it.fromUserId }
                    .distinct()
                if (userIds.isNotEmpty()) {
                    loadFollowRelations(userIds)
                }
            }
    }

    private fun loadFollowRelations(userIds: List<String>) {
        viewModelScope.launch {
            val newFollowingMap = _followingMap.value.toMutableMap()
            val newFollowedByMap = _followedByMap.value.toMutableMap()
            userIds.forEach { targetUid ->
                newFollowingMap[targetUid] = followRepository.isFollowing(targetUid)
                newFollowedByMap[targetUid] = followRepository.isFollowedBy(targetUid)
            }
            _followingMap.value = newFollowingMap
            _followedByMap.value = newFollowedByMap
        }
    }

    fun toggleFollow(targetUid: String) {
        viewModelScope.launch {
            val isCurrentlyFollowing = _followingMap.value[targetUid] ?: false
            val result = if (isCurrentlyFollowing) {
                followRepository.unfollowUser(targetUid)
            } else {
                followRepository.followUser(targetUid)
            }
            if (result.isSuccess) {
                val newFollowingMap = _followingMap.value.toMutableMap()
                newFollowingMap[targetUid] = !isCurrentlyFollowing
                _followingMap.value = newFollowingMap
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("notifications")
                    .document(notificationId)
                    .update("isRead", true)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearNotifications() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("notifications")
                    .get()
                    .await()
                
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
