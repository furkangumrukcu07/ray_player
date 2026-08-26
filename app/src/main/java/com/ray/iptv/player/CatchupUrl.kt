package com.ray.iptv.player

import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.data.remote.XtreamClient
import com.ray.iptv.data.repo.CatchupPreset
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Mina `CatchUpUrlBuilder` — Xtream timeshift / timeshift.php / özel şablon. */
object CatchupUrl {
    const val PATH_TEMPLATE =
        "{server}/timeshift/{username}/{password}/{duration}/{start_utc_ymd_hms}/{stream_id}.{extension}"
    const val PHP_TEMPLATE =
        "{server}/streaming/timeshift.php?username={username}&password={password}&stream={stream_id}&start={start_unix}&duration={duration}"

    fun build(
        source: SourceEntity,
        streamId: String,
        programme: EpgEntity,
        preset: CatchupPreset,
        custom: String,
        deviceTz: Boolean,
        extension: String = "ts"
    ): String? {
        if (preset == CatchupPreset.OFF) return null
        val template = when (preset) {
            CatchupPreset.OFF -> return null
            CatchupPreset.XTREAM_PATH -> PATH_TEMPLATE
            CatchupPreset.TIMESHIFT_PHP -> PHP_TEMPLATE
            CatchupPreset.CUSTOM -> custom.trim().ifBlank { PATH_TEMPLATE }
        }
        val durationSec = ((programme.endMs - programme.startMs) / 1000L).toInt().coerceAtLeast(60)
        val durationMin = (durationSec / 60).coerceAtLeast(1)
        val tz = if (deviceTz) TimeZone.getDefault() else TimeZone.getTimeZone("UTC")
        val utc = TimeZone.getTimeZone("UTC")
        fun fmt(pattern: String, zone: TimeZone, ms: Long) =
            SimpleDateFormat(pattern, Locale.US).apply { timeZone = zone }.format(Date(ms))
        val server = XtreamClient.normalize(source.baseUrl)
        val user = enc(source.username)
        val pass = enc(source.password)
        val durationValue = if (preset == CatchupPreset.TIMESHIFT_PHP) durationSec else durationMin
        val map = mapOf(
            "server" to server,
            "username" to user,
            "password" to pass,
            "stream_id" to streamId,
            "stream" to streamId,
            "duration" to durationValue.toString(),
            "duration_sec" to durationSec.toString(),
            "start_unix" to (programme.startMs / 1000).toString(),
            "end_unix" to (programme.endMs / 1000).toString(),
            "start_ms" to programme.startMs.toString(),
            "end_ms" to programme.endMs.toString(),
            "start_utc_ymd_hms" to fmt("yyyy-MM-dd:HH-mm-ss", utc, programme.startMs),
            "start_utc_ymd_hm" to fmt("yyyy-MM-dd:HH-mm", utc, programme.startMs),
            "start_local_ymd_hms" to fmt("yyyy-MM-dd:HH-mm-ss", tz, programme.startMs),
            "extension" to extension.trim().ifBlank { "ts" }
        )
        return Regex("""\{([a-z0-9_]+)\}""").replace(template) { m ->
            map[m.groupValues[1]] ?: m.value
        }
    }

    private fun enc(v: String) = URLEncoder.encode(v, Charsets.UTF_8.name()).replace("+", "%20")
}
