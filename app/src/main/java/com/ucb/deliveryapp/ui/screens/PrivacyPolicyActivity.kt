package com.ucb.deliveryapp.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrivacyPolicyScreen(onBackPressed = { onBackPressed() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackPressed: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Política de Privacidad") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                "Política de Privacidad - Packify Delivery",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            // Contenido de la política de privacidad
            PrivacyPolicyContent()
        }
    }
}

@Composable
fun PrivacyPolicyContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrivacySection(
            title = "1. Información que Recopilamos",
            content = "• Email y nombre de usuario para autenticación\n" +
                    "• Datos de ubicación para servicios de mapas\n" +
                    "• Información de paquetes (destinatario, dirección, etc.)\n" +
                    "• Datos de uso de la aplicación"
        )

        PrivacySection(
            title = "2. Uso de la Información",
            content = "• Proveer el servicio de tracking de paquetes\n" +
                    "• Mejorar la experiencia de usuario\n" +
                    "• Comunicarnos con usuarios sobre sus envíos\n" +
                    "• Personalizar el servicio de entrega"
        )

        PrivacySection(
            title = "3. Almacenamiento y Seguridad",
            content = "• Los datos se almacenan en Firebase (Google Cloud Platform)\n" +
                    "• Las contraseñas están encriptadas con SHA-256\n" +
                    "• Cumplimos con leyes de protección de datos internacionales\n" +
                    "• Los datos de ubicación se usan solo durante la sesión activa"
        )

        PrivacySection(
            title = "4. Compartición de Datos",
            content = "• NO compartimos datos personales con terceros\n" +
                    "• Los datos solo se usan para funcionalidades de la app\n" +
                    "• Información anónima para análisis de uso"
        )

        PrivacySection(
            title = "5. Tus Derechos",
            content = "• Acceder a tus datos personales\n" +
                    "• Solicitar la eliminación de tu cuenta\n" +
                    "• Exportar tus datos\n" +
                    "• Revocar permisos en cualquier momento"
        )

        PrivacySection(
            title = "6. Contacto",
            content = "Para preguntas sobre privacidad:\n" +
                    "• Email: privacidad@packify.com\n" +
                    "• Universidad: UCB - Bolivia\n" +
                    "Esta política puede actualizarse. Revisa periódicamente."
        )

        Text(
            "Última actualización: ${java.time.LocalDate.now()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}