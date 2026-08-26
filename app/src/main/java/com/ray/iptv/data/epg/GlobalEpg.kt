package com.ray.iptv.data.epg

/**
 * Mina `IptvOrgEpg` — EPGShare01 ülke XMLTV adresleri.
 * Birincil XMLTV değil; [GlobalEpgService] yedek rehberinin kaynak listesi.
 */
object GlobalEpg {
    const val HOST = "https://epgshare01.online/epgshare01/epg_ripper_"

    val countries = listOf(
        "TR" to url("TR"),
        "DE" to url("DE"),
        "UK" to url("UK"),
        "US" to url("US"),
        "FR" to url("FR"),
        "IT" to url("IT"),
        "ES" to url("ES"),
        "NL" to url("NL")
    )

    private val mapped = mapOf(
        "TR" to listOf(url("TR")),
        "DE" to listOf(url("DE")),
        "UK" to listOf(url("UK")),
        "GB" to listOf(url("UK")),
        "EN" to listOf(url("US")),
        "US" to listOf(url("US")),
        "FR" to listOf(url("FR")),
        "IT" to listOf(url("IT")),
        "ES" to listOf(url("ES")),
        "NL" to listOf(url("NL"))
    )

    fun url(code: String): String = "${HOST}${code.uppercase()}1.xml.gz"

    fun getCountryGuideUrls(countryCode: String): List<String> {
        val code = countryCode.trim().uppercase()
        if (code.isEmpty()) return defaultGuideCandidates()
        return mapped[code] ?: listOf(url(code))
    }

    /** Boş XMLTV ayarında birincil M3U rehberi — tespit edilen ülke, yoksa TR. */
    fun defaultGuideCandidates(languageCode: String? = null): List<String> {
        val code = languageCode?.trim().orEmpty()
        return if (code.isNotEmpty()) getCountryGuideUrls(code) else getCountryGuideUrls("TR")
    }
}
