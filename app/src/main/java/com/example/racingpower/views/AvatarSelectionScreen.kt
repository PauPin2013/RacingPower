package com.example.racingpower.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.racingpower.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.viewmodels.AuthViewModel

@Composable
fun AvatarSelectionScreen(userId: String, navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel() // Obtén una instancia del AuthViewModel

    // Definir los avatares disponibles como pares de ID de recurso y su nombre de String
    val avatarPairs = listOf(
        Pair(R.drawable.avatar1, "avatar1"),
        Pair(R.drawable.avatar2, "avatar2"),
        Pair(R.drawable.avatar3, "avatar3"),
        Pair(R.drawable.avatar4, "avatar4"),
        Pair(R.drawable.avatar5, "avatar5"),
        Pair(R.drawable.avatar6, "avatar6")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A49)) // Añadido para que el fondo sea consistente
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Elige tu avatar", fontSize = 22.sp, color = Color.White)

        Spacer(modifier = Modifier.height(24.dp))

        for (row in avatarPairs.chunked(2)) { // chunked(2) para que haya dos avatares por fila
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (avatarPair in row) {
                    val avatarResId = avatarPair.first
                    val avatarName = avatarPair.second // Nombre del recurso del avatar
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(120.dp)
                            .clickable {
                                // LLAMA AL MÉTODO DEL VIEWMODEL PARA ACTUALIZAR EL AVATAR
                                authViewModel.updateAvatar(
                                    userId,
                                    avatarName,
                                    onSuccess = { // <--- Implementación de onSuccess
                                        Toast.makeText(context, "Avatar actualizado", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack() // Vuelve a la pantalla anterior
                                    },
                                    onError = { errorMessage -> // <--- Implementación de onError
                                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            painter = painterResource(id = avatarResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}