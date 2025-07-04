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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    // RECOLECTA EL StateFlow<String?> como un State<String?>
    val errorMessageState by authViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val registrationSuccessToastFormat = stringResource(id = R.string.registration_success_toast)
    val closeActionLabel = stringResource(id = R.string.close_action_label)
    val enterUsernameToastText = stringResource(id = R.string.enter_username_toast)
    val enterCredentialsToastText = stringResource(id = R.string.enter_credentials_toast)
    val passwordsDoNotMatchToastText = stringResource(id = R.string.passwords_do_not_match_toast)

    // Observar cambios en el estado de autenticación (AuthViewModel.authState)
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                val welcomeMessage = String.format(registrationSuccessToastFormat, usernameInput.ifBlank { user.email ?: "" })
                Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show()
                navController.navigate("game_selection_screen/${user.uid}") {
                    popUpTo("login_screen") { inclusive = true }
                }
            }
            // Si el error se maneja a través del AuthState.Error, el mensaje se mostrará aquí.
            is AuthState.Error -> {
                val message = (authState as AuthState.Error).message
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = closeActionLabel,
                        duration = SnackbarDuration.Long
                    )
                }
                // Si el error ya se mostró por authState, no es estrictamente necesario que
                // el ViewModel lo limpie aquí, a menos que quieras que el mensaje desaparezca
                // incluso si el usuario no hizo nada.
                // authViewModel.clearErrorMessage() // Opcional, dependiendo de la lógica de tu ViewModel
            }
            else -> { /* Do nothing for Loading or Unauthenticated states here */ }
        }
    }

    // Observar mensajes de error específicos (AuthViewModel.errorMessage)
    LaunchedEffect(errorMessageState) {
        // Usa `it` implícitamente o nombra el parámetro lambda explícitamente `currentErrorMessage`
        errorMessageState?.let { currentErrorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = currentErrorMessage, // Usa el valor del mensaje de error
                    actionLabel = closeActionLabel,
                    duration = SnackbarDuration.Long
                )
            }
            // Importante: Llama al método en el ViewModel para limpiar el mensaje de error
            // una vez que ha sido mostrado. Esto evita que se muestre repetidamente.
            authViewModel.clearErrorMessage()
        }
    }

    val currentLanguage = remember { mutableStateOf(LocaleHelper.getPersistedLocale(context)) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1B2A49))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(id = R.string.create_account_title),
                fontSize = 32.sp,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text(stringResource(id = R.string.username_label)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User icon") },
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

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email_label)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email icon") },
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

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.password_label)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock icon") },
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
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(id = R.string.confirm_password_label)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock icon") },
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

            Button(
                onClick = {
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
                        // Pasando los callbacks onSuccess y onError
                        authViewModel.register(
                            email,
                            password,
                            usernameInput,
                            onSuccess = { user ->
                                // La lógica de éxito (Toast y navegación) ya se maneja en LaunchedEffect(authState)
                                // cuando authState cambia a Authenticated.
                            },
                            onError = { errorMessageFromViewModel ->
                                // Aquí puedes manejar errores específicos de la llamada a register si no quieres que pasen por el errorMessageState general.
                                // Sin embargo, dado que el ViewModel ya establece _errorMessage.value en caso de error,
                                // el LaunchedEffect(errorMessageState) se encargará de mostrarlo.
                                // Puedes poner un Log.e aquí si quieres.
                            }
                        )
                    }
                },
                enabled = authState != AuthState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(id = R.string.register_button_text), fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.already_have_account_text), color = Color.White.copy(alpha = 0.7f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
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

            Text(
                text = stringResource(id = R.string.current_language),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}