package com.ucb.deliveryapp.features.notification.data.service

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ucb.deliveryapp.core.remoteconfig.RemoteConfigManager

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "delivery_channel"
        const val CHANNEL_NAME = "Delivery Notifications"
    }

    override fun onNewToken(token: String) {
        println("FCM Token actualizado: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!RemoteConfigManager.isNotificationsEnabled()) {
            return
        }

        val soundEnabled = RemoteConfigManager.getNotificationSound() != "none"
        val vibrationEnabled = RemoteConfigManager.isVibrationEnabled()

        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Packify Delivery",
                body = notification.body ?: "Tienes una nueva actualización",
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled
            )
        }

        message.data.let { data ->
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                if (soundEnabled) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(soundUri, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (vibrationEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationPattern = longArrayOf(0, 500, 250, 500)
                    vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            } catch (e: SecurityException) {
                println("Sin permiso para vibrar")
            }
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}