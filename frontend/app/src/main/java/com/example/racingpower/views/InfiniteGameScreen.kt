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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.models.UserProfile
import com.example.racingpower.utils.LocaleHelper
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.clickable

// Composable que representa la pantalla de juego infinito de coches.
// Contiene la lógica del juego, el renderizado de gráficos y la interfaz de usuario.
@Composable
fun InfiniteGameScreen(
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
    // Tamaño del coche del jugador.
    val carSize = 80f
    // Ancho y alto del área de dibujo del juego.
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    // Número de carriles en la carretera.
    val laneCount = 3
    // Carril actual del jugador.
    var playerLane by remember { mutableStateOf(1) }
    // Lista de coches enemigos en pantalla.
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    // Lista de objetos de combustible en pantalla.
    var fuels by remember { mutableStateOf(listOf<Offset>()) }
    // Estado que indica si el juego ha terminado.
    var isGameOver by remember { mutableStateOf(false) }
    // Offset para animar las líneas de la carretera.
    var lineOffset by remember { mutableStateOf(0f) }
    // Almacena la puntuación en la que se generó el último combustible para evitar duplicados.
    var lastFuelScore by remember { mutableStateOf(0) }

    // Carga las imágenes de los recursos del juego.
    val playerCar = ImageBitmap.imageResource(id = R.drawable.car_blue)
    val enemyCar = ImageBitmap.imageResource(id = R.drawable.car_red)
    val fuelIcon = ImageBitmap.imageResource(id = R.drawable.fuel_icon)

    // Reproductor de sonido para el choque.
    var crashPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    // Reproductor de música de fondo del juego.
    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music) }

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
    val gameOverText = stringResource(id = R.string.game_over_text)
    val restartButtonText = stringResource(id = R.string.restart_button_text)
    val goodLuckToastText = stringResource(id = R.string.good_luck_toast)
    val userDisplayLabelFormat = stringResource(id = R.string.user_display_label)
    val highScoreLabel = stringResource(id = R.string.high_score_label)
    val currentScoreLabel = stringResource(id = R.string.current_score_label)

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
        viewModel.startGame(userId, "cars", currentUserDisplayName) // Inicializa el ViewModel del juego.
        while (true) { // Bucle infinito para la animación y lógica del juego.
            if (!isGameOver) {
                // Actualiza el offset de las líneas de la carretera para la animación.
                lineOffset += 10f
                if (lineOffset >= 40f) lineOffset = 0f

                // Calcula el ancho de los carriles y sus posiciones X.
                val laneWidth = canvasWidth / laneCount
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSize / 2
                }

                // Genera nuevos coches enemigos si hay menos de dos en pantalla.
                if (enemies.size < 2) {
                    val lane = lanePositions.random() // Selecciona un carril aleatorio.
                    enemies = enemies + Offset(lane, -carSize) // Añade un nuevo enemigo fuera de la pantalla superior.
                }
                // Mueve todos los enemigos hacia abajo según la velocidad del juego.
                enemies = enemies.map { it.copy(y = it.y + speed) }

                // Genera combustible si la puntuación es un múltiplo de 500 y no se ha generado en esta puntuación.
                if (fuels.isEmpty() && score % 500 == 0 && score != lastFuelScore) {
                    // Selecciona un carril aleatorio para el combustible que no esté ocupado por un enemigo.
                    val lane = lanePositions.shuffled().firstOrNull { laneX ->
                        enemies.none { enemy -> enemy.x == laneX }
                    }
                    lane?.let {
                        fuels = fuels + Offset(it, -carSize) // Añade combustible.
                        lastFuelScore = score // Actualiza la puntuación de la última generación de combustible.
                    }
                }
                // Mueve todos los combustibles hacia abajo.
                fuels = fuels.map { it.copy(y = it.y + speed) }

                // Elimina los enemigos que han pasado la pantalla e incrementa la puntuación.
                val passed = enemies.filter { it.y > canvasHeight }
                if (passed.isNotEmpty()) {
                    passed.forEach { viewModel.onCarPassed() }
                    enemies = enemies - passed.toSet()
                }

                // Elimina el combustible que ha pasado la pantalla.
                val fuelPassed = fuels.filter { it.y > canvasHeight }
                if (fuelPassed.isNotEmpty()) {
                    fuels = fuels - fuelPassed.toSet()
                }

                // Calcula la posición del coche del jugador.
                val playerY = canvasHeight - carSize - 16f
                val playerX = lanePositions[playerLane]

                // Detecta colisiones entre el coche del jugador y los enemigos.
                if (enemies.any {
                        it.x == playerX &&
                                it.y <= playerY + carSize &&
                                it.y + carSize >= playerY
                    }) {
                    isGameOver = true // Establece el estado de juego terminado.
                    viewModel.gameOver() // Llama a la función de fin de juego del ViewModel.
                    crashPlayer?.release() // Libera cualquier sonido de choque anterior.
                    crashPlayer = MediaPlayer.create(context, R.raw.crash_sound) // Crea y reproduce el sonido de choque.
                    crashPlayer?.start()
                }

                // Detecta la recogida de combustible.
                val collectedFuel = fuels.firstOrNull {
                    it.x == playerX &&
                            it.y <= playerY + carSize &&
                            it.y + carSize >= playerY
                }
                if (collectedFuel != null) {
                    fuels = fuels - collectedFuel // Elimina el combustible recogido.
                    viewModel.incrementScore(100) // Incrementa la puntuación por recoger combustible.
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
                .background(Color.DarkGray) // Fondo de la carretera.
                .focusable() // Hace que el Box sea enfocable para eventos de teclado.
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
        ) {
            // Canvas donde se dibujan todos los elementos del juego.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // Captura el tamaño del canvas una vez que se ha medido.
                    .onSizeChanged { size ->
                        canvasWidth = size.width.toFloat()
                        canvasHeight = size.height.toFloat()
                    }
            ) {
                val laneWidth = size.width / laneCount // Ancho de cada carril.
                val carSizeCanvas = 110f // Tamaño de los coches en el canvas.
                // Posiciones X de los carriles.
                val lanePositions = List(laneCount) { index ->
                    laneWidth * index + laneWidth / 2 - carSizeCanvas / 2
                }
                // Dibuja el fondo de la carretera.
                drawRect(Color.DarkGray, size = Size(size.width, size.height))

                // Dibuja las líneas de la carretera.
                for (i in 1 until laneCount) {
                    val x = laneWidth * i
                    var y = -40f + lineOffset // Usa lineOffset para animar las líneas.
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

                // Calcula la posición Y del coche del jugador.
                val playerY = size.height - carSizeCanvas - 16f
                // Dibuja el coche del jugador.
                drawImage(
                    image = playerCar,
                    dstOffset = IntOffset(lanePositions[playerLane].toInt(), playerY.toInt()),
                    dstSize = IntSize(carSizeCanvas.toInt(), carSizeCanvas.toInt())
                )

                // Dibuja todos los coches enemigos.
                enemies.forEach { enemy ->
                    drawImage(
                        image = enemyCar,
                        dstOffset = IntOffset(enemy.x.toInt(), enemy.y.toInt()),
                        dstSize = IntSize(carSizeCanvas.toInt(), carSizeCanvas.toInt())
                    )
                }

                // Dibuja todos los objetos de combustible.
                fuels.forEach { fuel ->
                    drawImage(
                        image = fuelIcon,
                        dstOffset = IntOffset(fuel.x.toInt(), fuel.y.toInt()),
                        dstSize = IntSize(carSizeCanvas.toInt(), carSizeCanvas.toInt())
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
                    Text(gameOverText, color = Color.White) // Texto de "Game Over".
                    Spacer(modifier = Modifier.height(16.dp))
                    // Botón para reiniciar el juego.
                    androidx.compose.material3.Button(
                        onClick = {
                            isGameOver = false // Restablece el estado de juego terminado.
                            viewModel.resetGame() // Reinicia la puntuación y velocidad en el ViewModel.
                            enemies = emptyList() // Borra todos los enemigos.
                            fuels = emptyList() // Borra todos los combustibles.
                            crashPlayer?.release() // Libera el sonido de choque.
                            crashPlayer = null
                            // La música de fondo se reiniciará automáticamente gracias a LaunchedEffect(isGameOver)
                            Toast.makeText(context, goodLuckToastText, Toast.LENGTH_SHORT).show() // Muestra un Toast.
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
                Text(text = String.format(userDisplayLabelFormat, currentUserDisplayName), color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                // Puntuación más alta.
                Text(text = highScoreLabel, color = Color.White)
                Text(text = "$highScore", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                // Puntuación actual.
                Text(text = currentScoreLabel, color = Color.White)
                Text(text = "$score", color = Color.White)
            }

            // Icono de volver, centrado horizontalmente y clickable.
            Image(
                painter = painterResource(id = R.drawable.exit_icon),
                contentDescription = "Botón Volver",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable { navController.popBackStack() } // Navega de vuelta a la pantalla anterior.
                    .padding(bottom = 16.dp)
            )
        }
    }
}