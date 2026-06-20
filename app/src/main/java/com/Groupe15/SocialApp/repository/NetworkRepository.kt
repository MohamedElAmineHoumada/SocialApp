package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.SuggestionUser
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.models.Post
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection = firestore.collection("users")
    private val followsCollection = firestore.collection("follows")

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

    suspend fun getTrendingPosts(pageSize: Int = 10, lastDocument: DocumentSnapshot? = null): Pair<List<Post>, DocumentSnapshot?> {
        return try {
            var query = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()
            val posts = snapshot.toObjects(Post::class.java)
            val lastVisible = if (snapshot.documents.isNotEmpty()) snapshot.documents[snapshot.size() - 1] else null
            
            Pair(posts, lastVisible)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), null)
        }
    }

    suspend fun searchUsers(query: String): List<SuggestionUser> {
        if (query.isBlank()) return emptyList()
        return try {
            // Firestore doesn't support case-insensitive contains, so we do a simple prefix search.
            // A better solution would be using Algolia or keeping a lowercase field.
            val snapshot = usersCollection
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                user?.let {
                    SuggestionUser(
                        id = it.id,
                        name = it.displayName.ifBlank { it.username },
                        role = it.role,
                        mutualFriends = 0, // Simplified for search
                        avatarUrl = it.profileImageUrl.ifBlank { null },
                        isOnline = false
                    )
                }
            }

            // Also search by username if results are few
            if (results.size < 5) {
                val usernameSnapshot = usersCollection
                    .whereGreaterThanOrEqualTo("username", query)
                    .whereLessThanOrEqualTo("username", query + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()
                
                val usernameResults = usernameSnapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                    user?.let {
                        SuggestionUser(
                            id = it.id,
                            name = it.displayName.ifBlank { it.username },
                            role = it.role,
                            mutualFriends = 0,
                            avatarUrl = it.profileImageUrl.ifBlank { null },
                            isOnline = false
                        )
                    }
                }
                (results + usernameResults).distinctBy { it.id }
            } else {
                results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

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
