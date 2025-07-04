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
import com.example.racingpower.models.UserProfile // Asegúrate de que esta importación esté presente

@Composable
fun GameSelectionScreen(
    userId: String,
    navController: NavController,
    onLogoutNotification: () -> Unit
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val auth: FirebaseAuth = Firebase.auth

    // Observar el userProfile del AuthViewModel.
    val userProfileState: State<UserProfile?> = authViewModel.userProfile.collectAsState()
    val userProfile = userProfileState.value // Obtiene el valor actual del perfil

    // --- CORRECCIÓN PARA EL AVATAR EN EL LOBBY ---
    // Este LaunchedEffect asegura que el perfil se recargue cada vez que la pantalla
    // se "re-entra" o se recompone (por ejemplo, al volver de la selección de avatar).
    // Tu AuthViewModel ya tiene el método loadUserProfile(userId).
    LaunchedEffect(userId) {
        authViewModel.loadUserProfile(userId)
    }
    // --- FIN CORRECCIÓN ---

    // Usar el displayName del userProfile o el guestDisplayName si el perfil es nulo
    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    val usernameToDisplay = userProfile?.displayName?.ifBlank { null }
        ?: auth.currentUser?.displayName?.ifBlank { null }
        ?: guestDisplayName

    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music3) }
    var isMuted by remember { mutableStateOf(false) }

    // Avatar por defecto si no se carga del perfil o el perfil es nulo
    val defaultAvatarResId = R.drawable.avatar1

    // Determinar el ID del recurso del avatar a mostrar
    // El 'remember' con userProfile?.avatarName como key asegura que el Image se recomponga
    // si el avatarName dentro del userProfile cambia.
    val currentAvatarName = userProfile?.avatarName ?: "avatar1"
    val currentAvatarResId = remember(currentAvatarName) {
        context.resources.getIdentifier(currentAvatarName, "drawable", context.packageName)
    }.takeIf { it != 0 } ?: defaultAvatarResId


    LaunchedEffect(Unit) {
        backgroundPlayer.isLooping = true
        backgroundPlayer.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            backgroundPlayer.stop()
            backgroundPlayer.release()
        }
    }

    val currentLanguage = remember(LocaleHelper.getPersistedLocale(context)) {
        mutableStateOf(LocaleHelper.getPersistedLocale(context))
    }

    val scrollState = rememberScrollState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A49))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 64.dp, bottom = 24.dp)
        ) {
            Text(
                text = String.format(welcomeUserFormat, usernameToDisplay),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Image(
                painter = painterResource(id = currentAvatarResId),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .padding(8.dp)
                    .clickable {
                        // Navega a la pantalla de selección de avatar
                        navController.navigate("avatar_selection_screen/$userId")
                    }
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = selectGameTitle,
                fontSize = 18.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(75.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GameOption(
                        title = gameCarsTitle,
                        imageRes = R.drawable.car_icon,
                        onClick = {
                            // Pasa el userId y el nombre a mostrar a la pantalla del juego
                            navController.navigate("game_screen_cars/$userId?displayName=${usernameToDisplay}")
                        }
                    )
                }
                item {
                    GameOption(
                        title = gamePlanesTitle,
                        imageRes = R.drawable.plane_icon,
                        onClick = {
                            navController.navigate("game_screen_planes/$userId?displayName=${usernameToDisplay}")
                        }
                    )
                }
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

            Spacer(modifier = Modifier.height(95.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login_screen") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    Toast.makeText(context, logoutToastMessage, Toast.LENGTH_SHORT).show()
                    onLogoutNotification()
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

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val newLanguage = if (currentLanguage.value == "es") "en" else "es"
                    LocaleHelper.changeAndRestart(context, newLanguage)
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentLanguageDisplay,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

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
                        isMuted = !isMuted
                        backgroundPlayer.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                    }
            )
        }
    }
}

@Composable
fun GameOption(
    title: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}