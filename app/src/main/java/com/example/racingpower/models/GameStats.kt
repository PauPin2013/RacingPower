package com.example.racingpower.models

// Este modelo representa los datos que irán dentro de los mapas 'cars' o 'planes'
// (es decir, { highScore: 12345 } )
data class GameStats(
    val highScore: Int = 0
    // Si más adelante quieres añadir la fecha de la última partida, la pondrías aquí:
    // val lastPlayed: String = ""
) {
    // Constructor sin argumentos para que Firestore pueda deserializar
    constructor() : this(0)
}