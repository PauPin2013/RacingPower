// ViewModelFactory.kt
package com.racingpower.ui.game

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class InfiniteGameViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InfiniteGameViewModel::class.java)) {
            return InfiniteGameViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
