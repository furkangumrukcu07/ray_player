package com.ray.iptv.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class StalkerChannel(
    val id: String,
    val name: String,
    val number: Int,
    val logo: String,
    val genreId: String,
    val cmd: String,
    val archive: Boolean
)

data class StalkerGenre(val id: String, val name: String)

@Singleton
class StalkerClient @Inject constructor(
    private val http: OkHttpClient,
    private val json: Json
) {
    suspend fun handshake(portal: String, mac: String): String {
        val obj = get(portal, mac, "type=stb&action=handshake")
        val js = obj["js"]?.jsonObject ?: obj
        return js.str("token").ifBlank { error("Stalker handshake failed") }
    }

    suspend fun genres(portal: String, mac: String, token: String): List<StalkerGenre> {
        val obj = get(portal, mac, "type=itv&action=get_genres", token)
        val arr = obj["js"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it.jsonObject
            val id = o.str("id")
            if (id.isBlank()) null else StalkerGenre(id, o.str("title").ifBlank { o.str("alias") })
        }
    }

    suspend fun channels(portal: String, mac: String, token: String): List<StalkerChannel> {
        val out = mutableListOf<StalkerChannel>()
        var page = 1
        while (page <= 40) {
            val obj = get(portal, mac, "type=itv&action=get_ordered_list&genre=*&p=$page", token)
            val js = obj["js"]?.jsonObject ?: break
            val data = js["data"]?.jsonArray ?: break
            if (data.isEmpty()) break
            data.forEach { el ->
                val o = el.jsonObject
                out += StalkerChannel(
                    id = o.str("id"),
                    name = o.str("name"),
                    number = o.int("number").takeIf { it > 0 } ?: out.size + 1,
                    logo = o.str("logo"),
                    genreId = o.str("tv_genre_id"),
                    cmd = o.str("cmd"),
                    archive = o.int("tv_archive") == 1 || o.str("archive") == "1"
                )
            }
            val total = js.str("total_items").toIntOrNull() ?: out.size
            if (out.size >= total) break
            page++
        }
        return out
    }

    suspend fun vodGenres(portal: String, mac: String, token: String): List<StalkerGenre> {
        val obj = get(portal, mac, "type=vod&action=get_categories", token)
        val arr = obj["js"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it.jsonObject
            val id = o.str("id")
            if (id.isBlank()) null else StalkerGenre(id, o.str("title").ifBlank { o.str("category_alias") })
        }
    }

    suspend fun vods(portal: String, mac: String, token: String): List<StalkerChannel> {
        val out = mutableListOf<StalkerChannel>()
        var page = 1
        while (page <= 25) {
            val obj = get(portal, mac, "type=vod&action=get_ordered_list&category=*&p=$page", token)
            val js = obj["js"]?.jsonObject ?: break
            val data = js["data"]?.jsonArray ?: break
            if (data.isEmpty()) break
            data.forEach { el ->
                val o = el.jsonObject
                out += StalkerChannel(
                    id = o.str("id"),
                    name = o.str("name").ifBlank { o.str("title") },
                    number = out.size + 1,
                    logo = o.str("screenshot_uri").ifBlank { o.str("poster") },
                    genreId = o.str("category_id"),
                    cmd = o.str("cmd"),
                    archive = false
                )
            }
            val total = js.str("total_items").toIntOrNull() ?: out.size
            if (out.size >= total) break
            page++
        }
        return out
    }

    suspend fun createLink(portal: String, mac: String, token: String, cmd: String): String {
        val encoded = java.net.URLEncoder.encode(cmd, "UTF-8")
        val obj = get(portal, mac, "type=itv&action=create_link&cmd=$encoded", token)
        val js = obj["js"]?.jsonObject ?: obj
        val raw = js.str("cmd").ifBlank { js.str("js") }
        return raw.substringAfter(" ").trim().ifBlank { raw }
    }

    private suspend fun get(portal: String, mac: String, query: String, token: String = ""): JsonObject =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val base = portal.trim().removeSuffix("/")
            val url = if (base.contains("portal.php")) "$base&$query" else "$base/portal.php?$query"
            val req = Request.Builder().url(url)
                .header("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/Istanbul")
                .header("X-User-Agent", "Model: MAG250; Link: Ethernet")
                .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3")
                .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
                .get().build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                json.parseToJsonElement(body).jsonObject
            }
        }
}
