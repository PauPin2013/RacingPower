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
import androidx.compose.ui.res.stringResource // Importa stringResource
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
import com.example.racingpower.utils.LocaleHelper // Importa LocaleHelper

@Composable
fun GameSelectionScreen(
    userId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val auth: FirebaseAuth = Firebase.auth
    // Usa stringResource para "Invitado"
    val guestDisplayName = stringResource(id = R.string.guest_display_name)
    val currentUser = auth.currentUser
    val usernameToDisplay = currentUser?.displayName ?: guestDisplayName

    // Strings para los Toasts
    val logoutToastText = stringResource(id = R.string.logout_toast)
    val welcomeUserFormat = stringResource(id = R.string.welcome_user_format)

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

    // Obtenemos el idioma actual para mostrarlo y para la lógica del botón
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getPersistedLocale(context)) }


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
                contentDescription = "Mute Button", // Considera traducir este contentDescription
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
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
        ) {
            Text(
                text = String.format(welcomeUserFormat, usernameToDisplay), // Usa stringResource con formato
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(id = R.string.select_game_title), // Usa stringResource
                fontSize = 18.sp,
                color = Color.LightGray
            )
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                GameOption(
                    title = stringResource(id = R.string.game_cars_title), // Usa stringResource
                    imageRes = R.drawable.car_icon,
                    onClick = {
                        navController.navigate("game_screen_cars/$userId")
                    }
                )
                GameOption(
                    title = stringResource(id = R.string.game_planes_title), // Usa stringResource
                    imageRes = R.drawable.plane_icon,
                    onClick = {
                        navController.navigate("game_screen_planes/$userId")
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login_screen") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    Toast.makeText(context, logoutToastText, Toast.LENGTH_SHORT).show() // Usa el string pre-obtenido
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Text(stringResource(id = R.string.logout_button_text), fontSize = 18.sp) // Usa stringResource
            }

            Spacer(modifier = Modifier.height(24.dp))

            // INICIO DEL CAMBIO
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Agrupamos el botón y el texto en una Column. Aquí controlaremos el espaciado
                verticalArrangement = Arrangement.Center // Puedes usar Center o Top para alinear si hay espacio disponible
            ) {
                // Botón para cambiar el idioma
                Button(
                    onClick = {
                        val newLanguage = if (currentLanguage.value == "es") "en" else "es"
                        LocaleHelper.changeAndRestart(context, newLanguage)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(id = R.string.change_language_button))
                }

                // NUEVO SPACER: Añade este Spacer para dar el espacio deseado
                Spacer(modifier = Modifier.height(6.dp)) // Ajusta este valor (e.g., 2.dp, 6.dp)

                // Mostrar el idioma actual
                Text(
                    text = stringResource(id = R.string.current_language),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            // FIN DEL CAMBIO
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
            contentDescription = title, // Considera traducir este contentDescription
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}