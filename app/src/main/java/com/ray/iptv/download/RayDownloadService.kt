package com.ray.iptv.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ray.iptv.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RayDownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = NotificationChannel("ray_dl", "Downloads", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val n: Notification = NotificationCompat.Builder(this, "ray_dl")
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Downloading…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(42, n)
        }
        return START_NOT_STICKY
    }
}
