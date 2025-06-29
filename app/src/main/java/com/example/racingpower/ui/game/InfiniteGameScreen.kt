package com.example.racingpower.ui.game

import android.app.Application
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.input.pointer.pointerInput
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth // Importa la extensión ktx para FirebaseAuth
import com.google.firebase.auth.FirebaseAuth


@Composable
fun InfiniteGameScreen(
    userId: String // Ahora recibe el userId
) {
    val context = LocalContext.current
    val viewModel: InfiniteGameViewModel = remember {
        InfiniteGameViewModel(context.applicationContext as Application)
    }

    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

    val carSize = 80f
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    val laneCount = 3
    var playerLane by remember { mutableStateOf(1) }
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    var isGameOver by remember { mutableStateOf(false) }
    var lineOffset by remember { mutableStateOf(0f) }

    val playerCar = ImageBitmap.imageResource(id = R.drawable.car_blue)
    val enemyCar = ImageBitmap.imageResource(id = R.drawable.car_red)

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
    LaunchedEffect(Unit) {
        viewModel.startGame(userId) // Pasa el userId al ViewModel
        while (true) {
            if (!isGameOver) {
                lineOffset += 10f
                if (lineOffset >= 40f) lineOffset = 0f

                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSize / 2
                }

                if (enemies.size < 2) {
                    val lane = lanePositions.random()
                    enemies = enemies + Offset(lane, -carSize)
                }

                enemies = enemies.map { it.copy(y = it.y + speed) }

                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() }
                    enemies = enemies - passed.toSet()
                }

                val playerY = canvasHeight - carSize - 16f
                val playerX = lanePositions[playerLane]
                if (enemies.any {
                        it.x == playerX &&
                                it.y <= playerY + carSize &&
                                it.y + carSize >= playerY
                    }) {
                    isGameOver = true
                    viewModel.gameOver()
                    crashPlayer?.release()
                    crashPlayer = MediaPlayer.create(context, R.raw.crash_sound)
                    crashPlayer?.start()
                }
            }
            delay(16L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            backgroundPlayer.stop()
            backgroundPlayer.release()
            crashPlayer?.release()
        }
    }

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
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount

                            if (totalDrag > 100f) { // deslizó a la derecha
                                playerLane = (playerLane + 1).coerceAtMost(laneCount - 1)
                                totalDrag = 0f
                            } else if (totalDrag < -100f) { // deslizó a la izquierda
                                playerLane = (playerLane - 1).coerceAtLeast(0)
                                totalDrag = 0f
                            }
                        },
                        onDragEnd = {
                            totalDrag = 0f // Reiniciar al soltar el dedo
                        },
                        onDragCancel = {
                            totalDrag = 0f
                        }
                    )
                }
        ){
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
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSize / 2
                }

                drawRect(Color.DarkGray, size = Size(size.width, size.height))

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

                val playerY = size.height - carSize - 16f
                drawImage(
                    image = playerCar,
                    dstOffset = IntOffset(lanePositions[playerLane].toInt(), playerY.toInt()),
                    dstSize = IntSize(carSize.toInt(), carSize.toInt())
                )

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

        // Sidebar con información del usuario
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
                // Mostrar el userId o parte de él
                Text(text = "User ID: ${userId.take(8)}...", color = Color.White) // Muestra solo los primeros 8 caracteres
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "High Score", color = Color.White)
                Text(text = "$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Latest Game", color = Color.White)
                Text(text = "$score", color = Color.White)
            }

            // Botón de Cerrar Sesión
            Button(
                onClick = {
                    val auth: FirebaseAuth = Firebase.auth
                    auth.signOut() // Cierra la sesión del usuario
                    Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                    (context as? ComponentActivity)?.finish() // Cierra la actividad para forzar el re-inicio
                    // Opcional: Navegar de vuelta a la pantalla de login si quieres
                    // navController.navigate("login_screen") { popUpTo(0) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Text("Cerrar Sesión")
            }
        }
    }
}
