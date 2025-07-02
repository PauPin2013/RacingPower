package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.GameStats // <<< ¡IMPORTANTE! Asegúrate de importar GameStats
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InfiniteGameViewModel(application: Application) : AndroidViewModel(application) {
    val score = mutableStateOf(0)
    val highScore = mutableStateOf(0)
    val speed = mutableStateOf(5f)
    private var currentUserId: String = ""
    private var currentGameType: String = "" // Será "cars" o "planes"
    private var currentUserName: String = ""
    private val db: FirebaseFirestore = Firebase.firestore

    fun startGame(userId: String, gameType: String, userName: String) {
        currentUserId = userId
        currentGameType = gameType
        currentUserName = userName
        score.value = 0
        speed.value = 5f
        viewModelScope.launch {
            try {
                // Referencia al documento del usuario
                val userDocRef = db.collection("users").document(currentUserId)

                val document = userDocRef.get().await()
                if (document.exists()) {
                    // El documento del usuario existe.
                    // Intentar obtener los datos del campo específico del juego (ej. "cars" o "planes").
                    val gameStatsMap = document.get(currentGameType) as? Map<String, Any>

                    if (gameStatsMap != null) {
                        // Si el campo del juego existe, cargamos el highScore
                        // Usamos Long para leer de Firestore y luego lo convertimos a Int
                        val loadedHighScore = (gameStatsMap["highScore"] as? Long)?.toInt() ?: 0
                        highScore.value = loadedHighScore
                        Log.d("InfiniteGameViewModel", "High score loaded from Firestore for UID ${currentUserId}, game ${currentGameType}: ${loadedHighScore}")
                    } else {
                        // Si el campo del juego (ej. "cars") no existe, inicializarlo a 0.
                        highScore.value = 0
                        Log.d("InfiniteGameViewModel", "No game stats found for ${currentGameType} for UID ${currentUserId}, initializing to 0.")

                        val initialGameStats = mapOf("highScore" to 0) // Solo highScore en este mapa
                        // Usar update para añadir o modificar solo el campo específico (currentGameType)
                        userDocRef.update(currentGameType, initialGameStats).await()
                        Log.d("InfiniteGameViewModel", "Initial game stats field '${currentGameType}' created for UID ${currentUserId} in Firestore.")
                    }

                    // Puedes cargar el username y email aquí si los necesitas en el ViewModel
                    val loadedUsername = document.getString("username") ?: ""
                    // val loadedEmail = document.getString("email") ?: "" // Si el email existe en el documento
                    if (currentUserName.isEmpty() && loadedUsername.isNotEmpty()) {
                        currentUserName = loadedUsername // Actualizar si el ViewModel no lo tiene
                    }

                } else {
                    // Si el documento del usuario no existe, crearlo con los campos iniciales
                    highScore.value = 0
                    Log.d("InfiniteGameViewModel", "User document not found for UID ${currentUserId}, creating it.")

                    val initialUserData = mutableMapOf<String, Any>(
                        "username" to currentUserName // Asegurarse de que el username se guarde
                        // Si tienes el email al crear el usuario, lo añades aquí:
                        // "email" to "correo@ejemplo.com" // O el email que venga del auth
                    )
                    // Añadir el mapa inicial para el tipo de juego actual
                    initialUserData[currentGameType] = mapOf("highScore" to 0)

                    userDocRef.set(initialUserData).await()
                    Log.d("InfiniteGameViewModel", "Initial user document and game stats created for UID ${currentUserId}, game ${currentGameType} in Firestore.")
                }
            } catch (e: Exception) {
                Log.e("InfiniteGameViewModel", "Error loading high score from Firestore for UID ${currentUserId}, game ${currentGameType}: ${e.message}")
                highScore.value = 0
            }
        }
    }

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

    fun gameOver() {
        // La lógica de guardar el high score ya está en onCarPassed() si es necesario
    }

    fun resetGame() {
        score.value = 0
        speed.value = 5f
    }

    private fun saveHighScore() {
        viewModelScope.launch {
            val userIdToSave = currentUserId
            val gameTypeToSave = currentGameType
            val currentHighScore = highScore.value
            if (userIdToSave.isNotEmpty() && gameTypeToSave.isNotEmpty()) {
                val gameStatsToSave = mapOf("highScore" to currentHighScore) // Solo highScore
                try {
                    // Actualizar solo el campo específico (ej. "cars" o "planes") dentro del documento del usuario
                    db.collection("users").document(userIdToSave)
                        .update(gameTypeToSave, gameStatsToSave) // Usa update para modificar solo este campo
                        .await()
                    Log.d("InfiniteGameViewModel", "High score saved to Firestore for UID ${userIdToSave}, game ${gameTypeToSave}: $currentHighScore")
                } catch (e: Exception) {
                    Log.e("InfiniteGameViewModel", "Error saving high score to Firestore for UID ${userIdToSave}, game ${gameTypeToSave}: ${e.message}")
                }
            }
        }
    }

    fun incrementScore(amount: Int) {
        score.value += amount
        if (score.value > highScore.value) {
            highScore.value = score.value
            saveHighScore()
        }
    }
}