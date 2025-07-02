package com.example.racingpower.views

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.res.stringResource
import com.example.racingpower.utils.LocaleHelper

@Composable
fun GameSelectionScreen(
    userId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val auth: FirebaseAuth = Firebase.auth
    val currentUser = auth.currentUser
    val guestDisplayName = stringResource(id = R.string.guest_display_name) // Asegúrate de obtener "Invitado" localizado
    val usernameToDisplay = currentUser?.displayName ?: guestDisplayName // Usa el stringResource para Invitado

    // 🎵 Música de fondo
    val backgroundPlayer = remember { MediaPlayer.create(context, R.raw.background_music3) }
    var isMuted by remember { mutableStateOf(false) }

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

    // Obtener el idioma actual para mostrarlo y para la lógica del botón
    // Usamos `remember(key = ...)` para que se recomponga si el locale cambia
    val currentLanguage = remember(LocaleHelper.getPersistedLocale(context)) {
        mutableStateOf(LocaleHelper.getPersistedLocale(context))
    }

    // --- Carga todos los string resources aquí ---
    val welcomeUserFormat = stringResource(id = R.string.welcome_user_format)
    val selectGameTitle = stringResource(id = R.string.select_game_title)
    val gameCarsTitle = stringResource(id = R.string.game_cars_title)
    val gamePlanesTitle = stringResource(id = R.string.game_planes_title)
    val leaderboardButtonText = stringResource(id = R.string.leaderboard_button)
    val logoutButtonText = stringResource(id = R.string.logout_button_text)
    val logoutToastMessage = stringResource(id = R.string.logout_toast)
    val changeLanguageButtonText = stringResource(id = R.string.change_language_button)
    val currentLanguageDisplay = stringResource(id = R.string.current_language)
    // --- Fin de carga de string resources ---


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A49))
    ) {
        // 🔇 Icono para mutear música
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
                        if (isMuted) {
                            backgroundPlayer.setVolume(0f, 0f)
                        } else {
                            backgroundPlayer.setVolume(1f, 1f)
                        }
                    }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 16.dp)
        ) {
            Text(
                text = String.format(welcomeUserFormat, usernameToDisplay), // Usando stringResource y el username
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = selectGameTitle, // Usando stringResource
                fontSize = 18.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                GameOption(
                    title = gameCarsTitle, // Usando stringResource
                    imageRes = R.drawable.car_icon,
                    onClick = {
                        navController.navigate("game_screen_cars/$userId")
                    }
                )
                GameOption(
                    title = gamePlanesTitle, // Usando stringResource
                    imageRes = R.drawable.plane_icon,
                    onClick = {
                        navController.navigate("game_screen_planes/$userId")
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Botón de Clasificación
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

            // Botón "Cerrar Sesión"
            Button(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login_screen") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    Toast.makeText(context, logoutToastMessage, Toast.LENGTH_SHORT).show()
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

            // Botón de Cambio de Idioma
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
                Text(changeLanguageButtonText, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentLanguageDisplay, // Usando stringResource
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun GameOption(
    title: String, // Este title ahora será el stringResource cargado
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
            contentDescription = title, // El contentDescription también puede ser el título localizado
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}