package com.ray.iptv.data.meta

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mina `translator` paketi ile aynı ücretsiz Google Translate uç noktası
 * (`translate.googleapis.com/translate_a/single`, client=gtx).
 */
@Singleton
class GoogleTranslateClient @Inject constructor(
    private val http: OkHttpClient
) {
    private val mem = LinkedHashMap<String, String>(64, 0.75f, true)

    fun translate(text: String, to: String): String {
        val src = text.trim()
        if (src.isEmpty() || to.equals("en", true) && looksEnglish(src)) return src
        val key = "$to:${src.hashCode()}"
        synchronized(mem) { mem[key] }?.let { return it }
        val url = "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", "auto")
            .addQueryParameter("tl", to)
            .addQueryParameter("dt", "t")
            .addQueryParameter("q", src)
            .build()
        val body = runCatching {
            http.newCall(Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build())
                .execute().use { it.body?.string().orEmpty() }
        }.getOrDefault("")
        val out = parse(body).ifBlank { src }
        synchronized(mem) {
            if (mem.size > 80) mem.remove(mem.keys.first())
            mem[key] = out
        }
        return out
    }

    private fun parse(raw: String): String {
        if (raw.isBlank() || !raw.startsWith("[")) return ""
        return runCatching {
            val root = JSONArray(raw)
            val sentences = root.optJSONArray(0) ?: return@runCatching ""
            buildString {
                for (i in 0 until sentences.length()) {
                    val row = sentences.optJSONArray(i) ?: continue
                    append(row.optString(0))
                }
            }.trim()
        }.getOrDefault("")
    }

    private fun looksEnglish(s: String): Boolean {
        val letters = s.count { it.isLetter() }
        val ascii = s.count { it.isLetter() && it.code < 128 }
        return letters > 8 && ascii * 10 >= letters * 8
    }
}
