package com.example.racingpower.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.racingpower.R
import com.example.racingpower.viewmodels.LeaderboardViewModel

@Composable
fun LeaderboardScreen(
    navController: NavController
) {
    val viewModel: LeaderboardViewModel = viewModel()

    // Estado para controlar qué tabla de clasificación se muestra ("cars", "planes", "boats")
    var selectedGameType by remember { mutableStateOf("cars") } // Por defecto, mostrar carros

    // Cargar los datos cuando la pantalla se compone y cuando cambia el tipo de juego seleccionado
    LaunchedEffect(selectedGameType) {
        viewModel.loadLeaderboard(selectedGameType)
    }

    val leaderboardTitle = when (selectedGameType) {
        "cars" -> stringResource(id = R.string.cars_leaderboard_title)
        "planes" -> stringResource(id = R.string.planes_leaderboard_title)
        "boats" -> stringResource(id = R.string.boats_leaderboard_title) // ¡NUEVO!
        else -> ""
    }
    val usernameHeader = stringResource(id = R.string.leaderboard_username)
    val scoreHeader = stringResource(id = R.string.leaderboard_score)
    val selectGameTypeText = stringResource(id = R.string.select_game_type)
    val carsButtonText = stringResource(id = R.string.cars_button)
    val planesButtonText = stringResource(id = R.string.planes_button)
    val boatsButtonText = stringResource(id = R.string.boats_button) // ¡NUEVO!
    val backButtonText = stringResource(id = R.string.back_button_text)
    val loadingText = stringResource(id = R.string.loading_leaderboard)
    val noDataText = stringResource(id = R.string.no_data_available)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A49)) // Fondo oscuro
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = leaderboardTitle,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Botones para seleccionar el tipo de juego
        Text(
            text = selectGameTypeText,
            fontSize = 16.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { selectedGameType = "cars" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedGameType == "cars") Color.Blue.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(carsButtonText)
            }
            Button(
                onClick = { selectedGameType = "planes" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedGameType == "planes") Color.Blue.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(planesButtonText)
            }
            // ¡NUEVO BOTÓN PARA BOTES!
            Button(
                onClick = { selectedGameType = "boats" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedGameType == "boats") Color.Blue.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(boatsButtonText)
            }
        }

        // Encabezado de la tabla
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C3E50), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pos",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(0.5f)
            )
            Text(
                text = usernameHeader,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = scoreHeader,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Contenido de la tabla de clasificación
        if (viewModel.isLoading.value) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(loadingText, color = Color.White, fontSize = 16.sp)
            }
        } else if (viewModel.errorMessage.value != null) {
            Text(
                text = "Error: ${viewModel.errorMessage.value}",
                color = Color.Red,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else if (viewModel.leaderboardEntries.isEmpty()) {
            Text(
                text = noDataText,
                color = Color.LightGray,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                itemsIndexed(viewModel.leaderboardEntries) { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(0.5f)
                        )
                        Text(
                            text = entry.username,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "${entry.score}",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de Volver
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(alpha = 0.7f))
        ) {
            Text(backButtonText)
        }
    }
}