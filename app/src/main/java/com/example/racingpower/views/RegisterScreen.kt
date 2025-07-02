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
import androidx.compose.ui.res.stringResource // ¡IMPORTA stringResource!
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.AuthState
import com.example.racingpower.utils.LocaleHelper // ¡IMPORTA LocaleHelper!
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
    var username by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    val errorMessage by authViewModel.errorMessage

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- Obtener strings fuera de los LaunchedEffect y funciones locales asíncronas ---
    val registrationSuccessToastFormat = stringResource(id = R.string.registration_success_toast)
    val closeActionLabel = stringResource(id = R.string.close_action_label)
    val enterUsernameToastText = stringResource(id = R.string.enter_username_toast)
    val enterCredentialsToastText = stringResource(id = R.string.enter_credentials_toast)
    val passwordsDoNotMatchToastText = stringResource(id = R.string.passwords_do_not_match_toast)


    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                // Usa el string format ya obtenido
                val welcomeMessage = String.format(registrationSuccessToastFormat, user.email ?: "")
                Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show()
                navController.navigate("game_selection_screen/${user.uid}") {
                    popUpTo("login_screen") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                val message = (authState as AuthState.Error).message
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = closeActionLabel, // Usa el string pre-obtenido
                        duration = SnackbarDuration.Long
                    )
                }
                authViewModel.errorMessage.value = null // Clear the error after showing it
            }
            else -> { /* Do nothing for Loading or Unauthenticated states here */ }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = closeActionLabel, // Usa el string pre-obtenido
                    duration = SnackbarDuration.Long
                )
            }
            authViewModel.errorMessage.value = null // Clear the error after showing it
        }
    }

    // Obtenemos el idioma actual para mostrarlo y para la lógica del botón
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
                text = stringResource(id = R.string.create_account_title), // Usa stringResource
                fontSize = 32.sp,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Username input field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(id = R.string.username_label)) }, // Usa stringResource
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

            // Email input field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email_label)) }, // Usa stringResource
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

            // Password input field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.password_label)) }, // Usa stringResource
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

            // Confirm password input field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(id = R.string.confirm_password_label)) }, // Usa stringResource
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
                    if (username.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = enterUsernameToastText, // Usa el string pre-obtenido
                                actionLabel = closeActionLabel,
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else if (email.isBlank() || password.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = enterCredentialsToastText, // Usa el string pre-obtenido
                                actionLabel = closeActionLabel,
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else if (password != confirmPassword) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = passwordsDoNotMatchToastText, // Usa el string pre-obtenido
                                actionLabel = closeActionLabel,
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else {
                        authViewModel.register(email, password, username)
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
                    Text(stringResource(id = R.string.register_button_text), fontSize = 18.sp) // Usa stringResource
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { navController.popBackStack() }, // Go back to login screen
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.already_have_account_text), color = Color.White.copy(alpha = 0.7f)) // Usa stringResource
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón para cambiar el idioma
            Button(
                onClick = {
                    val newLanguage = if (currentLanguage.value == "es") "en" else "es"
                    LocaleHelper.changeAndRestart(context, newLanguage)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(id = R.string.change_language_button)) // Usa stringResource
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar el idioma actual
            Text(
                text = stringResource(id = R.string.current_language), // Usa stringResource
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}