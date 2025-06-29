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
    // El username ahora se pasará como userId (el UID de Firebase Auth)
    // Ya no necesitamos un mutableStateOf para el username aquí.
    private var currentUserId: String = "" // Almacenar el UID del usuario actual

    private val db: FirebaseFirestore = Firebase.firestore

    // La función startGame ahora recibe el userId de Firebase Auth
    fun startGame(userId: String) {
        currentUserId = userId // Guarda el UID del usuario
        score.value = 0
        speed.value = 5f

        viewModelScope.launch {
            try {
                // Usa el userId como ID del documento en Firestore
                val docRef = db.collection("scores").document(currentUserId)
                val document = docRef.get().await()

                if (document.exists()) {
                    val savedScore = document.toObject(Score::class.java)
                    savedScore?.let {
                        highScore.value = it.highScore
                        Log.d("InfiniteGameViewModel", "High score cargado desde Firestore para UID ${currentUserId}: ${it.highScore}")
                    }
                } else {
                    highScore.value = 0
                    Log.d("InfiniteGameViewModel", "No se encontró high score para UID ${currentUserId}, inicializando a 0.")
                    // Crea un documento inicial para el usuario si no existe, usando su UID como username
                    val newScore = Score(username = currentUserId, highScore = 0) // Usamos el UID como username en el modelo
                    db.collection("scores").document(currentUserId).set(newScore).await()
                    Log.d("InfiniteGameViewModel", "Documento inicial creado para UID ${currentUserId} en Firestore.")
                }
            } catch (e: Exception) {
                Log.e("InfiniteGameViewModel", "Error al cargar high score desde Firestore para UID ${currentUserId}: ${e.message}")
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
        // La lógica de guardado ya se maneja en onCarPassed()
    }

    fun resetGame() {
        score.value = 0
        speed.value = 5f
    }

    private fun saveHighScore() {
        viewModelScope.launch {
            // Usa el userId que se guardó en currentUserId
            val userIdToSave = currentUserId
            val currentHighScore = highScore.value
            if (userIdToSave.isNotEmpty()) {
                // Guarda el score usando el UID del usuario como el "username" en el modelo Score
                val scoreToSave = Score(userIdToSave, currentHighScore)
                try {
                    db.collection("scores").document(userIdToSave).set(scoreToSave).await()
                    Log.d("InfiniteGameViewModel", "High score guardado en Firestore para UID ${userIdToSave}: $currentHighScore")
                } catch (e: Exception) {
                    Log.e("InfiniteGameViewModel", "Error al guardar high score en Firestore para UID ${userIdToSave}: ${e.message}")
                }
            }
        }
    }
}
