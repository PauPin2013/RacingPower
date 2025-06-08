package com.example.racingpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.ui.theme.RacingPowerTheme
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.AuthState
import com.example.racingpower.views.InfiniteGameScreen
import com.example.racingpower.views.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RacingPowerTheme {
                // Obtiene una instancia del AuthViewModel
                val authViewModel: AuthViewModel = viewModel()
                // Recopila el estado de autenticación como un State
                val authState by authViewModel.authState.collectAsState()

                // AnimatedContent para transiciones suaves entre pantallas
                AnimatedContent(
                    targetState = authState,
                    transitionSpec = {
                        // Define la animación de transición
                        fadeIn(animationSpec = tween(durationMillis = 300)) + slideInVertically(
                            animationSpec = tween(durationMillis = 300),
                            initialOffsetY = { fullHeight -> fullHeight / 4 }
                        ) togetherWith fadeOut(animationSpec = tween(durationMillis = 300)) + slideOutVertically(
                            animationSpec = tween(durationMillis = 300),
                            targetOffsetY = { fullHeight -> fullHeight / 4 }
                        )
                    }, label = "AuthScreenTransition"
                ) { targetState ->
                    when (targetState) {
                        is AuthState.Loading -> {
                            // Puedes mostrar una pantalla de carga aquí
                            // Por ahora, solo un Box vacío o un CircularProgressIndicator
                            // No es necesario un Composable completo para esto
                        }
                        is AuthState.Authenticated -> {
                            // Si el usuario está autenticado, muestra la pantalla del juego
                            val user = targetState.user
                            InfiniteGameScreen(
                                userId = user.uid,
                                displayName = user.email ?: user.displayName ?: "Jugador",
                                onLogout = { authViewModel.logout() } // Pasa el callback de cerrar sesión
                            )
                        }
                        AuthState.Unauthenticated, is AuthState.Error -> {
                            // Si no hay usuario autenticado o hay un error, muestra la pantalla de inicio de sesión
                            LoginScreen(
                                onLoginSuccess = { uid, displayName ->
                                    // Cuando el login es exitoso, el AuthStateListener actualizará el estado,
                                    // y AnimatedContent se encargará de la transición a InfiniteGameScreen.
                                    // No es necesario llamar a ningún navigation aquí directamente.
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
