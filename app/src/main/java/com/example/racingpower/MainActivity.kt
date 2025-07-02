package com.example.racingpower

import android.content.Context // Importa Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.AuthState
import com.example.racingpower.viewmodels.InfiniteGameViewModel
import com.example.racingpower.views.GameSelectionScreen
import com.example.racingpower.views.InfiniteGameScreen
import com.example.racingpower.views.InfinitePlaneGameScreen
import com.example.racingpower.views.LoginScreen
import com.example.racingpower.views.RegisterScreen
import com.example.racingpower.utils.LocaleHelper // Importa tu LocaleHelper
import com.example.racingpower.views.LeaderboardScreen

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    // SOBREESCRIBE attachBaseContext para aplicar el Locale antes de que la Activity sea creada
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RacingPowerTheme {
                val navController = rememberNavController()
                val authState by authViewModel.authState.collectAsState()

                NavHost(navController = navController, startDestination = "splash_screen") {

                    composable("splash_screen") {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        LaunchedEffect(authState) {
                            when (val currentState = authState) {
                                is AuthState.Authenticated -> {
                                    navController.navigate("game_selection_screen/${currentState.user.uid}") {
                                        popUpTo("splash_screen") { inclusive = true }
                                    }
                                }
                                AuthState.Unauthenticated, is AuthState.Error -> {
                                    navController.navigate("login_screen") {
                                        popUpTo("splash_screen") { inclusive = true }
                                    }
                                }
                                else -> {
                                    // Estado de carga, no hacer nada aún
                                }
                            }
                        }
                    }

                    composable("login_screen") {
                        LoginScreen(navController = navController)
                    }

                    composable("register_screen") {
                        RegisterScreen(navController = navController)
                    }

                    composable(
                        "game_selection_screen/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        GameSelectionScreen(userId = userId, navController = navController)
                    }

                    composable(
                        "game_screen_cars/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        InfiniteGameScreen(username = userId, navController = navController)
                    }

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
                    composable("leaderboard_screen") {
                        LeaderboardScreen(navController = navController)
                    }
                }
            }
        }
    }
}

