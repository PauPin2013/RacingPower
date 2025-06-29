package com.example.racingpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlayerScore(
    @PrimaryKey val username: String,
    val highScore: Int,
    val latestScore: Int
)