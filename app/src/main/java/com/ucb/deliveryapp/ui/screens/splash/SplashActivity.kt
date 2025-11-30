package com.ucb.deliveryapp.ui.screens.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ucb.deliveryapp.MainActivity
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.ui.screens.login.LoginActivity
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val SPLASH_DURATION = 1500L
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ USA EL MISMO SHAREDPREFERENCES QUE LOGINACTIVITY
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        setContent {
            SplashScreenContent()
        }
    }

    @Composable
    fun SplashScreenContent() {
        LaunchedEffect(Unit) {
            delay(SPLASH_DURATION)
            checkUserSessionAndNavigate()
        }

        SplashScreen()
    }

    private fun checkUserSessionAndNavigate() {
        try {
            // ✅ MISMA LÓGICA QUE TU ESTRUCTURA ACTUAL
            val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

            val intent = if (isLoggedIn) {
                // ✅ USUARIO YA LOGUEADO: Va directo a MainActivity (HomeScreen)
                Intent(this, MainActivity::class.java)
            } else {
                // ❌ USUARIO NO LOGUEADO: Va a LoginActivity
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // ✅ MANEJO SEGURO: Si hay error, va al login por seguridad
            e.printStackTrace()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00a76d)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = "App Logo",
                modifier = Modifier.size(180.dp)
            )

            Text(
                text = "Rápido, seguro y directo a tu puerta",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}