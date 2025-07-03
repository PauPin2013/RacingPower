package com.example.racingpower

import android.content.Context
import android.os.Build
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.racingpower.ui.theme.RacingPowerTheme
import com.example.racingpower.utils.LocaleHelper
import com.example.racingpower.viewmodels.AuthState
import com.example.racingpower.viewmodels.AuthViewModel
import com.example.racingpower.viewmodels.InfiniteGameViewModel
// Importaciones de vistas explícitas para claridad
import com.example.racingpower.views.GameSelectionScreen
import com.example.racingpower.views.InfiniteBoatGameScreen
import com.example.racingpower.views.InfiniteGameScreen
import com.example.racingpower.views.InfinitePlaneGameScreen
import com.example.racingpower.views.LeaderboardScreen
import com.example.racingpower.views.LoginScreen
import com.example.racingpower.views.RegisterScreen
import com.example.racingpower.views.AvatarSelectionScreen // Asegúrate de que esta esté importada

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    companion object {
        const val MY_CHANNEL_ID = "MyChannel"
        const val MY_CHANNEL_NAME = "General Notifications" // Nombre del canal
        const val MY_CHANNEL_DESCRIPTION = "Notifications for general app events" // Descripción del canal
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) { // Cambiado a !isGranted para un mensaje más informativo si deniega
            Toast.makeText(this, "Permiso de notificación denegado. Algunas notificaciones no se mostrarán.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Crea el canal de notificación al inicio de la aplicación
        createNotificationChannel()

        // Solicita el permiso POST_NOTIFICATIONS si es necesario (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU = API 33
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent {
            RacingPowerTheme {
                val navController = rememberNavController()
                val authState by authViewModel.authState.collectAsState()

                // Define las funciones de notificación aquí, usando getString para los mensajes
                val showWelcomeNotification: (String) -> Unit = { username ->
                    showNotification(
                        title = getString(R.string.notification_welcome_title),
                        message = getString(R.string.notification_welcome_message, username),
                        notificationId = 1001 // ID único para bienvenida
                    )
                }

                val showLogoutNotification: () -> Unit = {
                    showNotification(
                        title = getString(R.string.notification_logout_title),
                        message = getString(R.string.notification_logout_message),
                        notificationId = 1002 // ID único para cerrar sesión
                    )
                }

                NavHost(navController = navController, startDestination = "splash_screen") {

                    composable("splash_screen") {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
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
                                    // loading state
                                }
                            }
                        }
                    }

                    composable("login_screen") {
                        LoginScreen(
                            navController = navController,
                            onLoginSuccessNotification = showWelcomeNotification // Pasa la función de bienvenida
                        )
                    }

                    composable("register_screen") {
                        RegisterScreen(navController = navController)
                    }

                    composable(
                        "game_selection_screen/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        GameSelectionScreen(
                            userId = userId,
                            navController = navController,
                            onLogoutNotification = showLogoutNotification // Pasa la función de despedida
                        )
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

                    composable(
                        "game_screen_boats/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        val boatGameViewModel: InfiniteGameViewModel = remember {
                            InfiniteGameViewModel(application)
                        }
                        InfiniteBoatGameScreen(
                            username = userId,
                            viewModel = boatGameViewModel,
                            navController = navController
                        )
                    }

                    composable("leaderboard_screen") {
                        LeaderboardScreen(navController = navController)
                    }

                    composable(
                        "avatar_selection_screen/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        AvatarSelectionScreen(userId = userId, navController = navController)
                    }
                }
            }
        }
    }
    // Función para crear el canal de notificación
    private fun createNotificationChannel() {
        // Solo necesario para Android 8.0 (API 26) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = MY_CHANNEL_NAME
            val descriptionText = MY_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(MY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Registrar el canal con el sistema
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Función unificada para mostrar notificaciones
    private fun showNotification(title: String, message: String, notificationId: Int){
        // Verifica si el permiso de notificaciones está concedido antes de mostrar la notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // No mostrar un Toast aquí, ya se manejó la denegación en requestPermissionLauncher
                return // Sale de la función si no hay permiso
            }
        }

        var builder = NotificationCompat.Builder(this, MY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // ¡Considera cambiar este ícono por uno de tu app!
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Opcional: cierra la notificación cuando se toca

        with(NotificationManagerCompat.from(this)){
            notify(notificationId, builder.build())
        }
    }
}