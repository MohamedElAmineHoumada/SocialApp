package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.Call
import com.Groupe15.SocialApp.models.CallStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import android.util.Log
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val callsCollection = firestore.collection("calls")

    fun observeCall(callId: String): Flow<Call?> = callbackFlow {
        val listener = callsCollection.document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CallRepository", "Error observing call $callId", error)
                    return@addSnapshotListener
                }
                val call = snapshot?.toObject(Call::class.java)
                trySend(call)
            }
        awaitClose { listener.remove() }
    }

    fun observeIncomingCalls(userId: String): Flow<Call?> = callbackFlow {
        // Uniquement les appels récents (moins de 1 minute) pour éviter les fantômes au démarrage
        val oneMinuteAgo = Timestamp(Date(System.currentTimeMillis() - 60 * 1000))
        
        val listener = callsCollection
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", CallStatus.RINGING.name)
            .whereGreaterThan("timestamp", oneMinuteAgo)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CallRepository", "Error observing incoming calls", error)
                    return@addSnapshotListener
                }
                val call = snapshot?.documents?.firstOrNull()?.toObject(Call::class.java)
                trySend(call)
            }
        awaitClose { listener.remove() }
    }

    suspend fun startCall(call: Call): String {
        val docRef = callsCollection.document()
        val newCall = call.copy(callId = docRef.id, status = CallStatus.RINGING.name, timestamp = Timestamp.now())
        docRef.set(newCall).await()
        Log.d("CallRepository", "Call started with ID: ${docRef.id}")
        return docRef.id
    }

    suspend fun updateCallStatus(callId: String, status: CallStatus) {
        Log.d("CallRepository", "Updating call $callId status to ${status.name}")
        callsCollection.document(callId).update("status", status.name).await()
    }

    suspend fun endCall(callId: String) {
        updateCallStatus(callId, CallStatus.ENDED)
    }

    suspend fun declineCall(callId: String) {
        updateCallStatus(callId, CallStatus.DECLINED)
    }

    suspend fun acceptCall(callId: String) {
        updateCallStatus(callId, CallStatus.CONNECTED)
    }

    fun getCallHistory(userId: String): Flow<List<Call>> = callbackFlow {
        val listener = callsCollection
            .where(Filter.or(
                Filter.equalTo("callerId", userId),
                Filter.equalTo("receiverId", userId)
            ))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CallRepository", "Error fetching call history", error)
                    return@addSnapshotListener
                }
                val allCalls = snapshot?.documents?.mapNotNull { it.toObject(Call::class.java) } ?: emptyList()
                val history = allCalls.filter { 
                    it.status == CallStatus.ENDED.name || 
                    it.status == CallStatus.DECLINED.name || 
                    it.status == CallStatus.MISSED.name
                }.sortedByDescending { it.timestamp }
                
                Log.d("CallRepository", "Fetched ${history.size} history items for $userId")
                trySend(history)
            }
        awaitClose { listener.remove() }
    }
}
