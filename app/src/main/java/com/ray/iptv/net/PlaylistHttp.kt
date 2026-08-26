package com.ray.iptv.net

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FilterInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Mina IPTV playlist indirme kuralları: TinyURL önizleme, kısa link yönlendirmesi,
 * Google Drive `confirm=t`, http/https şema yedeklemesi, tarayıcı UA.
 */
object PlaylistHttp {
    private const val MaxHops = 8
    private val ShortHosts = setOf(
        "tinyurl.com",
        "preview.tinyurl.com",
        "bit.ly",
        "bitly.com",
        "j.mp",
        "is.gd",
        "t.ly",
        "cutt.ly",
        "rb.gy",
        "ow.ly",
        "shorturl.at",
        "rebrand.ly",
        "s.id"
    )
    private val MetaRefresh = Regex(
        """http-equiv\s*=\s*["']refresh["'][^>]*content\s*=\s*["'][^"']*url\s*=\s*([^"';>\s]+)""",
        RegexOption.IGNORE_CASE
    )
    private val Href = Regex(
        """href\s*=\s*["'](https?://[^"'>\s]+)["']""",
        RegexOption.IGNORE_CASE
    )

    fun driveConfirmUrl(url: String): String {
        if (!url.contains("drive.google.com/uc") || url.contains("confirm=")) return url
        return url + (if ('?' in url) '&' else '?') + "confirm=t"
    }

    fun swapScheme(url: String): String? {
        val trimmed = url.trim()
        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("https://") -> "http://" + trimmed.substring(8)
            lower.startsWith("http://") -> "https://" + trimmed.substring(7)
            else -> null
        }
    }

    fun isShortUrl(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host?.lowercase() ?: return false
        return ShortHosts.any { host == it || host.endsWith(".$it") }
    }

    /** Yönlendirme + TinyURL önizlemesinden sonraki gerçek playlist / panel URL'si. */
    fun resolve(http: OkHttpClient, url: String): String {
        val trimmed = url.trim()
        if (isShortUrl(trimmed)) {
            return resolveOnce(playlistClient(http, fast = false), trimmed, 0)
        }
        val swapped = swapScheme(trimmed)
        return try {
            resolveOnce(playlistClient(http, fast = swapped != null), trimmed, 0)
        } catch (first: Exception) {
            if (swapped == null) throw first
            resolveOnce(playlistClient(http, fast = false), swapped, 0)
        }
    }

    fun openStream(http: OkHttpClient, url: String): InputStream {
        val trimmed = url.trim()
        if (isShortUrl(trimmed)) {
            return openOnce(playlistClient(http, fast = false), trimmed, 0)
        }
        val swapped = swapScheme(trimmed)
        return try {
            openOnce(playlistClient(http, fast = swapped != null), trimmed, 0)
        } catch (first: Exception) {
            if (swapped == null) throw first
            openOnce(playlistClient(http, fast = false), swapped, 0)
        }
    }

    private fun playlistClient(http: OkHttpClient, fast: Boolean): OkHttpClient {
        val connect = if (fast) 8L else 45L
        val read = if (fast) 12L else 120L
        return http.newBuilder()
            .connectTimeout(connect, TimeUnit.SECONDS)
            .readTimeout(read, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun resolveOnce(http: OkHttpClient, url: String, hop: Int): String {
        require(hop < MaxHops) { "Çok fazla yönlendirme" }
        openResponse(http, url).use { resp ->
            tinyTarget(resp)?.let { return resolveOnce(http, it, hop + 1) }
            val peek = peekText(resp)
            if (isHtmlMiss(peek, resp.header("Content-Type"))) {
                extractTarget(peek, resp.request.url.toString())?.let {
                    return resolveOnce(http, it, hop + 1)
                }
            }
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.request.url.toString()
        }
    }

    private fun openOnce(http: OkHttpClient, url: String, hop: Int): InputStream {
        require(hop < MaxHops) { "Çok fazla yönlendirme" }
        val resp = openResponse(http, url)
        try {
            tinyTarget(resp)?.let {
                resp.close()
                return openOnce(http, it, hop + 1)
            }
            val peek = peekText(resp)
            if (isHtmlMiss(peek, resp.header("Content-Type"))) {
                val next = extractTarget(peek, resp.request.url.toString())
                resp.close()
                if (!next.isNullOrBlank()) return openOnce(http, next, hop + 1)
                error("Liste HTML önizleme döndü")
            }
            if (!resp.isSuccessful) {
                resp.close()
                error("HTTP ${resp.code}")
            }
            val body = resp.body ?: run {
                resp.close()
                error("Boş yanıt")
            }
            return object : FilterInputStream(body.byteStream()) {
                override fun close() {
                    super.close()
                    resp.close()
                }
            }
        } catch (t: Throwable) {
            resp.close()
            throw t
        }
    }

    private fun openResponse(http: OkHttpClient, url: String): Response {
        val target = driveConfirmUrl(url.trim())
        val req = Request.Builder()
            .url(target)
            .header("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, audio/mpegurl, audio/x-mpegurl, text/plain, */*")
            .get()
            .build()
        return http.newCall(req).execute()
    }

    private fun tinyTarget(resp: Response): String? {
        val raw = resp.header("x-tinyurl-target")
            ?: resp.header("X-TinyURL-Target")
            ?: return null
        val next = raw.trim()
        return next.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun peekText(resp: Response): String =
        runCatching { resp.peekBody(8 * 1024).string() }.getOrDefault("")

    private fun isHtmlMiss(peek: String, contentType: String?): Boolean {
        val t = peek.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        if (t.startsWith("#EXTM3U", ignoreCase = true) || t.startsWith("#EXTINF", ignoreCase = true)) {
            return false
        }
        val ct = contentType.orEmpty().lowercase()
        if (ct.contains("mpegurl") || ct.contains("m3u") || ct.contains("text/plain")) return false
        return ct.contains("html") ||
            t.startsWith("<!doctype", ignoreCase = true) ||
            t.startsWith("<html", ignoreCase = true) ||
            t.contains("<html", ignoreCase = true)
    }

    private fun extractTarget(html: String, currentUrl: String): String? {
        MetaRefresh.find(html)?.groupValues?.getOrNull(1)?.let { raw ->
            absolutize(currentUrl, raw.trim())?.let { return it }
        }
        val currentHost = currentUrl.toHttpUrlOrNull()?.host?.lowercase().orEmpty()
        val hrefs = Href.findAll(html).map { it.groupValues[1] }.toList()
        hrefs.firstOrNull { candidate ->
            val host = candidate.toHttpUrlOrNull()?.host?.lowercase().orEmpty()
            host.isNotEmpty() && host != currentHost && !isShortHost(host)
        }?.let { return it }
        hrefs.firstOrNull { !it.equals(currentUrl, ignoreCase = true) }?.let { return it }
        return null
    }

    private fun absolutize(base: String, raw: String): String? {
        val trimmed = raw.trim().trim('"', '\'')
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return base.toHttpUrlOrNull()?.resolve(trimmed)?.toString()
    }

    private fun isShortHost(host: String): Boolean =
        ShortHosts.any { host == it || host.endsWith(".$it") }
}
