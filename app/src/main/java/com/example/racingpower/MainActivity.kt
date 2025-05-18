package com.example.racingpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.app.Application
import com.example.racingpower.ui.theme.RacingPowerTheme
import com.racingpower.ui.game.InfiniteGameScreen
import com.racingpower.ui.game.InfiniteGameViewModel
import com.racingpower.ui.game.InfiniteGameViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InfiniteGameViewModelFactory(application as Application)
        val viewModel = factory.create(InfiniteGameViewModel::class.java)

        setContent {
            RacingPowerTheme {
                InfiniteGameScreen(
                    username = "Jugador1", // puedes cambiar esto luego por el del login
                    viewModel = viewModel
                )
            }
        }
    }
}