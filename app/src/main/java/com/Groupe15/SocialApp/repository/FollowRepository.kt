package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.FollowRequest
import com.Groupe15.SocialApp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gère les actions de suivi : follow, unfollow, vérification du statut,
 * liste des comptes suivis, et demandes en attente pour les comptes privés.
 *
 * Schéma Firestore :
 * - "users"   : un document par utilisateur (id = uid), champs = modèle User
 * - "follows" : un document par relation, id = "{followerId}_{followingId}"
 *      champs : followerId, followingId, status ("accepted" | "pending"), timestamp
 *
 * L'utilisateur courant est lu directement via FirebaseAuth, donc les méthodes
 * publiques n'ont besoin que de targetUserId.
 */
@Singleton
class FollowRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val usersCollection = firestore.collection("users")
    private val followsCollection = firestore.collection("follows")

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun followDocId(followerId: String, followingId: String) = "${followerId}_$followingId"

    /**
     * Suit [targetUserId].
     * - Compte public -> suivi immédiat (status = "accepted"), compteurs mis à jour.
     * - Compte privé  -> demande en attente (status = "pending").
     */
    suspend fun followUser(targetUserId: String): Result<Unit> {
        return try {
            val uid = currentUserId ?: return Result.failure(Exception("Utilisateur non connecté"))

            val targetUser = usersCollection.document(targetUserId).get().await()
                .toObject(User::class.java) ?: return Result.failure(Exception("Utilisateur introuvable"))

            val status = if (targetUser.isPrivate) "pending" else "accepted"

            followsCollection.document(followDocId(uid, targetUserId)).set(
                mapOf(
                    "followerId" to uid,
                    "followingId" to targetUserId,
                    "status" to status,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            if (status == "accepted") {
                incrementCounters(uid, targetUserId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Annule un suivi existant, ou retire une demande en attente.
     */
    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        return try {
            val uid = currentUserId ?: return Result.failure(Exception("Utilisateur non connecté"))

            val docRef = followsCollection.document(followDocId(uid, targetUserId))
            val wasAccepted = docRef.get().await().getString("status") == "accepted"

            docRef.delete().await()

            if (wasAccepted) {
                decrementCounters(uid, targetUserId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Indique si l'utilisateur courant suit déjà [targetUserId] (status = "accepted").
     * Une demande "pending" est considérée comme "pas encore suivi".
     */
    suspend fun isFollowing(targetUserId: String): Boolean {
        val uid = currentUserId ?: return false
        val doc = followsCollection.document(followDocId(uid, targetUserId)).get().await()
        return doc.exists() && doc.getString("status") == "accepted"
    }

    /**
     * Liste des utilisateurs suivis (status = "accepted") par [uid].
     */
    suspend fun getFollowingUsers(uid: String): List<User> {
        val followingIds = followsCollection
            .whereEqualTo("followerId", uid)
            .whereEqualTo("status", "accepted")
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followingId") }

        if (followingIds.isEmpty()) return emptyList()

        return followingIds.mapNotNull { id ->
            usersCollection.document(id).get().await()
                .toObject(User::class.java)?.copy(id = id)
        }
    }

    /**
     * Demandes de suivi en attente reçues par l'utilisateur courant
     * (pertinent si son compte est privé).
     */
    suspend fun getPendingFollowRequests(): List<FollowRequest> {
        val uid = currentUserId ?: return emptyList()

        val pendingDocs = followsCollection
            .whereEqualTo("followingId", uid)
            .whereEqualTo("status", "pending")
            .get()
            .await()
            .documents

        return pendingDocs.mapNotNull { doc ->
            val followerId = doc.getString("followerId") ?: return@mapNotNull null
            val follower = usersCollection.document(followerId).get().await()
                .toObject(User::class.java) ?: return@mapNotNull null

            FollowRequest(
                id = doc.id,
                name = follower.displayName.ifBlank { follower.username },
                role = follower.role,
                mutualFriends = 0, // calcul possible plus tard, comme dans NetworkRepository
                avatarUrl = follower.profileImageUrl.ifBlank { null }
            )
        }
    }

    /**
     * Accepte une demande de suivi en attente (id = id du document "follows").
     */
    suspend fun acceptFollowRequest(requestDocId: String) {
        val doc = followsCollection.document(requestDocId).get().await()
        val followerId = doc.getString("followerId") ?: return
        val followingId = doc.getString("followingId") ?: return

        followsCollection.document(requestDocId).update("status", "accepted").await()
        incrementCounters(followerId, followingId)
    }

    /**
     * Refuse / supprime une demande de suivi en attente.
     */
    suspend fun rejectFollowRequest(requestDocId: String) {
        followsCollection.document(requestDocId).delete().await()
    }

    private suspend fun incrementCounters(followerId: String, followingId: String) {
        usersCollection.document(followerId)
            .set(mapOf("followingCount" to FieldValue.increment(1)), SetOptions.merge()).await()
        usersCollection.document(followingId)
            .set(mapOf("followersCount" to FieldValue.increment(1)), SetOptions.merge()).await()
    }

    private suspend fun decrementCounters(followerId: String, followingId: String) {
        usersCollection.document(followerId)
            .set(mapOf("followingCount" to FieldValue.increment(-1)), SetOptions.merge()).await()
        usersCollection.document(followingId)
            .set(mapOf("followersCount" to FieldValue.increment(-1)), SetOptions.merge()).await()
    }
}