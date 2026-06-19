package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    /**
     * Inscription classique par email/mot de passe.
     */
    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val username = displayName.lowercase().replace(" ", "_")
                    .filter { it.isLetterOrDigit() || it == '_' }

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Échec de la création du compte.")

                val existingUser = firestore.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()

                if (!existingUser.isEmpty) {
                    _state.value = AuthState.Error("Ce nom d'utilisateur est déjà pris.")
                    return@launch
                }

                val user = hashMapOf(
                    "id" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "username" to username,
                    "bio" to "",
                    "profileImageUrl" to "",
                    "followersCount" to 0,
                    "followingCount" to 0,
                    "postsCount" to 0,
                    "isPrivate" to false
                )
                firestore.collection("users").document(uid).set(user).await()
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Erreur lors de l'inscription")
            }
        }
    }

    /**
     * Connexion/inscription via un provider social (Google ou Facebook).
     * Crée le document Firestore uniquement si l'utilisateur est nouveau.
     */
    fun signInWithSocialCredential(credential: AuthCredential) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw Exception("Échec de la connexion.")
                val uid = user.uid

                val userDoc = firestore.collection("users").document(uid).get().await()

                if (!userDoc.exists()) {
                    val rawName = user.displayName ?: "Utilisateur"
                    val userEmail = user.email ?: ""

                    var username = rawName.lowercase().replace(" ", "_")
                        .filter { it.isLetterOrDigit() || it == '_' }
                    if (username.isBlank()) username = "user_${uid.take(6)}"

                    val existing = firestore.collection("users")
                        .whereEqualTo("username", username)
                        .get()
                        .await()
                    if (!existing.isEmpty) {
                        username = "${username}_${uid.take(4)}"
                    }

                    val userData = hashMapOf(
                        "id" to uid,
                        "email" to userEmail,
                        "displayName" to rawName,
                        "username" to username,
                        "bio" to "",
                        "profileImageUrl" to (user.photoUrl?.toString() ?: ""),
                        "followersCount" to 0,
                        "followingCount" to 0,
                        "postsCount" to 0,
                        "isPrivate" to false
                    )
                    firestore.collection("users").document(uid).set(userData).await()
                }

                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Erreur d'authentification")
            }
        }
    }
}