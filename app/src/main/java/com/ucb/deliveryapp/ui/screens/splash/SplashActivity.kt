package com.ucb.deliveryapp.ui.screens.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.ucb.deliveryapp.MainActivity
import com.ucb.deliveryapp.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val SPLASH_DURATION = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CORRECCIÓN: Cambiado de R.xml a R.layout
        // Asume que tienes un archivo llamado "activity_splash.xml" en tu carpeta res/layout/
        setContentView(R.layout.activity_splash)

        // 2. CORRECCIÓN: La siguiente línea fue eliminada porque "supportActionBar" no existe en ComponentActivity.
        // supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_DURATION)
    }
}
