package com.example.racingpower.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.* // Asegúrate de que todas las dependencias de Material3 estén importadas
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.racingpower.R // Asegúrate de tener un drawable para el logo si lo usas
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth // Importa la extensión ktx para FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val auth: FirebaseAuth = Firebase.auth

    // Función para manejar el inicio de sesión
    fun performLogin() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Por favor, ingresa correo y contraseña.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(context, "¡Bienvenido, ${user?.email}!", Toast.LENGTH_SHORT).show()
                    // Navegar a la pantalla del juego pasando el UID del usuario
                    navController.navigate("game_screen/${user?.uid}") {
                        popUpTo("login_screen") { inclusive = true } // Elimina la pantalla de login del back stack
                    }
                } else {
                    Toast.makeText(context, "Error de inicio de sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Función para manejar el registro
    fun performRegister() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Por favor, ingresa correo y contraseña para registrarte.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(context, "¡Registro exitoso! ¡Bienvenido, ${user?.email}!", Toast.LENGTH_SHORT).show()
                    // Navegar a la pantalla del juego después del registro exitoso
                    navController.navigate("game_screen/${user?.uid}") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Error de registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A49)) // Color de fondo consistente con el juego
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo o icono de la aplicación (opcional)
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Reemplaza con tu propio logo
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Racing Power",
            fontSize = 32.sp,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            // CORRECCIÓN: Usar focusedIndicatorColor y unfocusedIndicatorColor para el borde
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.White, // Usar indicatorColor para el borde del OutlinedTextField
                unfocusedIndicatorColor = Color.LightGray, // Usar indicatorColor para el borde del OutlinedTextField
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            // CORRECCIÓN: Usar focusedIndicatorColor y unfocusedIndicatorColor para el borde
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.White, // Usar indicatorColor para el borde del OutlinedTextField
                unfocusedIndicatorColor = Color.LightGray, // Usar indicatorColor para el borde del OutlinedTextField
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { performLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading // Deshabilita el botón mientras se carga
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Iniciar Sesión", fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { performRegister() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("¿No tienes cuenta? Regístrate aquí", color = Color.White.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para saltar el login (para desarrollo o prueba)
        TextButton(
            onClick = {
                // Navegar directamente a la pantalla del juego sin autenticación real
                // ¡Usa esto con precaución, idealmente solo para desarrollo!
                navController.navigate("game_screen/guest_user") { // Usar un ID de invitado
                    popUpTo("login_screen") { inclusive = true }
                }
                Toast.makeText(context, "Iniciando como invitado.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Continuar como invitado (para probar)", color = Color.Gray)
        }
    }
}
