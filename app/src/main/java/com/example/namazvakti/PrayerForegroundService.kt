package com.example.namazvakti

import com.firstnamazvakti.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "sabit_namaz_vakti_channel"

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 60000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Namaz Vakitleri Yükleniyor...", "")
        startForeground(NOTIFICATION_ID, notification)
        handler.post(updateRunnable)
        return START_STICKY
    }

    private fun updateNotification() {
        val prefs = getSharedPreferences("namaz", Context.MODE_PRIVATE)
        val cache = prefs.getString("cache", "") ?: ""
        val now = System.currentTimeMillis()

        val allTimes = cache.split(";").filter { it.contains("|") }.mapNotNull {
            val parts = it.split("|")
            val t = parts[0].toLongOrNull()
            if (t == null) null else t to parts[1]
        }

        val future = allTimes.filter { it.first > now }
        if (future.isNotEmpty()) {
            val next = future.first()
            val nextTimeMs = next.first
            val nextName = next.second

            val diffMs = nextTimeMs - now
            val totalMinutes = diffMs / (1000 * 60)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60

            val clockStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTimeMs))

            val title = "Malatya - $nextName: $clockStr"
            val body = if (hours > 0) {
                "$hours saat $mins dk kaldı."
            } else {
                "$mins dk kaldı."
            }

            val notification = buildNotification(title, body)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, body: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_crescent)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sabit Namaz Vakti Bildirimi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}