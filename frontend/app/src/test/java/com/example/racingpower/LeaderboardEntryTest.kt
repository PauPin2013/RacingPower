package com.example.racingpower.models

// No se requieren importaciones de librerías de prueba (JUnit, Kotlin Test)

object LeaderboardEntryTest { // Usamos un 'object' para tener una función main
    @JvmStatic
    fun main() {
        println("--- Ejecutando prueba para LeaderboardEntry ---")

        // Prueba 1: Valores por defecto
        val entry1 = LeaderboardEntry()
        val expectedUsername1 = "N/A"
        val expectedScore1 = 0

        if (entry1.username == expectedUsername1 && entry1.score == expectedScore1) {
            println("✅ Prueba 'valores por defecto': PASADA. Username: '${entry1.username}', Score: ${entry1.score}.")
        } else {
            println("❌ Prueba 'valores por defecto': FALLIDA. Se esperaba Username: '$expectedUsername1', Score: $expectedScore1. Se obtuvo Username: '${entry1.username}', Score: ${entry1.score}.")
        }

        // Prueba 2: Valores personalizados
        val customUsername = "JugadorEjemplo"
        val customScore = 1500
        val entry2 = LeaderboardEntry(username = customUsername, score = customScore)

        if (entry2.username == customUsername && entry2.score == customScore) {
            println("✅ Prueba 'valores personalizados': PASADA. Username: '${entry2.username}', Score: ${entry2.score}.")
        } else {
            println("❌ Prueba 'valores personalizados': FALLIDA. Se esperaba Username: '$customUsername', Score: $customScore. Se obtuvo Username: '${entry2.username}', Score: ${entry2.score}.")
        }

        println("--- Fin de prueba para LeaderboardEntry ---")
    }
}