// InfiniteGameScreen.kt
package com.racingpower.ui.game

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun InfiniteGameScreen(
    username: String,
    viewModel: InfiniteGameViewModel
) {
    val context = LocalContext.current

    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

    // Estados de juego
    var playerX by remember { mutableStateOf(200f) }
    val playerY = 700f
    val playerSize = 80f

    // Lista de obstáculos (autos)
    var enemies by remember { mutableStateOf(listOf<Offset>()) }

    // Estado de “game over”
    var isGameOver by remember { mutableStateOf(false) }

    // Arranca el juego al componer
    LaunchedEffect(Unit) {
        viewModel.startGame(username)
        // Loop de creación y movimiento de enemigos
        while (true) {
            if (!isGameOver) {
                // Generar un nuevo enemigo arriba cada cierto tiempo
                if (enemies.size < 5) {
                    val x = listOf(50f, 200f, 350f).random()
                    enemies = enemies + Offset(x, -100f)
                }
                // Moverlos y detectar eventos
                enemies = enemies.map { it.copy(y = it.y + speed) }
                // Detectar paso y colisión
                val passed = enemies.filter { it.y > 800f }
                if (passed.isNotEmpty()) {
                    passed.forEach { _ -> viewModel.onCarPassed() }
                    enemies = enemies - passed.toSet()
                }
                // Colisión: rect overlap
                if (enemies.any { enemy ->
                        enemy.x in playerX..(playerX + playerSize) &&
                                enemy.y + playerSize >= playerY &&
                                enemy.y <= playerY + playerSize
                    }
                ) {
                    isGameOver = true
                    viewModel.gameOver()
                }
            }
            delay(16L) // ~60 FPS
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .focusable()      // para recibir KeyEvents
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft  -> playerX = (playerX - 100f).coerceAtLeast(0f)
                        Key.DirectionRight -> playerX = (playerX + 100f).coerceAtMost(400f)
                        else -> {}
                    }
                }
                true
            }
    ) {
        // Barra de información
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Usuario: $username", color = Color.White)
            Text("Puntaje: $score", color = Color.White)
            Text("Mejor: $highScore", color = Color.White)
        }

        // Zona de juego
        Box(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dibuja al jugador
                drawRect(
                    color = Color.Cyan,
                    topLeft = Offset(playerX, playerY),
                    size = androidx.compose.ui.geometry.Size(playerSize, playerSize)
                )
                // Dibuja enemigos
                enemies.forEach { enemy ->
                    drawRect(
                        color = Color.Red,
                        topLeft = enemy,
                        size = androidx.compose.ui.geometry.Size(playerSize, playerSize)
                    )
                }
            }
        }

        // Botón reiniciar si hubo game over
        if (isGameOver) {
            Button(
                onClick = {
                    isGameOver = false
                    viewModel.resetGame()
                    enemies = emptyList()
                    Toast.makeText(context, "¡Buena suerte!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reiniciar")
            }
        }
    }
}
