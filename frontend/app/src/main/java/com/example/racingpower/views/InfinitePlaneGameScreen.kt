package com.example.racingpower.views

import android.app.Application
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.models.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.racingpower.utils.LocaleHelper
import kotlin.math.sin

// Composable que representa la pantalla del juego infinito de aviones.
// Maneja la lógica del juego, el renderizado de gráficos y la interfaz de usuario.
@Composable
fun InfinitePlaneGameScreen(
    userId: String, // ID del usuario actual.
    displayName: String?, // Nombre a mostrar del usuario.
    viewModel: InfiniteGameViewModel, // ViewModel que gestiona la lógica del juego.
    navController: NavController // Controlador para la navegación entre pantallas.
) {
    val context = LocalContext.current
    // Observa el estado de la puntuación, puntuación más alta y velocidad del ViewModel del juego.
    val score by viewModel.score
    val highScore by viewModel.highScore
    val speed by viewModel.speed
    // Tamaño del avión del jugador y enemigos.
    val planeSize = 120f
    // Ancho y alto del área de dibujo del juego.
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    // Número de carriles para los aviones.
    val laneCount = 3
    // Carril actual del jugador.
    var playerLane by remember { mutableStateOf(1) }
    // Lista de aviones enemigos en pantalla.
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    // Estado que indica si el juego ha terminado.
    var isGameOver by remember { mutableStateOf(false) }
    // Offset para animar las ondas de calor o turbulencias.
    var waveOffset by remember { mutableStateOf(0f) }

    // Estado para controlar el salto (elevación del avión). 0: no saltando, 1: subiendo/bajando.
    var isJumping by remember { mutableStateOf(0) }
    // Offset vertical del avión debido al salto.
    var jumpOffset by remember { mutableStateOf(0f) }
    // Número de saltos restantes.
    var jumpsLeft by remember { mutableStateOf(10) }
    // Altura máxima del salto.
    val jumpHeight = 100f
    // Duración del salto en milisegundos.
    val jumpDuration = 1600L
    // Alcance del coroutine para lanzar efectos secundarios.
    val scope = rememberCoroutineScope()

    // Carga las imágenes de los recursos del juego.
    val playerPlane = ImageBitmap.imageResource(id = R.drawable.plane_blue)
    val enemyPlane = ImageBitmap.imageResource(id = R.drawable.plane_red)
    // Reproductor de sonido para el choque.
    var crashPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    // Reproductor de música de fondo del juego.
    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music2) }

    // Obtiene una instancia del AuthViewModel para acceder a los datos del perfil de usuario.
    val authViewModel: AuthViewModel = viewModel()
    // Observa el estado del perfil de usuario desde el AuthViewModel.
    val userProfileState: State<UserProfile?> = authViewModel.userProfile.collectAsState()
    val userProfile = userProfileState.value

    // Determina el nombre de usuario a mostrar, priorizando displayName, luego userProfile, y finalmente un nombre de invitado.
    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    val currentUserDisplayName = displayName?.ifBlank { null }
        ?: userProfile?.displayName?.ifBlank { null }
        ?: guestDisplayName

    // Determina el ID del recurso del avatar actual del usuario.
    val defaultAvatarResId = R.drawable.avatar1
    val currentAvatarResId = remember(userProfile?.avatarName) {
        val avatarName = userProfile?.avatarName ?: "avatar1"
        context.resources.getIdentifier(avatarName, "drawable", context.packageName)
    }.takeIf { it != 0 } ?: defaultAvatarResId

    // Cadenas de recursos para los textos de la UI.
    val planeDownText = stringResource(id = R.string.plane_down_text)
    val restartButtonText = stringResource(id = R.string.restart_button_text)
    val takeOffAgainToastText = stringResource(id = R.string.take_off_again_toast)
    val userDisplayLabelFormat = stringResource(id = R.string.user_display_label)
    val highScoreLabel = stringResource(id = R.string.high_score_label)
    val currentScoreLabel = stringResource(id = R.string.current_score_label)
    val jumpsRemainingLabel = stringResource(id = R.string.jumps_remaining_label)

    // Listas para elementos decorativos del fondo (pájaros y nubes).
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

    // Efecto lanzado cuando `isGameOver` cambia para controlar la música de fondo.
    LaunchedEffect(isGameOver) {
        if (!isGameOver) {
            if (!backgroundPlayer.isPlaying) {
                backgroundPlayer.isLooping = true
                backgroundPlayer.start()
            }
        } else {
            if (backgroundPlayer.isPlaying) {
                backgroundPlayer.pause()
                backgroundPlayer.seekTo(0) // Reinicia la música al principio para la próxima vez.
            }
        }
    }

    // Efecto lanzado para inicializar el juego y ejecutar el bucle principal del juego.
    LaunchedEffect(userId, currentUserDisplayName) {
        viewModel.startGame(userId, "planes", currentUserDisplayName) // Inicializa el ViewModel del juego.
        while (true) { // Bucle infinito para la animación y lógica del juego.
            if (!isGameOver) {
                // Actualiza el offset de la onda para animar el fondo.
                waveOffset += 0.8f
                if (waveOffset >= 120f) waveOffset = 0f

                // Calcula el ancho de los carriles y sus posiciones X.
                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - planeSize / 2
                }

                // Genera nuevos aviones enemigos si hay menos de dos en pantalla.
                if (enemies.size < 2) {
                    val lane = lanePositions.random() // Selecciona un carril aleatorio.
                    enemies = enemies + Offset(lane, -planeSize) // Añade un nuevo enemigo fuera de la pantalla superior.
                }

                // Mueve todos los enemigos hacia abajo según la velocidad del juego.
                enemies = enemies.map { it.copy(y = it.y + speed) }

                // Elimina los enemigos que han pasado la pantalla e incrementa la puntuación.
                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() } // onCarPassed es genérico para cualquier objeto que pase
                    enemies = enemies - passed.toSet()
                }

                // Calcula la posición Y del avión del jugador, ajustando por el salto.
                val playerY = canvasHeight - planeSize - 16f - jumpOffset
                val playerX = lanePositions[playerLane]

                // Detecta colisiones si el jugador no está saltando.
                if (isJumping == 0 && enemies.any {
                        it.x == playerX &&
                                it.y <= playerY + planeSize &&
                                it.y + planeSize >= playerY
                    }) {
                    isGameOver = true // Establece el estado de juego terminado.
                    viewModel.gameOver() // Llama a la función de fin de juego del ViewModel.
                    crashPlayer?.release() // Libera cualquier sonido de choque anterior.
                    crashPlayer = MediaPlayer.create(context, R.raw.crash_sound2) // Crea y reproduce el sonido de choque.
                    crashPlayer?.start()
                }
            }
            delay(16L) // Pausa para controlar la velocidad de actualización del juego (aprox. 60 FPS).
        }
    }

    // Efecto de descarte para liberar recursos de los reproductores de medios al salir de la composición.
    DisposableEffect(Unit) {
        onDispose {
            backgroundPlayer.stop()
            backgroundPlayer.release()
            crashPlayer?.release()
        }
    }

    // Diseño principal de la pantalla, dividida en el área de juego y el panel de información.
    Row(modifier = Modifier.fillMaxSize()) {
        // Área de juego (ocupa 3/4 del ancho).
        Box(
            modifier = Modifier
                .weight(3f)
                .focusable() // Hace que el Box sea enfocable para eventos de teclado.
                // Manejo de gestos de arrastre horizontal para mover el jugador.
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                            if (totalDrag > 100f) { // Umbral para detectar un "swipe" a la derecha.
                                playerLane = (playerLane + 1).coerceAtMost(laneCount - 1)
                                totalDrag = 0f
                            } else if (totalDrag < -100f) { // Umbral para detectar un "swipe" a la izquierda.
                                playerLane = (playerLane - 1).coerceAtLeast(0)
                                totalDrag = 0f
                            }
                        },
                        onDragEnd = { totalDrag = 0f }, // Reinicia el contador de arrastre al finalizar.
                        onDragCancel = { totalDrag = 0f } // Reinicia el contador si el arrastre es cancelado.
                    )
                }
                // Manejo de toques para activar el salto.
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (isJumping == 0 && jumpsLeft > 0) { // Solo si no está saltando y quedan saltos.
                            isJumping = 1 // Establece el estado de salto.
                            jumpOffset = jumpHeight // Aplica el offset de salto.
                            jumpsLeft-- // Decrementa los saltos restantes.
                            scope.launch {
                                delay(jumpDuration) // Espera la duración del salto.
                                jumpOffset = 0f // Reinicia el offset de salto.
                                isJumping = 0 // Reinicia el estado de salto.
                            }
                        }
                    }
                }
                // Manejo de eventos de teclado (flechas izquierda/derecha) para mover el jugador.
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
                // Dibuja el fondo del cielo con un gradiente.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF003366), Color(0xFF004C99), Color(0xFF0077BE))
                    ),
                    size = Size(canvasWidth, canvasHeight)
                )

                // Dibuja las líneas decorativas de ondas de calor/turbulencias.
                for (i in 0 until 6) {
                    val x = (i * 160f + waveOffset) % canvasWidth
                    drawLine(
                        color = Color(0xFFFFA726).copy(alpha = 0.08f), // Color naranja transparente.
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 60f
                    )
                }

                // Dibuja las nubes decorativas, con movimiento y fase.
                clouds.forEachIndexed { i, cloud ->
                    val cx = (cloud.x + waveOffset * 0.1f * (i + 1)) % canvasWidth
                    val cy = (cloud.y + 10f * sin((waveOffset + i * 10) / 30f)) % canvasHeight
                    drawCircle(Color.White.copy(alpha = 0.25f), 40f, Offset(cx, cy))
                    drawCircle(Color.White.copy(alpha = 0.25f), 30f, Offset(cx + 30f, cy - 10f))
                    drawCircle(Color.White.copy(alpha = 0.25f), 25f, Offset(cx - 30f, cy - 5f))
                }

                // Dibuja los pájaros decorativos, con movimiento y fase.
                birds.forEachIndexed { i, bird ->
                    val bx = (bird.x + waveOffset * 0.1f * (i + 1)) % canvasWidth
                    val by = (bird.y + 3f * sin((waveOffset + i * 15) / 35f)) % canvasHeight
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(bx - 5f, by), Offset(bx + 5f, by), strokeWidth = 2f)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(bx - 5f, by), Offset(bx - 3f, by - 3f), strokeWidth = 2f)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(bx + 5f, by), Offset(bx + 3f, by - 3f), strokeWidth = 2f)
                }

                // Calcula el ancho de los carriles y sus posiciones X.
                val laneWidth = size.width / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - planeSize / 2
                }

                // Calcula la posición Y del avión del jugador, ajustando por el salto.
                val playerY = size.height - planeSize - 16f - jumpOffset
                // Dibuja el avión del jugador.
                drawImage(
                    image = playerPlane,
                    dstOffset = IntOffset(lanePositions[playerLane].toInt(), playerY.toInt()),
                    dstSize = IntSize(planeSize.toInt(), planeSize.toInt())
                )

                // Dibuja todos los aviones enemigos.
                enemies.forEach { enemy ->
                    drawImage(
                        image = enemyPlane,
                        dstOffset = IntOffset(enemy.x.toInt(), enemy.y.toInt()),
                        dstSize = IntSize(planeSize.toInt(), planeSize.toInt())
                    )
                }
            }

            // Muestra la superposición de "Game Over" si el juego ha terminado.
            if (isGameOver) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)), // Fondo semitransparente oscuro.
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(planeDownText, color = Color.White) // Texto de "Avión Derribado".
                    Spacer(modifier = Modifier.height(16.dp))
                    // Botón para reiniciar el juego.
                    Button(
                        onClick = {
                            isGameOver = false // Restablece el estado de juego terminado.
                            viewModel.resetGame() // Reinicia la puntuación y velocidad en el ViewModel.
                            enemies = emptyList() // Borra todos los enemigos.
                            crashPlayer?.release() // Libera el sonido de choque.
                            crashPlayer = null
                            jumpsLeft = 10 // Reinicia los saltos disponibles.
                            // La música de fondo se reiniciará automáticamente gracias a LaunchedEffect(isGameOver)
                            Toast.makeText(context, takeOffAgainToastText, Toast.LENGTH_SHORT).show() // Muestra un Toast.
                        }
                    ) {
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
            // Sección superior con avatar, nombre de usuario y puntuaciones.
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
                Spacer(modifier = Modifier.height(16.dp))
                // Saltos restantes.
                Text(jumpsRemainingLabel, color = Color.White)
                Text("$jumpsLeft", color = Color.White)
            }

            // Icono de volver, centrado horizontalmente y clickable.
            Image(
                painter = painterResource(id = R.drawable.exit_icon),
                contentDescription = "Salir",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable { navController.popBackStack() } // Navega de vuelta a la pantalla anterior.
                    .padding(bottom = 16.dp)
            )
        }
    }
}