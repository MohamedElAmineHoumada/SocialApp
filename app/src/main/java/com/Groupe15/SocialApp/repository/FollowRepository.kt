package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.FollowRequest
import com.Groupe15.SocialApp.models.Notification
import com.Groupe15.SocialApp.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

            // Current user data for notification
            val currentUserDoc = usersCollection.document(currentUid).get().await()
            val currentUser = currentUserDoc.toObject(User::class.java)

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

                // Notification for follow (accepted)
                val notificationRef = usersCollection.document(targetUid).collection("notifications").document()
                val notification = Notification(
                    id = notificationRef.id,
                    type = "follow_accept",
                    fromUserId = currentUid,
                    fromUserName = currentUser?.displayName ?: currentUser?.username ?: "Quelqu'un",
                    fromUserAvatar = currentUser?.profileImageUrl ?: "",
                    content = "${currentUser?.displayName ?: currentUser?.username} a commencé à vous suivre",
                    timestamp = Timestamp.now(),
                    targetId = currentUid
                )
                batch.set(notificationRef, notification)

                // Trigger Push Notification
                sendPushNotification(targetUid, "Nouvel abonné", "${currentUser?.displayName ?: currentUser?.username} a commencé à vous suivre", currentUid, "follow")
            } else {
                // Notification for follow request (pending)
                val notificationRef = usersCollection.document(targetUid).collection("notifications").document()
                val notification = Notification(
                    id = notificationRef.id,
                    type = "follow_request",
                    fromUserId = currentUid,
                    fromUserName = currentUser?.displayName ?: currentUser?.username ?: "Quelqu'un",
                    fromUserAvatar = currentUser?.profileImageUrl ?: "",
                    content = "${currentUser?.displayName ?: currentUser?.username} vous demande de vous suivre",
                    timestamp = Timestamp.now(),
                    targetId = currentUid
                )
                batch.set(notificationRef, notification)

                // Trigger Push Notification
                sendPushNotification(targetUid, "Demande de suivi", "${currentUser?.displayName ?: currentUser?.username} souhaite vous suivre", currentUid, "follow_request")
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendPushNotification(targetUid: String, title: String, body: String, senderId: String, type: String) {
        try {
            val userDoc = usersCollection.document(targetUid).get().await()
            val fcmToken = userDoc.getString("fcmToken")

            if (!fcmToken.isNullOrBlank()) {
                val pushData = hashMapOf(
                    "to" to fcmToken,
                    "title" to title,
                    "body" to body,
                    "data" to mapOf(
                        "senderId" to senderId,
                        "type" to type
                    ),
                    "timestamp" to Timestamp.now()
                )
                firestore.collection("outgoing_notifications").add(pushData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

            // Get current user info for notification
            val currentUserDoc = usersCollection.document(currentUid).get().await()
            val currentUser = currentUserDoc.toObject(User::class.java)

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

                    // Notification for acceptance
                    val notificationRef = usersCollection.document(followerId).collection("notifications").document()
                    val notification = Notification(
                        id = notificationRef.id,
                        type = "follow_accept",
                        fromUserId = currentUid,
                        fromUserName = currentUser?.displayName ?: currentUser?.username ?: "Quelqu'un",
                        fromUserAvatar = currentUser?.profileImageUrl ?: "",
                        content = "${currentUser?.displayName ?: currentUser?.username} a accepté votre demande de suivi",
                        timestamp = Timestamp.now(),
                        targetId = currentUid
                    )
                    transaction.set(notificationRef, notification)

                    // Trigger Push Notification
                    CoroutineScope(Dispatchers.IO).launch {
                        sendPushNotification(followerId, "Demande acceptée", "${currentUser?.displayName ?: currentUser?.username} a accepté votre demande", currentUid, "follow_accept")
                    }
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
     * de documents VALIDES (dont l'utilisateur cible existe encore) dans les
     * sous-collections. Corrige les écarts éventuels.
     *
     * IMPORTANT : on ne se contente pas de compter les documents bruts
     * (followersSnapshot.size()), car des relations "fantômes" peuvent exister
     * si un compte a été supprimé ou n'a jamais été créé correctement
     * (ex. comptes de test). Un document fantôme ferait gonfler le compteur
     * sans jamais apparaître dans la liste affichée (getUsersByIds l'ignore
     * silencieusement car le document users/{uid} n'existe plus).
     *
     * On vérifie donc l'existence réelle de chaque utilisateur référencé,
     * et on supprime au passage les références fantômes pour que ce problème
     * ne se reproduise plus à l'avenir (nettoyage définitif, pas juste un
     * correctif d'affichage).
     */
    suspend fun resyncCounts(uid: String): Result<Unit> {
        return try {
            if (uid.isEmpty()) return Result.success(Unit)

            // ✅ Garde de sécurité : si le profil cible n'existe plus du tout
            // (compte supprimé), on ne tente même pas de le mettre à jour —
            // ça évite l'erreur Firestore "NOT_FOUND: No document to update"
            // qui bloquait le chargement du profil indéfiniment.
            val targetUserDoc = usersCollection.document(uid).get().await()
            if (!targetUserDoc.exists()) {
                return Result.success(Unit)
            }

            val followersSnapshot = usersCollection.document(uid).collection("followers").get().await()
            val followingSnapshot = usersCollection.document(uid).collection("following")
                .whereEqualTo("status", "accepted").get().await()

            // Vérifie pour chaque relation que l'utilisateur référencé existe encore.
            // Supprime la relation si l'utilisateur n'existe plus (nettoyage des fantômes).
            var validFollowersCount = 0
            for (doc in followersSnapshot.documents) {
                val exists = try {
                    usersCollection.document(doc.id).get().await().exists()
                } catch (e: Exception) {
                    false
                }
                if (exists) {
                    validFollowersCount++
                } else {
                    // Relation fantôme : on la supprime définitivement (best-effort,
                    // ne doit jamais faire échouer toute la resynchronisation)
                    try { doc.reference.delete().await() } catch (e: Exception) { /* ignorer */ }
                }
            }

            var validFollowingCount = 0
            for (doc in followingSnapshot.documents) {
                val exists = try {
                    usersCollection.document(doc.id).get().await().exists()
                } catch (e: Exception) {
                    false
                }
                if (exists) {
                    validFollowingCount++
                } else {
                    try { doc.reference.delete().await() } catch (e: Exception) { /* ignorer */ }
                }
            }

            // ✅ set(..., merge = true) au lieu de update() : ne plante jamais même
            // si le document a été supprimé entre le check d'existence ci-dessus
            // et cet appel (cas rare mais possible en cas de suppression concurrente).
            usersCollection.document(uid).set(
                mapOf(
                    "followersCount" to validFollowersCount,
                    "followingCount" to validFollowingCount
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            // On ne propage jamais l'erreur : une resynchronisation qui échoue
            // ne doit pas empêcher l'affichage du profil avec les valeurs existantes.
            Result.success(Unit)
        }
    }
}