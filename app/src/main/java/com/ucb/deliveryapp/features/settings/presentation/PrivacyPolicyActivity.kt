package com.ucb.deliveryapp.features.settings.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ucb.deliveryapp.R
import java.text.SimpleDateFormat
import java.util.*

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
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.nombre),
                        contentDescription = stringResource(R.string.logo_menu),
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(max = 200.dp)
                            .padding(start = 92.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.volver),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00A76D)
                )
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
                stringResource(R.string.politica_privacidad_packify),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

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
            title = stringResource(R.string.titulo_clausula_uno),
            content = stringResource(R.string.clausula_uno_punto_uno) +
                    stringResource(R.string.clausula_uno_punto_dos) +
                    stringResource(R.string.clausula_uno_punto_tres) +
                    stringResource(R.string.clausula_uno_punto_cuatro)
        )

        PrivacySection(
            title = stringResource(R.string.titulo_clausula_dos),
            content = stringResource(R.string.clausula_dos_punto_uno) +
                    stringResource(R.string.clausula_dos_punto_dos) +
                    stringResource(R.string.clausula_dos_punto_tres) +
                    stringResource(R.string.clausula_dos_punto_cuatro)
        )

        PrivacySection(
            title = stringResource(R.string.titulo_clausula_tres),
            content = stringResource(R.string.clausula_tres_punto_uno) +
                    stringResource(R.string.clausula_tres_punto_dos) +
                    stringResource(R.string.clausula_tres_punto_tres) +
                    stringResource(R.string.clausula_tres_punto_cuatro)
        )

        PrivacySection(
            title = stringResource(R.string.titulo_clausula_cuatro),
            content = stringResource(R.string.clausula_cuatro_punto_uno) +
                    stringResource(R.string.clausula_cuatro_punto_dos) +
                    stringResource(R.string.clausula_cuatro_punto_tres)
        )

        PrivacySection(
            title = stringResource(R.string.titulo_clausula_cinco),
            content = stringResource(R.string.clausula_cinco_punto_uno) +
                    stringResource(R.string.clausula_cinco_punto_dos) +
                    stringResource(R.string.clausula_cinco_punto_tres) +
                    stringResource(R.string.clausula_cinco_punto_cuatro)
        )

        PrivacySection(
            title = stringResource(R.string.titulo_clausula_seis),
            content = stringResource(R.string.clausula_seis_punto_uno) +
                    stringResource(R.string.email_privacidad_packify_com) +
                    stringResource(R.string.universidad_ucb_bolivia) +
                    stringResource(R.string.clausula_seis_punto_dos)
        )

        Text(
            stringResource(R.string.ultima_actualizacion, getCurrentDate()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
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

private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}