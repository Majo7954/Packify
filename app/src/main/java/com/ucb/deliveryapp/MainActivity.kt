package com.ucb.deliveryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ucb.deliveryapp.core.datastore.LoginDataStore
import com.ucb.deliveryapp.core.remoteconfig.RemoteConfigManager
import com.ucb.deliveryapp.features.maintenance.presentation.MaintenanceScreen
import com.ucb.deliveryapp.navigation.AppNavHost
import com.ucb.deliveryapp.ui.theme.DeliveryappTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        RemoteConfigManager.initialize(this)

        setContent {
            DeliveryappTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DeliveryApp(
                        initialRouteFromNotification = intent?.getStringExtra("open_route")
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun DeliveryApp(initialRouteFromNotification: String?) {
    val context = LocalContext.current
    val loginDataStore = remember { LoginDataStore(context) }

    var showMaintenance by remember { mutableStateOf(false) }
    var showForceUpdate by remember { mutableStateOf(false) }
    var requiredVersion by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var pendingRoute by remember { mutableStateOf(initialRouteFromNotification) }

    val activity = context as ComponentActivity
    LaunchedEffect(activity.intent) {
        pendingRoute = activity.intent?.getStringExtra("open_route") ?: pendingRoute
    }

    LaunchedEffect(Unit) {
        delay(500)

        RemoteConfigManager.forceFetch()
        if (RemoteConfigManager.isMaintenanceMode()) {
            showMaintenance = true
            isLoading = false
            return@LaunchedEffect
        }

        if (RemoteConfigManager.isForceUpdate()) {
            requiredVersion = RemoteConfigManager.getRequiredVersion()
            showForceUpdate = true
            isLoading = false
            return@LaunchedEffect
        }

        isLoggedIn = runBlocking { loginDataStore.isLoggedIn() }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00A76D)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    when {
        showMaintenance -> MaintenanceScreen(isForceUpdate = false)
        showForceUpdate -> MaintenanceScreen(isForceUpdate = true, requiredVersion = requiredVersion)
        else -> {
            AppNavHost(
                isLoggedIn = isLoggedIn,
                onLoginStateChange = { loggedIn -> isLoggedIn = loggedIn },
                pendingRoute = pendingRoute,
                onPendingRouteHandled = { pendingRoute = null }
            )
        }
    }
}
