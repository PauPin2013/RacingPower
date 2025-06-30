package com.example.racingpower.models

import androidx.room.Entity

@Entity(primaryKeys = ["username", "gameType"]) // Clave primaria compuesta
data class PlayerScore(
    val username: String,
    val gameType: String, // "cars" or "planes"
    val highScore: Int,
    val latestScore: Int
)