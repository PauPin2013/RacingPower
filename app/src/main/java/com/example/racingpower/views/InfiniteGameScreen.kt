package com.example.racingpower.views

import android.app.Application
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
// Importaciones del AuthViewModel y UserProfile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.models.UserProfile // Asegúrate de que esta esté importada

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import com.example.racingpower.utils.LocaleHelper

// Eliminamos la importación directa de FirebaseFirestore ya que AuthViewModel la maneja
// import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun InfiniteGameScreen(
    userId: String, // Cambiado a userId para mayor claridad
    displayName: String?, // ¡Nuevo parámetro para recibir el nombre a mostrar!
    viewModel: InfiniteGameViewModel, // Inyectado desde MainActivity
    navController: NavController
) {
    val context = LocalContext.current
    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed
    val carSize = 80f
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    val laneCount = 3
    var playerLane by remember { mutableStateOf(1) }
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    var fuels by remember { mutableStateOf(listOf<Offset>()) }
    var isGameOver by remember { mutableStateOf(false) }
    var lineOffset by remember { mutableStateOf(0f) }
    var lastFuelScore by remember { mutableStateOf(0) }

    val playerCar = ImageBitmap.imageResource(id = R.drawable.car_blue)
    val enemyCar = ImageBitmap.imageResource(id = R.drawable.car_red)
    val fuelIcon = ImageBitmap.imageResource(id = R.drawable.fuel_icon)

    var crashPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music) }

    // --- CAMBIOS PARA EL NOMBRE Y AVATAR ---
    val authViewModel: AuthViewModel = viewModel() // Obtén la instancia del AuthViewModel
    val userProfileState: State<UserProfile?> = authViewModel.userProfile.collectAsState()
    val userProfile = userProfileState.value // Obtiene el valor actual del perfil

    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    // Prioriza el displayName pasado por navegación, luego el de Firestore, luego invitado
    val currentUserDisplayName = displayName?.ifBlank { null }
        ?: userProfile?.displayName?.ifBlank { null }
        ?: guestDisplayName

    val defaultAvatarResId = R.drawable.avatar1
    val currentAvatarResId = remember(userProfile?.avatarName) { // Observa el avatarName del userProfile
        val avatarName = userProfile?.avatarName ?: "avatar1"
        context.resources.getIdentifier(avatarName, "drawable", context.packageName)
    }.takeIf { it != 0 } ?: defaultAvatarResId
    // --- FIN DE CAMBIOS PARA EL NOMBRE Y AVATAR ---

    // Strings para los Toasts y textos de la UI
    val gameOverText = stringResource(id = R.string.game_over_text)
    val restartButtonText = stringResource(id = R.string.restart_button_text)
    val goodLuckToastText = stringResource(id = R.string.good_luck_toast)
    val userDisplayLabelFormat = stringResource(id = R.string.user_display_label)
    val highScoreLabel = stringResource(id = R.string.high_score_label)
    val currentScoreLabel = stringResource(id = R.string.current_score_label)
    val backButtonText = stringResource(id = R.string.back_button_text)


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

    LaunchedEffect(userId, currentUserDisplayName) { // Ahora depende de userId y el nombre a mostrar
        viewModel.startGame(userId, "cars", currentUserDisplayName) // Pasa userId y el nombre
        while (true) {
            if (!isGameOver) {
                lineOffset += 10f
                if (lineOffset >= 40f) lineOffset = 0f
                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSize / 2
                }

                // Enemigos
                if (enemies.size < 2) {
                    val lane = lanePositions.random()
                    enemies = enemies + Offset(lane, -carSize)
                }
                enemies = enemies.map { it.copy(y = it.y + speed) }

                // Combustible cada 500 puntos (y solo uno por vez)
                if (fuels.isEmpty() && score % 500 == 0 && score != lastFuelScore) {
                    val lane = lanePositions.shuffled().firstOrNull { laneX ->
                        enemies.none { enemy -> enemy.x == laneX }
                    }
                    lane?.let {
                        fuels = fuels + Offset(it, -carSize)
                        lastFuelScore = score
                    }
                }
                fuels = fuels.map { it.copy(y = it.y + speed) }

                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() }
                    enemies = enemies - passed.toSet()
                }

                val fuelPassed = fuels.filter { it.y > canvasHeight }
                if (fuelPassed.isNotEmpty()) {
                    fuels = fuels - fuelPassed.toSet()
                }

                val playerY = canvasHeight - carSize - 16f
                val playerX = lanePositions[playerLane]

                // Colisión con enemigos
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

                // Colisión con gasolina
                val collectedFuel = fuels.firstOrNull {
                    it.x == playerX &&
                            it.y <= playerY + carSize &&
                            it.y + carSize >= playerY
                }
                if (collectedFuel != null) {
                    fuels = fuels - collectedFuel
                    viewModel.incrementScore(100)
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

    // Obtenemos el idioma actual para mostrarlo y para la lógica del botón
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getPersistedLocale(context)) }


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
                val carSizeCanvas = 110f
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSizeCanvas / 2
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

                val playerY = size.height - carSizeCanvas - 16f
                drawImage(
                    image = playerCar,
                    dstOffset = IntOffset(lanePositions[playerLane].toInt(), playerY.toInt()),
                    dstSize = IntSize(carSizeCanvas.toInt(), carSizeCanvas.toInt())
                )

                enemies.forEach { enemy ->
                    drawImage(
                        image = enemyCar,
                        dstOffset = IntOffset(enemy.x.toInt(), enemy.y.toInt()),
                        dstSize = IntSize(carSizeCanvas.toInt(), carSizeCanvas.toInt())
                    )
                }

                fuels.forEach { fuel ->
                    drawImage(
                        image = fuelIcon,
                        dstOffset = IntOffset(fuel.x.toInt(), fuel.y.toInt()),
                        dstSize = IntSize(carSizeCanvas.toInt(), carSizeCanvas.toInt())
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
                    Button(
                        onClick = {
                            isGameOver = false
                            viewModel.resetGame()
                            enemies = emptyList()
                            fuels = emptyList()
                            crashPlayer?.release()
                            crashPlayer = null
                            Toast.makeText(context, goodLuckToastText, Toast.LENGTH_SHORT).show()
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
                // Avatar del usuario
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = currentAvatarResId), // Usa el avatar reactivo
                    contentDescription = "Avatar de usuario",
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Transparent, shape = RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = String.format(userDisplayLabelFormat, currentUserDisplayName), color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = highScoreLabel, color = Color.White)
                Text(text = "$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = currentScoreLabel, color = Color.White)
                Text(text = "$score", color = Color.White)
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