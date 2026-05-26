package com.Groupe15.SocialApp.repository


import com.google.firebase.auth.FirebaseAuth
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

            // Ajouter targetUid dans following du currentUser
            val followingRef = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)
            batch.set(followingRef, mapOf("uid" to targetUid))

            // Ajouter currentUid dans followers du targetUser
            val followerRef = usersCollection
                .document(targetUid)
                .collection("followers")
                .document(currentUid)
            batch.set(followerRef, mapOf("uid" to currentUid))

            // Incrémenter followingCount du currentUser
            val currentUserRef = usersCollection.document(currentUid)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(1))

            // Incrémenter followersCount du targetUser
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

            val batch = firestore.batch()

            // Supprimer targetUid de following du currentUser
            val followingRef = usersCollection
                .document(currentUid)
                .collection("following")
                .document(targetUid)
            batch.delete(followingRef)

            // Supprimer currentUid de followers du targetUser
            val followerRef = usersCollection
                .document(targetUid)
                .collection("followers")
                .document(currentUid)
            batch.delete(followerRef)

            // Décrémenter followingCount du currentUser
            val currentUserRef = usersCollection.document(currentUid)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(-1))

            // Décrémenter followersCount du targetUser
            val targetUserRef = usersCollection.document(targetUid)
            batch.update(targetUserRef, "followersCount", FieldValue.increment(-1))

            batch.commit().await()
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
}