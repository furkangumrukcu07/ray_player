package com.ray.iptv.player

import com.ray.iptv.data.repo.StreamFormat

/** Mina `MinaIptvMediaSourceFactory` URL ipuçları. */
internal object StreamHints {
    fun hls(url: String): Boolean {
        val raw = url.lowercase()
        return raw.contains(".m3u8") ||
            raw.contains("output=m3u8") ||
            raw.contains("output=m3u") ||
            raw.contains("output=hls") ||
            raw.contains("type=m3u8") ||
            raw.contains("type=hls") ||
            raw.contains("container=m3u8") ||
            raw.contains("ext=m3u8")
    }

    fun mpegTs(url: String): Boolean {
        val raw = url.lowercase()
        val path = raw.substringBefore('?')
        return path.endsWith(".ts") ||
            raw.contains("output=ts") ||
            raw.contains("output=mpegts") ||
            raw.contains("output=mpeg-ts") ||
            raw.contains("output=m2ts") ||
            raw.contains("ext=ts") ||
            raw.contains("type=ts") ||
            raw.contains("type=mpegts") ||
            raw.contains("container=ts")
    }

    fun dash(url: String): Boolean {
        val raw = url.lowercase()
        return raw.contains(".mpd") ||
            raw.contains("output=mpd") ||
            raw.contains("type=dash")
    }

    fun liveIptv(url: String): Boolean {
        val raw = url.lowercase()
        return raw.contains("/live/") ||
            (raw.contains("get.php") && raw.contains("stream_id")) ||
            raw.contains("output=ts") ||
            raw.contains("output=m3u8") ||
            mpegTs(url) ||
            hls(url)
    }

    fun kind(url: String, format: StreamFormat, live: Boolean = true): Kind {
        if (url.startsWith("rtsp://", true)) return Kind.RTSP
        if (dash(url)) return Kind.DASH
        if (hls(url)) return Kind.HLS
        if (mpegTs(url)) return Kind.TS
        if (live) {
            if (format == StreamFormat.HLS) return Kind.HLS
            if (format == StreamFormat.TS) return Kind.TS
            if (liveIptv(url)) return Kind.TS
        }
        return Kind.OTHER
    }

    fun extensionlessWebManifest(url: String): Boolean {
        val u = url.trim().lowercase()
        if (!u.startsWith("http")) return false
        val path = u.substringAfter("://").substringAfter('/').substringBefore('?')
        val media = listOf(
            ".mp4", ".mkv", ".avi", ".mov", ".webm", ".m4v", ".wmv",
            ".mpg", ".mpeg", ".ts", ".m3u8", ".m3u", ".mpd", ".flv"
        )
        if (media.any { path.endsWith(it) }) return false
        if (u.contains("/live/") || u.contains("/movie/") || u.contains("/series/")) return false
        if (u.contains("get.php") || u.contains("output=") || u.contains("m3u8") || u.contains("mpd")) return false
        return Regex("/tt\\d{6,}").containsMatchIn(u) ||
            listOf("/vs/", "/embed/", "/watch/", "/player/").any { u.contains(it) }
    }

    enum class Kind { RTSP, HLS, DASH, TS, OTHER }
}
