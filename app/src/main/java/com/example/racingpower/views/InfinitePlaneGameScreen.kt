package com.example.racingpower.views

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InfinitePlaneGameScreen(
    username: String,
    viewModel: InfiniteGameViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed
    val planeSize = 120f
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    val laneCount = 3
    var playerLane by remember { mutableStateOf(1) }
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    var isGameOver by remember { mutableStateOf(false) }
    var waveOffset by remember { mutableStateOf(0f) }

    var isJumping by remember { mutableStateOf(false) }
    var jumpOffset by remember { mutableStateOf(0f) }
    var jumpsLeft by remember { mutableStateOf(10) }
    val jumpHeight = 100f
    val jumpDuration = 1600L
    val scope = rememberCoroutineScope()

    val playerPlane = ImageBitmap.imageResource(id = R.drawable.plane_blue)
    val enemyPlane = ImageBitmap.imageResource(id = R.drawable.plane_red)
    var crashPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music2) }

    val firebaseAuth: FirebaseAuth = Firebase.auth
    val currentUserDisplayName = firebaseAuth.currentUser?.displayName ?: "Invitado"

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

    LaunchedEffect(Unit) {
        viewModel.startGame(username, "planes", currentUserDisplayName)
        while (true) {
            if (!isGameOver) {
                waveOffset += 5f
                if (waveOffset >= 60f) waveOffset = 0f
                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - planeSize / 2
                }

                if (enemies.size < 2) {
                    val lane = lanePositions.random()
                    enemies = enemies + Offset(lane, -planeSize)
                }

                enemies = enemies.map { it.copy(y = it.y + speed) }

                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() }
                    enemies = enemies - passed.toSet()
                }

                val playerY = canvasHeight - planeSize - 16f - jumpOffset
                val playerX = lanePositions[playerLane]

                if (!isJumping && enemies.any {
                        it.x == playerX &&
                                it.y <= playerY + planeSize &&
                                it.y + planeSize >= playerY
                    }) {
                    isGameOver = true
                    viewModel.gameOver()
                    crashPlayer?.release()
                    crashPlayer = MediaPlayer.create(context, R.raw.crash_sound2)
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
                .background(Color(0xFF001F3F))
                .focusable()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                            if (totalDrag > 100f) {
                                playerLane = (playerLane + 1).coerceAtMost(laneCount - 1)
                                totalDrag = 0f
                            } else if (totalDrag < -100f) {
                                playerLane = (playerLane - 1).coerceAtLeast(0)
                                totalDrag = 0f
                            }
                        },
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f }
                    )
                }

                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!isJumping && jumpsLeft > 0) {
                            isJumping = true
                            jumpOffset = jumpHeight
                            jumpsLeft--
                            scope.launch {
                                delay(jumpDuration)
                                jumpOffset = 0f
                                isJumping = false
                            }
                        }
                    }
                }
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
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - planeSize / 2
                }

                drawRect(Color(0xFF001F3F), size = Size(size.width, size.height))

                for (waveLine in 0..2) {
                    var y = waveOffset + waveLine * 20
                    while (y < size.height) {
                        drawLine(
                            color = Color.Cyan.copy(alpha = 0.2f + 0.1f * waveLine),
                            start = Offset(0f, y),
                            end = Offset(size.width, y + 10f),
                            strokeWidth = 4f
                        )
                        y += 60f
                    }
                }

                val playerY = size.height - planeSize - 16f - jumpOffset
                drawImage(
                    image = playerPlane,
                    dstOffset = IntOffset(lanePositions[playerLane].toInt(), playerY.toInt()),
                    dstSize = IntSize(planeSize.toInt(), planeSize.toInt())
                )

                enemies.forEach { enemy ->
                    drawImage(
                        image = enemyPlane,
                        dstOffset = IntOffset(enemy.x.toInt(), enemy.y.toInt()),
                        dstSize = IntSize(planeSize.toInt(), planeSize.toInt())
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
                    Text("¡Avión derribado!", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isGameOver = false
                            viewModel.resetGame()
                            enemies = emptyList()
                            crashPlayer?.release()
                            crashPlayer = null
                            jumpsLeft = 10
                            Toast.makeText(context, "¡Vuelve a despegar!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Reiniciar")
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
                Text(text = "Usuario: $currentUserDisplayName", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "High Score", color = Color.White)
                Text(text = "$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Puntaje Actual", color = Color.White)
                Text(text = "$score", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Saltos restantes", color = Color.White)
                Text(text = "$jumpsLeft", color = Color.White)
            }

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(alpha = 0.7f))
            ) {
                Text("Volver")
            }
        }
    }
}
