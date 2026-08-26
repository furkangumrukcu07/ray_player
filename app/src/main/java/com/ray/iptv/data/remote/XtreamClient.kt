package com.ray.iptv.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class XtreamSession(
    val base: String,
    val username: String,
    val password: String,
    val expires: String,
    val status: String,
    val liveUrl: (String, String) -> String,
    val vodUrl: (String, String) -> String,
    val seriesUrl: (String, String) -> String
)

data class XtreamUserInfo(
    val username: String = "",
    val password: String = "",
    val status: String = "",
    val message: String = "",
    val auth: Int? = null,
    val expiryEpochSec: Long? = null,
    val createdEpochSec: Long? = null,
    val isTrial: Boolean = false,
    val activeCons: Int = 0,
    val maxCons: Int = 0,
    val allowedOutputs: List<String> = emptyList()
)

data class XtreamServerInfo(
    val url: String = "",
    val port: String = "",
    val httpsPort: String = "",
    val rtmpPort: String = "",
    val protocol: String = "",
    val timezone: String = "",
    val timeNow: String = "",
    val timestampNow: Long? = null,
    val process: Boolean? = null,
    val revision: String = ""
) {
    val isEmpty: Boolean
        get() = url.isBlank() &&
            port.isBlank() &&
            httpsPort.isBlank() &&
            rtmpPort.isBlank() &&
            protocol.isBlank() &&
            timezone.isBlank() &&
            timeNow.isBlank() &&
            timestampNow == null &&
            process == null &&
            revision.isBlank()
}

data class XtreamAccountSnapshot(
    val user: XtreamUserInfo?,
    val server: XtreamServerInfo?,
    val baseUrl: String
)

@Singleton
class XtreamClient @Inject constructor(
    private val http: OkHttpClient,
    private val json: Json
) {
    suspend fun authenticate(baseRaw: String, user: String, pass: String): XtreamSession {
        val base = normalize(baseRaw)
        val obj = getJson(base, "username" to user, "password" to pass).jsonObject
        val userInfo = obj["user_info"]?.jsonObject
        val server = obj["server_info"]?.jsonObject
        require(userInfo != null) { "Xtream login failed" }
        val auth = userInfo.str("auth")
        if (auth == "0" || auth.equals("false", true)) error("Invalid credentials")
        val protocol = server.str("server_protocol").ifBlank { "http" }
        val host = server.str("url").ifBlank { base.removePrefix("http://").removePrefix("https://").substringBefore("/") }
        val port = server.str("port")
        val origin = if (port.isBlank() || port == "80" || port == "443") {
            "$protocol://$host"
        } else {
            "$protocol://$host:$port"
        }
        return XtreamSession(
            base = origin,
            username = user,
            password = pass,
            expires = userInfo.str("exp_date"),
            status = userInfo.str("status"),
            liveUrl = { id, ext -> "$origin/live/$user/$pass/$id.${ext.ifBlank { "m3u8" }}" },
            vodUrl = { id, ext -> "$origin/movie/$user/$pass/$id.${ext.ifBlank { "mp4" }}" },
            seriesUrl = { id, ext -> "$origin/series/$user/$pass/$id.${ext.ifBlank { "mp4" }}" }
        )
    }

    /** Mina `getXtreamAccountSnapshot` — `player_api.php` user_info + server_info. */
    suspend fun fetchAccountSnapshot(baseRaw: String, user: String, pass: String): XtreamAccountSnapshot? {
        val primary = normalize(baseRaw)
        val swapped = swapScheme(primary)
        val first = snapshotAt(primary, user, pass)
        if (first != null && (first.user != null || first.server != null)) return first
        if (swapped != null && swapped != primary) {
            val second = snapshotAt(swapped, user, pass)
            if (second != null) return second
        }
        return first
    }

    private suspend fun snapshotAt(base: String, user: String, pass: String): XtreamAccountSnapshot? {
        val obj = runCatching {
            getJson(base, "username" to user, "password" to pass).jsonObject
        }.getOrNull() ?: return null
        val userObj = runCatching { obj["user_info"]?.jsonObject }.getOrNull()
        val serverObj = runCatching { obj["server_info"]?.jsonObject }.getOrNull()
        return XtreamAccountSnapshot(
            user = parseUserInfo(userObj),
            server = parseServerInfo(serverObj),
            baseUrl = base
        )
    }

    private fun parseUserInfo(map: JsonObject?): XtreamUserInfo? {
        if (map == null || map.isEmpty()) return null
        val authRaw = map.str("auth")
        val auth = when {
            authRaw.isBlank() -> null
            authRaw == "1" || authRaw.equals("true", true) -> 1
            authRaw == "0" || authRaw.equals("false", true) -> 0
            else -> authRaw.toIntOrNull()
        }
        return XtreamUserInfo(
            username = map.str("username"),
            password = map.str("password"),
            status = map.str("status"),
            message = map.str("message"),
            auth = auth,
            expiryEpochSec = map.epochSec("exp_date"),
            createdEpochSec = map.epochSec("created_at"),
            isTrial = map.bool("is_trial"),
            activeCons = map.int("active_cons"),
            maxCons = map.int("max_connections"),
            allowedOutputs = map.strList("allowed_output_formats")
        )
    }

    private fun parseServerInfo(map: JsonObject?): XtreamServerInfo? {
        if (map == null || map.isEmpty()) return null
        val info = XtreamServerInfo(
            url = map.str("url"),
            port = map.str("port"),
            httpsPort = map.str("https_port"),
            rtmpPort = map.str("rtmp_port"),
            protocol = map.str("server_protocol"),
            timezone = map.str("timezone"),
            timeNow = map.str("time_now"),
            timestampNow = map.epochSec("timestamp_now"),
            process = map.boolOrNull("process"),
            revision = map.str("revision")
        )
        return info.takeUnless { it.isEmpty }
    }

    /** Mina `loadFromXtreamResolved`: http/https kapalıysa karşı şemayı paralel dener. */
    suspend fun authenticateResolved(baseRaw: String, user: String, pass: String): XtreamSession {
        val primary = normalize(baseRaw)
        val swapped = swapScheme(primary) ?: return authenticate(primary, user, pass)
        return supervisorScope {
            val done = CompletableDeferred<XtreamSession>()
            val fails = java.util.concurrent.atomic.AtomicInteger(0)
            val primaryErr = java.util.concurrent.atomic.AtomicReference<Throwable>()
            fun attempt(base: String, isPrimary: Boolean) {
                launch {
                    runCatching { authenticate(base, user, pass) }
                        .onSuccess { done.complete(it) }
                        .onFailure { err ->
                            if (isPrimary) primaryErr.set(err)
                            if (fails.incrementAndGet() >= 2) {
                                done.completeExceptionally(primaryErr.get() ?: err)
                            }
                        }
                }
            }
            attempt(primary, true)
            attempt(swapped, false)
            done.await()
        }
    }

    suspend fun categories(base: String, user: String, pass: String, action: String): List<JsonObject> =
        array(base, user, pass, action)

    suspend fun streams(base: String, user: String, pass: String, action: String): List<JsonObject> =
        array(base, user, pass, action)

    /** Listeyi bellekte çoğaltmadan her öğeyi işler (TV kutusu OOM). */
    suspend fun forEachStream(
        base: String,
        user: String,
        pass: String,
        action: String,
        onEach: suspend (JsonObject) -> Unit
    ) {
        val el = getJson(base, "username" to user, "password" to pass, "action" to action)
        val arr = when {
            el is JsonArray -> el
            el is JsonObject && el["epg_listings"] is JsonArray -> el["epg_listings"]!!.jsonArray
            else -> JsonArray(emptyList())
        }
        for (item in arr) {
            val o = runCatching { item.jsonObject }.getOrNull() ?: continue
            onEach(o)
        }
    }

    suspend fun vodInfo(base: String, user: String, pass: String, id: String): JsonObject =
        getJson(base, "username" to user, "password" to pass, "action" to "get_vod_info", "vod_id" to id).jsonObject

    suspend fun seriesInfo(base: String, user: String, pass: String, id: String): JsonObject =
        getJson(base, "username" to user, "password" to pass, "action" to "get_series_info", "series_id" to id).jsonObject

    suspend fun shortEpg(base: String, user: String, pass: String, streamId: String, limit: Int = 12): JsonObject =
        getJson(
            base,
            "username" to user,
            "password" to pass,
            "action" to "get_short_epg",
            "stream_id" to streamId,
            "limit" to limit.toString()
        ).jsonObject

    suspend fun xmltv(base: String, user: String, pass: String): String {
        val url = "${normalize(base)}/xmltv.php?username=$user&password=$pass"
        return body(url, maxBytes = XMLTV_STRING_MAX_BYTES)
    }

    suspend fun <T> readXmltv(base: String, user: String, pass: String, consume: (java.io.InputStream) -> T): T {
        val url = "${normalize(base)}/xmltv.php?username=$user&password=$pass"
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val req = Request.Builder().url(url).get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("empty xmltv")
                consume(body.byteStream())
            }
        }
    }

    /** Mina `get_all_live_epg` — stream_id anahtarlı panel EPG; xmltv.php yedeğinden önce dener. */
    suspend fun getAllLiveEpg(
        base: String,
        user: String,
        pass: String,
        maxBytes: Long = ALL_LIVE_EPG_MAX_BYTES
    ): Map<String, List<com.ray.iptv.data.epg.XtreamEpgProgramme>> {
        val root = "${normalize(base)}/player_api.php".toHttpUrlOrNull() ?: return emptyMap()
        val url = root.newBuilder()
            .addQueryParameter("username", user)
            .addQueryParameter("password", pass)
            .addQueryParameter("action", "get_all_live_epg")
            .build()
            .toString()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyMap()
                    val bodyStream = resp.body?.byteStream() ?: return@use emptyMap()
                    val raw = bodyStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    if (raw.isBlank()) return@use emptyMap()
                    val trimmed = raw.trimStart()
                    if (trimmed.startsWith("<")) return@use emptyMap()
                    com.ray.iptv.data.epg.XtreamAllLiveEpg.parse(json.parseToJsonElement(raw))
                }
            }.getOrDefault(emptyMap())
        }
    }

    fun timeshiftUrl(base: String, user: String, pass: String, streamId: String, start: String, durationMin: Int): String {
        val origin = normalize(base)
        return "$origin/timeshift/$user/$pass/$durationMin/$start/$streamId.ts"
    }

    private suspend fun array(base: String, user: String, pass: String, action: String): List<JsonObject> {
        val el = getJson(base, "username" to user, "password" to pass, "action" to action)
        val arr = when {
            el is JsonArray -> el
            el is JsonObject && el["epg_listings"] is JsonArray -> el["epg_listings"]!!.jsonArray
            else -> JsonArray(emptyList())
        }
        return arr.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
    }

    private suspend fun getJson(base: String, vararg queries: Pair<String, String>): kotlinx.serialization.json.JsonElement {
        val root = "${normalize(base)}/player_api.php".toHttpUrlOrNull()
            ?: error("Bad server URL")
        val builder = root.newBuilder()
        queries.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return json.parseToJsonElement(body(builder.build().toString()))
    }

    private suspend fun body(url: String, maxBytes: Long = DEFAULT_MAX_BYTES): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val req = Request.Builder().url(url).get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val source = resp.body?.source() ?: return@use ""
                val buf = okio.Buffer()
                var copied = 0L
                while (!source.exhausted()) {
                    val n = source.read(buf, 8192)
                    if (n <= 0L) break
                    copied += n
                    if (copied > maxBytes) error("response too large")
                }
                buf.readUtf8()
            }
        }

    companion object {
        private const val DEFAULT_MAX_BYTES = 24L * 1024 * 1024
        private const val ALL_LIVE_EPG_MAX_BYTES = 32L * 1024 * 1024
        private const val XMLTV_STRING_MAX_BYTES = 8L * 1024 * 1024

        fun normalize(raw: String): String {
            var s = raw.trim().removeSuffix("/")
            if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
            return s.substringBefore("/player_api.php")
                .substringBefore("/get.php")
                .substringBefore("/playlist.php")
        }

        fun swapScheme(url: String): String? = when {
            url.startsWith("https://", ignoreCase = true) -> "http://" + url.substringAfter("://")
            url.startsWith("http://", ignoreCase = true) -> "https://" + url.substringAfter("://")
            else -> null
        }
    }
}

fun JsonObject?.str(key: String): String {
    val el = this?.get(key) ?: return ""
    return el.jsonPrimitive.contentOrNull
        ?: el.jsonPrimitive.intOrNull?.toString()
        ?: el.jsonPrimitive.booleanOrNull?.toString().orEmpty()
}

fun JsonObject?.bool(key: String): Boolean {
    val v = str(key)
    return v == "1" || v.equals("true", true)
}

fun JsonObject?.int(key: String): Int = str(key).toIntOrNull() ?: 0

fun JsonObject?.epochSec(key: String): Long? {
    val s = str(key).trim()
    if (s.isEmpty() || s == "0" || s.equals("null", true) || s.equals("false", true)) return null
    return s.toLongOrNull()?.takeIf { it > 0 }
}

fun JsonObject?.boolOrNull(key: String): Boolean? {
    val el = this?.get(key) ?: return null
    val p = runCatching { el.jsonPrimitive }.getOrNull() ?: return null
    p.booleanOrNull?.let { return it }
    p.intOrNull?.let { return it != 0 }
    val s = p.contentOrNull?.trim()?.lowercase().orEmpty()
    return when (s) {
        "1", "true" -> true
        "0", "false" -> false
        else -> null
    }
}

fun JsonObject?.strList(key: String): List<String> {
    val el = this?.get(key) ?: return emptyList()
    if (el is JsonArray) {
        return el.mapNotNull { item ->
            val p = runCatching { item.jsonPrimitive }.getOrNull() ?: return@mapNotNull null
            (p.contentOrNull ?: p.intOrNull?.toString()).orEmpty().trim().takeIf { it.isNotEmpty() }
        }
    }
    return str(key).split(Regex("[,;\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
}
