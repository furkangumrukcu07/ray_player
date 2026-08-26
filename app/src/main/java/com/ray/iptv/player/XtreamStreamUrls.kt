package com.ray.iptv.player

import android.net.Uri
import com.ray.iptv.data.repo.StreamFormat

/**
 * Mina `PlayerPlaybackController` canlı yedek URL zinciri:
 * `/live/.../id.ts` ↔ `.m3u8`, `get.php&output=ts` ↔ `m3u8`,
 * gerekirse `get.php` ↔ `/live/` yolu.
 */
object XtreamStreamUrls {

    fun applyFormat(url: String, format: StreamFormat): String {
        if (url.isBlank()) return url
        return when (format) {
            StreamFormat.AUTO -> {
                if (StreamHints.hls(url) || StreamHints.mpegTs(url) || StreamHints.dash(url)) url
                else if (looksLiveXtream(url)) forceContainer(url, hls = false)
                else url
            }
            StreamFormat.HLS -> forceContainer(url, hls = true)
            StreamFormat.TS -> forceContainer(url, hls = false)
        }
    }

    /** Canlı Xtream URL'sini HLS veya MPEG-TS kabına zorlar (uzantı / output=). */
    fun forceContainer(url: String, hls: Boolean): String {
        val u = url.trim()
        if (u.isEmpty()) return url
        val uri = runCatching { Uri.parse(u) }.getOrNull() ?: return url
        val path = uri.path.orEmpty()
        val pathLc = path.lowercase()
        val wantExt = if (hls) "m3u8" else "ts"
        if (pathLc.endsWith("get.php")) {
            val out = uri.getQueryParameter("output").orEmpty().lowercase()
            val already = if (hls) out in hlsOutputs else out in tsOutputs
            if (already) return u
            return withOutput(uri, wantExt)
        }
        if (pathLc.contains("/live/")) {
            val slash = path.lastIndexOf('/')
            val file = if (slash >= 0) path.substring(slash + 1) else path
            val dot = file.lastIndexOf('.')
            val newFile = if (dot > 0) file.substring(0, dot) + ".$wantExt" else "$file.$wantExt"
            val newPath = if (slash >= 0) path.substring(0, slash + 1) + newFile else newFile
            if (newPath == path) return u
            return uri.buildUpon().path(newPath).build().toString()
        }
        return naiveExt(u, if (hls) StreamFormat.HLS else StreamFormat.TS)
    }

    /** Canlı Xtream TS ↔ HLS. Uygun değilse null. */
    fun swapLiveTsHls(url: String): String? {
        val u = url.trim()
        if (u.isEmpty()) return null
        val uri = Uri.parse(u)
        val path = uri.path.orEmpty().lowercase()
        val raw = u.lowercase()

        if (path.contains("/live/")) {
            val p = uri.path ?: return null
            return when {
                raw.substringBefore('?').endsWith(".ts") ->
                    uri.buildUpon().path(p.dropLast(3) + ".m3u8").build().toString()
                raw.substringBefore('?').endsWith(".m3u8") ->
                    uri.buildUpon().path(p.dropLast(5) + ".ts").build().toString()
                else -> null
            }
        }

        if (path.endsWith("get.php")) {
            val streamId = uri.getQueryParameter("stream_id").orEmpty()
            if (streamId.isBlank()) return null
            val out = uri.getQueryParameter("output").orEmpty().lowercase()
            return when {
                out.isBlank() || out in tsOutputs -> withOutput(uri, "m3u8")
                out in hlsOutputs -> withOutput(uri, "ts")
                else -> null
            }
        }
        return null
    }

    /** `get.php?...` ↔ `/live/user/pass/id.(ts|m3u8)` — ikinci basamak yedek. */
    fun swapGetPhpAndLivePath(url: String): String? {
        val u = url.trim()
        if (u.isEmpty()) return null
        val uri = Uri.parse(u)
        val path = uri.path.orEmpty().lowercase()
        if (path.endsWith("get.php")) return getPhpToLive(uri)
        if (path.contains("/live/")) return liveToGetPhp(uri)
        return null
    }

    fun looksLiveXtream(url: String): Boolean {
        val raw = url.lowercase()
        return raw.contains("/live/") ||
            raw.contains("get.php") && raw.contains("stream_id")
    }

    private fun getPhpToLive(uri: Uri): String? {
        val id = uri.getQueryParameter("stream_id").orEmpty()
        val user = uri.getQueryParameter("username").orEmpty()
        val pass = uri.getQueryParameter("password").orEmpty()
        if (id.isBlank() || user.isBlank() || pass.isBlank()) return null
        val out = uri.getQueryParameter("output").orEmpty().lowercase()
        val ext = when {
            out in hlsOutputs -> "m3u8"
            out.contains("mpd") -> "mpd"
            else -> "ts"
        }
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "${uri.scheme}://${uri.host}$port/live/${Uri.encode(user)}/${Uri.encode(pass)}/$id.$ext"
    }

    private fun liveToGetPhp(uri: Uri): String? {
        val parts = uri.pathSegments.filter { it.isNotBlank() }
        if (parts.size < 4 || !parts[0].equals("live", true)) return null
        val user = Uri.decode(parts[1])
        val pass = Uri.decode(parts[2])
        val last = parts[3]
        val dot = last.lastIndexOf('.')
        if (dot <= 0) return null
        val id = last.substring(0, dot)
        val output = last.substring(dot + 1)
        if (id.toIntOrNull() == null) return null
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "${uri.scheme}://${uri.host}$port/get.php?username=${Uri.encode(user)}" +
            "&password=${Uri.encode(pass)}&stream_id=$id&output=$output"
    }

    private fun withOutput(uri: Uri, output: String): String {
        val b = uri.buildUpon().clearQuery()
        uri.queryParameterNames.forEach { name ->
            if (name.equals("output", true)) return@forEach
            uri.getQueryParameters(name).forEach { b.appendQueryParameter(name, it) }
        }
        b.appendQueryParameter("output", output)
        return b.build().toString()
    }

    private fun naiveExt(url: String, format: StreamFormat): String = when (format) {
        StreamFormat.HLS -> url.replace(Regex("""\.ts(\?|$)""", RegexOption.IGNORE_CASE), ".m3u8$1")
        StreamFormat.TS -> url.replace(Regex("""\.m3u8(\?|$)""", RegexOption.IGNORE_CASE), ".ts$1")
        StreamFormat.AUTO -> url
    }

    private val tsOutputs = setOf("ts", "mpegts", "mpeg-ts", "m2ts")
    private val hlsOutputs = setOf("m3u8", "m3u", "hls")
}
