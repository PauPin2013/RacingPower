package com.example.racingpower.models

import com.google.firebase.firestore.Exclude

data class UserProfile(
    val userId: String = "",
    val displayName: String = "", // Nombre de usuario registrado
    val avatarName: String = "avatar1", // Nombre del recurso del avatar (ej. "avatar1")
    val cars: GameStats = GameStats(), // Estadísticas para el juego de coches
    val planes: GameStats = GameStats(), // Estadísticas para el juego de aviones
    val boats: GameStats = GameStats() // Estadísticas para el juego de botes (¡Asegúrate de que esta línea esté!)
) {
    // Constructor sin argumentos para que Firestore pueda deserializar
    constructor() : this("", "", "avatar1", GameStats(), GameStats(), GameStats())

    // Función auxiliar para obtener las GameStats de un tipo de juego específico
    fun getGameStats(gameType: String): GameStats {
        return when (gameType) {
            "cars" -> cars
            "planes" -> planes
            "boats" -> boats // Añadido para botes
            else -> GameStats() // Retorna un objeto GameStats vacío si el tipo no es reconocido
        }
    }
}