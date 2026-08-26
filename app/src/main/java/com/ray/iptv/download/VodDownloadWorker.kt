package com.ray.iptv.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.ray.iptv.data.local.DownloadEntity
import com.ray.iptv.data.local.RayDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import android.content.pm.ServiceInfo
import android.os.Build
import java.io.File
import java.util.UUID

@HiltWorker
class VodDownloadWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val db: RayDatabase,
    private val http: OkHttpClient
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: UUID.randomUUID().toString()
        val url = inputData.getString(KEY_URL).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val poster = inputData.getString(KEY_POSTER).orEmpty()
        val profile = inputData.getString(KEY_PROFILE).orEmpty()
        val media = inputData.getString(KEY_MEDIA).orEmpty()
        if (url.isBlank() || profile.isBlank()) return Result.failure()
        val dir = File(ctx.filesDir, "downloads").apply { mkdirs() }
        val dest = File(dir, "$id.bin")
        setForeground(fg("Downloading $title"))
        db.downloads().upsert(
            DownloadEntity(id, profile, media, title, poster, url, dest.absolutePath, 0, 0, "QUEUED", System.currentTimeMillis())
        )
        return runCatching {
            val req = Request.Builder().url(url).get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val total = resp.body?.contentLength() ?: -1L
                resp.body!!.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var read = 0L
                        var lastDbMs = 0L
                        var lastReported = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            read += n
                            val now = System.currentTimeMillis()
                            if (now - lastDbMs >= 1500L || read - lastReported >= 1024L * 1024L) {
                                lastDbMs = now
                                lastReported = read
                                db.downloads().upsert(
                                    DownloadEntity(id, profile, media, title, poster, url, dest.absolutePath, read, total, "RUNNING", now)
                                )
                            }
                        }
                    }
                }
                db.downloads().upsert(
                    DownloadEntity(id, profile, media, title, poster, url, dest.absolutePath, dest.length(), dest.length(), "DONE", System.currentTimeMillis())
                )
            }
            Result.success()
        }.getOrElse {
            db.downloads().upsert(
                DownloadEntity(id, profile, media, title, poster, url, dest.absolutePath, 0, 0, "FAILED", System.currentTimeMillis())
            )
            Result.failure()
        }
    }

    private fun fg(text: String): ForegroundInfo {
        val nm = NotificationManagerCompat.from(ctx)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder("ray_dl", NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Downloads")
                .build()
        )
        val n = NotificationCompat.Builder(ctx, "ray_dl")
            .setContentTitle("Ray IPTV")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(43, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(43, n)
        }
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_POSTER = "poster"
        const val KEY_PROFILE = "profile"
        const val KEY_MEDIA = "media"
    }
}
