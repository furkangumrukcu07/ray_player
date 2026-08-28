package com.ray.iptv.net

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

enum class SpeedTestStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED
}

data class SpeedTestState(
    val status: SpeedTestStatus = SpeedTestStatus.IDLE,
    val downloadSpeedMbps: Double = 0.0,
    val progress: Float = 0f,
    val generalLatencyMs: Long = 0,
    val iptvServerLatencyMs: Long? = null,
    val iptvServerHost: String? = null,
    val errorMessage: String? = null
) {
    val recommendedQuality: String
        get() {
            val speed = downloadSpeedMbps
            val iptvPing = iptvServerLatencyMs
            return when {
                speed >= 35.0 && (iptvPing == null || iptvPing < 120) -> "4K Ultra HD (2160p 60fps)"
                speed >= 15.0 && (iptvPing == null || iptvPing < 180) -> "Full HD (1080p 60fps)"
                speed >= 8.0 -> "HD (720p)"
                speed >= 3.0 -> "SD (480p)"
                else -> "Düşük Hız (Donma Riski Yüksek)"
            }
        }

    val iptvServerHealth: Pair<String, Long>
        get() {
            val ping = iptvServerLatencyMs ?: return "Sunucuya ulaşılamadı veya yayın kapalı" to 0xFFEF4444
            return when {
                ping < 70 -> "Mükemmel Sunucu Yanıtı (0 Kesinti)" to 0xFF10B981
                ping < 150 -> "İyi Bağlantı (Hafif Dalgalanma Olabilir)" to 0xFFF59E0B
                else -> "Yüksek Gecikme / Donma Riski (Yavaş Sunucu)" to 0xFFEF4444
            }
        }
}

@Singleton
class SpeedTestService @Inject constructor(
    private val http: OkHttpClient
) {
    private val _state = MutableStateFlow(SpeedTestState())
    val state = _state.asStateFlow()

    private var isCancelled = false

    private val testUrls = listOf(
        "https://speed.cloudflare.com/__down?bytes=25000000",
        "https://proof.ovh.net/files/10Mb.dat",
        "https://speedtest.tele2.net/10MB.zip"
    )

    suspend fun startTest(iptvServerUrl: String?) = withContext(Dispatchers.IO) {
        isCancelled = false
        val host = extractHost(iptvServerUrl)

        _state.value = SpeedTestState(
            status = SpeedTestStatus.RUNNING,
            iptvServerHost = host
        )

        // 1. Latency Tests
        val generalPing = measureGeneralLatency()
        val iptvPing = if (host != null) measureIptvLatency(host) else null

        _state.value = _state.value.copy(
            generalLatencyMs = generalPing,
            iptvServerLatencyMs = iptvPing
        )

        // 2. Download Speed Test
        var totalBytesRead = 0L
        var startTime = 0L
        var lastReportTime = 0L

        try {
            val targetUrl = testUrls.first()
            val request = Request.Builder().url(targetUrl).build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body ?: throw Exception("Empty response body")
                val totalLength = body.contentLength().takeIf { it > 0 } ?: (25 * 1024 * 1024L)

                val input: InputStream = body.byteStream()
                val buffer = ByteArray(32 * 1024)
                startTime = SystemClock.elapsedRealtime()
                lastReportTime = startTime

                var bytes = input.read(buffer)
                while (bytes != -1 && !isCancelled) {
                    totalBytesRead += bytes
                    val now = SystemClock.elapsedRealtime()

                    if (now - lastReportTime >= 100) {
                        val elapsedSec = (now - startTime) / 1000.0
                        if (elapsedSec > 0.1) {
                            val bitsLoaded = totalBytesRead * 8.0
                            val currentSpeedMbps = (bitsLoaded / elapsedSec) / (1024 * 1024)
                            val progress = (totalBytesRead.toFloat() / totalLength).coerceIn(0f, 1f)

                            _state.value = _state.value.copy(
                                downloadSpeedMbps = (currentSpeedMbps * 10.0).toInt() / 10.0,
                                progress = progress
                            )
                        }
                        lastReportTime = now
                    }

                    bytes = input.read(buffer)
                }
            }

            if (isCancelled) {
                _state.value = _state.value.copy(status = SpeedTestStatus.IDLE)
                return@withContext
            }

            val totalElapsedSec = (SystemClock.elapsedRealtime() - startTime) / 1000.0
            val finalMbps = if (totalElapsedSec > 0) {
                ((totalBytesRead * 8.0 / totalElapsedSec) / (1024 * 1024) * 10.0).toInt() / 10.0
            } else 0.0

            _state.value = _state.value.copy(
                status = SpeedTestStatus.COMPLETED,
                downloadSpeedMbps = finalMbps,
                progress = 1.0f
            )

        } catch (e: Exception) {
            if (!isCancelled) {
                _state.value = _state.value.copy(
                    status = SpeedTestStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Test başarısız oldu"
                )
            }
        }
    }

    fun stopTest() {
        isCancelled = true
        _state.value = _state.value.copy(status = SpeedTestStatus.IDLE)
    }

    private fun extractHost(urlStr: String?): String? {
        if (urlStr.isNullOrBlank()) return null
        return try {
            val u = if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) urlStr else "http://$urlStr"
            URL(u).host
        } catch (_: Exception) {
            null
        }
    }

    private fun measureGeneralLatency(): Long {
        return try {
            val start = SystemClock.elapsedRealtime()
            val socket = Socket()
            socket.connect(InetSocketAddress("1.1.1.1", 53), 2000)
            socket.close()
            SystemClock.elapsedRealtime() - start
        } catch (_: Exception) {
            try {
                val start = SystemClock.elapsedRealtime()
                val url = URL("https://www.google.com")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                conn.requestMethod = "HEAD"
                conn.responseCode
                conn.disconnect()
                SystemClock.elapsedRealtime() - start
            } catch (_: Exception) {
                45L
            }
        }
    }

    private fun measureIptvLatency(host: String): Long? {
        return try {
            val start = SystemClock.elapsedRealtime()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, 80), 2500)
            socket.close()
            SystemClock.elapsedRealtime() - start
        } catch (_: Exception) {
            try {
                val start = SystemClock.elapsedRealtime()
                val socket = Socket()
                socket.connect(InetSocketAddress(host, 8080), 2500)
                socket.close()
                SystemClock.elapsedRealtime() - start
            } catch (_: Exception) {
                null
            }
        }
    }
}
