package com.ray.iptv.data.parser

import android.net.Uri

data class XtreamSniff(
    val baseUrl: String,
    val username: String,
    val password: String
)

/**
 * Mina `M3uXtreamSniffer`: M3U URL'sinde Xtream kimliği varsa
 * `scheme://host[:port]` + user/pass üretir. Yol (`get.php` / `playlist.php`) yok sayılır.
 *
 * Tipik kalıplar:
 * * `get.php?username=&password=&type=m3u_plus`
 * * `get.php?...&output=ts`
 * * `playlist.php?username=&password=`
 * * `auth=` + `auth_password=`
 *
 * Path-style `/live/user/pass/id` **sniff edilmez**.
 */
object M3uXtreamSniffer {
    private val userKeys = listOf("username", "user", "auth")
    private val passKeys = listOf("password", "pass", "auth_password")

    fun toXtreamSource(rawUrl: String): XtreamSniff? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null
        val input = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "http://$trimmed"
        } else trimmed
        val uri = runCatching { Uri.parse(input) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host.orEmpty()
        if (host.isEmpty()) return null

        val ciParams = linkedMapOf<String, String>()
        for (key in uri.queryParameterNames) {
            val values = uri.getQueryParameters(key)
            val v = values.firstOrNull { it.trim().isNotEmpty() }?.trim().orEmpty()
            val k = key.lowercase()
            if (v.isNotEmpty() && k !in ciParams) ciParams[k] = v
        }

        fun pick(keys: List<String>): String? {
            for (k in keys) {
                val v = ciParams[k]
                if (!v.isNullOrEmpty()) return v
            }
            return null
        }

        val username = pick(userKeys) ?: return null
        val password = pick(passKeys) ?: return null

        val port = uri.port
        val portPart = if (port > 0) ":$port" else ""
        val baseUrl = "$scheme://$host$portPart"
        return XtreamSniff(baseUrl, username, password)
    }

    fun looksLikeXtream(rawUrl: String): Boolean = toXtreamSource(rawUrl) != null

    /** `output=` → `"ts"` / `"hls"` / null (Mina `liveFormatHint`). */
    fun liveFormatHint(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null
        val input = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "http://$trimmed"
        } else trimmed
        val uri = runCatching { Uri.parse(input) }.getOrNull() ?: return null
        var out: String? = null
        for (key in uri.queryParameterNames) {
            if (!key.equals("output", ignoreCase = true)) continue
            for (v in uri.getQueryParameters(key)) {
                val t = v.trim().lowercase()
                if (t.isNotEmpty()) {
                    out = t
                    break
                }
            }
        }
        val o = out ?: return null
        return when (o) {
            "ts", "mpegts", "mpeg-ts", "m2ts" -> "ts"
            "m3u8", "m3u", "hls" -> "hls"
            else -> null
        }
    }
}
