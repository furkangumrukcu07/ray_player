package com.ray.iptv.data.meta

import com.ray.iptv.data.local.EpisodeEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.data.repo.VodInfoEngine
import com.ray.iptv.data.repo.XtreamVodDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class CastPerson(
    val name: String,
    val character: String = "",
    val photo: String = "",
    val tmdbPersonId: Int? = null
)

data class ActorProfileResult(
    val name: String,
    val bio: String = "",
    val photo: String = "",
    val birthday: String = "",
    val placeOfBirth: String = "",
    val filmographyTitles: List<String> = emptyList()
)

data class VodMeta(
    val plot: String,
    val poster: String,
    val rating: String,
    val genre: String,
    val year: String,
    val backdrop: String = "",
    val runtime: String = "",
    val cast: String = "",
    val people: List<CastPerson> = emptyList(),
    val trailerUrl: String = "",
    val tmdbId: Int = 0
)

/**
 * Mina `MovieService.getMovieWithFallback` ile aynı sıra:
 * Xtream `get_vod_info` boşsa TMDB (`/search` + ayrı `/credits` + `/videos`) ve
 * OMDb (`plot=full`). Google Translate özet/türü uygulama diline çevirir.
 */
@Singleton
class TmdbOmdbService @Inject constructor(
    private val http: OkHttpClient,
    private val translate: GoogleTranslateClient
) {
    private var tmdbIndex = 0
    private var omdbIndex = 0
    private var omdbDisabled = false
    private val mem = LinkedHashMap<String, VodMeta>(48, 0.75f, true)
    private val seasonMem = LinkedHashMap<String, Map<Int, Triple<String, String, String>>>(24, 0.75f, true)
    private val personMem = LinkedHashMap<String, String>(64, 0.75f, true)

    @Volatile var lastExtras: VodMeta = VodMeta("", "", "", "", "")
        private set

    suspend fun enrich(
        item: VodEntity,
        settings: RaySettings,
        xtream: XtreamVodDetail = XtreamVodDetail()
    ): VodEntity = withContext(Dispatchers.IO) {
        val seeded = item.copy(
            plot = bestPlot(xtream.plot, item.plot),
            poster = xtream.poster.ifBlank { item.poster },
            rating = xtream.rating.ifBlank { item.rating },
            genre = xtream.genre.ifBlank { item.genre },
            year = xtream.year.ifBlank { item.year }
        )
        val xtreamPeople = namesToPeople(xtream.cast)

        if (settings.vodInfoEngine == VodInfoEngine.XTREAM_ONLY) {
            lastExtras = VodMeta(
                plot = seeded.plot,
                poster = seeded.poster,
                rating = seeded.rating,
                genre = seeded.genre,
                year = seeded.year,
                cast = xtream.cast,
                people = xtreamPeople,
                trailerUrl = xtream.trailer
            )
            return@withContext seeded
        }

        val series = seeded.kind.equals("SERIES", true)
        val cacheKey = cacheKey(seeded, settings)
        synchronized(mem) { mem[cacheKey] }?.let { cached ->
            return@withContext finish(seeded, cached.copy(trailerUrl = cached.trailerUrl.ifBlank { xtream.trailer }), settings.vodInfoEngine)
        }

        val cleaned = cleanNameAndYear(seeded.name, series, seeded.year)
        val tmdbLang = settings.lang.tmdbLanguage()
        val imdbHint = imdbIdIn(seeded.streamUrl)
        val tmdb = searchTmdb(cleaned.name, cleaned.year, series, tmdbLang)
        var omdb = when {
            !imdbHint.isNullOrBlank() -> fetchOmdbByImdb(imdbHint)
            tmdb.imdbId.isNotBlank() -> fetchOmdbByImdb(tmdb.imdbId)
            else -> VodMeta("", "", "", "", "")
        }
        if (!usable(omdb.plot) && !usable(omdb.rating)) {
            omdb = searchOmdbByTitle(cleaned.name, cleaned.year, series)
        }

        val targetLang = settings.lang.translateCode()
        val shouldTranslate = settings.translateMeta && targetLang != "en"

        var omdbPlot = omdb.plot
        var omdbGenre = omdb.genre
        if (shouldTranslate) {
            if (usable(omdbPlot) && looksMostlyEnglish(omdbPlot)) omdbPlot = translate.translate(omdbPlot, targetLang)
            if (usable(omdbGenre) && looksMostlyEnglish(omdbGenre)) omdbGenre = translate.translate(omdbGenre, targetLang)
        }

        var tmdbPlot = tmdb.plot
        if (shouldTranslate && usable(tmdbPlot) && looksMostlyEnglish(tmdbPlot)) {
            tmdbPlot = translate.translate(tmdbPlot, targetLang)
        }

        var plot = bestPlot(omdbPlot, tmdbPlot, seeded.plot)
        if (shouldTranslate && usable(plot) && looksMostlyEnglish(plot)) {
            plot = translate.translate(plot, targetLang)
        }
        var genre = pick(tmdb.genre, omdbGenre, seeded.genre)
        if (shouldTranslate && usable(genre) && looksMostlyEnglish(genre)) {
            genre = translate.translate(genre, targetLang)
        }

        var people = tmdb.people.ifEmpty { omdb.people.ifEmpty { xtreamPeople } }
        people = fillMissingPhotos(people)
        val meta = VodMeta(
            plot = plot,
            poster = pick(tmdb.poster, omdb.poster, seeded.poster),
            rating = pick(omdb.rating, tmdb.rating, seeded.rating),
            genre = genre,
            year = pick(tmdb.year, omdb.year, cleaned.year, seeded.year),
            backdrop = tmdb.backdrop,
            runtime = tmdb.runtime.ifBlank { omdb.runtime },
            cast = tmdb.cast.ifBlank { omdb.cast }.ifBlank { xtream.cast }.ifBlank { people.joinToString(", ") { it.name } },
            people = people,
            trailerUrl = tmdb.trailerUrl.ifBlank { xtream.trailer },
            tmdbId = tmdb.tmdbId
        )
        synchronized(mem) {
            if (mem.size > 60) mem.remove(mem.keys.first())
            mem[cacheKey] = meta
        }
        finish(seeded, meta, settings.vodInfoEngine)
    }

    fun peek(item: VodEntity, settings: RaySettings): VodMeta? {
        if (settings.vodInfoEngine == VodInfoEngine.XTREAM_ONLY) return null
        synchronized(mem) { return mem[cacheKey(item, settings)] }
    }

    fun seed(item: VodEntity, settings: RaySettings): VodMeta {
        peek(item, settings)?.let { return it }
        return VodMeta(
            plot = item.plot,
            poster = item.poster,
            rating = item.rating,
            genre = item.genre,
            year = item.year,
            backdrop = item.poster
        )
    }

    private fun cacheKey(item: VodEntity, settings: RaySettings): String {
        val series = item.kind.equals("SERIES", true)
        return "${item.name}|${item.year}|$series|${settings.lang}|${settings.vodInfoEngine}|${settings.translateMeta}|${item.plot.length}"
    }

    suspend fun fillEpisodeGaps(
        series: VodEntity,
        episodes: List<EpisodeEntity>,
        settings: RaySettings
    ): List<EpisodeEntity> = withContext(Dispatchers.IO) {
        if (episodes.isEmpty() || settings.vodInfoEngine == VodInfoEngine.XTREAM_ONLY) return@withContext episodes
        val dropLocal = settings.vodInfoEngine == VodInfoEngine.TMDB_OMDB_ONLY
        val needsFill = episodes.any { dropLocal || it.plot.isBlank() || it.still.isBlank() }
        if (!needsFill) return@withContext episodes

        val tmdbLang = settings.lang.tmdbLanguage()
        val tvId = lastExtras.tmdbId.takeIf { it > 0 }
            ?: searchTmdb(cleanNameAndYear(series.name, true, series.year).name, series.year, true, tmdbLang).tmdbId
        if (tvId <= 0) return@withContext episodes

        val targetLang = settings.lang.translateCode()
        val shouldTranslate = settings.translateMeta && targetLang != "en"
        episodes.map { ep ->
            val season = ep.season.coerceAtLeast(1)
            val info = seasonEpisodes(tvId, season, tmdbLang)[ep.episode]
                ?: seasonEpisodes(tvId, season, "en-US")[ep.episode]
                ?: return@map ep
            var overview = info.second
            if (shouldTranslate && usable(overview) && looksMostlyEnglish(overview)) {
                overview = translate.translate(overview, targetLang)
            } else if (shouldTranslate && usable(overview) && tmdbLang.startsWith("tr") && targetLang != "tr") {
                overview = translate.translate(overview, targetLang)
            }
            ep.copy(
                plot = if (dropLocal || ep.plot.isBlank()) overview.ifBlank { ep.plot } else bestPlot(ep.plot, overview),
                name = if (ep.name.isBlank()) info.first.ifBlank { ep.name } else ep.name,
                still = if (ep.still.isBlank()) info.third.ifBlank { ep.still } else ep.still
            )
        }
    }

    private fun finish(item: VodEntity, meta: VodMeta, engine: VodInfoEngine): VodEntity {
        val merged = item.merge(meta, engine)
        lastExtras = meta.copy(
            plot = merged.plot,
            poster = merged.poster,
            rating = merged.rating,
            genre = merged.genre,
            year = merged.year
        )
        return merged
    }

    private fun VodEntity.merge(m: VodMeta, engine: VodInfoEngine): VodEntity {
        val dropLocal = engine == VodInfoEngine.TMDB_OMDB_ONLY
        return copy(
            plot = if (dropLocal) m.plot.ifBlank { plot } else bestPlot(m.plot, plot),
            poster = if (dropLocal || poster.isBlank()) m.poster.ifBlank { poster } else pick(m.poster, poster),
            rating = if (dropLocal || rating.isBlank()) m.rating.ifBlank { rating } else pick(m.rating, rating),
            genre = if (dropLocal || genre.isBlank()) m.genre.ifBlank { genre } else pick(m.genre, genre),
            year = if (dropLocal || year.isBlank()) m.year.ifBlank { year } else pick(m.year, year)
        )
    }

    private data class TmdbHit(
        val plot: String = "",
        val poster: String = "",
        val rating: String = "",
        val genre: String = "",
        val year: String = "",
        val backdrop: String = "",
        val runtime: String = "",
        val cast: String = "",
        val trailerUrl: String = "",
        val tmdbId: Int = 0,
        val imdbId: String = "",
        val people: List<CastPerson> = emptyList()
    )

    private fun searchTmdb(name: String, year: String, series: Boolean, language: String): TmdbHit {
        if (name.isBlank()) return TmdbHit()
        val folded = foldTurkish(name)
        val attempts = buildList {
            add(name to year)
            if (year.length == 4) add(name to "")
            if (folded != name) {
                add(folded to year)
                if (year.length == 4) add(folded to "")
            }
        }
        for ((q, y) in attempts.distinct()) {
            val hit = searchTmdbOnce(q, y, series, language)
            if (hit.tmdbId > 0) return hit
        }
        return TmdbHit()
    }

    private fun searchTmdbOnce(name: String, year: String, series: Boolean, language: String): TmdbHit {
        val want = if (series) "tv" else "movie"
        val dedicated = if (series) "/search/tv" else "/search/movie"
        fun params(includeYear: Boolean, yearKey: String) = buildMap {
            put("query", name)
            put("language", language)
            if (includeYear && year.length == 4) put(yearKey, year)
        }
        var search = tmdbGet(
            dedicated,
            params(year.length == 4, if (series) "first_air_date_year" else "year")
        )
        var results = search?.optJSONArray("results")
        if (results == null || results.length() == 0) {
            search = tmdbGet("/search/multi", params(year.length == 4, "year"))
            results = search?.optJSONArray("results")
        }
        if (results == null || results.length() == 0) return TmdbHit()

        var match: JSONObject? = null
        for (i in 0 until results.length()) {
            val row = results.optJSONObject(i) ?: continue
            val type = jsonStr(row, "media_type").ifBlank { want }
            if (type == want) {
                match = row
                break
            }
        }
        if (match == null) match = results.optJSONObject(0)
        val id = match?.optInt("id") ?: 0
        if (id <= 0) return TmdbHit()
        val mediaType = jsonStr(match, "media_type").ifBlank { want }

        var detail = tmdbGet(
            "/$mediaType/$id",
            mapOf(
                "language" to language,
                "append_to_response" to "credits,videos,external_ids"
            )
        ) ?: match ?: return TmdbHit()

        var plot = na(jsonStr(detail, "overview"))
        if (!usable(plot) && !language.startsWith("en")) {
            tmdbGet("/$mediaType/$id", mapOf("language" to "en-US"))?.let {
                plot = na(jsonStr(it, "overview"))
                if (usable(jsonStr(it, "poster_path")) && !usable(jsonStr(detail, "poster_path"))) {
                    detail = it
                }
            }
        }

        var people = parseCast(detail.optJSONObject("credits"))
        if (people.none { it.photo.isNotBlank() }) {
            val credits = tmdbGet("/$mediaType/$id/credits", emptyMap())
            val separate = parseCast(credits)
            if (separate.isNotEmpty()) people = separate
        }

        val posterPath = jsonStr(detail, "poster_path")
        val backdropPath = jsonStr(detail, "backdrop_path")
        val genres = detail.optJSONArray("genres")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { g -> jsonStr(g, "name") } }
                .filter { it.isNotBlank() }
        }.orEmpty().joinToString(", ")
        val vote = detail.optDouble("vote_average", 0.0)
        val date = jsonStr(detail, "release_date").ifBlank { jsonStr(detail, "first_air_date") }
        val runtimeMin = detail.optInt("runtime").takeIf { it > 0 }
            ?: detail.optJSONArray("episode_run_time")?.optInt(0)?.takeIf { it > 0 }
        val trailerKey = trailerKeyFrom(detail.optJSONObject("videos")?.optJSONArray("results"))
            .ifBlank {
                trailerKeyFrom(tmdbGet("/$mediaType/$id/videos", mapOf("language" to language))?.optJSONArray("results"))
            }
            .ifBlank {
                trailerKeyFrom(tmdbGet("/$mediaType/$id/videos", emptyMap())?.optJSONArray("results"))
            }
        val imdb = jsonStr(detail, "imdb_id").ifBlank {
            jsonStr(detail.optJSONObject("external_ids"), "imdb_id").ifBlank {
                jsonStr(tmdbGet("/$mediaType/$id/external_ids", emptyMap()), "imdb_id")
            }
        }.let { if (it.startsWith("tt")) it else "" }
        return TmdbHit(
            plot = plot,
            poster = tmdbImage(ApiKeys.tmdbPoster, posterPath),
            rating = if (vote > 0) "%.1f".format(vote) else "",
            genre = genres,
            year = date.take(4),
            backdrop = tmdbImage(ApiKeys.tmdbBackdrop, backdropPath),
            runtime = runtimeMin?.let { "$it min" }.orEmpty(),
            cast = people.joinToString(", ") { it.name },
            trailerUrl = if (trailerKey.isBlank()) "" else "https://www.youtube.com/watch?v=$trailerKey",
            tmdbId = id,
            imdbId = imdb,
            people = people
        )
    }

    private fun parseCast(credits: JSONObject?): List<CastPerson> {
        val arr = credits?.optJSONArray("cast") ?: return emptyList()
        return (0 until minOf(arr.length(), 12)).mapNotNull { i ->
            val row = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = jsonStr(row, "name")
            if (name.isBlank()) return@mapNotNull null
            CastPerson(
                name = name,
                character = jsonStr(row, "character"),
                photo = tmdbImage(ApiKeys.tmdbProfile, jsonStr(row, "profile_path")),
                tmdbPersonId = row.optInt("id").takeIf { it > 0 }
            )
        }
    }

    suspend fun fetchActorProfile(name: String, personId: Int? = null): ActorProfileResult = withContext(Dispatchers.IO) {
        val targetId = personId ?: run {
            val search = tmdbGet("/search/person", mapOf("query" to name))
            search?.optJSONArray("results")?.optJSONObject(0)?.optInt("id")
        } ?: return@withContext ActorProfileResult(name = name)

        val detail = tmdbGet("/person/$targetId", mapOf("append_to_response" to "combined_credits"))
            ?: return@withContext ActorProfileResult(name = name)

        val rawBio = jsonStr(detail, "biography")
        val bio = if (rawBio.isNotBlank()) translate.translate(rawBio, "tr") else ""
        val photoPath = jsonStr(detail, "profile_path")
        val birthday = jsonStr(detail, "birthday")
        val placeOfBirth = jsonStr(detail, "place_of_birth")

        val creditsObj = detail.optJSONObject("combined_credits")
        val castArr = creditsObj?.optJSONArray("cast")
        val titles = ArrayList<String>()
        if (castArr != null) {
            for (i in 0 until minOf(castArr.length(), 30)) {
                val item = castArr.optJSONObject(i) ?: continue
                val title = jsonStr(item, "title").ifBlank { jsonStr(item, "name") }
                if (title.isNotBlank() && !titles.contains(title)) titles.add(title)
            }
        }

        ActorProfileResult(
            name = jsonStr(detail, "name").ifBlank { name },
            bio = bio.ifBlank { rawBio },
            photo = tmdbImage(ApiKeys.tmdbProfile, photoPath),
            birthday = birthday,
            placeOfBirth = placeOfBirth,
            filmographyTitles = titles
        )
    }

    private fun trailerKeyFrom(arr: JSONArray?): String {
        if (arr == null) return ""
        val rows = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        val youtube = rows.filter { it.optString("site").equals("YouTube", true) }
        return youtube.firstOrNull { it.optString("type") == "Trailer" }?.optString("key")
            ?: youtube.firstOrNull { it.optString("type") == "Teaser" }?.optString("key")
            ?: youtube.firstOrNull()?.optString("key")
            ?: ""
    }

    private fun fillMissingPhotos(people: List<CastPerson>): List<CastPerson> {
        if (people.isEmpty()) return people
        if (people.count { it.photo.isNotBlank() } >= 3) return people
        var filled = 0
        return people.map { person ->
            if (person.photo.isNotBlank() || filled >= 8) return@map person
            val url = personPhoto(person.name)
            if (url.isNotBlank()) {
                filled++
                person.copy(photo = url)
            } else person
        }
    }

    private fun personPhoto(name: String): String {
        val key = name.trim().lowercase()
        if (key.isBlank()) return ""
        synchronized(personMem) { personMem[key] }?.let { return it }
        val obj = tmdbGet("/search/person", mapOf("query" to name))
        val path = obj?.optJSONArray("results")?.optJSONObject(0)?.let { jsonStr(it, "profile_path") }.orEmpty()
        val url = tmdbImage(ApiKeys.tmdbProfile, path)
        synchronized(personMem) {
            if (personMem.size > 80) personMem.remove(personMem.keys.first())
            personMem[key] = url
        }
        return url
    }

    private fun namesToPeople(raw: String): List<CastPerson> =
        raw.split(',', '|', ';').map { it.trim() }.filter { it.isNotBlank() && !it.equals("N/A", true) }
            .take(10).map { CastPerson(it) }

    private fun seasonEpisodes(tvId: Int, season: Int, language: String): Map<Int, Triple<String, String, String>> {
        val key = "$tvId|$season|$language"
        synchronized(seasonMem) { seasonMem[key] }?.let { return it }
        val obj = tmdbGet("/tv/$tvId/season/$season", mapOf("language" to language))
        val out = linkedMapOf<Int, Triple<String, String, String>>()
        val arr = obj?.optJSONArray("episodes")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val num = row.optInt("episode_number")
                if (num <= 0) continue
                out[num] = Triple(
                    jsonStr(row, "name"),
                    na(jsonStr(row, "overview")),
                    tmdbImage(ApiKeys.tmdbPoster, jsonStr(row, "still_path"))
                )
            }
        }
        synchronized(seasonMem) {
            if (seasonMem.size > 32) seasonMem.remove(seasonMem.keys.first())
            seasonMem[key] = out
        }
        return out
    }

    private fun fetchOmdbByImdb(imdbId: String): VodMeta {
        if (omdbDisabled || imdbId.isBlank()) return VodMeta("", "", "", "", "")
        return parseOmdb(omdbGet(mapOf("i" to imdbId, "plot" to "full")))
    }

    private fun searchOmdbByTitle(name: String, year: String, series: Boolean): VodMeta {
        if (omdbDisabled || name.isBlank()) return VodMeta("", "", "", "", "")
        val type = if (series) "series" else "movie"
        val queries = buildList {
            add(name)
            val folded = foldTurkish(name)
            if (folded != name) add(folded)
        }
        for (q in queries) {
            val direct = parseOmdb(
                omdbGet(
                    buildMap {
                        put("t", q)
                        put("plot", "full")
                        put("type", type)
                        if (year.length == 4) put("y", year)
                    }
                )
            )
            if (usable(direct.plot) || usable(direct.rating) || usable(direct.poster)) return direct
        }
        val search = omdbGet(
            buildMap {
                put("s", foldTurkish(name).ifBlank { name })
                put("type", type)
                if (year.length == 4) put("y", year)
            }
        ) ?: return VodMeta("", "", "", "", "")
        if (search.optString("Response") != "True") return VodMeta("", "", "", "", "")
        val first = search.optJSONArray("Search")?.optJSONObject(0) ?: return VodMeta("", "", "", "", "")
        val id = first.optString("imdbID")
        return if (id.startsWith("tt")) fetchOmdbByImdb(id) else VodMeta("", "", "", "", "")
    }

    private fun parseOmdb(obj: JSONObject?): VodMeta {
        if (obj == null || obj.optString("Response") == "False") return VodMeta("", "", "", "", "")
        val actors = na(jsonStr(obj, "Actors"))
        val people = namesToPeople(actors)
        return VodMeta(
            plot = na(jsonStr(obj, "Plot")),
            poster = jsonStr(obj, "Poster").takeIf { it.startsWith("http") && !it.equals("N/A", true) }.orEmpty(),
            rating = na(jsonStr(obj, "imdbRating")),
            genre = na(jsonStr(obj, "Genre")),
            year = jsonStr(obj, "Year").take(4),
            runtime = na(jsonStr(obj, "Runtime")),
            cast = actors,
            people = people
        )
    }

    private fun tmdbGet(path: String, params: Map<String, String>): JSONObject? {
        val keys = ApiKeys.tmdbKeys
        var start = tmdbIndex
        repeat(keys.size) {
            val key = keys[start]
            val url = (ApiKeys.tmdbBase + path).toHttpUrl().newBuilder()
                .addQueryParameter("api_key", key)
                .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
                .build()
                .toString()
            val (code, obj) = requestJson(url)
            if (!tmdbKeyInvalid(code, obj)) {
                tmdbIndex = start
                return obj
            }
            start = (start + 1) % keys.size
        }
        tmdbIndex = 0
        val bearerUrl = (ApiKeys.tmdbBase + path).toHttpUrl().newBuilder()
            .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()
            .toString()
        val (code, obj) = requestJson(bearerUrl, "Bearer ${ApiKeys.tmdbReadAccessToken}")
        return if (tmdbKeyInvalid(code, obj)) null else obj
    }

    private fun tmdbKeyInvalid(code: Int, obj: JSONObject?): Boolean {
        val status = obj?.optInt("status_code") ?: 0
        return code == 401 || status == 7 || status == 401 ||
            (obj?.optBoolean("success", true) == false && status in listOf(7, 401, 3))
    }

    private fun omdbGet(params: Map<String, String>): JSONObject? {
        if (omdbDisabled) return null
        val keys = ApiKeys.omdbKeys
        var start = omdbIndex
        repeat(keys.size) {
            val key = keys[start]
            val url = ApiKeys.omdbBase.toHttpUrl().newBuilder()
                .addQueryParameter("apikey", key)
                .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
                .build()
                .toString()
            val (code, obj) = requestJson(url)
            val err = obj?.optString("Error").orEmpty()
            val exhausted = code == 401 ||
                err.contains("limit", true) ||
                err.contains("invalid api", true) ||
                err.contains("api key", true) ||
                err.contains("unauthorized", true)
            if (!exhausted) {
                omdbIndex = start
                return obj
            }
            start = (start + 1) % keys.size
        }
        omdbDisabled = true
        return null
    }

    private fun requestJson(url: String, bearer: String? = null): Pair<Int, JSONObject?> = runCatching {
        val req = Request.Builder().url(url).get()
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
        if (!bearer.isNullOrBlank()) req.header("Authorization", bearer)
        http.newCall(req.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val obj = if (raw.startsWith("{")) JSONObject(raw) else null
            resp.code to obj
        }
    }.getOrDefault(0 to null)

    private data class Cleaned(val name: String, val year: String)

    private fun cleanNameAndYear(raw: String, series: Boolean, itemYear: String): Cleaned {
        var s = raw
        if (series) {
            s = s.replace(Regex("""[sS]\d{1,2}\s?[eE]\d{1,2}"""), "")
                .replace(Regex("""[sS]\d{1,2}"""), "")
                .replace(Regex("""[eE]\d{1,2}"""), "")
                .replace(Regex("""\d{1,2}\.\s?(Sezon|Bölüm)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""(Sezon|Bölüm)\s?\d{1,2}""", RegexOption.IGNORE_CASE), "")
        }
        val yearFromName = Regex("""\b(19|20)\d{2}\b""").find(s)?.value.orEmpty()
        if (yearFromName.isNotBlank()) s = s.replaceFirst(yearFromName, "")
        s = s.replace(Regex("""\b(4K|UHD|FHD|HD|H\.?265|HEVC|HDR|DV|Atmos)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(.*?\)"""), " ")
            .replace(Regex("""\[.*?\]"""), " ")
            .replace(Regex("""[-._]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val year = itemYear.filter { it.isDigit() }.take(4).ifBlank { yearFromName }
        return Cleaned(s, year)
    }

    private fun foldTurkish(s: String): String {
        if (s.none { it in "ğüşıöçĞÜŞİÖÇâîûÂÎÛ" }) return s
        return s.replace("ğ", "g").replace("ü", "u").replace("ş", "s").replace("ı", "i")
            .replace("ö", "o").replace("ç", "c").replace("Ğ", "G").replace("Ü", "U")
            .replace("Ş", "S").replace("İ", "I").replace("Ö", "O").replace("Ç", "C")
            .replace("â", "a").replace("î", "i").replace("û", "u")
            .replace("Â", "A").replace("Î", "I").replace("Û", "U")
            .replace(Regex("""[^a-zA-Z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun tmdbImage(base: String, path: String): String {
        var p = path.trim()
        if (p.isEmpty() || p.equals("null", true) || p.equals("undefined", true) || p.equals("N/A", true)) return ""
        if (p.startsWith("http")) return p
        if (!p.startsWith("/")) p = "/$p"
        return base + p
    }

    private fun jsonStr(obj: JSONObject?, key: String): String {
        if (obj == null || !obj.has(key) || obj.isNull(key)) return ""
        return obj.optString(key).trim().let { if (it.equals("null", true)) "" else it }
    }

    private fun imdbIdIn(url: String): String? =
        Regex("""tt\d{6,}""").find(url)?.value

    private fun usable(v: String?): Boolean {
        val t = v?.trim().orEmpty()
        return t.isNotEmpty() && !t.equals("N/A", true)
    }

    private fun na(v: String): String = if (usable(v)) v.trim() else ""

    private fun pick(vararg values: String): String = values.firstOrNull { usable(it) }.orEmpty()

    private fun bestPlot(vararg values: String): String =
        values.map { it.trim() }.filter { usable(it) }.maxByOrNull { it.length }.orEmpty()

    private fun looksMostlyEnglish(s: String): Boolean {
        val letters = s.count { it.isLetter() }
        val ascii = s.count { it.isLetter() && it.code < 128 }
        return letters > 8 && ascii * 10 >= letters * 8
    }

    private fun AppLang.tmdbLanguage(): String = tmdb
    private fun AppLang.translateCode(): String = code
}
