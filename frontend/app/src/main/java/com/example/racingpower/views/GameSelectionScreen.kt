package com.example.racingpower.views

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.utils.LocaleHelper
import com.example.racingpower.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.lazy.LazyRow
import com.example.racingpower.models.UserProfile
import androidx.compose.ui.layout.ContentScale

// Composable que representa la pantalla de selección de juego.
// Muestra el nombre del usuario, avatar, opciones de juego y controles de la aplicación.
@Composable
fun GameSelectionScreen(
    userId: String, // ID del usuario actual.
    navController: NavController, // Controlador de navegación para la pantalla.
    onLogoutNotification: () -> Unit // Función de callback para disparar una notificación de cierre de sesión.
) {
    val context = LocalContext.current
    // Obtiene una instancia del AuthViewModel.
    val authViewModel: AuthViewModel = viewModel()
    // Obtiene una instancia de FirebaseAuth.
    val auth: FirebaseAuth = Firebase.auth

    // Observa el estado del perfil de usuario desde el AuthViewModel.
    val userProfileState: State<UserProfile?> = authViewModel.userProfile.collectAsState()
    val userProfile = userProfileState.value

    // Lanza un efecto cuando el userId cambia para cargar el perfil del usuario.
    LaunchedEffect(userId) {
        authViewModel.loadUserProfile(userId)
    }

    // Obtiene el nombre de visualización del usuario o usa un nombre de invitado si no está disponible.
    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    val usernameToDisplay = userProfile?.displayName?.ifBlank { null }
        ?: auth.currentUser?.displayName?.ifBlank { null }
        ?: guestDisplayName

    // Inicializa el reproductor de música de fondo.
    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music3) }
    // Estado mutable para controlar si la música está silenciada.
    var isMuted by remember { mutableStateOf(false) }

    // ID del recurso del avatar predeterminado.
    val defaultAvatarResId = R.drawable.avatar1

    // Determina el ID del recurso del avatar actual del usuario.
    val currentAvatarName = userProfile?.avatarName ?: "avatar1"
    val currentAvatarResId = remember(currentAvatarName) {
        context.resources.getIdentifier(currentAvatarName, "drawable", context.packageName)
    }.takeIf { it != 0 } ?: defaultAvatarResId

    // Lanza un efecto una sola vez al componer para iniciar la música de fondo en bucle.
    LaunchedEffect(Unit) {
        backgroundPlayer.isLooping = true
        backgroundPlayer.start()
    }

    // Lanza un efecto que se ejecuta al salir de la composición para liberar los recursos del reproductor.
    DisposableEffect(Unit) {
        onDispose {
            backgroundPlayer.stop()
            backgroundPlayer.release()
        }
    }

    // Estado mutable para el idioma actual de la aplicación.
    val currentLanguage = remember(LocaleHelper.getPersistedLocale(context)) {
        mutableStateOf(LocaleHelper.getPersistedLocale(context))
    }

    // Estado de desplazamiento para la columna principal.
    val scrollState = rememberScrollState()

    // Obtiene las cadenas de recursos localizadas.
    val welcomeUserFormat = stringResource(id = R.string.welcome_user_format)
    val selectGameTitle = stringResource(id = R.string.select_game_title)
    val gameCarsTitle = stringResource(id = R.string.game_cars_title)
    val gamePlanesTitle = stringResource(id = R.string.game_planes_title)
    val gameBoatsTitle = stringResource(id = R.string.game_boats_title)
    val leaderboardButtonText = stringResource(id = R.string.leaderboard_button)
    val logoutButtonText = stringResource(id = R.string.logout_button_text)
    val logoutToastMessage = stringResource(id = R.string.logout_toast)
    val changeLanguageButtonText = stringResource(id = R.string.change_language_button)
    val currentLanguageDisplay = stringResource(id = R.string.current_language)

    // Contenedor principal que llena toda la pantalla.
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Imagen de fondo que cubre todo el espacio del Box.
        Image(
            painter = painterResource(id = R.drawable.fondo2),
            contentDescription = null, // Descripción nula ya que es una imagen decorativa de fondo.
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Escala la imagen para que llene el Box, recortando si es necesario.
        )

        // Capa de superposición semitransparente para oscurecer la imagen de fondo,
        // mejorando la legibilidad del texto en primer plano.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Ajusta el valor alpha para controlar la oscuridad.
        )

        // Columna principal que contiene el contenido desplazable de la pantalla.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, // Centra los elementos hijos horizontalmente.
            verticalArrangement = Arrangement.Top, // Alinea los elementos hijos en la parte superior.
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState) // Hace que la columna sea desplazable verticalmente.
                .padding(top = 64.dp, bottom = 24.dp) // Padding superior e inferior.
        ) {
            // Texto de bienvenida al usuario.
            Text(
                text = String.format(welcomeUserFormat, usernameToDisplay),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Imagen del avatar del usuario, clickable para ir a la pantalla de selección de avatar.
            Image(
                painter = painterResource(id = currentAvatarResId),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .padding(8.dp)
                    .clickable {
                        navController.navigate("avatar_selection_screen/$userId")
                    }
            )

            // Espacio vertical para separación.
            Spacer(modifier = Modifier.height(30.dp))

            // Título para la selección de juego.
            Text(
                text = selectGameTitle,
                fontSize = 18.sp,
                color = Color.LightGray
            )

            // Espacio vertical para separación.
            Spacer(modifier = Modifier.height(50.dp))

            // Fila desplazable horizontalmente que muestra las opciones de juego.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre elementos.
            ) {
                // Opción para el juego de coches.
                item {
                    GameOption(
                        title = gameCarsTitle,
                        imageRes = R.drawable.car_icon,
                        onClick = {
                            navController.navigate("game_screen_cars/$userId?displayName=${usernameToDisplay}")
                        }
                    )
                }
                // Opción para el juego de aviones.
                item {
                    GameOption(
                        title = gamePlanesTitle,
                        imageRes = R.drawable.plane_icon,
                        onClick = {
                            navController.navigate("game_screen_planes/$userId?displayName=${usernameToDisplay}")
                        }
                    )
                }
                // Opción para el juego de botes.
                item {
                    GameOption(
                        title = gameBoatsTitle,
                        imageRes = R.drawable.boat_icon,
                        onClick = {
                            navController.navigate("game_screen_boats/$userId?displayName=${usernameToDisplay}")
                        }
                    )
                }
            }

            // Espacio vertical para separación.
            Spacer(modifier = Modifier.height(50.dp))

            // Botón para navegar a la tabla de clasificación.
            Button(
                onClick = {
                    navController.navigate("leaderboard_screen")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.7f))
            ) {
                Text(leaderboardButtonText, fontSize = 18.sp)
            }

            // Espacio vertical para separación.
            Spacer(modifier = Modifier.height(12.dp))

            // Botón para cerrar la sesión del usuario.
            Button(
                onClick = {
                    authViewModel.logout() // Llama a la función de cierre de sesión del ViewModel.
                    navController.navigate("login_screen") { // Navega a la pantalla de login.
                        popUpTo(navController.graph.id) { inclusive = true } // Limpia la pila de navegación.
                    }
                    Toast.makeText(context, logoutToastMessage, Toast.LENGTH_SHORT).show() // Muestra un Toast.
                    onLogoutNotification() // Dispara la notificación de cierre de sesión.
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Text(logoutButtonText, fontSize = 18.sp)
            }

            // Espacio vertical para separación.
            Spacer(modifier = Modifier.height(12.dp))

            // Botón para cambiar el idioma de la aplicación.
            Button(
                onClick = {
                    val newLanguage = if (currentLanguage.value == "es") "en" else "es"
                    LocaleHelper.changeAndRestart(context, newLanguage) // Cambia el idioma y reinicia la app.
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
            ) {
                Text(changeLanguageButtonText)
            }

            // Espacio vertical para separación.
            Spacer(modifier = Modifier.height(8.dp))

            // Muestra el idioma actual de la aplicación.
            Text(
                text = currentLanguageDisplay,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Botón de mute/desmute de la música, posicionado en la esquina superior derecha.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            val iconRes = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Mute Button",
                modifier = Modifier
                    .size(36.dp)
                    .clickable {
                        isMuted = !isMuted // Cambia el estado de mute.
                        // Ajusta el volumen del reproductor de música.
                        backgroundPlayer.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                    }
            )
        }
    }
}

// Composable auxiliar que representa una opción de juego individual (imagen y título).
@Composable
fun GameOption(
    title: String, // Título del juego.
    imageRes: Int, // ID del recurso de la imagen del juego.
    onClick: () -> Unit // Callback que se ejecuta al hacer clic en la opción.
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, // Centra el contenido horizontalmente.
        modifier = Modifier
            .width(140.dp) // Ancho fijo de la columna.
            .clickable { onClick() } // Hace que la columna sea clickable.
            .background(Color.White, shape = RoundedCornerShape(12.dp)) // Fondo blanco con esquinas redondeadas.
            .padding(16.dp) // Padding interno.
    ) {
        // Imagen del icono del juego.
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title, // Descripción de la imagen.
            modifier = Modifier.size(80.dp) // Tamaño de la imagen.
        )
        // Espacio vertical.
        Spacer(modifier = Modifier.height(8.dp))
        // Título del juego.
        Text(text = title, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}