package com.example.racingpower.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.R
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.AuthState
import androidx.compose.ui.graphics.SolidColor

/**
 * Composable para la pantalla de inicio de sesión.
 * Permite a los usuarios iniciar sesión o registrarse.
 * @param onLoginSuccess Callback que se llama cuando el inicio de sesión es exitoso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit // uid, displayName
) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val errorMessage by authViewModel.errorMessage

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Nuevo estado para controlar si estamos en modo registro o login
    var isRegistering by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        isLoading = authState is AuthState.Loading
        if (authState is AuthState.Authenticated) {
            val user = (authState as AuthState.Authenticated).user
            onLoginSuccess(user.uid, user.email ?: user.displayName ?: "Usuario")
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C3E50))
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.login_background),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // --- CAMBIO AQUÍ: Color del texto del título a blanco ---
                    Text(
                        text = if (isRegistering) "Crea tu cuenta" else "Bienvenido de nuevo",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, // ¡AHORA SE VERÁ!
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE74C3C),
                            unfocusedBorderColor = Color(0xFFBDC3C7),
                            // --- CAMBIO AQUÍ: Color de la etiqueta a blanco ---
                            focusedLabelColor = Color.White, // ¡AHORA SE VERÁ!
                            unfocusedLabelColor = Color(0xFFBDC3C7),
                            // --- OPCIONAL: Color del texto de entrada a blanco ---
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE74C3C),
                            unfocusedBorderColor = Color(0xFFBDC3C7),
                            // --- CAMBIO AQUÍ: Color de la etiqueta a blanco ---
                            focusedLabelColor = Color.White, // ¡AHORA SE VERÁ!
                            unfocusedLabelColor = Color(0xFFBDC3C7),
                            // --- OPCIONAL: Color del texto de entrada a blanco ---
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón principal (Iniciar Sesión o Registrarse)
                    Button(
                        onClick = {
                            if (isRegistering) {
                                authViewModel.register(email, password)
                            } else {
                                authViewModel.login(email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                    ) {
                        if (isLoading && authState is AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isRegistering) "Registrarse" else "Iniciar Sesión",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón para alternar entre modos
                    TextButton(
                        onClick = {
                            isRegistering = !isRegistering // Cambia el estado
                            email = "" // Limpia los campos al cambiar de modo
                            password = ""
                            authViewModel.errorMessage.value = null // Limpia errores
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRegistering) "¿Ya tienes una cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate aquí",
                            color = Color(0xFF3498DB), // Un color azul para el enlace (se ve bien con fondo oscuro)
                            fontSize = 16.sp
                        )
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}