package com.example.racingpower.models

// No se necesitan importaciones de Room aquí

data class Score(
    val username: String = "",
    val highScore: Int = 0
) {
    // Constructor sin argumentos requerido por Firestore para la deserialización
    // Firebase usa la reflexión para crear instancias, por lo que necesita un constructor público sin argumentos.
    constructor() : this("", 0)
}
