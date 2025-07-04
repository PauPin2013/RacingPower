package com.example.racingpower.views

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

import androidx.lifecycle.viewmodel.compose.viewModel // Importa viewModel
import com.example.racingpower.viewmodels.AuthViewModel // Importa AuthViewModel
import com.example.racingpower.models.UserProfile // Para el tipo de UserProfile


@Composable
fun InfiniteBoatGameScreen(
    userId: String,
    displayName: String?, // Recibe el nombre a mostrar como parámetro opcional
    viewModel: InfiniteGameViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

    val boatSize = 150f
    val sharkSize = 180f
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    val laneCount = 3
    var playerLane by remember { mutableStateOf(1) }
    var sharks by remember { mutableStateOf(listOf<Pair<Offset, Float>>()) }
    var isGameOver by remember { mutableStateOf(false) }

    val playerBoat = ImageBitmap.imageResource(id = R.drawable.boat_blue)
    val sharkEnemy = ImageBitmap.imageResource(id = R.drawable.shark)

    var crashSound: MediaPlayer? by remember { mutableStateOf(null) }
    val backgroundMusic = remember { MediaPlayer.create(context, R.raw.ocean_music) }

    var wavePhase by remember { mutableStateOf(0f) }
    val fishes by remember {
        mutableStateOf(List(30) {
            Offset(
                x = (0..1000).random().toFloat(),
                y = (50..900).random().toFloat()
            )
        })
    }
    val birds by remember { mutableStateOf(List(4) { Offset((it * 180).toFloat(), (it * 60).toFloat()) }) }

    // --- OBTENER EL NOMBRE DE USUARIO Y AVATAR DEL AUTHVIEWMODEL ---
    val authViewModel: AuthViewModel = viewModel() // Obtén la instancia del AuthViewModel
    // No necesitamos un LaunchedEffect aquí para el perfil, ya que el avatar ya está
    // en el userProfile de AuthViewModel. La GameSelectionScreen ya lo recarga.
    val userProfileState: State<UserProfile?> = authViewModel.userProfile.collectAsState()
    val userProfile = userProfileState.value

    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    // Prioriza el displayName pasado por navegación, luego el de Firestore, luego invitado
    val currentUserDisplayName = displayName?.ifBlank { null }
        ?: userProfile?.displayName?.ifBlank { null }
        ?: guestDisplayName

    val defaultAvatarResId = R.drawable.avatar1
    // Observa el avatarName del userProfile para recomposición
    val currentAvatarResId = remember(userProfile?.avatarName) {
        val avatarName = userProfile?.avatarName ?: "avatar1"
        context.resources.getIdentifier(avatarName, "drawable", context.packageName)
    }.takeIf { it != 0 } ?: defaultAvatarResId
    // --- FIN DEL CAMBIO ---

    val gameOverText = stringResource(id = R.string.game_over_text)
    val restartButtonText = stringResource(id = R.string.restart_button_text)
    val goodLuckToastText = stringResource(id = R.string.good_luck_toast)
    val userDisplayLabelFormat = stringResource(id = R.string.user_display_label)
    val highScoreLabel = stringResource(id = R.string.high_score_label) // ¡CORREGIDO: Usando stringResource!
    val currentScoreLabel = stringResource(id = R.string.current_score_label)
    val backButtonText = stringResource(id = R.string.back_button_text)

    LaunchedEffect(userId, currentUserDisplayName) {
        // En startGame, pasamos el userId como 'username' y el nombre a mostrar como 'displayName'
        viewModel.startGame(userId, "boats", currentUserDisplayName)
        backgroundMusic.isLooping = true
        backgroundMusic.start()
        while (true) {
            if (!isGameOver) {
                wavePhase += 0.2f

                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - sharkSize / 2
                }

                if (sharks.size < 2) {
                    val lane = lanePositions.random()
                    sharks = sharks + (Offset(lane, -sharkSize) to 0f)
                }

                sharks = sharks.map { (pos, phase) ->
                    val newY = pos.y + speed
                    val newX = pos.x + (10f * sin(phase))
                    Offset(newX, newY) to (phase + 0.2f)
                }

                val passed = sharks.filter { it.first.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() }
                    sharks = sharks - passed.toSet()
                }

                val playerY = canvasHeight - boatSize - 16f
                val playerX = (canvasWidth / laneCount) * playerLane + (canvasWidth / laneCount) / 2 - boatSize / 2

                if (sharks.any {
                        val (pos, _) = it
                        pos.x in playerX..(playerX + boatSize) &&
                                pos.y + sharkSize >= playerY &&
                                pos.y <= playerY + boatSize
                    }) {
                    isGameOver = true
                    viewModel.gameOver()
                    backgroundMusic.pause()
                    crashSound?.release()
                    crashSound = MediaPlayer.create(context, R.raw.shark_bite)
                    crashSound?.start()
                }
            }
            delay(16L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            backgroundMusic.stop()
            backgroundMusic.release()
            crashSound?.release()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(3f)
                .pointerInput(Unit) {
                    var dragAmountTotal = 0f
                    detectHorizontalDragGestures { _, dragAmount ->
                        dragAmountTotal += dragAmount
                        if (dragAmountTotal > 100f) {
                            playerLane = (playerLane + 1).coerceAtMost(laneCount - 1)
                            dragAmountTotal = 0f
                        } else if (dragAmountTotal < -100f) {
                            playerLane = (playerLane - 1).coerceAtLeast(0)
                            dragAmountTotal = 0f
                        }
                    }
                }
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
                    .onSizeChanged {
                        canvasWidth = it.width.toFloat()
                        canvasHeight = it.height.toFloat()
                    }
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF003366), Color(0xFF0077BE), Color(0xFF00BFFF))
                    ),
                    size = Size(canvasWidth, canvasHeight)
                )

                for (i in 0..5) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset((i * 100f + wavePhase * 5f) % canvasWidth, 0f),
                        end = Offset((i * 100f + wavePhase * 5f) % canvasWidth + 30f, canvasHeight),
                        strokeWidth = 60f
                    )
                }

                for (y in 0 until canvasHeight.toInt() step 50) {
                    val alpha = 0.03f + ((y % 100) / 400f)
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(0f, y + wavePhase % 50),
                        end = Offset(canvasWidth, y + 10 + wavePhase % 50),
                        strokeWidth = 1f
                    )
                }

                for (x in 0..canvasWidth.toInt() step 20) {
                    val y = canvasHeight - 30f + (5f * sin((x + wavePhase * 10) / 20f))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = 6f,
                        center = Offset(x.toFloat(), y)
                    )
                }

                val wavePath = Path().apply {
                    moveTo(0f, canvasHeight * 0.1f)
                    for (x in 0..canvasWidth.toInt() step 20) {
                        val y = (canvasHeight * 0.1f) + (10f * sin((x + wavePhase) / 20f))
                        lineTo(x.toFloat(), y)
                    }
                    lineTo(canvasWidth, 0f)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(path = wavePath, color = Color.White.copy(alpha = 0.05f))

                fishes.forEachIndexed { i, fish ->
                    val fx = (fish.x + wavePhase * (i + 1) * 0.5f) % canvasWidth
                    val fy = (fish.y + 5f * sin((wavePhase + i * 15) / 10f)) % canvasHeight
                    val isDolphin = i % 8 == 0
                    drawCircle(
                        color = if (isDolphin) Color.Gray.copy(alpha = 0.5f) else Color.Yellow.copy(alpha = 0.3f),
                        radius = if (isDolphin) 12f else 6f,
                        center = Offset(fx, fy)
                    )
                }

                birds.forEachIndexed { i, bird ->
                    val bx = (bird.x + wavePhase * (i + 1) * 1.2f) % canvasWidth
                    val by = (bird.y + 10f * sin((wavePhase + i * 15) / 8f)) % (canvasHeight * 0.3f)
                    drawArc(
                        color = Color.White.copy(alpha = 0.4f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(bx, by),
                        size = Size(20f, 10f)
                    )
                }

                val playerY = canvasHeight - boatSize - 16f
                val laneWidth = canvasWidth / laneCount
                val playerX = laneWidth * playerLane + laneWidth / 2 - boatSize / 2

                drawImage(
                    image = playerBoat,
                    dstOffset = IntOffset(playerX.toInt(), playerY.toInt()),
                    dstSize = IntSize(boatSize.toInt(), boatSize.toInt())
                )

                sharks.forEach { (shark, _) ->
                    drawImage(
                        image = sharkEnemy,
                        dstOffset = IntOffset(shark.x.toInt(), shark.y.toInt()),
                        dstSize = IntSize(sharkSize.toInt(), sharkSize.toInt())
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
                    Text(gameOverText, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        isGameOver = false
                        viewModel.resetGame()
                        sharks = emptyList()
                        crashSound?.release()
                        crashSound = null
                        backgroundMusic.start()
                        Toast.makeText(context, goodLuckToastText, Toast.LENGTH_SHORT).show()
                    }) {
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
                // Muestra el avatar del usuario, que ahora debería ser reactivo a los cambios del AuthViewModel
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = currentAvatarResId),
                    contentDescription = "Avatar de usuario",
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Transparent, shape = RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Usa el nombre a mostrar, que se pasa como argumento de navegación o del perfil
                Text(String.format(userDisplayLabelFormat, currentUserDisplayName), color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                // --- CORRECCIÓN PARA "High Score" ---
                // Ahora usa la cadena localizada.
                Text(highScoreLabel, color = Color.White)
                // --- FIN CORRECCIÓN ---
                Text("$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentScoreLabel, color = Color.White)
                Text("$score", color = Color.White)
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