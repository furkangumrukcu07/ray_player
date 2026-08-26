package com.ray.iptv.data.meta

/** Mina `ApiConstants` — TMDB / OMDb yedek anahtar havuzu. */
object ApiKeys {
    val omdbKeys = listOf("f1969f57", "cfd39be0", "79dba62e", "989dbe84", "80ffbf02")
    const val omdbBase = "https://www.omdbapi.com/"

    val tmdbKeys = listOf(
        "ed5d296fe050f5f47145371758eea3ac",
        "e34828b0a831ab3c07b1cc6cb6ef6ab7"
    )
    const val tmdbBase = "https://api.themoviedb.org/3"
    const val tmdbPoster = "https://image.tmdb.org/t/p/w500"
    const val tmdbProfile = "https://image.tmdb.org/t/p/w185"
    const val tmdbBackdrop = "https://image.tmdb.org/t/p/w1280"
    const val tmdbImage = tmdbPoster
    /** Mina `tmdbReadAccessToken` — api_key başarısız olursa Bearer yedek. */
    const val tmdbReadAccessToken =
        "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlZDVkMjk2ZmUwNTBmNWY0NzE0NTM3MTc1OGVlYTNhYyIsIm5iZiI6MTc3NjAzNzQwNy43NzcsInN1YiI6IjY5ZGMyZTFmOGExMmZmZTlmOTM1OGEwOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.dLsmRVeirSsLMCmPtRbrFZ9Z1GtNPPbDJqUKmIKjGx0"
    const val openSubtitlesApiKey = "eW3RjU9mK91G6bOqfQJ9uM742kX8R4P1"
}
