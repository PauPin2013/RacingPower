// InfiniteGameScreen.kt
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.ImageBitmap
import com.example.racingpower.R
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.imageResource

@Composable
fun InfiniteGameScreen(
    username: String,
    viewModel: InfiniteGameViewModel
) {
    val context = LocalContext.current
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

    val carSize = 60
    val laneCount = 3
    val laneWidth = screenWidth / laneCount

    var playerLane by remember { mutableStateOf(1) } // 0 izquierda, 1 centro, 2 derecha
    val playerY = 700f

    var enemies by remember { mutableStateOf(listOf<Pair<Int, Float>>()) } // par de (lane, yPosition)
    var isGameOver by remember { mutableStateOf(false) }

    val playerCar = ImageBitmap.imageResource(id = R.drawable.car_blue)
    val enemyCar = ImageBitmap.imageResource(id = R.drawable.car_red)

    LaunchedEffect(Unit) {
        viewModel.startGame(username)
        while (true) {
            if (!isGameOver) {
                // Añadir enemigos si hay menos de 2
                if (enemies.size < 2) {
                    val lane = (0 until laneCount).random()
                    enemies = enemies + (lane to -100f)
                }

                // Mover enemigos
                enemies = enemies.map { (lane, y) -> lane to (y + speed) }

                // Detectar paso
                val passed = enemies.filter { (_, y) -> y > 800f }
                if (passed.isNotEmpty()) {
                    viewModel.onCarPassed()
                    enemies = enemies - passed.toSet()
                }

                // Colisión
                val playerX = laneWidth * playerLane + laneWidth / 2 - carSize / 2
                if (enemies.any { (lane, y) ->
                        val enemyX = laneWidth * lane + laneWidth / 2 - carSize / 2
                        enemyX < playerX + carSize && enemyX + carSize > playerX &&
                                y + carSize >= playerY && y <= playerY + carSize
                    }) {
                    isGameOver = true
                    viewModel.gameOver()
                }
            }
            delay(16L)
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

        Box(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Fondo
                drawRect(
                    color = Color.DarkGray,
                    size = Size(canvasWidth, canvasHeight)
                )

                // Líneas de carril
                for (i in 1 until laneCount) {
                    val x = laneWidth * i
                    drawLine(
                        color = Color.White,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 4f,
                        alpha = 0.3f
                    )
                }

                // Jugador
                val playerX = laneWidth * playerLane + laneWidth / 2 - carSize / 2
                drawImage(
                    image = playerCar,
                    dstOffset = IntOffset(playerX.toInt(), playerY.toInt()),
                    dstSize = IntSize(carSize, carSize)
                )

                // Enemigos
                enemies.forEach { (lane, y) ->
                    val enemyX = laneWidth * lane + laneWidth / 2 - carSize / 2
                    drawImage(
                        image = enemyCar,
                        dstOffset = IntOffset(enemyX.toInt(), y.toInt()),
                        dstSize = IntSize(carSize, carSize)
                    )
                }
            }
        }

        if (isGameOver) {
            Button(
                onClick = {
                    isGameOver = false
                    viewModel.resetGame()
                    enemies = emptyList()
                    Toast.makeText(context, "\u00a1Buena suerte!", Toast.LENGTH_SHORT).show()
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
