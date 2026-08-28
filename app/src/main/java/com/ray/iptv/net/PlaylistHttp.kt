package com.ray.iptv.net

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FilterInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Playlist indirme kuralları: TinyURL önizleme & yönlendirmeleri, URL şema normalizasyonu,
 * kısa link çözümü, Google Drive `confirm=t`, http/https şema yedeklemesi, tarayıcı UA.
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
        "v.gd",
        "t.ly",
        "cutt.ly",
        "rb.gy",
        "ow.ly",
        "shorturl.at",
        "shorturl.ac",
        "rebrand.ly",
        "s.id",
        "tiny.cc",
        "gg.gg",
        "clck.ru",
        "rotf.lol",
        "soo.gd",
        "dub.sh",
        "lnk.to",
        "urlr.me",
        "kutt.it",
        "qr.ae",
        "shorte.st",
        "trib.al",
        "t.co",
        "adf.ly",
        "bc.vc",
        "bit.do",
        "tr.im",
        "tiny.one",
        "linktr.ee",
        "url.kr",
        "zcu.io",
        "short.io",
        "bl.ink",
        "snip.ly",
        "cutt.us",
        "chilp.it",
        "buff.ly",
        "smarturl.it",
        "qr.net",
        "1url.com",
        "cur.lv",
        "vzturl.com",
        "u.to",
        "hopx.top"
    )

    private val MetaRefresh = Regex(
        """http-equiv\s*=\s*["']?refresh["']?[^>]*content\s*=\s*["']?[^"'>]*url\s*=\s*([^"';>\s]+)""",
        RegexOption.IGNORE_CASE
    )
    private val Href = Regex(
        """href\s*=\s*["'](https?://[^"'>\s]+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val RedirectAnchor = Regex(
        """<a[^>]+(?:id|class)\s*=\s*["'](?:redirecturl|redirect|target)[^"']*["'][^>]+href\s*=\s*["']([^"'>\s]+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val JsRedirect = Regex(
        """(?:window\.|document\.)?location(?:\.href)?\s*=\s*["'](https?://[^"'>\s]+)["']|location\.replace\s*\(\s*["'](https?://[^"'>\s]+)["']\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val JsonTarget = Regex(
        """["'](?:target_url|destination|long_url|url)["']\s*:\s*["'](https?://[^"'>\s]+)["']""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Kullanıcının girdiği URL'deki görünmez karakterleri, tırnakları temizler ve
     * şeması yoksa (örn. `www.tinyurl.com/xxx` veya `tinyurl.com/xxx` veya `panel:8080`)
     * doğru `https://` veya `http://` ön ekini ekler.
     */
    fun normalizeUrl(raw: String): String {
        var s = raw.trim()
            .trimStart('\uFEFF', '\u200B', '\u00A0', ' ', '\t', '\n', '\r')
            .trimEnd('\uFEFF', '\u200B', '\u00A0', ' ', '\t', '\n', '\r')
            .trim('"', '\'', '`')
            .trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("file://", ignoreCase = true) ||
            s.startsWith("content://", ignoreCase = true) ||
            s.startsWith("stalker:", ignoreCase = true)
        ) {
            return s
        }
        if (s.startsWith("//")) {
            s = "https:$s"
        }
        val lower = s.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return s
        }
        val hostCandidate = s.substringBefore('/').substringBefore(':').lowercase()
        val preferHttps = isShortHost(hostCandidate) ||
            hostCandidate == "raw.githubusercontent.com" ||
            hostCandidate == "gist.githubusercontent.com" ||
            hostCandidate == "pastebin.com"

        return if (preferHttps) "https://$s" else "http://$s"
    }

    fun driveConfirmUrl(url: String): String {
        if (!url.contains("drive.google.com/uc") || url.contains("confirm=")) return url
        return url + (if ('?' in url) '&' else '?') + "confirm=t"
    }

    fun swapScheme(url: String): String? {
        val trimmed = normalizeUrl(url)
        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("https://") -> "http://" + trimmed.substring(8)
            lower.startsWith("http://") -> "https://" + trimmed.substring(7)
            else -> null
        }
    }

    fun isShortUrl(url: String): Boolean {
        val normalized = normalizeUrl(url)
        val parsed = normalized.toHttpUrlOrNull() ?: return false
        val host = parsed.host.lowercase()
        if (isShortHost(host)) return true
        // Tek segmentli çok kısa slug (örn: domain.com/abc123)
        val segments = parsed.pathSegments.filter { it.isNotEmpty() }
        if (segments.size == 1 && segments[0].length <= 10 && !segments[0].contains('.')) {
            return true
        }
        return false
    }

    fun isShortHost(host: String): Boolean =
        ShortHosts.any { host == it || host.endsWith(".$it") }

    /** Yönlendirme + TinyURL önizlemesinden sonraki gerçek playlist / panel URL'si. */
    fun resolve(http: OkHttpClient, url: String): String {
        val normalized = normalizeUrl(url)
        if (normalized.isBlank()) return url
        val swapped = swapScheme(normalized)
        return try {
            resolveOnce(playlistClient(http, fast = isShortUrl(normalized)), normalized, 0)
        } catch (first: Exception) {
            if (swapped == null) return normalized
            try {
                resolveOnce(playlistClient(http, fast = false), swapped, 0)
            } catch (_: Exception) {
                normalized
            }
        }
    }

    fun openStream(http: OkHttpClient, url: String): InputStream {
        val normalized = normalizeUrl(url)
        if (isShortUrl(normalized)) {
            return openOnce(playlistClient(http, fast = false), normalized, 0)
        }
        val swapped = swapScheme(normalized)
        return try {
            openOnce(playlistClient(http, fast = swapped != null), normalized, 0)
        } catch (first: Exception) {
            if (swapped == null) throw first
            openOnce(playlistClient(http, fast = false), swapped, 0)
        }
    }

    private fun playlistClient(http: OkHttpClient, fast: Boolean): OkHttpClient {
        val connect = if (fast) 15L else 35L
        val read = if (fast) 60L else 120L
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
        val target = driveConfirmUrl(normalizeUrl(url))
        val req = Request.Builder()
            .url(target)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )
            .header(
                "Accept",
                "application/x-mpegURL, application/vnd.apple.mpegurl, audio/mpegurl, audio/x-mpegurl, text/plain, */*"
            )
            .header("Accept-Language", "en-US,en;q=0.9,tr;q=0.8")
            .get()
            .build()
        return http.newCall(req).execute()
    }

    private fun tinyTarget(resp: Response): String? {
        val headersToCheck = listOf(
            "x-tinyurl-target",
            "X-TinyURL-Target",
            "x-lighttpd-longurl",
            "X-Lighttpd-LongUrl",
            "x-target-url",
            "X-Target-Url",
            "Location",
            "location",
            "Refresh",
            "refresh"
        )
        for (h in headersToCheck) {
            val raw = resp.header(h)?.trim() ?: continue
            if (raw.isBlank()) continue
            val target = if (raw.contains("url=", ignoreCase = true)) {
                raw.substringAfter("url=", "").substringAfter("URL=", "").trim().trim('"', '\'')
            } else raw
            val normalized = normalizeUrl(target)
            if (normalized.startsWith("http://", ignoreCase = true) || normalized.startsWith("https://", ignoreCase = true)) {
                return normalized
            }
        }
        return null
    }

    private fun peekText(resp: Response): String =
        runCatching { resp.peekBody(128 * 1024).string() }.getOrDefault("")

    private fun isM3uContent(peek: String): Boolean {
        val t = peek.trimStart('\uFEFF', '\u200B', '\u00A0', ' ', '\n', '\r', '\t')
        if (t.startsWith("#EXT", ignoreCase = true) || t.startsWith("#PLAYLIST", ignoreCase = true)) return true
        val firstLine = t.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return firstLine.startsWith("#EXT", ignoreCase = true) ||
            firstLine.startsWith("http://", ignoreCase = true) ||
            firstLine.startsWith("https://", ignoreCase = true) ||
            firstLine.startsWith("rtmp://", ignoreCase = true)
    }

    private fun isHtmlMiss(peek: String, contentType: String?): Boolean {
        if (isM3uContent(peek)) return false
        val ct = contentType.orEmpty().lowercase()
        if (ct.contains("mpegurl") || ct.contains("m3u") || ct.contains("audio/x-mpegurl") || ct.contains("video/mp2t")) {
            return false
        }
        val t = peek.trimStart('\uFEFF', '\u200B', '\u00A0', ' ', '\n', '\r', '\t').lowercase()
        return ct.contains("html") ||
            t.startsWith("<!doctype") ||
            t.startsWith("<html") ||
            t.contains("<html") ||
            t.contains("<body")
    }

    private fun extractTarget(html: String, currentUrl: String): String? {
        MetaRefresh.find(html)?.groupValues?.getOrNull(1)?.let { raw ->
            absolutize(currentUrl, raw.trim())?.let { return it }
        }
        RedirectAnchor.find(html)?.groupValues?.getOrNull(1)?.let { raw ->
            absolutize(currentUrl, raw.trim())?.let { return it }
        }
        JsRedirect.find(html)?.let { mr ->
            val match = mr.groupValues[1].ifEmpty { mr.groupValues.getOrNull(2).orEmpty() }
            if (match.isNotBlank()) absolutize(currentUrl, match.trim())?.let { return it }
        }
        JsonTarget.find(html)?.groupValues?.getOrNull(1)?.let { raw ->
            absolutize(currentUrl, raw.trim())?.let { return it }
        }
        val currentHost = currentUrl.toHttpUrlOrNull()?.host?.lowercase().orEmpty()
        val hrefs = Href.findAll(html).map { it.groupValues[1] }.toList()
        hrefs.firstOrNull { candidate ->
            val host = candidate.toHttpUrlOrNull()?.host?.lowercase().orEmpty()
            host.isNotEmpty() && host != currentHost && !isShortHost(host) &&
                !candidate.contains("/terms", ignoreCase = true) &&
                !candidate.contains("/privacy", ignoreCase = true) &&
                !candidate.contains("/login", ignoreCase = true) &&
                !candidate.contains("/register", ignoreCase = true) &&
                !candidate.contains("/abuse", ignoreCase = true)
        }?.let { return it }
        hrefs.firstOrNull { !it.equals(currentUrl, ignoreCase = true) }?.let { return it }
        return null
    }

    private fun absolutize(base: String, raw: String): String? {
        val trimmed = raw.trim().trim('"', '\'')
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        return base.toHttpUrlOrNull()?.resolve(trimmed)?.toString()
    }
}

