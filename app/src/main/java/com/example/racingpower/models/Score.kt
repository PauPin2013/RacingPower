package com.example.racingpower.models

// Removed Room imports as they are no longer needed for this model.
// No androidx.room.Entity, androidx.room.PrimaryKey, etc.

data class Score(
    val userId: String = "", // Ahora usaremos userId para el UID
    val username: String = "", // Este será el displayName del usuario
    val gameType: String = "",
    val highScore: Int = 0
) {
    constructor() : this("", "", "", 0)
}