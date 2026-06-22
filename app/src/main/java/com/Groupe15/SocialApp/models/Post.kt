package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Post(
    @get:PropertyName("id") @set:PropertyName("id") var postId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var authorUid: String = "",
    var authorUsername: String = "",
    var authorProfileUrl: String = "",
    @get:PropertyName("caption") @set:PropertyName("caption") var content: String = "",
    @get:PropertyName("imageUrls") @set:PropertyName("imageUrls") var imageUrls: List<String> = emptyList(),
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var oldImageUrl: String = "",
    var likesCount: Int = 0,
    var commentsCount: Int = 0,
    var createdAt: Any? = null
) {
    fun getCreatedAtMillis(): Long {
        return when (val date = createdAt) {
            is Timestamp -> date.toDate().time
            is Long -> date
            else -> System.currentTimeMillis()
        }
    }

    // Compatibilité avec le code existant qui attend 'imageUrl' (1ère image ou ancien champ unique)
    @get:Exclude
    val imageUrl: String
        get() = when {
            imageUrls.isNotEmpty() -> imageUrls.first()
            oldImageUrl.isNotBlank() -> oldImageUrl
            else -> ""
        }

    /**
     * Âge du post en heures, utilisé pour le scoring "For You".
     */
    @Exclude
    fun getAgeInHours(): Double {
        val ageMillis = System.currentTimeMillis() - getCreatedAtMillis()
        return (ageMillis / (1000.0 * 60.0 * 60.0)).coerceAtLeast(0.0)
    }
}