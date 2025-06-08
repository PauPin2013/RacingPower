package com.racingpower.ui.game

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.imageResource
import com.example.racingpower.R
import androidx.compose.ui.layout.onSizeChanged



@Composable
fun InfiniteGameScreen(
    username: String,
    viewModel: InfiniteGameViewModel
) {
    val context = LocalContext.current

    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

    val carSize = 60f
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    val laneCount = 3
    val playerY = 700f
    var playerLane by remember { mutableStateOf(1) } // Carril central
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    var isGameOver by remember { mutableStateOf(false) }
    var lineOffset by remember { mutableStateOf(0f) }

    val playerCar = ImageBitmap.imageResource(id = R.drawable.car_blue)
    val enemyCar = ImageBitmap.imageResource(id = R.drawable.car_red)

    // Iniciar lógica del juego
    LaunchedEffect(Unit) {
        viewModel.startGame(username)
        while (true) {
            if (!isGameOver) {
                lineOffset += 10f
                if (lineOffset >= 40f) lineOffset = 0f

                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSize / 2
                }

                // Generar enemigos desde arriba
                if (enemies.size < 2) {
                    val lane = lanePositions.random()
                    enemies = enemies + Offset(lane, -100f)
                }

                // Mover enemigos hacia abajo
                enemies = enemies.map { it.copy(y = it.y + speed) }

                // Enemigos pasados
                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() }
                    enemies = enemies - passed.toSet()
                }

                // Verificar colisiones
                val playerX = lanePositions[playerLane]
                if (enemies.any {
                        it.x == playerX &&
                                it.y <= playerY + carSize &&
                                it.y + carSize >= playerY
                    }
                ) {
                    isGameOver = true
                    viewModel.gameOver()
                }
            }
            delay(16L) // 60fps
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> playerLane = (playerLane - 1).coerceAtLeast(0)
                        Key.DirectionRight -> playerLane = (playerLane + 1).coerceAtMost(laneCount - 1)
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
            Canvas(modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    canvasWidth = size.width.toFloat()
                    canvasHeight = size.height.toFloat()
                }
            ) {
                val laneWidth = size.width / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSize / 2
                }

                // Fondo carretera
                drawRect(
                    color = Color.DarkGray,
                    size = Size(size.width, size.height)
                )

                // Líneas divisorias
                for (i in 1 until laneCount) {
                    val x = laneWidth * i
                    var y = -40f + lineOffset
                    while (y < size.height) {
                        drawLine(
                            color = Color.White,
                            start = Offset(x, y),
                            end = Offset(x, y + 20f),
                            strokeWidth = 4f,
                            alpha = 0.4f
                        )
                        y += 40f
                    }
                }

                // Jugador
                drawImage(
                    image = playerCar,
                    dstOffset = IntOffset(lanePositions[playerLane].toInt(), playerY.toInt()),
                    dstSize = IntSize(carSize.toInt(), carSize.toInt())
                )

                // Enemigos
                enemies.forEach { enemy ->
                    drawImage(
                        image = enemyCar,
                        dstOffset = IntOffset(enemy.x.toInt(), enemy.y.toInt()),
                        dstSize = IntSize(carSize.toInt(), carSize.toInt())
                    )
                }
            }
        }

        // Botón de reinicio
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
