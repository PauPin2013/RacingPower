package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.LeaderboardEntry
import com.example.racingpower.models.UserProfile // ¡IMPORTA UserProfile!
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db: FirebaseFirestore = Firebase.firestore

    val leaderboardEntries = mutableStateListOf<LeaderboardEntry>()
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Carga los datos de la tabla de clasificación para un tipo de juego específico.
     * Ahora utiliza el modelo UserProfile.
     * @param gameType El tipo de juego ("cars", "planes", "boats").
     */
    fun loadLeaderboard(gameType: String) {
        isLoading.value = true
        errorMessage.value = null
        leaderboardEntries.clear()

        viewModelScope.launch {
            try {
                val usersSnapshot = db.collection("users")
                    .get()
                    .await()

                val tempEntries = mutableListOf<LeaderboardEntry>()

                for (document in usersSnapshot.documents) {
                    // Convertir el documento directamente a un objeto UserProfile
                    val userProfile = document.toObject(UserProfile::class.java)

                    // Solo procesar si el perfil se deserializó correctamente y tiene un nombre de visualización
                    if (userProfile != null && userProfile.displayName.isNotBlank()) {
                        val gameStats = userProfile.getGameStats(gameType)
                        val highScore = gameStats.highScore

                        // Solo añadir si el highScore es mayor que 0
                        if (highScore > 0) {
                            tempEntries.add(LeaderboardEntry(userProfile.displayName, highScore))
                        }
                    } else {
                        Log.w("LeaderboardViewModel", "Skipping user document ${document.id} due to invalid UserProfile or missing display name.")
                    }
                }

                val sortedEntries = tempEntries.sortedByDescending { it.score }
                leaderboardEntries.addAll(sortedEntries)

                Log.d("LeaderboardViewModel", "Leaderboard loaded successfully for $gameType. Found ${leaderboardEntries.size} entries.")

            } catch (e: Exception) {
                errorMessage.value = "Error loading leaderboard: ${e.message}"
                Log.e("LeaderboardViewModel", "Error loading leaderboard for $gameType: ${e.message}", e)
            } finally {
                isLoading.value = false
            }
        }
    }
}