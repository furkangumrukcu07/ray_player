package com.ray.iptv.player

import androidx.media3.common.C
import androidx.media3.common.ParserException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException

/** Mina `MinaIptvLoadErrorHandlingPolicy` — canlı IPTV için hızlı yeniden deneme. */
@UnstableApi
internal class IptvLoadErrorPolicy(
    private val live: Boolean
) : DefaultLoadErrorHandlingPolicy() {

    override fun getMinimumLoadableRetryCount(dataType: Int): Int =
        if (live) 18 else super.getMinimumLoadableRetryCount(dataType)

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val code = httpCode(loadErrorInfo.exception)
        if (code == 401 || code == 403 || code == 404) return C.TIME_UNSET
        if (code == 456 || code == 509) return 1_500L
        if (live && malformedHls(loadErrorInfo.exception)) return 800L
        if (live && transient(loadErrorInfo.exception)) return 700L
        val base = super.getRetryDelayMsFor(loadErrorInfo)
        return if (live) minOf(base, 2_500L).coerceAtLeast(400L) else base
    }

    override fun getFallbackSelectionFor(
        fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
    ): LoadErrorHandlingPolicy.FallbackSelection? {
        val code = httpCode(loadErrorInfo.exception)
        if (code == 456 || code == 509) return null
        return super.getFallbackSelectionFor(fallbackOptions, loadErrorInfo)
    }

    private fun httpCode(cause: IOException): Int {
        var c: Throwable? = cause
        while (c != null) {
            if (c is HttpDataSource.InvalidResponseCodeException) return c.responseCode
            c = c.cause
        }
        return -1
    }

    private fun malformedHls(cause: IOException): Boolean {
        var c: Throwable? = cause
        while (c != null) {
            if (c is ParserException) return true
            val msg = c.message.orEmpty().lowercase()
            if (msg.contains("hls") || msg.contains("playlist") || msg.contains("manifest")) return true
            c = c.cause
        }
        return false
    }

    private fun transient(cause: IOException): Boolean {
        var c: Throwable? = cause
        while (c != null) {
            val name = c.javaClass.simpleName.lowercase()
            val msg = c.message.orEmpty().lowercase()
            if (name.contains("timeout") ||
                name.contains("socket") ||
                name.contains("unknownhost") ||
                name.contains("interruptedio") ||
                msg.contains("timeout") ||
                msg.contains("connection reset") ||
                msg.contains("connection closed") ||
                msg.contains("broken pipe") ||
                msg.contains("software caused connection abort") ||
                msg.contains("unexpected end of stream")
            ) return true
            c = c.cause
        }
        return false
    }
}
