package com.example.racingpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Importa para usar by viewModels()
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator // Para la pantalla de carga
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

import com.example.racingpower.ui.theme.RacingPowerTheme
import com.example.racingpower.viewmodels.AuthViewModel // ASUME que ya tienes este ViewModel
import com.example.racingpower.viewmodels.AuthState // ASUME que ya tienes esta clase de estado
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import com.example.racingpower.views.GameSelectionScreen
import com.example.racingpower.views.InfiniteGameScreen
import com.example.racingpower.views.InfinitePlaneGameScreen
import com.example.racingpower.views.LoginScreen
import com.example.racingpower.views.RegisterScreen

class MainActivity : ComponentActivity() {
    // Inyecta el AuthViewModel usando by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RacingPowerTheme {
                val navController = rememberNavController()
                // Observa el estado de autenticación del AuthViewModel
                val authState by authViewModel.authState.collectAsState()

                // Define la ruta de inicio basada en el estado de autenticación
                // Usaremos "splash_screen" como destino inicial para manejar la lógica de carga
                NavHost(navController = navController, startDestination = "splash_screen") {

                    // 1. Pantalla de carga/Splash Screen
                    composable("splash_screen") {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator() // Muestra un indicador de carga
                        }
                        // Lanza un efecto para navegar una vez que el estado de autenticación se determine
                        LaunchedEffect(authState) {
                            when (val currentState = authState) {
                                is AuthState.Authenticated -> {
                                    // Si el usuario está autenticado, navega a la selección de juego
                                    navController.navigate("game_selection_screen/${currentState.user.uid}") {
                                        popUpTo("splash_screen") { inclusive = true } // Elimina la splash screen de la pila
                                    }
                                }
                                AuthState.Unauthenticated, is AuthState.Error -> {
                                    // Si no está autenticado o hay un error, navega a la pantalla de login
                                    navController.navigate("login_screen") {
                                        popUpTo("splash_screen") { inclusive = true } // Elimina la splash screen de la pila
                                    }
                                }
                                else -> {
                                    // Estado de carga, no hacer nada aún
                                }
                            }
                        }
                    }

                    // 2. Pantalla de Login
                    composable("login_screen") {
                        LoginScreen(navController = navController)
                    }

                    // 3. Pantalla de Registro
                    composable("register_screen") {
                        RegisterScreen(navController = navController)
                    }

                    // 4. Pantalla de Selección de Juego (requiere userId)
                    composable(
                        "game_selection_screen/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        // CORRECCIÓN AQUÍ: Cambiar 'username =' a 'userId ='
                        GameSelectionScreen(userId = userId, navController = navController)
                    }

                    // 5. Pantalla del Juego de Carros (requiere userId)
                    composable(
                        "game_screen_cars/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        InfiniteGameScreen(username = userId, navController = navController)
                    }

                    // 6. Pantalla del Juego de Aviones (requiere userId)
                    composable(
                        "game_screen_planes/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        val planeGameViewModel: InfiniteGameViewModel = remember {
                            InfiniteGameViewModel(application)
                        }
                        InfinitePlaneGameScreen(
                            username = userId,
                            viewModel = planeGameViewModel,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}