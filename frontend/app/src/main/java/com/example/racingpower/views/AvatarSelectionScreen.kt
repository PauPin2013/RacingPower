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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.racingpower.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racingpower.viewmodels.AuthViewModel
import androidx.compose.ui.layout.ContentScale

// Composable que permite al usuario seleccionar un avatar para su perfil.
// Requiere el ID del usuario y un NavController para la navegación.
@Composable
fun AvatarSelectionScreen(userId: String, navController: NavController) {
    val context = LocalContext.current
    // Obtiene una instancia del AuthViewModel.
    val authViewModel: AuthViewModel = viewModel()

    // Lista de pares que contienen el ID del recurso de la imagen del avatar y su nombre correspondiente.
    val avatarPairs = listOf(
        Pair(R.drawable.avatar1, "avatar1"),
        Pair(R.drawable.avatar2, "avatar2"),
        Pair(R.drawable.avatar3, "avatar3"),
        Pair(R.drawable.avatar4, "avatar4"),
        Pair(R.drawable.avatar5, "avatar5"),
        Pair(R.drawable.avatar6, "avatar6")
    )

    // Obtiene la cadena "choose_avatar" de los recursos.
    val chooseAvatar = stringResource(id = R.string.choose_avatar)

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

        // Columna que contiene todo el contenido interactivo de la pantalla.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Aplica un padding general al contenido.
            horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos horizontalmente.
        ) {
            // Título de la pantalla.
            Text(chooseAvatar, fontSize = 22.sp, color = Color.White)

            // Espacio vertical para separación.
            Spacer(modifier = Modifier.height(24.dp))

            // Itera sobre la lista de avatares, agrupándolos en filas de dos.
            for (row in avatarPairs.chunked(2)) {
                // Fila para mostrar dos avatares.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp), // Padding vertical entre filas.
                    horizontalArrangement = Arrangement.SpaceEvenly // Distribuye los avatares uniformemente.
                ) {
                    // Itera sobre cada par de avatar en la fila.
                    for (avatarPair in row) {
                        val avatarResId = avatarPair.first // ID del recurso de la imagen.
                        val avatarName = avatarPair.second // Nombre del avatar para guardar en la base de datos.

                        // Tarjeta clickable para cada avatar.
                        Card(
                            modifier = Modifier
                                .padding(8.dp) // Padding alrededor de cada tarjeta.
                                .size(120.dp) // Tamaño fijo para la tarjeta del avatar.
                                .clickable {
                                    // Al hacer clic, actualiza el avatar del usuario a través del ViewModel.
                                    authViewModel.updateAvatar(
                                        userId,
                                        avatarName,
                                        onSuccess = {
                                            // Muestra un mensaje de éxito y regresa a la pantalla anterior.
                                            Toast.makeText(context, "Avatar actualizado", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        },
                                        onError = { errorMessage ->
                                            // Muestra un mensaje de error si la actualización falla.
                                            Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(16.dp), // Forma de la tarjeta con esquinas redondeadas.
                            colors = CardDefaults.cardColors(containerColor = Color.White) // Color de fondo de la tarjeta.
                        ) {
                            // Muestra la imagen del avatar dentro de la tarjeta.
                            Image(
                                painter = painterResource(id = avatarResId),
                                contentDescription = null, // Descripción nula ya que la tarjeta es clickable y su propósito es obvio.
                                modifier = Modifier.fillMaxSize() // La imagen llena la tarjeta.
                            )
                        }
                    }
                }
            }
        }
    }
}