package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.Score
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InfiniteGameViewModel(application: Application) : AndroidViewModel(application) {
    val score = mutableStateOf(0)
    val highScore = mutableStateOf(0)
    val speed = mutableStateOf(5f)
    val username = mutableStateOf("") // Este será el display name del usuario (ej. su email)
    private var userId: String = "" // Nuevo: Para almacenar el UID de Firebase Auth

    // Inicializa la instancia de Firestore
    private val db: FirebaseFirestore = Firebase.firestore

    init {
        // No es necesario inicializar el DAO de Room aquí
    }

    /**
     * Inicia un nuevo juego para el usuario autenticado.
     * @param newUserId El UID del usuario autenticado de Firebase Auth.
     * @param newUsername El nombre de visualización (display name) del usuario (ej. su email).
     */
    fun startGame(newUserId: String, newUsername: String) {
        userId = newUserId // Almacena el UID
        username.value = newUsername // Almacena el nombre de visualización
        score.value = 0
        speed.value = 5f

        // Cargar el highScore existente para este usuario desde Firestore
        viewModelScope.launch {
            try {
                // Obtiene la referencia al documento del usuario en la colección "scores"
                // El ID del documento es el UID del usuario
                val docRef = db.collection("scores").document(userId)
                val document = docRef.get().await() // Espera a que la operación se complete

                if (document.exists()) {
                    // Si el documento existe, obtiene el highScore
                    val savedScore = document.toObject(Score::class.java)
                    savedScore?.let {
                        highScore.value = it.highScore
                        Log.d("InfiniteGameViewModel", "High score cargado desde Firestore para ${newUsername} (UID: $newUserId): ${it.highScore}")
                    }
                } else {
                    // Si el documento no existe para este usuario, inicializa highScore a 0
                    highScore.value = 0
                    Log.d("InfiniteGameViewModel", "No se encontró high score para ${newUsername} (UID: $newUserId), inicializando a 0.")
                    // Opcional: crea un documento inicial para el usuario si no existe
                    val newScore = Score(username = newUsername, highScore = 0)
                    db.collection("scores").document(userId).set(newScore).await()
                    Log.d("InfiniteGameViewModel", "Documento inicial creado para ${newUsername} (UID: $newUserId) en Firestore.")
                }
            } catch (e: Exception) {
                // Maneja cualquier error durante la carga
                Log.e("InfiniteGameViewModel", "Error al cargar high score desde Firestore para UID: $userId: ${e.message}")
                highScore.value = 0 // En caso de error, inicializa a 0
            }
        }
    }

    fun onCarPassed() {
        score.value += 50
        if (score.value > highScore.value) {
            highScore.value = score.value
            // Guarda el nuevo highScore inmediatamente cuando cambia
            saveHighScore()
        }
        if (score.value % 100 == 0) {
            speed.value += 1f
        }
    }

    fun gameOver() {
        // La lógica de guardado ya se maneja en onCarPassed()
        // No hay necesidad de forzar un guardado aquí a menos que quieras
        // guardar el score actual (no el high score) al finalizar el juego
    }

    fun resetGame() {
        score.value = 0
        speed.value = 5f
        // El highScore no se resetea al reiniciar el juego, solo el score actual
    }

    /**
     * Función para guardar el highScore en Firestore.
     * Utiliza el UID del usuario autenticado como ID del documento.
     */
    private fun saveHighScore() {
        viewModelScope.launch {
            val currentUserId = userId
            val currentUsername = username.value // Usar el nombre de visualización
            val currentHighScore = highScore.value

            if (currentUserId.isNotEmpty()) { // Asegúrate de tener un UID válido
                val scoreToSave = Score(currentUsername, currentHighScore)
                try {
                    // Guarda el objeto Score directamente en Firestore.
                    // Si el documento con ese ID (UID) ya existe, lo actualizará.
                    // Si no existe, lo creará.
                    db.collection("scores").document(currentUserId).set(scoreToSave).await()
                    Log.d("InfiniteGameViewModel", "High score guardado en Firestore para UID: $currentUserId - $currentHighScore")
                } catch (e: Exception) {
                    Log.e("InfiniteGameViewModel", "Error al guardar high score en Firestore para UID: $currentUserId: ${e.message}")
                }
            } else {
                Log.e("InfiniteGameViewModel", "No se puede guardar el high score: UID de usuario no disponible.")
            }
        }
    }
}
