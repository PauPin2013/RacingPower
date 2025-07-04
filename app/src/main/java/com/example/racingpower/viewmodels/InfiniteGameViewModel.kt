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
    private var currentGameType: String = ""
    private var currentUserNameDisplayed: String = "" // Cambiado de currentUserName a currentUserNameDisplayed para claridad
    private val db: FirebaseFirestore = Firebase.firestore

    fun startGame(userId: String, gameType: String, userNameToDisplay: String) { // Recibe el nombre a mostrar
        currentUserId = userId
        currentGameType = gameType
        currentUserNameDisplayed = userNameToDisplay // Guarda el nombre a mostrar
        score.value = 0
        speed.value = 5f
        viewModelScope.launch {
            try {
                val userDocRef = db.collection("users").document(currentUserId)
                val document = userDocRef.get().await()

                if (document.exists()) {
                    val gameStatsMap = document.get(currentGameType) as? Map<String, Any>

                    if (gameStatsMap != null) {
                        val loadedHighScore = (gameStatsMap["highScore"] as? Long)?.toInt() ?: 0
                        highScore.value = loadedHighScore
                        Log.d("InfiniteGameViewModel", "High score loaded from Firestore for UID ${currentUserId}, game ${currentGameType}: ${loadedHighScore}")
                    } else {
                        highScore.value = 0
                        Log.d("InfiniteGameViewModel", "No game stats found for ${currentGameType} for UID ${currentUserId}, initializing to 0.")

                        val initialGameStats = mapOf("highScore" to 0)
                        userDocRef.update(currentGameType, initialGameStats).await()
                        Log.d("InfiniteGameViewModel", "Initial game stats field '${currentGameType}' created for UID ${currentUserId} in Firestore.")
                    }
                } else {
                    highScore.value = 0
                    Log.d("InfiniteGameViewModel", "User document not found for UID ${currentUserId}, creating it.")

                    val initialUserData = mutableMapOf<String, Any>(
                        "displayName" to userNameToDisplay // Asegurarse de guardar el displayName
                    )
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