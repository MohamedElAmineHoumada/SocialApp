package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.FollowRequest
import com.Groupe15.SocialApp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gère les actions de suivi : follow, unfollow, vérification du statut,
 * liste des abonnés/abonnements, demandes en attente (comptes privés),
 * et utilitaires de synchronisation des compteurs.
 *
 * Schéma Firestore (sous-collections) :
 * - users/{uid}                    → document utilisateur (modèle User)
 * - users/{uid}/following/{targetUid} → { uid, status: "accepted"|"pending" }
 * - users/{uid}/followers/{targetUid} → { uid }
 *
 * Les comptes privés (isPrivate = true) reçoivent des demandes "pending"
 * dans la sous-collection following de l'initiateur, jusqu'à acceptation.
 */
@Singleton
class FollowRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")

    val currentUid get() = auth.currentUser?.uid

    // ─────────────────────────────────────────────
    // Follow / Unfollow
    // ─────────────────────────────────────────────

    /**
     * Suit [targetUid].
     * - Compte public  → suivi immédiat (status = "accepted"), compteurs mis à jour.
     * - Compte privé   → demande en attente (status = "pending"), sans toucher aux compteurs.
     */
    suspend fun followUser(targetUid: String): Result<Unit> {
        return try {
            val currentUid = currentUid ?: return Result.failure(Exception("Non connecté"))

            val targetUser = usersCollection.document(targetUid).get().await()
                .toObject(User::class.java)
                ?: return Result.failure(Exception("Utilisateur introuvable"))

            val status = if (targetUser.isPrivate) "pending" else "accepted"

            val batch = firestore.batch()

            // Entrée dans following de l'utilisateur courant
            val followingRef = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)
            batch.set(followingRef, mapOf("uid" to targetUid, "status" to status))

            if (status == "accepted") {
                // Entrée dans followers de la cible
                val followerRef = usersCollection
                    .document(targetUid)
                    .collection("followers")
                    .document(currentUid)
                batch.set(followerRef, mapOf("uid" to currentUid))

                // Mise à jour des compteurs
                batch.update(usersCollection.document(currentUid), "followingCount", FieldValue.increment(1))
                batch.update(usersCollection.document(targetUid), "followersCount", FieldValue.increment(1))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Annule un suivi existant ou retire une demande en attente.
     * Utilise une transaction pour éviter la désynchronisation des compteurs.
     */
    suspend fun unfollowUser(targetUid: String): Result<Unit> {
        return try {
            val currentUid = currentUid ?: return Result.failure(Exception("Non connecté"))

            val followingRef = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)

            val followerRef = usersCollection
                .document(targetUid)
                .collection("followers")
                .document(currentUid)

            firestore.runTransaction { transaction ->
                val followingSnap = transaction.get(followingRef)

                if (followingSnap.exists()) {
                    val wasAccepted = followingSnap.getString("status") == "accepted"

                    transaction.delete(followingRef)

                    if (wasAccepted) {
                        transaction.delete(followerRef)
                        transaction.update(usersCollection.document(currentUid), "followingCount", FieldValue.increment(-1))
                        transaction.update(usersCollection.document(targetUid), "followersCount", FieldValue.increment(-1))
                    }
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────
    // Demandes en attente (comptes privés)
    // ─────────────────────────────────────────────

    /**
     * Retourne les demandes de suivi en attente reçues par l'utilisateur courant.
     * Pertinent uniquement si son compte est privé (isPrivate = true).
     */
    suspend fun getPendingFollowRequests(): List<FollowRequest> {
        val uid = currentUid ?: return emptyList()

        return try {
            // On cherche tous les utilisateurs dont la sous-collection "following"
            // contient un document {uid} avec status = "pending"
            // → En pratique : on parcourt les followers potentiels via une requête de groupe
            val pendingSnap = firestore
                .collectionGroup("following")
                .whereEqualTo("uid", uid)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            pendingSnap.documents.mapNotNull { doc ->
                // L'id du parent du parent = uid de l'initiateur
                val followerId = doc.reference.parent.parent?.id ?: return@mapNotNull null

                val follower = usersCollection.document(followerId).get().await()
                    .toObject(User::class.java) ?: return@mapNotNull null

                FollowRequest(
                    id = doc.id,
                    name = follower.displayName.ifBlank { follower.username },
                    role = follower.role,
                    mutualFriends = 0,
                    avatarUrl = follower.profileImageUrl.ifBlank { null }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Accepte une demande de suivi en attente.
     * [followerId] = uid de la personne qui a envoyé la demande.
     */
    suspend fun acceptFollowRequest(followerId: String): Result<Unit> {
        return try {
            val currentUid = currentUid ?: return Result.failure(Exception("Non connecté"))

            val followingRef = usersCollection
                .document(followerId)
                .collection("following")
                .document(currentUid)

            firestore.runTransaction { transaction ->
                val snap = transaction.get(followingRef)
                if (snap.exists() && snap.getString("status") == "pending") {
                    // Passe le statut à "accepted"
                    transaction.update(followingRef, "status", "accepted")

                    // Ajoute dans les followers de l'utilisateur courant
                    val followerRef = usersCollection
                        .document(currentUid)
                        .collection("followers")
                        .document(followerId)
                    transaction.set(followerRef, mapOf("uid" to followerId))

                    // Met à jour les compteurs
                    transaction.update(usersCollection.document(followerId), "followingCount", FieldValue.increment(1))
                    transaction.update(usersCollection.document(currentUid), "followersCount", FieldValue.increment(1))
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refuse / supprime une demande de suivi en attente.
     * [followerId] = uid de la personne qui a envoyé la demande.
     */
    suspend fun rejectFollowRequest(followerId: String): Result<Unit> {
        return try {
            val currentUid = currentUid ?: return Result.failure(Exception("Non connecté"))

            usersCollection
                .document(followerId)
                .collection("following")
                .document(currentUid)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────
    // Lecture du statut / listes
    // ─────────────────────────────────────────────

    /**
     * Indique si l'utilisateur courant suit [targetUid] (status = "accepted").
     * Une demande "pending" est considérée comme "pas encore suivi".
     */
    suspend fun isFollowing(targetUid: String): Boolean {
        return try {
            val currentUid = currentUid ?: return false
            val doc = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)
                .get()
                .await()
            doc.exists() && doc.getString("status") == "accepted"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Indique si l'utilisateur courant a une demande en attente vers [targetUid].
     */
    suspend fun isPendingFollow(targetUid: String): Boolean {
        return try {
            val currentUid = currentUid ?: return false
            val doc = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)
                .get()
                .await()
            doc.exists() && doc.getString("status") == "pending"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowers(uid: String): List<String> {
        return try {
            usersCollection.document(uid).collection("followers").get().await()
                .documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowing(uid: String): List<String> {
        return try {
            usersCollection.document(uid).collection("following")
                .whereEqualTo("status", "accepted")
                .get().await()
                .documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowingUsers(uid: String): List<User> {
        return try {
            val followingIds = getFollowing(uid)
            if (followingIds.isEmpty()) return emptyList()

            followingIds.mapNotNull { id ->
                usersCollection.document(id).get().await()
                    .toObject(User::class.java)?.copy(id = id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUsersByIds(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()
        return try {
            ids.chunked(10).flatMap { chunk ->
                usersCollection.whereIn(FieldPath.documentId(), chunk).get().await()
                    .documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // Gestion des abonnés
    // ─────────────────────────────────────────────

    /**
     * Retire un abonné : [targetUid] arrête automatiquement de suivre l'utilisateur courant.
     */
    suspend fun removeFollower(targetUid: String): Result<Unit> {
        return try {
            val currentUid = currentUid ?: return Result.failure(Exception("Non connecté"))

            val followerRef = usersCollection
                .document(currentUid)
                .collection("followers")
                .document(targetUid)

            val followingRef = usersCollection
                .document(targetUid)
                .collection("following")
                .document(currentUid)

            firestore.runTransaction { transaction ->
                val followerSnap = transaction.get(followerRef)
                if (followerSnap.exists()) {
                    transaction.delete(followerRef)
                    transaction.delete(followingRef)
                    transaction.update(usersCollection.document(currentUid), "followersCount", FieldValue.increment(-1))
                    transaction.update(usersCollection.document(targetUid), "followingCount", FieldValue.increment(-1))
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────

    /**
     * Recalcule followersCount / followingCount à partir du nombre réel
     * de documents dans les sous-collections. Corrige les écarts éventuels.
     */
    suspend fun resyncCounts(uid: String): Result<Unit> {
        return try {
            val followersCount = usersCollection.document(uid).collection("followers").get().await().size()
            val followingCount = usersCollection.document(uid).collection("following")
                .whereEqualTo("status", "accepted").get().await().size()

            usersCollection.document(uid).update(
                mapOf(
                    "followersCount" to followersCount,
                    "followingCount" to followingCount
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}