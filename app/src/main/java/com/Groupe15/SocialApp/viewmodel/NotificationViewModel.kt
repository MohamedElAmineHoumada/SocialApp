package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Notification
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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

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
