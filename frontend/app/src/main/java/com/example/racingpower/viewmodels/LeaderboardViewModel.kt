package com.example.racingpower.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingpower.models.LeaderboardEntry
import com.example.racingpower.models.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel para la gestión y visualización de las tablas de clasificación de los juegos.
class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    // Instancia de Firebase Firestore para interactuar con la base de datos.
    private val db: FirebaseFirestore = Firebase.firestore

    // Lista mutable de entradas de la tabla de clasificación para ser observada por la UI.
    val leaderboardEntries = mutableStateListOf<LeaderboardEntry>()
    // Estado que indica si los datos de la tabla de clasificación están siendo cargados.
    val isLoading = mutableStateOf(false)
    // Mensaje de error, si ocurre alguno durante la carga de la tabla de clasificación.
    val errorMessage = mutableStateOf<String?>(null)

    // Carga los datos de la tabla de clasificación para un tipo de juego específico desde Firestore.
    // Los datos se obtienen de los perfiles de usuario y se ordenan por puntuación.
    // @param gameType El tipo de juego para el que se cargará la tabla de clasificación ("cars", "planes", "boats").
    fun loadLeaderboard(gameType: String) {
        isLoading.value = true
        errorMessage.value = null
        leaderboardEntries.clear()

        viewModelScope.launch {
            try {
                // Obtiene todos los documentos de la colección "users".
                val usersSnapshot = db.collection("users")
                    .get()
                    .await()

                val tempEntries = mutableListOf<LeaderboardEntry>()

                // Itera sobre cada documento de usuario.
                for (document in usersSnapshot.documents) {
                    // Intenta convertir el documento de Firestore a un objeto UserProfile.
                    val userProfile = document.toObject(UserProfile::class.java)

                    // Procesa el perfil solo si se deserializó correctamente y tiene un nombre de visualización válido.
                    if (userProfile != null && userProfile.displayName.isNotBlank()) {
                        // Obtiene las estadísticas de juego para el tipo de juego especificado.
                        val gameStats = userProfile.getGameStats(gameType)
                        val highScore = gameStats.highScore

                        // Añade una entrada a la lista temporal solo si la puntuación más alta es mayor que 0.
                        if (highScore > 0) {
                            tempEntries.add(LeaderboardEntry(userProfile.displayName, highScore))
                        }
                    } else {
                        // Registra una advertencia si el perfil del usuario es inválido o le falta el nombre de visualización.
                        Log.w("LeaderboardViewModel", "Skipping user document ${document.id} due to invalid UserProfile or missing display name.")
                    }
                }

                // Ordena las entradas de la tabla de clasificación de forma descendente por puntuación.
                val sortedEntries = tempEntries.sortedByDescending { it.score }
                // Añade las entradas ordenadas a la lista observable.
                leaderboardEntries.addAll(sortedEntries)

                Log.d("LeaderboardViewModel", "Leaderboard loaded successfully for $gameType. Found ${leaderboardEntries.size} entries.")

            } catch (e: Exception) {
                // Captura y registra cualquier error que ocurra durante la carga de la tabla de clasificación.
                errorMessage.value = "Error loading leaderboard: ${e.message}"
                Log.e("LeaderboardViewModel", "Error loading leaderboard for $gameType: ${e.message}", e)
            } finally {
                // Restablece el estado de carga una vez que la operación ha finalizado.
                isLoading.value = false
            }
        }
    }
}