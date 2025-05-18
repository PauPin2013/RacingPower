// InfiniteGameViewModel.kt
package com.racingpower.ui.game // ESTE PAQUETE DEBE COINCIDIR

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class InfiniteGameViewModel(application: Application) : AndroidViewModel(application) {
    val score = mutableStateOf(0)
    val highScore = mutableStateOf(0)
    val speed = mutableStateOf(5f)
    val username = mutableStateOf("")

    fun startGame(name: String) {
        username.value = name
        score.value = 0
        speed.value = 5f
    }

    fun onCarPassed() {
        score.value += 50
        if (score.value > highScore.value) {
            highScore.value = score.value
        }
        if (score.value % 1000 == 0) {
            speed.value += 1f
        }
    }

    fun gameOver() {
        viewModelScope.launch {
            // Guardar datos si tienes Room
        }
    }

    fun resetGame() {
        score.value = 0
        speed.value = 5f
    }
}