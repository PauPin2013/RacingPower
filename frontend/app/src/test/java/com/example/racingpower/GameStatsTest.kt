package com.example.racingpower.models

// No se requieren importaciones de librerías de prueba (JUnit, Kotlin Test)

object GameStatsTest { // Usamos un 'object' para tener una función main
    @JvmStatic // Permite que sea llamada como un método estático desde Java, útil para algunas configuraciones
    fun main() {
        println("--- Ejecutando prueba para GameStats ---")

        val gameStats = GameStats()

        // Verificación 1: highScore por defecto
        val expectedHighScore = 0
        if (gameStats.highScore == expectedHighScore) {
            println("✅ Prueba 'highScore por defecto': PASADA. Se inicializó con $expectedHighScore.")
        } else {
            println("❌ Prueba 'highScore por defecto': FALLIDA. Se esperaba $expectedHighScore pero se obtuvo ${gameStats.highScore}.")
        }

        println("--- Fin de prueba para GameStats ---")
    }
}