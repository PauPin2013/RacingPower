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

@Composable
fun GameSelectionScreen(
    userId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val auth: FirebaseAuth = Firebase.auth
    val currentUser = auth.currentUser
    val usernameToDisplay = currentUser?.displayName ?: "Invitado"

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
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
        ) {
            Text(
                text = "¡Bienvenido $usernameToDisplay!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Selecciona tu juego",
                fontSize = 18.sp,
                color = Color.LightGray
            )
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                GameOption(
                    title = "Juego de Carros",
                    imageRes = R.drawable.car_icon,
                    onClick = {
                        navController.navigate("game_screen_cars/$userId")
                    }
                )
                GameOption(
                    title = "Juego de Aviones",
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
                    Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Text("Cerrar Sesión", fontSize = 18.sp)
            }
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
