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
import androidx.compose.ui.res.stringResource // Importa stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.racingpower.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.example.racingpower.utils.LocaleHelper // Importa tu LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val auth: FirebaseAuth = Firebase.auth

    // --- Obtén los strings *fuera* de performLogin y del callback asíncrono ---
    val enterCredentialsToastText = stringResource(id = R.string.enter_credentials_toast)
    val welcomeMessageFormat = stringResource(id = R.string.welcome_message)
    val loginErrorFormat = stringResource(id = R.string.login_error)
    val guestModeToastText = stringResource(id = R.string.guest_mode_toast)

    // Función para manejar el inicio de sesión
    // Ya NO necesita ser @Composable si no llama a otras funciones @Composable directamente.
    fun performLogin() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, enterCredentialsToastText, Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Usa el string format ya obtenido
                    val welcomeMessage = String.format(welcomeMessageFormat, user?.email ?: "")
                    Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show()
                    navController.navigate("game_selection_screen/${user?.uid}") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                } else {
                    // Usa el string format ya obtenido
                    val errorMessage = String.format(loginErrorFormat, task.exception?.message ?: "Error desconocido")
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    // Obtenemos el idioma actual para mostrarlo y para la lógica del botón
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getPersistedLocale(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A49))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
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
            label = { Text(stringResource(id = R.string.email_label)) },
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
            // La llamada a performLogin() está bien aquí, ya que performLogin() ahora no es Composable
            // y no llama a Composable directamente en su cuerpo.
            onClick = { performLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(id = R.string.login_button_text), fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = { navController.navigate("register_screen") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(stringResource(id = R.string.no_account_text), color = Color.White.copy(alpha = 0.7f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = {
                navController.navigate("game_selection_screen/guest_user") {
                    popUpTo("login_screen") { inclusive = true }
                }
                Toast.makeText(context, guestModeToastText, Toast.LENGTH_SHORT).show() // Usa el string pre-obtenido
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(stringResource(id = R.string.guest_login_text), color = Color.Gray)
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
            Text(stringResource(id = R.string.change_language_button))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mostrar el idioma actual
        Text(
            text = stringResource(id = R.string.current_language),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}