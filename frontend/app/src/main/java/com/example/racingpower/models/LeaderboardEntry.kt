package com.example.racingpower.models

// Clase de datos que representa una única entrada (fila) en la tabla de clasificación.
data class LeaderboardEntry(
    // Nombre de usuario del jugador para esta entrada.
    val username: String = "N/A",
    // Puntuación del jugador para esta entrada.
    val score: Int = 0
)