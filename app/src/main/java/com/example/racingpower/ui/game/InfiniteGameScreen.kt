package com.racingpower.ui.game

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
<<<<<<< HEAD
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


=======
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.racingpower.R
import kotlinx.coroutines.delay
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1

@Composable
fun InfiniteGameScreen(
    username: String,
    viewModel: InfiniteGameViewModel
) {
    val context = LocalContext.current
<<<<<<< HEAD

=======
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

<<<<<<< HEAD
    val carSize = 60f
=======
    val carSize = 80f // Más grande que antes, pero ajustado al carril
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    val laneCount = 3
<<<<<<< HEAD
    val playerY = 700f
    var playerLane by remember { mutableStateOf(1) } // Carril central
=======
    var playerLane by remember { mutableStateOf(1) }
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    var isGameOver by remember { mutableStateOf(false) }
    var lineOffset by remember { mutableStateOf(0f) }

    val playerCar = ImageBitmap.imageResource(id = R.drawable.car_blue)
    val enemyCar = ImageBitmap.imageResource(id = R.drawable.car_red)

<<<<<<< HEAD
    // Iniciar lógica del juego
=======
    var crashPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music) }

    // Controlar sonido de fondo según estado del juego
    LaunchedEffect(isGameOver) {
        if (!isGameOver) {
            if (!backgroundPlayer.isPlaying) {
                backgroundPlayer.isLooping = true
                backgroundPlayer.start()
            }
        } else {
            if (backgroundPlayer.isPlaying) {
                backgroundPlayer.pause()
                backgroundPlayer.seekTo(0)
            }
        }
    }

    // Ciclo principal del juego
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
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

<<<<<<< HEAD
                // Generar enemigos desde arriba
                if (enemies.size < 2) {
                    val lane = lanePositions.random()
                    enemies = enemies + Offset(lane, -100f)
                }

                // Mover enemigos hacia abajo
                enemies = enemies.map { it.copy(y = it.y + speed) }

                // Enemigos pasados
=======
                if (enemies.size < 2) {
                    val lane = lanePositions.random()
                    enemies = enemies + Offset(lane, -carSize)
                }

                enemies = enemies.map { it.copy(y = it.y + speed) }

>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() }
                    enemies = enemies - passed.toSet()
                }

<<<<<<< HEAD
                // Verificar colisiones
=======
                val playerY = canvasHeight - carSize - 16f
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
                val playerX = lanePositions[playerLane]
                if (enemies.any {
                        it.x == playerX &&
                                it.y <= playerY + carSize &&
                                it.y + carSize >= playerY
<<<<<<< HEAD
                    }
                ) {
=======
                    }) {
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
                    isGameOver = true
                    viewModel.gameOver()
                    crashPlayer?.release()
                    crashPlayer = MediaPlayer.create(context, R.raw.crash_sound)
                    crashPlayer?.start()
                }
            }
            delay(16L) // 60fps
        }
    }

<<<<<<< HEAD
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
=======
    DisposableEffect(Unit) {
        onDispose {
            backgroundPlayer.stop()
            backgroundPlayer.release()
            crashPlayer?.release()
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
        }
    }

<<<<<<< HEAD
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
=======
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(3f)
                .background(Color.DarkGray)
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
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        canvasWidth = size.width.toFloat()
                        canvasHeight = size.height.toFloat()
                    }
            ) {
                val laneWidth = size.width / laneCount
                val carSize = 110f
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSize / 2
                }

<<<<<<< HEAD
                // Fondo carretera
                drawRect(
                    color = Color.DarkGray,
                    size = Size(size.width, size.height)
                )

                // Líneas divisorias
=======
                drawRect(Color.DarkGray, size = Size(size.width, size.height))

>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
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

<<<<<<< HEAD
                // Jugador
=======
                val playerY = size.height - carSize - 16f
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
                drawImage(
                    image = playerCar,
                    dstOffset = IntOffset(lanePositions[playerLane].toInt(), playerY.toInt()),
                    dstSize = IntSize(carSize.toInt(), carSize.toInt())
                )

<<<<<<< HEAD
                // Enemigos
=======
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
                enemies.forEach { enemy ->
                    drawImage(
                        image = enemyCar,
                        dstOffset = IntOffset(enemy.x.toInt(), enemy.y.toInt()),
                        dstSize = IntSize(carSize.toInt(), carSize.toInt())
                    )
                }
            }

            if (isGameOver) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Game Over", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isGameOver = false
                            viewModel.resetGame()
                            enemies = emptyList()
                            crashPlayer?.release()
                            crashPlayer = null
                            Toast.makeText(context, "¡Buena suerte!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Reiniciar")
                    }
                }
            }
        }

<<<<<<< HEAD
        // Botón de reinicio
        if (isGameOver) {
            Button(
                onClick = {
                    isGameOver = false
                    viewModel.resetGame()
                    enemies = emptyList()
                    Toast.makeText(context, "¡Buena suerte!", Toast.LENGTH_SHORT).show()
                },
=======
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF1B2A49))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = username, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "High Score", color = Color.White)
                Text(text = "$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Latest Game", color = Color.White)
                Text(text = "$score", color = Color.White)
            }

            Button(
                onClick = { /* Acción para editar */ },
>>>>>>> 168c170cca640e3ccf5df114f53b6261b0e185a1
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("EDIT")
            }
        }
    }
}
