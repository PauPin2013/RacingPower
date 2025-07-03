package com.example.racingpower.views

import android.widget.Toast
import androidx.compose.foundation.Image
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
import com.google.firebase.firestore.FirebaseFirestore
import com.example.racingpower.R

@Composable
fun AvatarSelectionScreen(userId: String, navController: NavController) {
    val context = LocalContext.current

    val avatarList = listOf(
        R.drawable.avatar1,
        R.drawable.avatar2,
        R.drawable.avatar3,
        R.drawable.avatar4,
        R.drawable.avatar5,
        R.drawable.avatar6
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Elige tu avatar", fontSize = 22.sp, color = Color.White)

        Spacer(modifier = Modifier.height(24.dp))

        for (row in avatarList.chunked(2)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (avatar in row) {
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(120.dp)
                            .clickable {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("users").document(userId)
                                    // *** CAMBIO CLAVE AQUÍ: Guarda el ID del recurso directamente ***
                                    .update("avatarResId", avatar) // <-- Guardamos el ID del recurso (Int)

                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Avatar actualizado", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error al actualizar avatar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            painter = painterResource(id = avatar),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}