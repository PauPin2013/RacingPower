package com.example.racingpower.views

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.models.UserProfile

// Composable que representa la pantalla del juego infinito de botes.
// Incluye la lógica del juego, renderizado de elementos y UI para puntuación.
@Composable
fun InfiniteBoatGameScreen(
    userId: String, // ID del usuario actual.
    displayName: String?, // Nombre a mostrar del usuario.
    viewModel: InfiniteGameViewModel, // ViewModel para la lógica del juego.
    navController: NavController // Controlador de navegación.
) {
    val context = LocalContext.current
    // Observa el estado de la puntuación, puntuación más alta y velocidad del ViewModel del juego.
    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed

    // Define el tamaño del bote y del tiburón en el juego.
    val boatSize = 150f
    val sharkSize = 180f
    // Estados para el ancho y alto del canvas de juego.
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    // Número de carriles en el juego.
    val laneCount = 3
    // Carril actual del jugador.
    var playerLane by remember { mutableStateOf(1) }
    // Lista de tiburones en pantalla (posición y fase para movimiento ondulatorio).
    var sharks by remember { mutableStateOf(listOf<Pair<Offset, Float>>()) }
    // Estado que indica si el juego ha terminado.
    var isGameOver by remember { mutableStateOf(false) }

    // Carga las imágenes de los recursos.
    val playerBoat = ImageBitmap.imageResource(id = R.drawable.boat_blue)
    val sharkEnemy = ImageBitmap.imageResource(id = R.drawable.shark)

    // Reproductor de sonido para el choque.
    var crashSound: MediaPlayer? by remember { mutableStateOf(null) }
    // Reproductor de música de fondo del océano.
    val backgroundMusic = remember { MediaPlayer.create(context, R.raw.ocean_music) }

    // Fase para la animación de las olas.
    var wavePhase by remember { mutableStateOf(0f) }
    // Lista de posiciones de peces decorativos.
    val fishes by remember {
        mutableStateOf(List(30) {
            Offset(
                x = (0..1000).random().toFloat(),
                y = (50..900).random().toFloat()
            )
        })
    }
    // Lista de posiciones de pájaros decorativos.
    val birds by remember { mutableStateOf(List(4) { Offset((it * 180).toFloat(), (it * 60).toFloat()) }) }

    // Obtiene una instancia del AuthViewModel.
    val authViewModel: AuthViewModel = viewModel()
    // Observa el perfil del usuario desde AuthViewModel.
    val userProfileState: State<UserProfile?> = authViewModel.userProfile.collectAsState()
    val userProfile = userProfileState.value

    // Obtiene el nombre a mostrar del usuario, priorizando displayName, luego userProfile, y finalmente invitado.
    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    val currentUserDisplayName = displayName?.ifBlank { null }
        ?: userProfile?.displayName?.ifBlank { null }
        ?: guestDisplayName

    // Obtiene el ID del recurso del avatar actual del usuario.
    val defaultAvatarResId = R.drawable.avatar1
    val currentAvatarResId = remember(userProfile?.avatarName) {
        val avatarName = userProfile?.avatarName ?: "avatar1"
        context.resources.getIdentifier(avatarName, "drawable", context.packageName)
    }.takeIf { it != 0 } ?: defaultAvatarResId

    // Cadenas de recursos para la UI del juego.
    val gameOverText = stringResource(id = R.string.game_over_text)
    val restartButtonText = stringResource(id = R.string.restart_button_text)
    val goodLuckToastText = stringResource(id = R.string.good_luck_toast)
    val userDisplayLabelFormat = stringResource(id = R.string.user_display_label)
    val highScoreLabel = stringResource(id = R.string.high_score_label)
    val currentScoreLabel = stringResource(id = R.string.current_score_label)

    // Efecto lanzado una sola vez para iniciar el juego, la música y la lógica principal del bucle de juego.
    LaunchedEffect(userId, currentUserDisplayName) {
        viewModel.startGame(userId, "boats", currentUserDisplayName)
        backgroundMusic.isLooping = true
        backgroundMusic.start()
        while (true) {
            if (!isGameOver) {
                // Actualiza la fase de las olas.
                wavePhase += 0.2f

                // Calcula las posiciones de los carriles.
                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - sharkSize / 2
                }

                // Genera nuevos tiburones si hay menos de dos en pantalla.
                if (sharks.size < 2) {
                    val lane = lanePositions.random()
                    sharks = sharks + (Offset(lane, -sharkSize) to 0f)
                }

                // Mueve los tiburones y actualiza su fase de movimiento.
                sharks = sharks.map { (pos, phase) ->
                    val newY = pos.y + speed
                    val newX = pos.x + (10f * sin(phase)) // Movimiento ondulatorio
                    Offset(newX, newY) to (phase + 0.2f)
                }

                // Identifica los tiburones que han pasado la pantalla e incrementa la puntuación.
                val passed = sharks.filter { it.first.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() } // onCarPassed es genérico para cualquier objeto que pase
                    sharks = sharks - passed.toSet()
                }

                // Calcula la posición del bote del jugador.
                val playerY = canvasHeight - boatSize - 16f
                val playerX = (canvasWidth / laneCount) * playerLane + (canvasWidth / laneCount) / 2 - boatSize / 2

                // Detecta colisiones entre el bote del jugador y los tiburones.
                if (sharks.any {
                        val (pos, _) = it
                        pos.x in playerX..(playerX + boatSize) &&
                                pos.y + sharkSize >= playerY &&
                                pos.y <= playerY + boatSize
                    }) {
                    isGameOver = true // Establece el estado de juego terminado.
                    viewModel.gameOver() // Llama a la función de fin de juego del ViewModel.
                    backgroundMusic.pause() // Pausa la música de fondo.
                    crashSound?.release() // Libera cualquier sonido de choque anterior.
                    crashSound = MediaPlayer.create(context, R.raw.shark_bite) // Crea y reproduce el sonido de mordisco.
                    crashSound?.start()
                }
            }
            delay(16L) // Pequeña pausa para controlar la velocidad del bucle.
        }
    }

    // Efecto de descarte para liberar recursos de los reproductores de medios al salir de la composición.
    DisposableEffect(Unit) {
        onDispose {
            backgroundMusic.stop()
            backgroundMusic.release()
            crashSound?.release()
        }
    }

    // Diseño principal de la pantalla, dividida en el área de juego y el panel de información.
    Row(modifier = Modifier.fillMaxSize()) {
        // Área de juego (ocupa 3/4 del ancho).
        Box(
            modifier = Modifier
                .weight(3f)
                // Habilita gestos de arrastre horizontal para mover el bote.
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
                // Habilita el enfoque para manejar eventos de teclado.
                .focusable()
                // Maneja eventos de teclado para mover el bote (flechas izquierda/derecha).
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
            // Canvas donde se dibujan todos los elementos del juego.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // Captura el tamaño del canvas una vez que se ha medido.
                    .onSizeChanged {
                        canvasWidth = it.width.toFloat()
                        canvasHeight = it.height.toFloat()
                    }
            ) {
                // Dibuja el fondo del agua con un gradiente.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF003366), Color(0xFF0077BE), Color(0xFF00BFFF))
                    ),
                    size = Size(canvasWidth, canvasHeight)
                )

                // TODO: Implementar dibujo de olas, peces, pájaros si se desean animaciones visuales más complejas.
                // Actualmente, solo se definen las listas de posiciones, pero no se dibujan explícitamente aquí.

                // Calcula la posición final del bote del jugador.
                val playerY = canvasHeight - boatSize - 16f
                val laneWidth = canvasWidth / laneCount
                val playerX = laneWidth * playerLane + laneWidth / 2 - boatSize / 2

                // Dibuja el bote del jugador.
                drawImage(
                    image = playerBoat,
                    dstOffset = IntOffset(playerX.toInt(), playerY.toInt()),
                    dstSize = IntSize(boatSize.toInt(), boatSize.toInt())
                )

                // Dibuja cada tiburón en su posición actual.
                sharks.forEach { (shark, _) ->
                    drawImage(
                        image = sharkEnemy,
                        dstOffset = IntOffset(shark.x.toInt(), shark.y.toInt()),
                        dstSize = IntSize(sharkSize.toInt(), sharkSize.toInt())
                    )
                }
            }

            // Superposición de fin de juego.
            if (isGameOver) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)), // Fondo semitransparente oscuro.
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Texto de "Game Over".
                    Text(gameOverText, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    // Botón de reiniciar juego.
                    Button(onClick = {
                        isGameOver = false // Restablece el estado de juego terminado.
                        viewModel.resetGame() // Reinicia la puntuación y velocidad en el ViewModel.
                        sharks = emptyList() // Borra todos los tiburones.
                        crashSound?.release() // Libera el sonido de choque.
                        crashSound = null
                        backgroundMusic.start() // Reanuda la música de fondo.
                        Toast.makeText(context, goodLuckToastText, Toast.LENGTH_SHORT).show() // Muestra un Toast.
                    }) {
                        Text(restartButtonText)
                    }
                }
            }
        }

        // Panel lateral (ocupa 1/4 del ancho) para mostrar información del juego y opciones.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF1B2A49)) // Color de fondo del panel.
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween // Distribuye el contenido verticalmente.
        ) {
            // Sección superior con información del usuario y puntuaciones.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                // Avatar del usuario.
                Image(
                    painter = painterResource(id = currentAvatarResId),
                    contentDescription = "Avatar de usuario",
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Transparent, shape = RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Nombre de usuario.
                Text(String.format(userDisplayLabelFormat, currentUserDisplayName), color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                // Puntuación más alta.
                Text(highScoreLabel, color = Color.White)
                Text("$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                // Puntuación actual.
                Text(currentScoreLabel, color = Color.White)
                Text("$score", color = Color.White)
            }

            // Icono de volver, clickable para regresar a la pantalla anterior.
            Image(
                painter = painterResource(id = R.drawable.exit_icon),
                contentDescription = "Volver",
                modifier = Modifier
                    .size(48.dp)
                    .clickable { navController.popBackStack() } // Navega de vuelta a la pantalla anterior.
                    .padding(bottom = 16.dp)
            )
        }
    }
}