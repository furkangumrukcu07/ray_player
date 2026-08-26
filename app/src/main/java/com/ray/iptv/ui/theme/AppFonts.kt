package com.ray.iptv.ui.theme

import androidx.compose.ui.text.font.FontFamily

data class AppFontOption(
    val key: String,
    val label: String,
    val preview: String
)

val appFontOptions = listOf(
    AppFontOption("sony", "Sony", "Sony (TV tarzı)"),
    AppFontOption("roboto", "Roboto", "Roboto (açık lisans)"),
    AppFontOption("noto", "Noto Sans", "Noto Sans (açık lisans)"),
    AppFontOption("mono", "Monospace", "Monospace (sistem)"),
    AppFontOption("roboto_flex", "Roboto Flex", "Android / Google TV yerel tarzı"),
    AppFontOption("poppins", "Poppins", "Geometrik, yuvarlak — modern & premium"),
    AppFontOption("rubik", "Rubik", "Yumuşak köşeli — TV ekranında rahat"),
    AppFontOption("montserrat", "Montserrat", "Güçlü başlık fontu — sinematik")
)

fun appFontFamily(key: String): FontFamily = when (key) {
    "mono" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

fun appFontOption(key: String): AppFontOption =
    appFontOptions.firstOrNull { it.key == key } ?: appFontOptions.first()
