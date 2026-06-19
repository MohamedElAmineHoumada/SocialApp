package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
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

    fun getCurrentUser(): Flow<User?> = callbackFlow {
        var docListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            docListener?.remove()
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                docListener = firestore.collection("users").document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null) {
                            trySend(snapshot?.toObject(User::class.java))
                        } else {
                            trySend(null)
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

    fun getUserById(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, displayName: String): Result<Unit> {
        return try {
            val username = displayName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            
            // 1. Créer le compte Auth d'abord pour être authentifié
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Échec de la création du compte.")

            // 2. Maintenant qu'on est connecté, on peut vérifier le pseudo et créer le document
            val existingUser = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            
            if (!existingUser.isEmpty) {
                // Si le pseudo est pris, on pourrait supprimer le compte Auth ou demander de changer
                // Pour simplifier ici, on ajoute un suffixe aléatoire ou on renvoie une erreur
                return Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
            }

            val user = hashMapOf(
                "id"              to uid,
                "email"           to email,
                "displayName"     to displayName,
                "username"        to username,
                "bio"             to "",
                "profileImageUrl" to "",
                "followersCount"  to 0,
                "followingCount"  to 0,
                "postsCount"      to 0,
                "isPrivate"       to false
            )
            firestore.collection("users").document(uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
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
            
            // Supprimer les données de l'utilisateur dans Firestore
            firestore.collection("users").document(uid).delete().await()
            
            // Supprimer le compte Auth
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
}