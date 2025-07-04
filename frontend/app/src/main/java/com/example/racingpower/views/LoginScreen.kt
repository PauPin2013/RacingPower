package com.example.racingpower.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.utils.LocaleHelper
import com.example.racingpower.viewmodels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.layout.ContentScale // Importa ContentScale

// Anotación para indicar que se están utilizando APIs experimentales de Material3 (como TextFieldDefaults.colors)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController, // Controlador de navegación para la pantalla.
    authViewModel: AuthViewModel = viewModel(), // ViewModel para la autenticación, con valor por defecto.
    onLoginSuccessNotification: (String) -> Unit // Callback para notificar el éxito del login con el nombre de usuario.
) {
    val context = LocalContext.current // Obtiene el contexto local de la composición.
    var email by remember { mutableStateOf("") } // Estado mutable para el campo de email.
    var password by remember { mutableStateOf("") } // Estado mutable para el campo de contraseña.
    var isLoading by remember { mutableStateOf(false) } // Estado mutable para indicar si se está cargando (login en progreso).

    // Observa los mensajes de error y el perfil de usuario del AuthViewModel.
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    // Cadenas de recursos localizadas para los mensajes de Toast y textos de UI.
    val enterCredentialsToastText = stringResource(id = R.string.enter_credentials_toast)
    val welcomeMessageFormat = stringResource(id = R.string.welcome_message)
    val guestModeToastText = stringResource(id = R.string.guest_mode_toast)

    // Efecto lanzado cuando el `errorMessage` cambia, para mostrar un Toast con el error.
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show() // Muestra el mensaje de error.
            authViewModel.clearErrorMessage() // Limpia el mensaje de error en el ViewModel después de mostrarlo.
        }
    }

    // Función para intentar iniciar sesión.
    fun performLogin() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, enterCredentialsToastText, Toast.LENGTH_SHORT).show() // Valida que los campos no estén vacíos.
            return
        }
        isLoading = true // Activa el indicador de carga.
        authViewModel.login(
            email = email,
            password = password,
            onSuccess = { firebaseUser ->
                isLoading = false // Desactiva el indicador de carga.
                // Determina el nombre de usuario para la notificación.
                val usernameForNotification = userProfile?.displayName ?: firebaseUser.email ?: "Usuario"
                // Formatea el mensaje de bienvenida.
                val welcomeMessage = String.format(welcomeMessageFormat, usernameForNotification)
                Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show() // Muestra el mensaje de bienvenida.
                onLoginSuccessNotification(usernameForNotification) // Llama al callback de éxito.
                // Navega a la pantalla de selección de juego y limpia la pila de navegación.
                navController.navigate("game_selection_screen/${firebaseUser.uid}") {
                    popUpTo("login_screen") { inclusive = true } // Elimina la pantalla de login de la pila.
                }
            },
            onError = { msg ->
                isLoading = false // Desactiva el indicador de carga.
                // El error ya es manejado por el LaunchedEffect que observa `errorMessage`.
            }
        )
    }

    // Estado mutable para la selección del idioma actual.
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getPersistedLocale(context)) }

    // BOX que contendrá la imagen de fondo y el contenido principal de la pantalla.
    Box(
        modifier = Modifier.fillMaxSize() // El Box ocupa todo el espacio disponible.
    ) {
        // Imagen de fondo que ocupa todo el espacio dentro del Box.
        Image(
            painter = painterResource(id = R.drawable.fondo), // Carga la imagen de fondo desde los recursos.
            contentDescription = null, // No es necesaria una descripción ya que es decorativa.
            modifier = Modifier.fillMaxSize(), // La imagen llena todo el espacio.
            contentScale = ContentScale.Crop // Escala la imagen para que llene el Box, recortando si es necesario.
        )

        // Capa de superposición para oscurecer la imagen de fondo y mejorar la legibilidad del texto.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Fondo negro semitransparente. Ajusta el alpha para más/menos oscuridad.
        )

        // Columna que contiene todos los elementos de la UI de login.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // Padding alrededor de la columna.
            verticalArrangement = Arrangement.Center, // Centra el contenido verticalmente.
            horizontalAlignment = Alignment.CenterHorizontally // Centra el contenido horizontalmente.
        ) {

            Spacer(modifier = Modifier.height(32.dp)) // Espaciador superior.
            // Título de la aplicación.
            Text(
                text = "Racing Power",
                fontSize = 32.sp,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Campo de texto para el email.
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email_label)) },
                colors = TextFieldDefaults.colors( // Define los colores del campo de texto.
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(), // El campo de texto ocupa todo el ancho disponible.
                singleLine = true // Permite una sola línea de texto.
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Campo de texto para la contraseña.
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.password_label)) },
                visualTransformation = PasswordVisualTransformation(), // Oculta el texto de la contraseña.
                colors = TextFieldDefaults.colors( // Define los colores del campo de texto.
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.LightGray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Botón de inicio de sesión.
            Button(
                onClick = { performLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading // Deshabilita el botón mientras se carga.
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) // Muestra un spinner si está cargando.
                } else {
                    Text(stringResource(id = R.string.login_button_text), fontSize = 18.sp) // Texto del botón.
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Botón para navegar a la pantalla de registro.
            TextButton(
                onClick = { navController.navigate("register_screen") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringResource(id = R.string.no_account_text), color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Botón para iniciar sesión como invitado.
            TextButton(
                onClick = {
                    // Navega a la pantalla de selección de juego como invitado y limpia la pila.
                    navController.navigate("game_selection_screen/guest_user") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                    Toast.makeText(context, guestModeToastText, Toast.LENGTH_SHORT).show() // Muestra un Toast de modo invitado.
                    onLoginSuccessNotification(context.getString(R.string.guest_display_name)) // Notifica el login como invitado.
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringResource(id = R.string.guest_login_text), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón para cambiar el idioma de la aplicación.
            Button(
                onClick = {
                    // Alterna entre "es" y "en" y reinicia la actividad para aplicar el cambio de idioma.
                    val newLanguage = if (currentLanguage.value == "es") "en" else "es"
                    LocaleHelper.changeAndRestart(context, newLanguage)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(id = R.string.change_language_button))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Muestra el idioma actual.
            Text(
                text = stringResource(id = R.string.current_language),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}