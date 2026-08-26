package com.ray.iptv.player

/** OSD’de seçilen altyazı dilini film/dizi arasında eşlemek için. */
object SubtitleLanguages {
    fun normalize(raw: String): String = raw.trim().lowercase()
        .replace('ı', 'i')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ö', 'o')
        .replace('ç', 'c')
        .replace('á', 'a')
        .replace('à', 'a')
        .replace('é', 'e')
        .replace('è', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ñ', 'n')

    fun canonical(raw: String): String {
        val n = normalize(raw).substringBefore('[').substringBefore('(').trim()
        if (n.isEmpty()) return ""
        ALIASES[n]?.let { return it }
        val first = n.split(Regex("[\\s_\\-./,+]+")).firstOrNull().orEmpty()
        ALIASES[first]?.let { return it }
        return if (first.length in 2..3) first else n
    }

    fun tokenOf(language: String, label: String): String {
        val fromLang = canonical(language)
        if (fromLang.isNotBlank()) return fromLang
        val fromLabel = canonical(label)
        if (fromLabel.isNotBlank()) return fromLabel
        return normalize(label)
    }

    fun matches(language: String, label: String, token: String): Boolean {
        val want = canonical(token)
        if (want.isEmpty()) return false
        val cLang = canonical(language)
        val cLabel = canonical(label)
        if (cLang == want || cLabel == want) return true
        val nToken = normalize(token)
        val nLabel = normalize(label)
        val nLang = normalize(language)
        if (nToken.length >= 3) {
            if (nLabel.contains(nToken) || (nLabel.length >= 3 && nToken.contains(nLabel))) return true
            if (nLang.contains(nToken) || (nLang.length >= 3 && nToken.contains(nLang))) return true
        }
        return false
    }

    private val ALIASES = mapOf(
        "tr" to "tr", "tur" to "tr", "turkish" to "tr", "turkce" to "tr", "turk" to "tr",
        "en" to "en", "eng" to "en", "english" to "en", "ingilizce" to "en",
        "de" to "de", "ger" to "de", "deu" to "de", "german" to "de", "almanca" to "de", "deutsch" to "de",
        "fr" to "fr", "fre" to "fr", "fra" to "fr", "french" to "fr", "fransizca" to "fr", "francais" to "fr",
        "es" to "es", "spa" to "es", "spanish" to "es", "ispanyolca" to "es", "espanol" to "es", "castilian" to "es",
        "ar" to "ar", "ara" to "ar", "arabic" to "ar", "arapca" to "ar",
        "ru" to "ru", "rus" to "ru", "russian" to "ru", "rusca" to "ru",
        "it" to "it", "ita" to "it", "italian" to "it", "italyanca" to "it", "italiano" to "it",
        "pt" to "pt", "por" to "pt", "portuguese" to "pt", "portekizce" to "pt", "pt-br" to "pt", "brazilian" to "pt",
        "nl" to "nl", "dut" to "nl", "nld" to "nl", "dutch" to "nl", "hollandaca" to "nl",
        "pl" to "pl", "pol" to "pl", "polish" to "pl", "lehce" to "pl",
        "el" to "el", "gre" to "el", "ell" to "el", "greek" to "el", "yunanca" to "el",
        "fa" to "fa", "per" to "fa", "fas" to "fa", "persian" to "fa", "farsi" to "fa", "farsca" to "fa",
        "he" to "he", "heb" to "he", "hebrew" to "he", "ibranice" to "he",
        "hi" to "hi", "hin" to "hi", "hindi" to "hi",
        "ja" to "ja", "jpn" to "ja", "japanese" to "ja", "japonca" to "ja",
        "ko" to "ko", "kor" to "ko", "korean" to "ko", "korece" to "ko",
        "zh" to "zh", "chi" to "zh", "zho" to "zh", "chinese" to "zh", "cince" to "zh", "cmn" to "zh",
        "ro" to "ro", "rum" to "ro", "ron" to "ro", "romanian" to "ro", "romence" to "ro",
        "hu" to "hu", "hun" to "hu", "hungarian" to "hu", "macarca" to "hu",
        "cs" to "cs", "cze" to "cs", "ces" to "cs", "czech" to "cs", "cekce" to "cs",
        "sv" to "sv", "swe" to "sv", "swedish" to "sv", "isvecce" to "sv",
        "no" to "no", "nor" to "no", "norwegian" to "no", "norvecce" to "no",
        "da" to "da", "dan" to "da", "danish" to "da", "danca" to "da",
        "fi" to "fi", "fin" to "fi", "finnish" to "fi", "fince" to "fi",
        "uk" to "uk", "ukr" to "uk", "ukrainian" to "uk", "ukraynaca" to "uk",
        "bg" to "bg", "bul" to "bg", "bulgarian" to "bg", "bulgarca" to "bg",
        "sr" to "sr", "srp" to "sr", "serbian" to "sr", "sirpca" to "sr",
        "hr" to "hr", "hrv" to "hr", "croatian" to "hr", "hirvatca" to "hr",
        "bs" to "bs", "bos" to "bs", "bosnian" to "bs", "bosnakca" to "bs",
        "sq" to "sq", "alb" to "sq", "albanian" to "sq", "arnavutca" to "sq",
        "ku" to "ku", "kur" to "ku", "kurdish" to "ku", "kurtce" to "ku",
        "az" to "az", "aze" to "az", "azerbaijani" to "az", "azerbaycan" to "az",
        "id" to "id", "ind" to "id", "indonesian" to "id", "endonezce" to "id",
        "ms" to "ms", "may" to "ms", "msa" to "ms", "malay" to "ms",
        "th" to "th", "tha" to "th", "thai" to "th", "tayca" to "th",
        "vi" to "vi", "vie" to "vi", "vietnamese" to "vi", "vietnamca" to "vi"
    )
}
