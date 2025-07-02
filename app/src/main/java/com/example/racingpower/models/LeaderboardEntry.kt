package com.example.racingpower.models

// Este modelo representará una fila en la tabla de clasificación
data class LeaderboardEntry(
    val username: String = "N/A",
    val score: Int = 0
)