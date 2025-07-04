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
import androidx.compose.runtime.remember // Mantén esta importación
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
import com.example.racingpower.views.InfiniteGameScreen // Asumo que es el de los coches
import com.example.racingpower.views.InfinitePlaneGameScreen
import com.example.racingpower.views.LeaderboardScreen
import com.example.racingpower.views.LoginScreen
import com.example.racingpower.views.RegisterScreen
import com.example.racingpower.views.AvatarSelectionScreen

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel // ¡Importante!

class MainActivity : ComponentActivity() {
    // Inicializa el AuthViewModel usando by viewModels() para que su ciclo de vida esté ligado a la actividad.
    private val authViewModel: AuthViewModel by viewModels()

    // Método para adjuntar el contexto base y aplicar la configuración regional (idioma).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    // Objeto complementario para constantes relacionadas con las notificaciones.
    companion object {
        const val MY_CHANNEL_ID = "MyChannel"
        const val MY_CHANNEL_NAME = "General Notifications"
        const val MY_CHANNEL_DESCRIPTION = "Notifications for general app events"
    }

    // Launcher para solicitar el permiso de POST_NOTIFICATIONS (para Android 13+).
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Si el permiso es denegado, muestra un mensaje Toast.
            Toast.makeText(this, "Permiso de notificación denegado. Algunas notificaciones no se mostrarán.", Toast.LENGTH_LONG).show()
        }
    }

    // Método onCreate de la actividad.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel() // Crea el canal de notificación.

        // Solicita el permiso de POST_NOTIFICATIONS en tiempo de ejecución para Android 13 (TIRAMISU) y superiores.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Configura el contenido de la actividad usando Jetpack Compose.
        setContent {
            // Aplica el tema de la aplicación.
            RacingPowerTheme {
                val navController = rememberNavController() // Controlador de navegación para Compose.
                val authState by authViewModel.authState.collectAsState() // Observa el estado de autenticación.
                // Observa el userProfile del AuthViewModel para acceder al displayName.
                val userProfile by authViewModel.userProfile.collectAsState()


                // Función lambda para mostrar una notificación de bienvenida.
                val showWelcomeNotification: (String) -> Unit = { username ->
                    showNotification(
                        title = getString(R.string.notification_welcome_title),
                        message = getString(R.string.notification_welcome_message, username),
                        notificationId = 1001
                    )
                }

                // Función lambda para mostrar una notificación de cierre de sesión.
                val showLogoutNotification: () -> Unit = {
                    showNotification(
                        title = getString(R.string.notification_logout_title),
                        message = getString(R.string.notification_logout_message),
                        notificationId = 1002
                    )
                }

                // Define el grafo de navegación de la aplicación.
                NavHost(navController = navController, startDestination = "splash_screen") {

                    // Pantalla de inicio (splash screen) para verificar el estado de autenticación.
                    composable("splash_screen") {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator() // Muestra un indicador de carga.
                        }
                        LaunchedEffect(authState, userProfile) { // Añade userProfile como key
                            when (val currentState = authState) {
                                is AuthState.Authenticated -> {
                                    // Si el usuario está autenticado, navega a la pantalla de selección de juego.
                                    // La notificación de bienvenida se dispara desde LoginScreen o RegisterScreen.
                                    navController.navigate("game_selection_screen/${currentState.user.uid}") {
                                        popUpTo("splash_screen") { inclusive = true } // Elimina el splash screen de la pila.
                                    }
                                }
                                AuthState.Unauthenticated, is AuthState.Error -> {
                                    // Si no está autenticado o hay un error, navega a la pantalla de login.
                                    navController.navigate("login_screen") {
                                        popUpTo("splash_screen") { inclusive = true } // Elimina el splash screen de la pila.
                                    }
                                }
                                else -> {
                                    // Estado de carga, no hace nada, el CircularProgressIndicator ya se muestra.
                                }
                            }
                        }
                    }

                    // Pantalla de Login.
                    composable("login_screen") {
                        LoginScreen(
                            navController = navController,
                            authViewModel = authViewModel, // Pasa el AuthViewModel a LoginScreen.
                            onLoginSuccessNotification = showWelcomeNotification // Pasa el callback para notificaciones.
                        )
                    }

                    // Pantalla de Registro.
                    composable("register_screen") {
                        RegisterScreen(
                            navController = navController,
                            authViewModel = authViewModel, // Pasa el AuthViewModel a RegisterScreen.
                            onLoginSuccessNotification = showWelcomeNotification // Pasa el callback para notificaciones.
                        )
                    }

                    // Pantalla de selección de juego, con un argumento de userId.
                    composable(
                        "game_selection_screen/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        GameSelectionScreen(
                            userId = userId,
                            navController = navController,
                            onLogoutNotification = showLogoutNotification // Pasa el callback para notificaciones.
                        )
                    }

                    // Pantalla del juego infinito de coches.
                    composable(
                        "game_screen_cars/{userId}?displayName={displayName}",
                        arguments = listOf(
                            navArgument("userId") { type = NavType.StringType },
                            navArgument("displayName") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        val displayName = backStackEntry.arguments?.getString("displayName")
                        InfiniteGameScreen( // Asumo que InfiniteGameScreen es para los coches.
                            userId = userId,
                            displayName = displayName,
                            viewModel = viewModel(), // Crea una nueva instancia del ViewModel para el juego.
                            navController = navController
                        )
                    }

                    // Pantalla del juego infinito de aviones.
                    composable(
                        "game_screen_planes/{userId}?displayName={displayName}",
                        arguments = listOf(
                            navArgument("userId") { type = NavType.StringType },
                            navArgument("displayName") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        val displayName = backStackEntry.arguments?.getString("displayName")
                        InfinitePlaneGameScreen(
                            userId = userId,
                            displayName = displayName,
                            viewModel = viewModel(), // Crea una nueva instancia del ViewModel para el juego.
                            navController = navController
                        )
                    }

                    // Pantalla del juego infinito de botes.
                    composable(
                        "game_screen_boats/{userId}?displayName={displayName}",
                        arguments = listOf(
                            navArgument("userId") { type = NavType.StringType },
                            navArgument("displayName") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: "guest_user"
                        val displayName = backStackEntry.arguments?.getString("displayName")
                        InfiniteBoatGameScreen(
                            userId = userId,
                            displayName = displayName,
                            viewModel = viewModel(), // Crea una nueva instancia del ViewModel para el juego.
                            navController = navController
                        )
                    }

                    // Pantalla de la tabla de clasificación.
                    composable("leaderboard_screen") {
                        LeaderboardScreen(navController = navController)
                    }

                    // Pantalla de selección de avatar.
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

    // Crea un canal de notificación (obligatorio para Android 8.0 Oreo y superiores).
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = MY_CHANNEL_NAME
            val descriptionText = MY_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Importancia por defecto.
            val channel = NotificationChannel(MY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Muestra una notificación.
    private fun showNotification(title: String, message: String, notificationId: Int) {
        // Verifica el permiso de notificación para Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return // Si no tiene permiso, no muestra la notificación.
            }
        }

        // Construye la notificación.
        var builder = NotificationCompat.Builder(this, MY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Icono pequeño de la notificación.
            .setContentTitle(title) // Título de la notificación.
            .setContentText(message) // Contenido principal de la notificación.
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Prioridad de la notificación.
            .setAutoCancel(true) // Cierra la notificación cuando el usuario la toca.

        // Muestra la notificación.
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }
}