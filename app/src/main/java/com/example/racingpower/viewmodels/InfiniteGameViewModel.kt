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
                // ¡MODIFICADO! Nueva ruta: users/{userId}/{gameType}/score
                val docRef = db.collection("users").document(currentUserId)
                    .collection(currentGameType) // Ahora currentGameType es la colección
                    .document("score") // Documento fijo para el puntaje dentro de esa colección

                val document = docRef.get().await()
                if (document.exists()) {
                    val savedScore = document.toObject(Score::class.java)
                    savedScore?.let {
                        highScore.value = it.highScore
                        Log.d("InfiniteGameViewModel", "High score loaded from Firestore for UID ${currentUserId}, game ${currentGameType}: ${it.highScore}")
                    }
                } else {
                    highScore.value = 0
                    Log.d("InfiniteGameViewModel", "No high score found for UID ${currentUserId}, game ${currentGameType}, initializing to 0.")
                    val newScore = Score(userId = currentUserId, username = currentUserName, gameType = currentGameType, highScore = 0)
                    // ¡MODIFICADO! Nueva ruta: users/{userId}/{gameType}/score
                    db.collection("users").document(currentUserId)
                        .collection(currentGameType).document("score").set(newScore).await()
                    Log.d("InfiniteGameViewModel", "Initial document created for UID ${currentUserId}, game ${currentGameType} in Firestore.")
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
        viewModelScope.launch {
            // No necesitas hacer nada especial aquí si saveHighScore() se llama en onCarPassed
        }
    }

    fun resetGame() {
        score.value = 0
        speed.value = 5f
    }

    private fun saveHighScore() {
        viewModelScope.launch {
            val userIdToSave = currentUserId
            val gameTypeToSave = currentGameType
            val userNameToSave = currentUserName
            val currentHighScore = highScore.value
            if (userIdToSave.isNotEmpty() && gameTypeToSave.isNotEmpty()) {
                val scoreToSave = Score(userId = userIdToSave, username = userNameToSave, gameType = gameTypeToSave, highScore = currentHighScore)
                try {
                    // ¡MODIFICADO! Nueva ruta: users/{userId}/{gameType}/score
                    db.collection("users").document(userIdToSave)
                        .collection(gameTypeToSave).document("score").set(scoreToSave).await()
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