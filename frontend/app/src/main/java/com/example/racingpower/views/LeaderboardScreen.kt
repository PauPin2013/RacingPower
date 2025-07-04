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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

// Composable que muestra la pantalla de la tabla de clasificación (Leaderboard).
// Permite al usuario seleccionar el tipo de juego para ver la clasificación.
@Composable
fun LeaderboardScreen(
    navController: NavController // Controlador de navegación para la pantalla.
) {
    // Obtiene una instancia del LeaderboardViewModel.
    val viewModel: LeaderboardViewModel = viewModel()

    // Estado mutable para el tipo de juego seleccionado, por defecto "cars".
    var selectedGameType by remember { mutableStateOf("cars") }

    // Lanza un efecto cuando el `selectedGameType` cambia para cargar la tabla de clasificación correspondiente.
    LaunchedEffect(selectedGameType) {
        viewModel.loadLeaderboard(selectedGameType)
    }

    // Determina el título de la tabla de clasificación según el tipo de juego seleccionado.
    val leaderboardTitle = when (selectedGameType) {
        "cars" -> stringResource(id = R.string.cars_leaderboard_title)
        "planes" -> stringResource(id = R.string.planes_leaderboard_title)
        "boats" -> stringResource(id = R.string.boats_leaderboard_title)
        else -> "" // Caso por defecto, aunque no debería ocurrir con los tipos definidos.
    }
    // Obtiene las cadenas de recursos localizadas para los encabezados y textos de la UI.
    val usernameHeader = stringResource(id = R.string.leaderboard_username)
    val scoreHeader = stringResource(id = R.string.leaderboard_score)
    val selectGameTypeText = stringResource(id = R.string.select_game_type)
    val carsButtonText = stringResource(id = R.string.cars_button)
    val planesButtonText = stringResource(id = R.string.planes_button)
    val boatsButtonText = stringResource(id = R.string.boats_button)
    val backButtonText = stringResource(id = R.string.back_button_text)
    val loadingText = stringResource(id = R.string.loading_leaderboard)
    val noDataText = stringResource(id = R.string.no_data_available)

    // Contenedor principal que llena toda la pantalla.
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Imagen de fondo que cubre todo el espacio del Box.
        Image(
            painter = painterResource(id = R.drawable.fondo2),
            contentDescription = null, // Descripción nula ya que es una imagen decorativa de fondo.
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Escala la imagen para que llene el Box, recortando si es necesario.
        )

        // Capa de superposición semitransparente para oscurecer la imagen de fondo,
        // mejorando la legibilidad del texto en primer plano.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Ajusta el valor alpha para controlar la oscuridad.
        )

        // Columna principal que contiene todo el contenido de la pantalla de la tabla de clasificación.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Este padding aplica al contenido sobre la imagen de fondo.
            horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos hijos horizontalmente.
        ) {
            // Título de la tabla de clasificación.
            Text(
                text = leaderboardTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Texto para indicar al usuario que seleccione un tipo de juego.
            Text(
                text = selectGameTypeText,
                fontSize = 16.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Fila de botones para seleccionar el tipo de juego.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly // Distribuye los botones uniformemente.
            ) {
                // Botón para seleccionar "Coches".
                Button(
                    onClick = { selectedGameType = "cars" },
                    colors = ButtonDefaults.buttonColors(
                        // Cambia el color del botón si está seleccionado.
                        containerColor = if (selectedGameType == "cars") Color.Blue.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(carsButtonText)
                }
                // Botón para seleccionar "Aviones".
                Button(
                    onClick = { selectedGameType = "planes" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedGameType == "planes") Color.Blue.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(planesButtonText)
                }
                // Botón para seleccionar "Botes".
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

            // Encabezado de la tabla de clasificación (Posición, Usuario, Puntuación).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C3E50), RoundedCornerShape(8.dp)) // Fondo oscuro con esquinas redondeadas.
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pos",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(0.5f) // Peso para distribuir el espacio.
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.End // Alinea el texto a la derecha.
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Muestra un indicador de carga, un mensaje de error o la lista de entradas de la tabla de clasificación.
            if (viewModel.isLoading.value) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White) // Indicador de progreso.
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
                // Lista desplazable perezosa para mostrar las entradas de la tabla de clasificación.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Ocupa el espacio restante disponible.
                ) {
                    itemsIndexed(viewModel.leaderboardEntries) { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(Color.Transparent), // Fondo transparente para las filas.
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.", // Posición en la tabla.
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = entry.username, // Nombre de usuario.
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "${entry.score}", // Puntuación.
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

            // Botón para volver a la pantalla anterior.
            Button(
                onClick = { navController.popBackStack() }, // Vuelve a la pantalla anterior en la pila.
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
}