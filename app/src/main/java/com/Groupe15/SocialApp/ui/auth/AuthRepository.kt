package com.Groupe15.SocialApp.ui.auth
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<Boolean> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun logout() {
        auth.signOut()
    }
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}