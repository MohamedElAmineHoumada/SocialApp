package com.Groupe15.SocialApp.repository


import com.Groupe15.SocialApp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")

    val currentUid get() = auth.currentUser?.uid

    suspend fun followUser(targetUid: String): Result<Unit> {
        return try {
            val currentUid = currentUid ?: return Result.failure(Exception("Non connecté"))

            val batch = firestore.batch()

            val followingRef = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)
            batch.set(followingRef, mapOf("uid" to targetUid))

            val followerRef = usersCollection
                .document(targetUid)
                .collection("followers")
                .document(currentUid)
            batch.set(followerRef, mapOf("uid" to currentUid))

            val currentUserRef = usersCollection.document(currentUid)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(1))

            val targetUserRef = usersCollection.document(targetUid)
            batch.update(targetUserRef, "followersCount", FieldValue.increment(1))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

            // Transaction : on vérifie que le lien existe vraiment avant de décrémenter,
            // ce qui évite que le compteur se désynchronise de la réalité.
            firestore.runTransaction { transaction ->
                val followingSnap = transaction.get(followingRef)

                if (followingSnap.exists()) {
                    transaction.delete(followingRef)
                    transaction.delete(followerRef)

                    val currentUserRef = usersCollection.document(currentUid)
                    transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))

                    val targetUserRef = usersCollection.document(targetUid)
                    transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(targetUid: String): Boolean {
        return try {
            val currentUid = currentUid ?: return false
            val doc = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowers(uid: String): List<String> {
        return try {
            val snapshot = usersCollection
                .document(uid)
                .collection("followers")
                .get()
                .await()
            snapshot.documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowing(uid: String): List<String> {
        return try {
            val snapshot = usersCollection
                .document(uid)
                .collection("following")
                .get()
                .await()
            snapshot.documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowingUsers(uid: String): List<User> {
        return try {
            val followingIds = getFollowing(uid)
            if (followingIds.isEmpty()) return emptyList()

            val users = mutableListOf<User>()
            for (id in followingIds) {
                val doc = usersCollection.document(id).get().await()
                doc.toObject(User::class.java)?.let {
                    users.add(it.copy(id = doc.id))
                }
            }
            users
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUsersByIds(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()
        return try {
            ids.chunked(10).flatMap { chunk ->
                val snapshot = usersCollection
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Recalcule followersCount / followingCount à partir du nombre réel
    // de documents dans les sous-collections. Corrige les écarts (ex: 10 affiché vs 3 réel).
    suspend fun resyncCounts(uid: String): Result<Unit> {
        return try {
            val followersSnapshot = usersCollection.document(uid).collection("followers").get().await()
            val followingSnapshot = usersCollection.document(uid).collection("following").get().await()

            val realFollowersCount = followersSnapshot.size()
            val realFollowingCount = followingSnapshot.size()

            usersCollection.document(uid).update(
                mapOf(
                    "followersCount" to realFollowersCount,
                    "followingCount" to realFollowingCount
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Retire un abonné : currentUid retire targetUid de SA liste de followers
    // (= targetUid arrête automatiquement de suivre currentUid)
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

                    val currentUserRef = usersCollection.document(currentUid)
                    transaction.update(currentUserRef, "followersCount", FieldValue.increment(-1))

                    val targetUserRef = usersCollection.document(targetUid)
                    transaction.update(targetUserRef, "followingCount", FieldValue.increment(-1))
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}