package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.SuggestionUser
import com.Groupe15.SocialApp.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gère la récupération des suggestions d'amis ("People You May Know").
 *
 * Schéma Firestore utilisé :
 * - "users"   : un document par utilisateur (id = uid), champs = modèle User
 * - "follows" : un document par relation de suivi
 *      id du document = "{followerId}_{followingId}"
 *      champs : followerId, followingId, status ("accepted" | "pending"), timestamp
 */
@Singleton
class NetworkRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection = firestore.collection("users")
    private val followsCollection = firestore.collection("follows")

    /**
     * Retourne la liste des suggestions pour [currentUserId].
     *
     * Étapes :
     * 1. On charge tous les utilisateurs sauf moi.
     * 2. On exclut ceux que je suis déjà ou à qui j'ai déjà envoyé une demande.
     * 3. Pour chaque candidat restant, on calcule le nombre d'amis en commun.
     *
     * Note pour plus tard : avec beaucoup d'utilisateurs, charger "tous les users"
     * ne sera plus viable. On pourra alors paginer (limit/startAfter) ou déplacer
     * ce calcul dans une Cloud Function avec des compteurs pré-calculés.
     */
    suspend fun getSuggestedUsers(currentUserId: String): List<SuggestionUser> {
        return try {
            val allUsers = usersCollection.get().await().documents
                .mapNotNull { doc -> doc.toObject(User::class.java)?.copy(id = doc.id) }
                .filter { it.id != currentUserId }

            val myFollowingIds = followsCollection
                .whereEqualTo("followerId", currentUserId)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("followingId") }
                .toSet()

            val candidates = allUsers.filterNot { myFollowingIds.contains(it.id) }

            candidates.map { candidate ->
                SuggestionUser(
                    id = candidate.id,
                    name = candidate.displayName.ifBlank { candidate.username },
                    role = candidate.role,
                    mutualFriends = computeMutualFriendsCount(candidate.id, myFollowingIds),
                    avatarUrl = candidate.profileImageUrl.ifBlank { null },
                    isOnline = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Amis en commun = personnes que je suis ET qui suivent aussi [candidateId].
     */
    private suspend fun computeMutualFriendsCount(
        candidateId: String,
        myFollowingIds: Set<String>
    ): Int {
        if (myFollowingIds.isEmpty()) return 0

        val candidateFollowerIds = followsCollection
            .whereEqualTo("followingId", candidateId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followerId") }
            .toSet()

        return myFollowingIds.intersect(candidateFollowerIds).size
    }
}