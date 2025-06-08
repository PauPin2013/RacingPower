package com.example.racingpower.views

import android.app.Application
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults // Importación para ButtonDefaults
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
import androidx.compose.ui.unit.sp // Importación para la unidad 'sp'
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.R
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.InfiniteGameViewModel

@Composable
fun InfiniteGameScreen(
    userId: String, // Nuevo: UID del usuario autenticado, requerido para guardar el high score
    displayName: String, // Nuevo: Nombre de visualización del usuario
    onLogout: () -> Unit // Nuevo: Callback para cerrar sesión
) {
    val context = LocalContext.current
    val viewModel: InfiniteGameViewModel = remember {
        InfiniteGameViewModel(context.applicationContext as Application)
    }
    val authViewModel: AuthViewModel = viewModel() // Instancia del AuthViewModel para logout

    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

    val carSize = 80f // Más grande que antes, pero ajustado al carril
    var canvasWidth by remember { mutableStateOf(0f)}
    var canvasHeight by remember {mutableStateOf(0f) }

    val laneCount = 3
    var playerLane by remember { mutableStateOf(1)}
    var enemies by remember { mutableStateOf(listOf<Offset>())}
    var isGameOver by remember { mutableStateOf(false)}
    var lineOffset by remember { mutableStateOf(0f)}

    val playerCar = ImageBitmap.imageResource(id = R.drawable.car_blue) // Asegúrate de tener estas imágenes
    val enemyCar = ImageBitmap.imageResource(id = R.drawable.car_red) // Asegúrate de tener estas imágenes

    var crashPlayer: MediaPlayer? by remember{ mutableStateOf(null) }
    val backgroundPlayer = remember {MediaPlayer.create(context, R.raw.background_music) } // Asegúrate de tener este sonido
    // REMOVIDO: val passedCarPlayer = remember {MediaPlayer.create(context, R.raw.passed_car) } // No solicitado

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
        viewModel.startGame(userId, displayName) // Pasa el UID y el nombre de visualización al ViewModel
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

                enemies = enemies.map {it.copy(y = it.y + speed) }

                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach {
                        viewModel.onCarPassed()
                        // REMOVIDO: Lógica para passedCarPlayer
                    }
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
                    crashPlayer = MediaPlayer.create(context, R.raw.crash_sound) // Sonido de choque
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
            // REMOVIDO: passedCarPlayer.release()
        }
    }

    Row(modifier = Modifier.fillMaxSize())
    {
        Box(
            modifier = Modifier
                .weight(3f)
                .background(Color.DarkGray)
                .focusable()
                .onKeyEvent { keyEvent->
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
                val carSize = 110f // Tamaño del coche para el dibujo
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
                        .background(Color(0xAA000000)), // Fondo semi-transparente oscuro
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("¡Game Over!", color = Color.White, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isGameOver = false
                            viewModel.resetGame()
                            enemies = emptyList()
                            crashPlayer?.release()
                            crashPlayer = null
                            Toast.makeText(context, "¡Buena suerte!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                    ) {
                        Text("Reiniciar", fontSize = 20.sp, color = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF1B2A49))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                // Icono de perfil o avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(50.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Puedes poner un icono aquí o la primera letra del nombre
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "",
                        color = Color.DarkGray,
                        fontSize = 36.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = displayName, color = Color.White, fontSize = 18.sp) // Muestra el display name
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "High Score", color = Color.White, fontSize = 16.sp)
                Text(text = "$highScore", color = Color.White, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Puntaje Actual", color = Color.White, fontSize = 16.sp)
                Text(text = "$score", color = Color.White, fontSize = 24.sp)
            }

            // Botón de cerrar sesión
            Button(
                onClick = onLogout, // Llama al callback de cerrar sesión
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)) // Color para cerrar sesión
            ) {
                Text("Cerrar Sesión", color = Color.White)
            }
        }
    }
}
