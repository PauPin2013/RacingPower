package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.UserProfile
import com.example.racingpower.models.GameStats
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

// Clase sellada que representa los diferentes estados de autenticación del usuario.
sealed class AuthState {
    // Estado de carga, indica que una operación de autenticación está en progreso.
    object Loading : AuthState()
    // Estado no autenticado, el usuario no ha iniciado sesión.
    object Unauthenticated : AuthState()
    // Estado autenticado, el usuario ha iniciado sesión con éxito.
    data class Authenticated(val user: FirebaseUser) : AuthState()
    // Estado de error, indica que ocurrió un problema durante la autenticación.
    data class Error(val message: String) : AuthState()
}

// ViewModel responsable de la lógica de autenticación y gestión del perfil de usuario.
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // Instancia de Firebase Authentication.
    private val auth: FirebaseAuth = Firebase.auth
    // Instancia de Firebase Firestore para la base de datos.
    private val db: FirebaseFirestore = Firebase.firestore

    // Flujo de estado mutable que representa el estado actual de autenticación.
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    // Flujo de estado inmutable para que la UI observe los cambios en el estado de autenticación.
    val authState: StateFlow<AuthState> = _authState

    // Flujo de estado mutable que contiene el perfil del usuario autenticado.
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    // Flujo de estado inmutable para que la UI observe los cambios en el perfil del usuario.
    val userProfile: StateFlow<UserProfile?> = _userProfile

    // Flujo de estado mutable para mensajes de error que pueden ser mostrados en la UI.
    private val _errorMessage = MutableStateFlow<String?>(null)
    // Flujo de estado inmutable para que la UI observe los mensajes de error.
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // Inicializa un listener que observa los cambios en el estado de autenticación de Firebase.
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Si hay un usuario autenticado, actualiza el estado y carga su perfil.
                _authState.value = AuthState.Authenticated(user)
                loadUserProfile(user.uid)
            } else {
                // Si no hay usuario, establece el estado como no autenticado y borra el perfil.
                _authState.value = AuthState.Unauthenticated
                _userProfile.value = null
            }
        }
    }

    // Carga el perfil de usuario desde Firestore basándose en el ID del usuario.
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

    // Intenta iniciar sesión con el correo electrónico y la contraseña proporcionados.
    // Llama a onSuccess si el inicio de sesión es exitoso, o onError si falla.
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
                    _errorMessage.value = msg
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Login failed."
                _authState.value = AuthState.Error(msg)
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    // Intenta registrar un nuevo usuario con el correo electrónico, contraseña y nombre de usuario.
    // Llama a onSuccess si el registro es exitoso, o onError si falla.
    fun register(
        email: String,
        password: String,
        username: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = userCredential.user

                if (firebaseUser != null) {
                    // Crea un nuevo perfil de usuario con datos iniciales.
                    val newUserProfile = UserProfile(
                        userId = firebaseUser.uid,
                        displayName = username,
                        avatarName = "avatar1",
                        cars = GameStats(),
                        planes = GameStats(),
                        boats = GameStats()
                    )

                    // Guarda el nuevo perfil de usuario en Firestore.
                    db.collection("users").document(firebaseUser.uid).set(newUserProfile).await()

                    _authState.value = AuthState.Authenticated(firebaseUser)
                    _userProfile.value = newUserProfile
                    onSuccess(firebaseUser)
                    Log.d("AuthViewModel", "User registered and profile created: ${newUserProfile.displayName}")
                } else {
                    val msg = "Registration failed: User is null after creation."
                    _authState.value = AuthState.Error(msg)
                    _errorMessage.value = msg
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Registration failed."
                _authState.value = AuthState.Error(msg)
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    // Cierra la sesión del usuario actual.
    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        _userProfile.value = null
        _errorMessage.value = null
        Log.d("AuthViewModel", "User logged out.")
    }

    // Limpia el mensaje de error actual.
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Actualiza el nombre del avatar del usuario en Firestore y en el perfil local.
    fun updateAvatar(userId: String, newAvatarName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("avatarName", newAvatarName)
                    .await()

                // Actualiza el StateFlow _userProfile para reflejar el cambio en la UI.
                _userProfile.value = _userProfile.value?.copy(avatarName = newAvatarName)

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

    // Actualiza el nombre de visualización del usuario en Firestore y en el perfil local.
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
                _errorMessage.value = msg
                onError(msg)
                Log.e("AuthViewModel", "Error updating display name for $userId: $msg", e)
            }
        }
    }

    // Guarda la puntuación más alta del juego para un tipo de juego específico y usuario.
    // Solo actualiza si la nueva puntuación es mayor que la existente.
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
                _errorMessage.value = e.message
            }
        }
    }
}