package com.example.racingpower.models

// Clase de datos que representa las estadísticas de un juego.
// Utilizada para almacenar información como la puntuación más alta en Firestore.
data class GameStats(
    // Puntuación más alta alcanzada en el juego.
    val highScore: Int = 0
) {
    // Constructor secundario sin argumentos requerido por Firestore para la deserialización de objetos.
    constructor() : this(0)
}