package com.example.racingpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// import com.example.racingpower.ui.game.InfiniteGameScreen // ELIMINAR ESTA LÍNEA
import com.example.racingpower.ui.theme.RacingPowerTheme
import com.example.racingpower.views.InfiniteGameScreen
import com.example.racingpower.views.LoginScreen // Importa LoginScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth // Importa la extensión ktx

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa Firebase Auth
        auth = Firebase.auth

        setContent {
            RacingPowerTheme {
                val navController = rememberNavController()

                // Verificar si hay un usuario logueado al iniciar la app
                val startDestination = remember {
                    if (auth.currentUser != null) {
                        // Si hay un usuario logueado, ir directamente a la pantalla del juego con su UID
                        "game_screen/${auth.currentUser?.uid}"
                    } else {
                        // Si no hay usuario, ir a la pantalla de login
                        "login_screen"
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login_screen") {
                        LoginScreen(navController = navController)
                    }
                    // Definir la ruta para la pantalla del juego, esperando un argumento 'userId'
                    composable("game_screen/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        InfiniteGameScreen(userId = userId) // Pasa el userId a InfiniteGameScreen
                    }
                }
            }
        }
    }
}
