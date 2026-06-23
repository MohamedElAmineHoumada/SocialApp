package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln


@Singleton
class RecommendationRepository @Inject constructor(
    private val postRepository: PostRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val WEIGHT_LIKE = 2.0
        private const val WEIGHT_COMMENT = 3.0
        private const val WEIGHT_SOCIAL_PROXIMITY = 8.0
        private const val DECAY_FACTOR = 1.5
    }


    private suspend fun computeSocialProximity(
        currentUid: String,
        authorUids: List<String>
    ): Map<String, Int> {
        if (authorUids.isEmpty()) return emptyMap()

        val myFollowing = postRepository.getFollowingUids(currentUid).toSet()
        if (myFollowing.isEmpty()) return emptyMap()

        val proximity = mutableMapOf<String, Int>()

        // Pour chaque personne que je suis, je regarde qui elle suit,
        // et j'incrémente le score de proximité des auteurs concernés.
        // Limité à un sous-ensemble pour éviter trop de lectures Firestore.
        myFollowing.take(30).forEach { followedUid ->
            try {
                val theirFollowing = firestore.collection("users")
                    .document(followedUid)
                    .collection("following")
                    .get()
                    .await()
                    .documents.map { it.id }

                theirFollowing.forEach { uid ->
                    if (uid in authorUids) {
                        proximity[uid] = (proximity[uid] ?: 0) + 1
                    }
                }
            } catch (_: Exception) {
                // on ignore les erreurs ponctuelles, le scoring reste approximatif
            }
        }

        return proximity
    }

    private fun engagementScore(post: Post): Double {
        val likeScore = post.likesCount * WEIGHT_LIKE
        val commentScore = post.commentsCount * WEIGHT_COMMENT
        // ln(1 + heures) lisse la décroissance : un post de 1h ne s'effondre pas brutalement
        val decay = DECAY_FACTOR * ln(1.0 + post.getAgeInHours())
        return likeScore + commentScore - decay
    }


    fun getForYouPosts(excludeAlreadyLiked: Boolean = false): Flow<List<Post>> {
        val currentUid = auth.currentUser?.uid
            ?: return postRepository.getAllPosts(limit = 200)

        val allPostsFlow = postRepository.getAllPosts(limit = 200)
        val followingFlow = postRepository.getFollowingUidsFlow(currentUid)

        return combine(allPostsFlow, followingFlow) { posts, followingList ->
            val myFollowing = followingList.toSet()

            android.util.Log.d(
                "ForYouDebug",
                "currentUid=$currentUid myFollowing=$myFollowing totalPosts=${posts.size}"
            )

            // Pool de découverte : ni soi-même, ni les comptes déjà suivis
            val discoveryPool = posts.filter { post ->
                post.authorUid != currentUid && post.authorUid !in myFollowing
            }

            android.util.Log.d(
                "ForYouDebug",
                "discoveryPool size=${discoveryPool.size} authors=${discoveryPool.map { it.authorUsername }}"
            )

            val authorUids = discoveryPool.map { it.authorUid }.distinct()
            val proximityMap = try {
                computeSocialProximity(currentUid, authorUids)
            } catch (_: Exception) {
                emptyMap()
            }

            val likedIds = if (excludeAlreadyLiked) {
                try {
                    postRepository.getLikedPostIds(discoveryPool.map { it.postId })
                } catch (_: Exception) {
                    emptySet()
                }
            } else emptySet()

            discoveryPool
                .filter { it.postId !in likedIds }
                .map { post ->
                    val proximity = proximityMap[post.authorUid] ?: 0
                    val socialBonus = proximity * WEIGHT_SOCIAL_PROXIMITY
                    val finalScore = engagementScore(post) + socialBonus
                    post to finalScore
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }
    }
}