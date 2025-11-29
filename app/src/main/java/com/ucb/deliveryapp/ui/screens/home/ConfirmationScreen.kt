// ConfirmationScreen.kt
package com.ucb.deliveryapp.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.ui.screens.packages.PackageListActivity

@Composable
fun ConfirmationScreen(
    onNavigateToPackages: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Imagen arriba - USANDO UN DRAWABLE EXISTENTE
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Envío confirmado",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(32.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Cuadrado blanco en la mitad inferior
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icono de confirmación
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Confirmado",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Título EXACTO como pediste
                Text(
                    "Tu pedido ha sido realizado con éxito.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mensaje EXACTO como pediste
                Text(
                    "Realice el seguimiento..",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón "Ver mis envíos" - REDIRIGE DIRECTAMENTE
                Button(
                    onClick = {
                        // Navegar directamente a PackageListActivity
                        val intent = Intent(context, PackageListActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Ver mis envíos", // TEXTO EXACTO como pediste
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}