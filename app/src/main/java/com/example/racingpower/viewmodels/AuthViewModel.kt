package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.UserProfile
import com.example.racingpower.models.GameStats // Asegúrate de importar GameStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Asegúrate de que AuthState esté definido
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    // --- ¡AQUÍ ESTÁ LA DEFINICIÓN CORRECTA DE errorMessage! ---
    private val _errorMessage = MutableStateFlow<String?>(null) // Internal mutable state
    val errorMessage: StateFlow<String?> = _errorMessage // Public immutable state for UI to collect
    // -----------------------------------------------------------

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
                loadUserProfile(user.uid)
            } else {
                _authState.value = AuthState.Unauthenticated
                _userProfile.value = null
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val document = db.collection("users").document(userId).get().await()
                val profile = document.toObject(UserProfile::class.java)
                _userProfile.value = profile
                Log.d("AuthViewModel", "UserProfile loaded: ${profile?.displayName}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading user profile: ${e.message}", e)
                _userProfile.value = null
            }
        }
    }

    fun login(email: String, password: String, onSuccess: (FirebaseUser) -> Unit, onError: (String) -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    _authState.value = AuthState.Authenticated(firebaseUser)
                    onSuccess(firebaseUser)
                    loadUserProfile(firebaseUser.uid)
                } else {
                    val msg = "Login failed: User is null after sign-in."
                    _authState.value = AuthState.Error(msg)
                    _errorMessage.value = msg // Set error message
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Login failed."
                _authState.value = AuthState.Error(msg)
                _errorMessage.value = msg // Set error message
                onError(msg)
            }
        }
    }

    fun register(
        email: String,
        password: String,
        username: String,
        onSuccess: (FirebaseUser) -> Unit, // Asegúrate de que estos callbacks estén aquí
        onError: (String) -> Unit
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = userCredential.user

                if (firebaseUser != null) {
                    val newUserProfile = UserProfile(
                        userId = firebaseUser.uid,
                        displayName = username,
                        avatarName = "avatar1",
                        cars = GameStats(), // Usa GameStats() aquí
                        planes = GameStats(),
                        boats = GameStats()
                    )

                    db.collection("users").document(firebaseUser.uid).set(newUserProfile).await()

                    _authState.value = AuthState.Authenticated(firebaseUser)
                    _userProfile.value = newUserProfile
                    onSuccess(firebaseUser) // Llama al callback de éxito
                    Log.d("AuthViewModel", "User registered and profile created: ${newUserProfile.displayName}")
                } else {
                    val msg = "Registration failed: User is null after creation."
                    _authState.value = AuthState.Error(msg)
                    _errorMessage.value = msg // Set error message
                    onError(msg) // Llama al callback de error
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Registration failed."
                _authState.value = AuthState.Error(msg)
                _errorMessage.value = msg // Set error message
                onError(msg) // Llama al callback de error
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        _userProfile.value = null
        _errorMessage.value = null // Clear error message on logout
        Log.d("AuthViewModel", "User logged out.")
    }

    // --- ¡AÑADE ESTE MÉTODO PARA LIMPIAR EL MENSAJE DE ERROR! ---
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    // -------------------------------------------------------------

    // ... (otras funciones como updateAvatar, updateDisplayName, saveGameScore) ...
    // Asegúrate de que updateAvatar también tenga onSuccess y onError callbacks
    fun updateAvatar(userId: String, newAvatarName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("avatarName", newAvatarName)
                    .await()

                // --- ¡CRÍTICO! Actualizar el StateFlow _userProfile en el ViewModel ---
                // Esto hará que GameSelectionScreen, que observa userProfile, se actualice.
                _userProfile.value = _userProfile.value?.copy(avatarName = newAvatarName)
                // -------------------------------------------------------------------

                onSuccess()
                Log.d("AuthViewModel", "Avatar updated for $userId to $newAvatarName")
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to update avatar."
                _errorMessage.value = msg
                onError(msg)
                Log.e("AuthViewModel", "Error updating avatar for $userId: $msg", e)
            }
        }
    }

    fun updateDisplayName(userId: String, newDisplayName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("displayName", newDisplayName)
                    .await()
                _userProfile.value = _userProfile.value?.copy(displayName = newDisplayName)
                onSuccess()
                Log.d("AuthViewModel", "Display name updated for $userId to $newDisplayName")
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to update display name."
                _errorMessage.value = msg // Set error message
                onError(msg)
                Log.e("AuthViewModel", "Error updating display name for $userId: $msg", e)
            }
        }
    }

    fun saveGameScore(userId: String, gameType: String, newScore: Int) {
        viewModelScope.launch {
            try {
                val userDocRef = db.collection("users").document(userId)
                val userProfileData = userDocRef.get().await().toObject(UserProfile::class.java)

                if (userProfileData != null) {
                    val currentStats = userProfileData.getGameStats(gameType)
                    if (newScore > currentStats.highScore) {
                        val updatedStats = currentStats.copy(highScore = newScore)
                        val updatedUserProfile = when (gameType) {
                            "cars" -> userProfileData.copy(cars = updatedStats)
                            "planes" -> userProfileData.copy(planes = updatedStats)
                            "boats" -> userProfileData.copy(boats = updatedStats)
                            else -> userProfileData
                        }

                        userDocRef.set(updatedUserProfile).await()
                        _userProfile.value = updatedUserProfile
                        Log.d("AuthViewModel", "High score updated for $gameType to $newScore for user $userId")
                    } else {
                        Log.d("AuthViewModel", "New score $newScore not higher than existing high score for $gameType.")
                    }
                } else {
                    Log.w("AuthViewModel", "UserProfile not found for userId: $userId")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error saving game score for $gameType: ${e.message}", e)
                _errorMessage.value = e.message // Set error message for game score errors too
            }
        }
    }
}