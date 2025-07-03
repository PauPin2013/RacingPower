package com.example.racingpower.views

import android.app.Application
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image // Importa Image para mostrar el avatar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.racingpower.utils.LocaleHelper
import kotlin.math.sin

// Importa FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestore

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

    var isJumping by remember { mutableStateOf(0) }
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
    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    val currentUserDisplayName = firebaseAuth.currentUser?.displayName ?: guestDisplayName

    // Estado para el avatar del usuario
    var avatarResId by remember { mutableStateOf(R.drawable.avatar1) } // Avatar predeterminado

    // Cargar avatar desde Firestore
    LaunchedEffect(username) {
        if (username != "guest_user") { // Solo carga si no es un usuario invitado
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(username).get().addOnSuccessListener { doc ->
                val resId = (doc.get("avatarResId") as? Long)?.toInt()
                if (resId != null) avatarResId = resId
            }
        }
    }


    val planeDownText = stringResource(id = R.string.plane_down_text)
    val restartButtonText = stringResource(id = R.string.restart_button_text)
    val takeOffAgainToastText = stringResource(id = R.string.take_off_again_toast)
    val userDisplayLabelFormat = stringResource(id = R.string.user_display_label)
    val highScoreLabel = stringResource(id = R.string.high_score_label)
    val currentScoreLabel = stringResource(id = R.string.current_score_label)
    val backButtonText = stringResource(id = R.string.back_button_text)
    val jumpsRemainingLabel = stringResource(id = R.string.jumps_remaining_label)

    val birds by remember {
        mutableStateOf(List(30) {
            Offset(
                x = (it * 50 + (it * 7) % 100).toFloat(),
                y = ((it * 80) % 1000).toFloat()
            )
        })
    }
    val clouds by remember {
        mutableStateOf(List(20) {
            Offset(
                x = (it * 100 + (it * 17) % 150).toFloat(),
                y = ((it * 90) % 1500).toFloat()
            )
        })
    }

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
                waveOffset += 0.8f
                if (waveOffset >= 120f) waveOffset = 0f
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

                if (isJumping == 0 && enemies.any {
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
                        if (isJumping == 0 && jumpsLeft > 0) {
                            isJumping = 1
                            jumpOffset = jumpHeight
                            jumpsLeft--
                            scope.launch {
                                delay(jumpDuration)
                                jumpOffset = 0f
                                isJumping = 0
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
                    .onSizeChanged {
                        canvasWidth = it.width.toFloat()
                        canvasHeight = it.height.toFloat()
                    }
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF003366), Color(0xFF004C99), Color(0xFF0077BE))
                    ),
                    size = Size(canvasWidth, canvasHeight)
                )

                for (i in 0 until 6) {
                    val x = (i * 160f + waveOffset) % canvasWidth
                    drawLine(
                        color = Color(0xFFFFA726).copy(alpha = 0.08f),
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 60f
                    )
                }

                clouds.forEachIndexed { i, cloud ->
                    val cx = (cloud.x + waveOffset * 0.1f * (i + 1)) % canvasWidth
                    val cy = (cloud.y + 10f * sin((waveOffset + i * 10) / 30f)) % canvasHeight
                    drawCircle(Color.White.copy(alpha = 0.25f), 40f, Offset(cx, cy))
                    drawCircle(Color.White.copy(alpha = 0.25f), 30f, Offset(cx + 30f, cy - 10f))
                    drawCircle(Color.White.copy(alpha = 0.25f), 25f, Offset(cx - 30f, cy - 5f))
                }

                birds.forEachIndexed { i, bird ->
                    val bx = (bird.x + waveOffset * 0.1f * (i + 1)) % canvasWidth
                    val by = (bird.y + 3f * sin((waveOffset + i * 15) / 35f)) % canvasHeight
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(bx - 5f, by), Offset(bx + 5f, by), strokeWidth = 2f)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(bx - 5f, by), Offset(bx - 3f, by - 3f), strokeWidth = 2f)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(bx + 5f, by), Offset(bx + 3f, by - 3f), strokeWidth = 2f)
                }

                val laneWidth = size.width / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - planeSize / 2
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
                    Text(planeDownText, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isGameOver = false
                            viewModel.resetGame()
                            enemies = emptyList()
                            crashPlayer?.release()
                            crashPlayer = null
                            jumpsLeft = 10
                            Toast.makeText(context, takeOffAgainToastText, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(restartButtonText)
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
                // Mostrar el avatar del usuario
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                    contentDescription = "Avatar de usuario",
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Transparent, shape = RoundedCornerShape(50)) // Fondo transparente para la imagen
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(String.format(userDisplayLabelFormat, currentUserDisplayName), color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(highScoreLabel, color = Color.White)
                Text("$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentScoreLabel, color = Color.White)
                Text("$score", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(jumpsRemainingLabel, color = Color.White)
                Text("$jumpsLeft", color = Color.White)
            }

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(alpha = 0.7f))
            ) {
                Text(backButtonText)
            }
        }
    }
}