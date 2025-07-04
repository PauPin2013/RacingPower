package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.GameStats
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel para la lógica de juego de tipo "infinito".
// Gestiona la puntuación, la velocidad y la interacción con Firebase para guardar la puntuación más alta.
class InfiniteGameViewModel(application: Application) : AndroidViewModel(application) {
    // Puntuación actual del juego.
    val score = mutableStateOf(0)
    // Puntuación más alta registrada para el juego actual.
    val highScore = mutableStateOf(0)
    // Velocidad actual del juego.
    val speed = mutableStateOf(5f)

    // ID del usuario actual.
    private var currentUserId: String = ""
    // Tipo de juego actual (ej. "cars", "planes", "boats").
    private var currentGameType: String = ""
    // Nombre de usuario a mostrar en la interfaz de usuario.
    private var currentUserNameDisplayed: String = ""
    // Instancia de Firebase Firestore para interactuar con la base de datos.
    private val db: FirebaseFirestore = Firebase.firestore

    // Inicializa el juego, cargando la puntuación más alta del usuario desde Firestore.
    // También crea el documento de usuario o el campo de estadísticas de juego si no existen.
    fun startGame(userId: String, gameType: String, userNameToDisplay: String) {
        currentUserId = userId
        currentGameType = gameType
        currentUserNameDisplayed = userNameToDisplay
        score.value = 0
        speed.value = 5f
        viewModelScope.launch {
            try {
                val userDocRef = db.collection("users").document(currentUserId)
                val document = userDocRef.get().await()

                if (document.exists()) {
                    // Si el documento del usuario existe, intenta cargar las estadísticas del juego específico.
                    val gameStatsMap = document.get(currentGameType) as? Map<String, Any>

                    if (gameStatsMap != null) {
                        // Si las estadísticas del juego existen, carga la puntuación más alta.
                        val loadedHighScore = (gameStatsMap["highScore"] as? Long)?.toInt() ?: 0
                        highScore.value = loadedHighScore
                        Log.d("InfiniteGameViewModel", "High score loaded from Firestore for UID ${currentUserId}, game ${currentGameType}: ${loadedHighScore}")
                    } else {
                        // Si las estadísticas del juego no existen, inicializa la puntuación más alta a 0
                        // y crea el campo en Firestore.
                        highScore.value = 0
                        Log.d("InfiniteGameViewModel", "No game stats found for ${currentGameType} for UID ${currentUserId}, initializing to 0.")

                        val initialGameStats = mapOf("highScore" to 0)
                        userDocRef.update(currentGameType, initialGameStats).await()
                        Log.d("InfiniteGameViewModel", "Initial game stats field '${currentGameType}' created for UID ${currentUserId} in Firestore.")
                    }
                } else {
                    // Si el documento del usuario no existe, inicializa la puntuación más alta a 0
                    // y crea el documento del usuario con el nombre a mostrar y las estadísticas iniciales del juego.
                    highScore.value = 0
                    Log.d("InfiniteGameViewModel", "User document not found for UID ${currentUserId}, creating it.")

                    val initialUserData = mutableMapOf<String, Any>(
                        "displayName" to userNameToDisplay
                    )
                    initialUserData[currentGameType] = mapOf("highScore" to 0)

                    userDocRef.set(initialUserData).await()
                    Log.d("InfiniteGameViewModel", "Initial user document and game stats created for UID ${currentUserId}, game ${currentGameType} in Firestore.")
                }
            } catch (e: Exception) {
                // Registra cualquier error que ocurra durante la carga de la puntuación más alta.
                Log.e("InfiniteGameViewModel", "Error loading high score from Firestore for UID ${currentUserId}, game ${currentGameType}: ${e.message}")
                highScore.value = 0
            }
        }
    }

    // Se llama cuando un "coche" (o elemento del juego) pasa, incrementando la puntuación y ajustando la velocidad.
    // También guarda la puntuación más alta si se supera.
    fun onCarPassed() {
        score.value += 50
        if (score.value > highScore.value) {
            highScore.value = score.value
            saveHighScore()
        }
        if (score.value % 100 == 0) {
            speed.value += 1f
        }
    }

    // Función para manejar el fin del juego.
    // La lógica de guardar la puntuación ya se maneja en 'onCarPassed' si es necesario.
    fun gameOver() {
        // No se requiere lógica adicional aquí ya que `saveHighScore` se llama en `onCarPassed`.
    }

    // Reinicia la puntuación y la velocidad del juego a sus valores iniciales.
    fun resetGame() {
        score.value = 0
        speed.value = 5f
    }

    // Guarda la puntuación más alta actual en Firestore para el usuario y tipo de juego.
    private fun saveHighScore() {
        viewModelScope.launch {
            val userIdToSave = currentUserId
            val gameTypeToSave = currentGameType
            val currentHighScore = highScore.value
            if (userIdToSave.isNotEmpty() && gameTypeToSave.isNotEmpty()) {
                val gameStatsToSave = mapOf("highScore" to currentHighScore)
                try {
                    // Actualiza solo el campo de estadísticas de juego específico dentro del documento del usuario.
                    db.collection("users").document(userIdToSave)
                        .update(gameTypeToSave, gameStatsToSave)
                        .await()
                    Log.d("InfiniteGameViewModel", "High score saved to Firestore for UID ${userIdToSave}, game ${gameTypeToSave}: $currentHighScore")
                } catch (e: Exception) {
                    Log.e("InfiniteGameViewModel", "Error saving high score to Firestore for UID ${userIdToSave}, game ${gameTypeToSave}: ${e.message}")
                }
            }
        }
    }

    // Incrementa la puntuación del juego en la cantidad especificada.
    // Si la nueva puntuación supera la más alta, la actualiza y la guarda.
    fun incrementScore(amount: Int) {
        score.value += amount
        if (score.value > highScore.value) {
            highScore.value = score.value
            saveHighScore()
        }
    }
}