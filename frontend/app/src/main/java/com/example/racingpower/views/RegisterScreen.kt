package com.example.racingpower.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.AuthState
import com.example.racingpower.utils.LocaleHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale

// Anotación para indicar que se están utilizando APIs experimentales de Material3 (como TextFieldDefaults.colors).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController, // Controlador de navegación para la pantalla.
    authViewModel: AuthViewModel = viewModel(), // ViewModel para la autenticación, con valor por defecto.
    onLoginSuccessNotification: (String) -> Unit // Callback para notificar el éxito del login con el nombre de usuario.
) {
    val context = LocalContext.current // Obtiene el contexto local de la composición.
    var email by remember { mutableStateOf("") } // Estado mutable para el campo de email.
    var password by remember { mutableStateOf("") } // Estado mutable para el campo de contraseña.
    var confirmPassword by remember { mutableStateOf("") } // Estado mutable para el campo de confirmación de contraseña.
    var usernameInput by remember { mutableStateOf("") } // Estado mutable para el campo de nombre de usuario.

    // Observa el estado de autenticación y los mensajes de error del AuthViewModel.
    val authState by authViewModel.authState.collectAsState()
    val errorMessageState by authViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() } // Estado para controlar el Snackbar.
    val scope = rememberCoroutineScope() // Alcance del coroutine para lanzar efectos secundarios.

    // Cadenas de recursos localizadas para los mensajes de Toast y textos de UI.
    val registrationSuccessToastFormat = stringResource(id = R.string.registration_success_toast)
    val closeActionLabel = stringResource(id = R.string.close_action_label)
    val enterUsernameToastText = stringResource(id = R.string.enter_username_toast)
    val enterCredentialsToastText = stringResource(id = R.string.enter_credentials_toast)
    val passwordsDoNotMatchToastText = stringResource(id = R.string.passwords_do_not_match_toast)

    // Efecto lanzado cuando el `authState` cambia para manejar el éxito o error de la autenticación.
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                // Usa `usernameInput` para el mensaje de bienvenida si no está vacío, de lo contrario el email.
                val usernameForNotification = usernameInput.ifBlank { user.email ?: "Usuario" }

                val welcomeMessage = String.format(registrationSuccessToastFormat, usernameForNotification)
                Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show()

                // Llama al callback de notificación de éxito de login.
                onLoginSuccessNotification(usernameForNotification)

                // Navega a la pantalla de selección de juego y limpia la pila de navegación.
                navController.navigate("game_selection_screen/${user.uid}") {
                    popUpTo("login_screen") { inclusive = true } // Elimina la pantalla de login de la pila.
                }
            }
            is AuthState.Error -> {
                val message = (authState as AuthState.Error).message
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = closeActionLabel,
                        duration = SnackbarDuration.Long
                    )
                }
            }
            else -> { /* No hace nada si el estado es Loading o Initial. */ }
        }
    }

    // Efecto lanzado cuando el `errorMessageState` cambia para mostrar errores en un Snackbar.
    LaunchedEffect(errorMessageState) {
        errorMessageState?.let { currentErrorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = currentErrorMessage,
                    actionLabel = closeActionLabel,
                    duration = SnackbarDuration.Long
                )
            }
            authViewModel.clearErrorMessage() // Limpia el mensaje de error en el ViewModel después de mostrarlo.
        }
    }

    // Estado mutable para la selección del idioma actual.
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getPersistedLocale(context)) }

    // Scaffold proporciona una estructura básica de diseño con soporte para Snackbar.
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica el padding del Scaffold.
        ) {
            // Imagen de fondo que ocupa todo el espacio.
            Image(
                painter = painterResource(id = R.drawable.fondo), // Carga la imagen de fondo.
                contentDescription = null, // No es necesaria una descripción ya que es decorativa.
                modifier = Modifier.fillMaxSize(), // La imagen llena todo el espacio.
                contentScale = ContentScale.Crop // Escala la imagen para que llene el Box, recortando si es necesario.
            )

            // Capa de superposición para oscurecer la imagen de fondo y mejorar la legibilidad del texto.
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)) // Fondo negro semitransparente.
            )

            // Columna que contiene todos los elementos de la UI de registro.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), // Padding alrededor de la columna.
                horizontalAlignment = Alignment.CenterHorizontally, // Centra los elementos hijos horizontalmente.
                verticalArrangement = Arrangement.Center // Centra los elementos hijos verticalmente.
            ) {

                Spacer(modifier = Modifier.height(32.dp)) // Espaciador superior.
                // Título de la pantalla de registro.
                Text(
                    text = stringResource(id = R.string.create_account_title),
                    fontSize = 32.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Campo de texto para el nombre de usuario.
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text(stringResource(id = R.string.username_label)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User icon") }, // Icono de persona.
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

                // Campo de texto para el email.
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.email_label)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email icon") }, // Icono de email.
                    colors = TextFieldDefaults.colors(
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
                Spacer(modifier = Modifier.height(16.dp))

                // Campo de texto para la contraseña.
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(id = R.string.password_label)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock icon") }, // Icono de candado.
                    visualTransformation = PasswordVisualTransformation(), // Oculta el texto de la contraseña.
                    colors = TextFieldDefaults.colors(
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
                Spacer(modifier = Modifier.height(16.dp))

                // Campo de texto para confirmar la contraseña.
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(id = R.string.confirm_password_label)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock icon") }, // Icono de candado.
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
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

                // Botón de registro.
                Button(
                    onClick = {
                        // Validaciones de los campos antes de intentar el registro.
                        if (usernameInput.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = enterUsernameToastText,
                                    actionLabel = closeActionLabel,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        } else if (email.isBlank() || password.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = enterCredentialsToastText,
                                    actionLabel = closeActionLabel,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        } else if (password != confirmPassword) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = passwordsDoNotMatchToastText,
                                    actionLabel = closeActionLabel,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        } else {
                            // Llama a la función de registro del ViewModel.
                            authViewModel.register(
                                email,
                                password,
                                usernameInput,
                                onSuccess = { user ->
                                    // La lógica de éxito (Toast y navegación) ya se maneja en LaunchedEffect(authState)
                                    // cuando authState cambia a Authenticated. La notificación también se llamará allí.
                                },
                                onError = { errorMessageFromViewModel ->
                                    // Los errores son manejados por LaunchedEffect(errorMessageState).
                                }
                            )
                        }
                    },
                    enabled = authState != AuthState.Loading, // Deshabilita el botón mientras se carga.
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (authState == AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) // Muestra un spinner si está cargando.
                    } else {
                        Text(stringResource(id = R.string.register_button_text), fontSize = 18.sp) // Texto del botón.
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Botón para volver a la pantalla de login.
                TextButton(
                    onClick = { navController.popBackStack() }, // Vuelve a la pantalla anterior en la pila.
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.already_have_account_text), color = Color.White.copy(alpha = 0.7f))
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
}