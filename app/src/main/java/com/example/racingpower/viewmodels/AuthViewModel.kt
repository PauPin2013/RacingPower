package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope //
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth //
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Sealed class para representar los diferentes estados de autenticación.
 */
sealed class AuthState {
    object Loading : AuthState() // Estado inicial mientras se verifica la autenticación
    data class Authenticated(val user: FirebaseUser) : AuthState() // Usuario autenticado
    object Unauthenticated : AuthState() // No hay usuario autenticado
    data class Error(val message: String) : AuthState() // Error durante la autenticación
}

/**
 * ViewModel para manejar la lógica de autenticación de usuario.
 * Utiliza Firebase Authentication.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth // Instancia de Firebase Auth
    private val db: FirebaseFirestore = Firebase.firestore // Firestore instance

    // MutableStateFlow para exponer el estado de autenticación de forma observable
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    // Observable mutable state for login/register errors
    val errorMessage = mutableStateOf<String?>(null) //

    init {
        // Inicializa el listener para observar cambios en el estado de autenticación de Firebase
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = when (val user = firebaseAuth.currentUser) { //
                null -> AuthState.Unauthenticated // No hay usuario logueado
                else -> AuthState.Authenticated(user) // Hay un usuario logueado
            }
            Log.d("AuthViewModel", "Estado de autenticación cambiado: ${_authState.value}") //
        }
    }

    /**
     * Intenta iniciar sesión con el correo electrónico y la contraseña proporcionados.
     * @param email El correo electrónico del usuario.
     * @param password La contraseña del usuario.
     */
    fun login(email: String, password: String) {
        errorMessage.value = null // Limpia cualquier mensaje de error anterior
        _authState.value = AuthState.Loading // Establece el estado de carga
        viewModelScope.launch { //
            try {
                // Intenta iniciar sesión con email y contraseña
                auth.signInWithEmailAndPassword(email, password).await()
                Log.d("AuthViewModel", "Inicio de sesión exitoso para: $email") //
                // El AuthStateListener se encargará de
                // actualizar el estado a Authenticated
            } catch (e: Exception) {
                // Maneja el error de inicio de sesión
                Log.e("AuthViewModel", "Error al iniciar sesión: ${e.message}", e) //
                errorMessage.value = e.message ?: "Error desconocido al iniciar sesión." //
                _authState.value = AuthState.Unauthenticated // Vuelve al estado no autenticado en caso de error
            }
        }
    }

    /**
     * Intenta registrar un nuevo usuario con el correo electrónico, contraseña y nombre de usuario proporcionados.
     * @param email El correo electrónico del nuevo usuario.
     * @param password La contraseña del nuevo usuario.
     * @param username El nombre de usuario del nuevo usuario.
     */
    // En tu AuthViewModel
    fun register(email: String, password: String, displayName: String? = null) {
        errorMessage.value = null
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
                Log.d("AuthViewModel", "Registro exitoso para: $email")

                // Actualizar el perfil del usuario con el nombre de visualización
                userCredential.user?.let { firebaseUser ->
                    if (displayName != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).await()
                        Log.d("AuthViewModel", "Nombre de visualización actualizado para el usuario: $displayName")
                    }
                }
                _authState.value = AuthState.Authenticated(userCredential.user!!)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al registrarse: ${e.message}", e)
                errorMessage.value = e.message ?: "Error desconocido al registrarse."
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() {
        Log.d("AuthViewModel", "Cerrando sesión...") //
        auth.signOut() // Cierra la sesión de Firebase
        errorMessage.value = null // Limpia cualquier mensaje de error
        _authState.value = AuthState.Unauthenticated // Establece el estado a no autenticado
    }

    /**
     * Obtiene el usuario de Firebase actualmente autenticado.
     * @return El objeto FirebaseUser si está autenticado, o null si no.
     */
    fun getCurrentUser(): FirebaseUser? { //
        return auth.currentUser //
    }
}