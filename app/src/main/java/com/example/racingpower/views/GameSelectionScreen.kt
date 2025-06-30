package com.example.racingpower.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Importar LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Importar viewModel
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.AuthViewModel // Asegúrate de que este import sea correcto
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@Composable
fun GameSelectionScreen(
    userId: String, // Esto es el UID del usuario
    navController: NavController
) {
    // Obtener el contexto local para el Toast
    val context = LocalContext.current // <--- CORRECCIÓN: Definir 'context'

    // Obtener la instancia de AuthViewModel
    val authViewModel: AuthViewModel = viewModel() // <--- CORRECCIÓN: Obtener 'authViewModel'

    val auth: FirebaseAuth = Firebase.auth
    val currentUser = auth.currentUser
    // Obtén el nombre de visualización o usa "Invitado" si no está disponible
    val usernameToDisplay = currentUser?.displayName ?: "Invitado"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A49)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "¡Bienvenido $usernameToDisplay!", // Usa el nombre de visualización
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
                        // Navegar a la pantalla de juego de carros, usando 'userId'
                        navController.navigate("game_screen_cars/$userId") // <--- CORRECCIÓN: Usar 'userId'
                    }
                )
                GameOption(
                    title = "Juego de Aviones",
                    imageRes = R.drawable.plane_icon,
                    onClick = {
                        // Navegar a la pantalla de juego de aviones, usando 'userId'
                        navController.navigate("game_screen_planes/$userId") // <--- CORRECCIÓN: Usar 'userId'
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            Button(
                onClick = {
                    authViewModel.logout() // Ahora 'authViewModel' está disponible
                    navController.navigate("login_screen") {
                        popUpTo(navController.graph.id) { inclusive = true } // Clear entire back stack
                    }
                    Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show() // Ahora 'context' está disponible
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