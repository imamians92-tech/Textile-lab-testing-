package com.example.data.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.TextileLabApp
import com.example.core.NetworkUtils

class LabServerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? TextileLabApp
        when (intent?.action) {
            ACTION_STOP -> {
                app?.container?.localServer?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val ip = NetworkUtils.getLocalWifiIpAddress()
                startForeground(NOTIFICATION_ID, buildNotification(ip))
                app?.container?.localServer?.start(NetworkUtils.DEFAULT_SERVER_PORT)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        val app = application as? TextileLabApp
        app?.container?.localServer?.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Textile Lab Local Wi-Fi Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the local Ktor REST & WebSocket server active for lab client devices."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(ipAddress: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Textile Lab Pro Server Online")
            .setContentText("Wi-Fi LAN Endpoint: http://$ipAddress:${NetworkUtils.DEFAULT_SERVER_PORT}")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "textile_lab_server_channel"
        private const val NOTIFICATION_ID = 4102
        const val ACTION_START = "com.example.action.START_LAB_SERVER"
        const val ACTION_STOP = "com.example.action.STOP_LAB_SERVER"

        fun start(context: Context) {
            val intent = Intent(context, LabServerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LabServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
