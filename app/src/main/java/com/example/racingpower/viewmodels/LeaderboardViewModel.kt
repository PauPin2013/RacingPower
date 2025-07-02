package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.LeaderboardEntry
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db: FirebaseFirestore = Firebase.firestore

    // Lista mutable para almacenar las entradas de la tabla de clasificación
    val leaderboardEntries = mutableStateListOf<LeaderboardEntry>()

    // Estado para saber si los datos están cargando
    val isLoading = mutableStateOf(false)

    // Estado para manejar errores (opcional)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Carga los datos de la tabla de clasificación para un tipo de juego específico.
     * @param gameType El tipo de juego ("cars" o "planes").
     */
    fun loadLeaderboard(gameType: String) {
        isLoading.value = true
        errorMessage.value = null
        leaderboardEntries.clear() // Limpia la lista antes de cargar nuevos datos

        viewModelScope.launch {
            try {
                // Obtener todos los documentos de la colección 'users'
                val usersSnapshot = db.collection("users")
                    .get()
                    .await()

                val tempEntries = mutableListOf<LeaderboardEntry>()

                for (document in usersSnapshot.documents) {
                    val username = document.getString("username")
                    // Intentar obtener los datos del campo específico del juego (ej. "cars" o "planes").
                    val gameStatsMap = document.get(gameType) as? Map<String, Any>

                    if (username != null && gameStatsMap != null) {
                        // Usamos Long para leer de Firestore y luego lo convertimos a Int
                        val highScore = (gameStatsMap["highScore"] as? Long)?.toInt() ?: 0
                        // Solo añadir si el highScore es mayor que 0 (para evitar entradas vacías si no hay datos)
                        if (highScore > 0) {
                            tempEntries.add(LeaderboardEntry(username, highScore))
                        }
                    }
                }

                // Ordenar las entradas por highScore de forma descendente
                val sortedEntries = tempEntries.sortedByDescending { it.score }

                // Añadir a la lista observable
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