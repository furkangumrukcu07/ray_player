package com.ray.iptv.data.meta

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class OpenSubtitleResult(
    val id: String,
    val fileId: Int,
    val title: String,
    val language: String,
    val langCode: String,
    val downloadCount: Int,
    val rating: Double,
    val format: String = "srt"
)

@Singleton
class OpenSubtitlesService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient
) {
    private val apiKey = ApiKeys.openSubtitlesApiKey

    suspend fun searchSubtitles(
        query: String,
        tmdbId: Int = 0,
        imdbId: String = "",
        lang: String = "tr"
    ): List<OpenSubtitleResult> = withContext(Dispatchers.IO) {
        val list = ArrayList<OpenSubtitleResult>()
        try {
            val urlBuilder = java.lang.StringBuilder("https://api.opensubtitles.com/api/v1/subtitles?")
            val params = ArrayList<String>()
            if (tmdbId > 0) params.add("tmdb_id=$tmdbId")
            if (imdbId.isNotBlank()) params.add("imdb_id=${imdbId.removePrefix("tt")}")
            if (query.isNotBlank()) params.add("query=${java.net.URLEncoder.encode(query, "UTF-8")}")
            params.add("languages=$lang,en")
            urlBuilder.append(params.joinToString("&"))

            val req = Request.Builder()
                .url(urlBuilder.toString())
                .header("Api-Key", apiKey)
                .header("User-Agent", "RayIPTV v1.0")
                .header("Accept", "application/json")
                .get()
                .build()

            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()

                for (i in 0 until minOf(data.length(), 25)) {
                    val item = data.optJSONObject(i) ?: continue
                    val attr = item.optJSONObject("attributes") ?: continue
                    val files = attr.optJSONArray("files")
                    val fileObj = files?.optJSONObject(0) ?: continue
                    val fileId = fileObj.optInt("file_id")
                    val fileName = fileObj.optString("file_name").ifBlank { attr.optString("release") }
                    val langName = attr.optString("language").ifBlank { lang }

                    list.add(
                        OpenSubtitleResult(
                            id = item.optString("id"),
                            fileId = fileId,
                            title = fileName,
                            language = langName,
                            langCode = attr.optString("language"),
                            downloadCount = attr.optInt("download_count"),
                            rating = attr.optDouble("ratings", 0.0)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun downloadSubtitle(fileId: Int): File? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "sub_$fileId.srt")
            if (cacheFile.exists() && cacheFile.length() > 100) return@withContext cacheFile

            val jsonBody = JSONObject().apply { put("file_id", fileId) }
            val req = Request.Builder()
                .url("https://api.opensubtitles.com/api/v1/download")
                .header("Api-Key", apiKey)
                .header("User-Agent", "RayIPTV v1.0")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            var downloadUrl: String? = null
            http.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string().orEmpty()
                    downloadUrl = JSONObject(bodyStr).optString("link")
                }
            }

            if (downloadUrl.isNullOrBlank()) return@withContext null

            val dlReq = Request.Builder().url(downloadUrl!!).get().build()
            http.newCall(dlReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.bytes()?.let { bytes ->
                        cacheFile.writeBytes(bytes)
                        return@withContext cacheFile
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
