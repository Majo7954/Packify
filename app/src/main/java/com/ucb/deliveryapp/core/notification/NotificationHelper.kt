package com.ucb.deliveryapp.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ucb.deliveryapp.MainActivity
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.navigation.Routes

object NotificationHelper {

    private const val CHANNEL_ID = "packify_updates"
    private const val CHANNEL_NAME = "Actualizaciones de paquetes"

    const val EXTRA_OPEN_PACKAGE_ID = "extra_open_package_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando cambia el estado de un paquete"
            }

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showPackageStatusChanged(
        context: Context,
        packageId: String,
        trackingNumber: String,
        newStatusText: String
    ) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_route", Routes.PACKAGES)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            packageId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_packify)
            .setLargeIcon(largeIcon)
            .setContentTitle("Paquete $trackingNumber")
            .setContentText("Estado actualizado: $newStatusText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Estado actualizado: $newStatusText")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(
            (trackingNumber.ifBlank { packageId }).hashCode(),
            notification
        )
    }
}
