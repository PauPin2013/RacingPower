package com.example.racingpower.models

import com.google.firebase.firestore.Exclude

// Clase de datos que representa el perfil de un usuario en la aplicación.
// Contiene información del usuario y sus estadísticas de juego.
data class UserProfile(
    // Identificador único del usuario.
    val userId: String = "",
    // Nombre de usuario visible para el jugador.
    val displayName: String = "",
    // Nombre del recurso de imagen del avatar seleccionado por el usuario.
    val avatarName: String = "avatar1",
    // Estadísticas del juego de coches asociadas a este perfil.
    val cars: GameStats = GameStats(),
    // Estadísticas del juego de aviones asociadas a este perfil.
    val planes: GameStats = GameStats(),
    // Estadísticas del juego de botes asociadas a este perfil.
    val boats: GameStats = GameStats()
) {
    // Constructor secundario sin argumentos requerido por Firestore para la deserialización.
    constructor() : this("", "", "avatar1", GameStats(), GameStats(), GameStats())

    // Función que devuelve las estadísticas de juego para un tipo de juego específico.
    // El tipo de juego se especifica como una cadena (ej. "cars", "planes", "boats").
    fun getGameStats(gameType: String): GameStats {
        return when (gameType) {
            "cars" -> cars
            "planes" -> planes
            "boats" -> boats
            // Devuelve un objeto GameStats vacío si el tipo de juego no coincide.
            else -> GameStats()
        }
    }
}