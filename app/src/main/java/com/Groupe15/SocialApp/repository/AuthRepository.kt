package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.User
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    fun getCurrentUser(): Flow<User?> = callbackFlow {
        var docListener: com.google.firebase.firestore.ListenerRegistration? = null
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            docListener?.remove()
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                docListener = firestore.collection("users").document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) { trySend(null); return@addSnapshotListener }
                        val user = snapshot?.toObject(User::class.java)
                        if (user != null) {
                            // Document existe → on l'envoie
                            trySend(user)
                        } else if (snapshot != null && !snapshot.exists()) {
                            // ✅ Document absent (compte créé directement dans Firebase Console
                            // ou inscription incomplète) → créer un profil minimal automatiquement
                            val firebaseUser = firebaseAuth.currentUser ?: return@addSnapshotListener
                            val displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Utilisateur"
                            var username = displayName.lowercase().replace(" ", "_")
                                .filter { it.isLetterOrDigit() || it == '_' }
                            if (username.isBlank()) username = "user_${uid.take(6)}"
                            val newUser = hashMapOf(
                                "id" to uid,
                                "email" to (firebaseUser.email ?: ""),
                                "displayName" to displayName,
                                "username" to username,
                                "bio" to "",
                                "profileImageUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                                "coverImageUrl" to "",
                                "website" to "",
                                "followersCount" to 0,
                                "followingCount" to 0,
                                "postsCount" to 0,
                                "isPrivate" to false,
                                "role" to "",
                                "fcmToken" to ""
                            )
                            firestore.collection("users").document(uid).set(newUser)
                                .addOnSuccessListener {
                                    trySend(
                                        User(
                                            id = uid,
                                            email = firebaseUser.email ?: "",
                                            displayName = displayName,
                                            username = username
                                        )
                                    )
                                }
                        }
                    }
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
            docListener?.remove()
        }
    }

    suspend fun register(email: String, password: String, displayName: String): Result<Unit> {
        return try {
            val username = displayName.lowercase().replace(" ", "_")
                .filter { it.isLetterOrDigit() || it == '_' }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Échec de la création du compte.")

            val existingUser = firestore.collection("users")
                .whereEqualTo("username", username).get().await()
            if (!existingUser.isEmpty) {
                return Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
            }

            val user = hashMapOf(
                "id" to uid, "email" to email, "displayName" to displayName,
                "username" to username, "bio" to "", "profileImageUrl" to "",
                "coverImageUrl" to "", "website" to "",
                "followersCount" to 0, "followingCount" to 0, "postsCount" to 0,
                "isPrivate" to false, "role" to "", "fcmToken" to "",
                "birthDate" to "", "gender" to "", "interests" to emptyList<String>(),
                "isOnboardingCompleted" to false
            )
            firestore.collection("users").document(uid).set(user).await()
            updateFcmToken()

            // ✅ Vrai email de vérification Firebase (lien cliquable envoyé par mail)
            auth.currentUser?.sendEmailVerification()?.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserById(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ -> trySend(snapshot?.toObject(User::class.java)) }
        awaitClose { listener.remove() }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            updateFcmToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Connexion Google via idToken (obtenu depuis Credential Manager).
     * Crée le document Firestore + envoie email de vérification si nouvel utilisateur.
     * Google fournit des emails déjà vérifiés, donc on marque isEmailVerified = true.
     */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Échec de la connexion Google.")

            // ✅ On ne se fie plus uniquement à isNewUser : ce flag peut être incorrect
            // si le compte Auth existe déjà mais que son document Firestore a été supprimé
            // ou n'a jamais été créé (ex: crash pendant une inscription précédente, ou
            // suppression manuelle pendant des tests). On vérifie l'existence RÉELLE du
            // document dans Firestore, ce qui garantit qu'aucun compte "fantôme"
            // (authentifié mais sans profil) ne peut se produire.
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDoc.exists()) {
                createFirestoreUserFromSocial(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "Utilisateur",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                )
                // Ne PAS envoyer d'email de vérification :
                // Firebase marque automatiquement isEmailVerified = true pour Google OAuth
            }
            updateFcmToken()
            // Que l'utilisateur soit nouveau ou existant → succès
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connexion Facebook via accessToken (obtenu depuis le SDK Facebook).
     * Crée le document Firestore si nouvel utilisateur.
     * Envoie un email de vérification car Facebook ne garantit pas la vérification d'email.
     */
    suspend fun signInWithFacebook(accessToken: String): Result<Unit> {
        return try {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Échec de la connexion Facebook.")

            // ✅ Même garde que pour Google : on vérifie l'existence réelle du document
            // Firestore plutôt que de se fier uniquement à isNewUser.
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDoc.exists()) {
                createFirestoreUserFromSocial(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "Utilisateur",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                )
                // Facebook ne garantit pas que l'email est vérifié côté Firebase
                if (!firebaseUser.isEmailVerified && firebaseUser.email?.isNotBlank() == true) {
                    try { firebaseUser.sendEmailVerification().await() } catch (_: Exception) {}
                }
            }
            updateFcmToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Renvoie l'email de vérification à l'utilisateur courant.
     */
    suspend fun resendVerificationEmail(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Aucun utilisateur connecté.")
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recharge l'état Firebase de l'utilisateur courant pour vérifier
     * si son email a été vérifié depuis la dernière session.
     */
    suspend fun reloadUser(): Result<Boolean> {
        return try {
            auth.currentUser?.reload()?.await()
            Result.success(auth.currentUser?.isEmailVerified == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Aucun utilisateur connecté")
            val uid = user.uid
            firestore.collection("users").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrivacyStatus(isPrivate: Boolean): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Utilisateur non connecté")
            firestore.collection("users").document(uid).update("isPrivate", isPrivate).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String? = null): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Non connecté"))
            val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()
            firestore.collection("users").document(uid).update("fcmToken", fcmToken).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBirthDate(birthDate: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            firestore.collection("users").document(uid).update("birthDate", birthDate).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGender(gender: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            firestore.collection("users").document(uid).update("gender", gender).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInterests(interests: List<String>): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            firestore.collection("users").document(uid).update(
                mapOf(
                    "interests" to interests,
                    "isOnboardingCompleted" to true
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Non connecté"))
            firestore.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Helpers privés ───────────────────────────────────────────────────────

    private suspend fun createFirestoreUserFromSocial(
        uid: String, email: String, displayName: String, photoUrl: String
    ) {
        var username = displayName.lowercase().replace(" ", "_")
            .filter { it.isLetterOrDigit() || it == '_' }
        if (username.isBlank()) username = "user_${uid.take(6)}"

        val existing = firestore.collection("users")
            .whereEqualTo("username", username).get().await()
        if (!existing.isEmpty) username = "${username}_${uid.take(4)}"

        val userData = hashMapOf(
            "id" to uid, "email" to email, "displayName" to displayName,
            "username" to username, "bio" to "", "profileImageUrl" to photoUrl,
            "followersCount" to 0, "followingCount" to 0, "postsCount" to 0, "isPrivate" to false,
            "fcmToken" to "", "birthDate" to "", "gender" to "",
            "interests" to emptyList<String>(), "isOnboardingCompleted" to false
        )
        firestore.collection("users").document(uid).set(userData).await()
    }
}