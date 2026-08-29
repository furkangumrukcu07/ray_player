package com.ray.iptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.ray.iptv.R
import com.ray.iptv.data.repo.GlassStyle

val MacBlue = Color(0xFF0A84FF)
val MacCyan = Color(0xFF64D2FF)
val MacLabel = Color(0xFFF5F5F7)
val MacSecondary = Color(0xFFA1A1A6)
val MacFill = Color(0x14FFFFFF)
val MacStroke = Color(0x2EFFFFFF)
val MacVoid = Color(0xFF0B0C10)

data class RayGlass(
    val panel: Color,
    val panelStrong: Color,
    val stroke: Color,
    val strokeFocus: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val danger: Color,
    val wallpaperDark: Color,
    val blobA: Color,
    val blobB: Color,
    val isLight: Boolean,
    val reduceEffects: Boolean = false,
    val flatWallpaper: Boolean = false,
    val wallpaperRes: Int = 0,
    val frostDark: Boolean = false,
    val kind: GlassStyle = GlassStyle.DARK
)

private fun readableFrost(
    accent: Color,
    wallpaperRes: Int,
    blobA: Color,
    blobB: Color,
    wallpaperDark: Color,
    strokeFocus: Color = accent
) = RayGlass(
    panel = Color(0x6610131B),
    panelStrong = Color(0x8C141822),
    stroke = Color(0x2EFFFFFF),
    strokeFocus = strokeFocus,
    text = Color.White,
    muted = Color(0xFFC7CBD6),
    accent = accent,
    danger = Color(0xFFFF453A),
    wallpaperDark = wallpaperDark,
    blobA = blobA,
    blobB = blobB,
    isLight = false,
    wallpaperRes = wallpaperRes,
    frostDark = true
)

val DarkGlass = readableFrost(
    accent = MacCyan,
    wallpaperRes = R.drawable.wallpaper_dark,
    blobA = Color(0x2E4A607A),
    blobB = Color(0x1F6B528A),
    wallpaperDark = Color(0xFF090A0E),
    strokeFocus = Color(0xFFFFFFFF)
)

val DarkGlassPopup = readableFrost(
    accent = MacCyan,
    wallpaperRes = 0,
    blobA = Color.Transparent,
    blobB = Color.Transparent,
    wallpaperDark = Color(0xFF090A0E),
    strokeFocus = MacCyan
)

val LightGlass = RayGlass(
    panel = Color(0xB3FFFFFF),
    panelStrong = Color(0xD9FFFFFF),
    stroke = Color(0x55FFFFFF),
    strokeFocus = Color(0xFF0A84FF),
    text = Color(0xFF1D1D1F),
    muted = Color(0xFF6E6E73),
    accent = MacBlue,
    danger = Color(0xFFD70015),
    wallpaperDark = Color(0xFFE8EEF6),
    blobA = Color(0x667EC8FF),
    blobB = Color(0x55E8C4FF),
    isLight = true
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }

private fun rayTypography(family: FontFamily) = Typography(
    displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.2.sp)
)

private fun scheme(glass: RayGlass): ColorScheme {
    val base = if (glass.isLight) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = glass.accent,
        onPrimary = if (glass.isLight) Color.White else Color.Black,
        background = glass.wallpaperDark,
        onBackground = glass.text,
        surface = glass.panel,
        onSurface = glass.text,
        border = glass.stroke
    )
}

fun GlassStyle.toPalette(): RayGlass = when (this) {
    GlassStyle.TV_LITE -> RayGlass(
        panel = Color(0xE6121212),
        panelStrong = Color(0xF01A1A1A),
        stroke = Color(0xFF3F3F3F),
        strokeFocus = Color(0xFFE3201C),
        text = Color(0xFFF5F5F5),
        muted = Color(0xFF9CA3AF),
        accent = Color(0xFFE3201C),
        danger = Color(0xFFE3201C),
        wallpaperDark = Color(0xFF000000),
        blobA = Color.Transparent,
        blobB = Color.Transparent,
        isLight = false,
        flatWallpaper = true,
        wallpaperRes = R.drawable.wallpaper_tv_lite
    )
    GlassStyle.MACOS_TV -> RayGlass(
        panel = Color(0x6610131B),
        panelStrong = Color(0x8C141822),
        stroke = Color(0x2EFFFFFF),
        strokeFocus = Color(0xFF007AFF),
        text = Color.White,
        muted = Color(0xFFA1A1A6),
        accent = Color(0xFF007AFF),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF090A0E),
        blobA = Color.Transparent,
        blobB = Color.Transparent,
        isLight = false,
        flatWallpaper = true,
        wallpaperRes = R.drawable.wallpaper_default
    )
    GlassStyle.DARK -> DarkGlass
    GlassStyle.AMOLED -> RayGlass(
        panel = Color(0xE6050507),
        panelStrong = Color(0xF0101012),
        stroke = Color(0xFF2A2A2E),
        strokeFocus = Color(0xFF22D3EE),
        text = Color(0xFFF4F4F5),
        muted = Color(0xFF9CA3AF),
        accent = Color(0xFF22D3EE),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF000000),
        blobA = Color(0x3322D3EE),
        blobB = Color(0x2267E8F9),
        isLight = false,
        flatWallpaper = true,
        wallpaperRes = R.drawable.wallpaper_amoled
    )
    GlassStyle.FLY_UI -> RayGlass(
        panel = Color(0x3326262C),
        panelStrong = Color(0x592C2C32),
        stroke = Color(0xFF454550),
        strokeFocus = Color(0xFF1BC9B8),
        text = Color(0xFFF2F3F5),
        muted = Color(0xFFA8ADB8),
        accent = Color(0xFF219BF0),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF1A1A1E),
        blobA = Color(0x66219BF0),
        blobB = Color(0x441BC9B8),
        isLight = false,
        wallpaperRes = R.drawable.wallpaper_fly_ui
    )
    GlassStyle.SEMC -> RayGlass(
        panel = Color(0x33161C20),
        panelStrong = Color(0x591C2428),
        stroke = Color(0xFF2E4038),
        strokeFocus = Color(0xFF00C989),
        text = Color(0xFFE8F0EC),
        muted = Color(0xFF8FA89C),
        accent = Color(0xFF00C989),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF0A0C10),
        blobA = Color(0x6600C989),
        blobB = Color(0x4433D4A8),
        isLight = false,
        wallpaperRes = R.drawable.wallpaper_semc
    )
    GlassStyle.DARK_FLAT -> RayGlass(
        panel = Color(0xFF1E1E22),
        panelStrong = Color(0xFF26262C),
        stroke = Color(0xFF3F3F4A),
        strokeFocus = Color(0xFF21E6EB),
        text = Color(0xFFF5F5F5),
        muted = Color(0xFF9CA3AF),
        accent = Color(0xFF21E6EB),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF121212),
        blobA = Color.Transparent,
        blobB = Color.Transparent,
        isLight = false,
        flatWallpaper = true,
        wallpaperRes = R.drawable.wallpaper_dark_flat
    )
    GlassStyle.FLAT_BLACK -> RayGlass(
        panel = Color(0xFF141414),
        panelStrong = Color(0xFF1A1A1A),
        stroke = Color(0xFF383838),
        strokeFocus = Color(0xFFE8E8E8),
        text = Color(0xFFF2F2F2),
        muted = Color(0xFF9E9E9E),
        accent = Color(0xFFE8E8E8),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF080808),
        blobA = Color.Transparent,
        blobB = Color.Transparent,
        isLight = false,
        flatWallpaper = true,
        wallpaperRes = R.drawable.wallpaper_flat_black
    )
    GlassStyle.GLASS_GRI -> RayGlass(
        panel = Color(0x332A2F38),
        panelStrong = Color(0x59343A45),
        stroke = Color(0xFF475569),
        strokeFocus = Color(0xFFCBD5E1),
        text = Color(0xFFF8FAFC),
        muted = Color(0xFF94A3B8),
        accent = Color(0xFFCBD5E1),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF1A1D21),
        blobA = Color(0x66CBD5E1),
        blobB = Color(0x4494A3B8),
        isLight = false,
        wallpaperRes = R.drawable.wallpaper_glass_gri
    )
    GlassStyle.MACOS_GLASS -> readableFrost(
        accent = Color(0xFF64D2FF),
        wallpaperRes = R.drawable.wallpaper_macos_glass,
        blobA = Color(0x3364D2FF),
        blobB = Color(0x22A78BFA),
        wallpaperDark = Color(0xFF090B10),
        strokeFocus = Color(0xFF64D2FF)
    )
    GlassStyle.IOS27 -> RayGlass(
        panel = Color(0x8A141722),
        panelStrong = Color(0xB8181C28),
        stroke = Color(0x3DFFFFFF),
        strokeFocus = Color(0xFF0A84FF),
        text = Color(0xFFF5F7FF),
        muted = Color(0xFFB9C2D6),
        accent = Color(0xFF0A84FF),
        danger = Color(0xFFFF375F),
        wallpaperDark = Color(0xFF080B14),
        blobA = Color(0x330A84FF),
        blobB = Color(0x225E5CE6),
        isLight = false,
        wallpaperRes = R.drawable.wallpaper_default
    )
    GlassStyle.MAC_TEMA -> RayGlass(
        panel = Color(0x99161720),
        panelStrong = Color(0xC71A1C26),
        stroke = Color(0x33FFFFFF),
        strokeFocus = Color(0xFF0A84FF),
        text = Color(0xFFF2F2F7),
        muted = Color(0xFFAEAEB2),
        accent = Color(0xFF0A84FF),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF0C0D12),
        blobA = Color(0x330A84FF),
        blobB = Color(0x225E5CE6),
        isLight = false,
        wallpaperRes = R.drawable.wallpaper_default
    )
    GlassStyle.GLASSMORPHISM -> readableFrost(
        accent = Color(0xFF7EB6FF),
        wallpaperRes = R.drawable.wallpaper_default,
        blobA = Color(0x337EB6FF),
        blobB = Color(0x224A607A),
        wallpaperDark = Color(0xFF0A0C10),
        strokeFocus = Color(0xCC7EB6FF)
    )
    GlassStyle.MINT -> RayGlass(
        panel = Color(0x331A2418),
        panelStrong = Color(0x5924302A),
        stroke = Color(0xFF3D4F38),
        strokeFocus = Color(0xFF87CF3E),
        text = Color(0xFFF2F7EC),
        muted = Color(0xFFA8B89C),
        accent = Color(0xFF87CF3E),
        danger = Color(0xFFFF453A),
        wallpaperDark = Color(0xFF12180F),
        blobA = Color(0x6687CF3E),
        blobB = Color(0x44A3D977),
        isLight = false,
        wallpaperRes = R.drawable.wallpaper_default
    )
    GlassStyle.LIGHT -> LightGlass.copy(wallpaperRes = R.drawable.wallpaper_default)
}.copy(kind = this)

fun GlassStyle.title(tr: Boolean): String = when (this) {
    GlassStyle.TV_LITE -> "TV Lite"
    GlassStyle.MACOS_TV -> "macOS TV"
    GlassStyle.DARK -> if (tr) "Koyu Cam" else "Dark Glass"
    GlassStyle.AMOLED -> "Amoled Black"
    GlassStyle.FLY_UI -> "Fly UI"
    GlassStyle.SEMC -> "SEMC Theme"
    GlassStyle.DARK_FLAT -> "Dark Flat"
    GlassStyle.FLAT_BLACK -> "Flat Black"
    GlassStyle.GLASS_GRI -> "Glass Gri"
    GlassStyle.MACOS_GLASS -> "macOS Glass"
    GlassStyle.IOS27 -> "OS27"
    GlassStyle.MAC_TEMA -> if (tr) "Mac Tema" else "Mac Theme"
    GlassStyle.GLASSMORPHISM -> "Glassmorphism"
    GlassStyle.MINT -> "Mint"
    GlassStyle.LIGHT -> if (tr) "Açık Cam" else "Light Glass"
}

fun GlassStyle.subtitle(tr: Boolean): String = when (this) {
    GlassStyle.TV_LITE -> if (tr) "Blur yok, siyah yüzey, kırmızı vurgu" else "No blur, black surfaces, red accent"
    GlassStyle.MACOS_TV -> if (tr) "Mat cam, %0 blur, Sequoia mavisi" else "Matte glass, zero blur, Sequoia blue"
    GlassStyle.DARK -> if (tr) "Koyu buzlu cam, cyan vurgu" else "Dark frosted glass, cyan accent"
    GlassStyle.AMOLED -> if (tr) "Saf siyah, camgöbeği vurgu" else "Pure black, cyan accent"
    GlassStyle.FLY_UI -> if (tr) "Flyme buzlu cam, mavi–camgöbeği" else "Flyme frosted glass, blue-cyan"
    GlassStyle.SEMC -> if (tr) "Xperia yeşil vurgu, koyu cam" else "Xperia green accent, dark glass"
    GlassStyle.DARK_FLAT -> if (tr) "Kömür yüzey, camgöbeği vurgu" else "Charcoal surface, cyan accent"
    GlassStyle.FLAT_BLACK -> if (tr) "Gri-siyah, renkli vurgu yok" else "Grey-black, no color accent"
    GlassStyle.GLASS_GRI -> if (tr) "Buzlu slate gri, nötr odak" else "Frosted slate grey, neutral focus"
    GlassStyle.MACOS_GLASS -> if (tr) "Şeffaf cam, cyan ışıma" else "Clear glass, cyan glow"
    GlassStyle.IOS27 -> if (tr) "iOS damla cam, sistem mavisi" else "iOS Liquid Glass, system blue"
    GlassStyle.MAC_TEMA -> if (tr) "macOS Tahoe, Apple mavisi" else "macOS Tahoe, Apple blue"
    GlassStyle.GLASSMORPHISM -> if (tr) "Mavi odaklı buzlu cam" else "Blue-focused frosted glass"
    GlassStyle.MINT -> if (tr) "Linux Mint yeşil, yarı saydam" else "Linux Mint green, semi-transparent"
    GlassStyle.LIGHT -> if (tr) "Açık buzlu cam paneller" else "Light frosted glass panels"
}

/** Mina `selectableThemesForLayout` — TV Lite telefon listesinde yok; Fly UI TV listesinde yok. */
fun glassStylesForLayout(mobile: Boolean, current: GlassStyle? = null): List<GlassStyle> {
    val mina = listOf(
        GlassStyle.TV_LITE,
        GlassStyle.MACOS_TV,
        GlassStyle.DARK,
        GlassStyle.AMOLED,
        GlassStyle.FLY_UI,
        GlassStyle.SEMC,
        GlassStyle.DARK_FLAT,
        GlassStyle.FLAT_BLACK,
        GlassStyle.GLASS_GRI
    )
    val extras = listOf(
        GlassStyle.MACOS_GLASS,
        GlassStyle.IOS27,
        GlassStyle.MAC_TEMA,
        GlassStyle.GLASSMORPHISM,
        GlassStyle.MINT,
        GlassStyle.LIGHT
    )
    val allowed = if (mobile) {
        mina.filter { it != GlassStyle.TV_LITE }
    } else {
        mina.filter {
            it != GlassStyle.FLY_UI
        } + extras
    }
    return if (current != null && current !in allowed) listOf(current) + allowed else allowed
}

val RayGlass.usesLightChrome: Boolean
    get() = kind == GlassStyle.FLY_UI ||
        kind == GlassStyle.GLASSMORPHISM ||
        kind == GlassStyle.GLASS_GRI ||
        kind == GlassStyle.LIGHT

val RayGlass.darkFlatStyle: Boolean
    get() = kind == GlassStyle.DARK_FLAT ||
        kind == GlassStyle.FLAT_BLACK ||
        kind == GlassStyle.TV_LITE ||
        kind == GlassStyle.MACOS_TV

fun RayGlass.capsuleGradient(): List<Color> = when (kind) {
    GlassStyle.MACOS_GLASS -> listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))
    GlassStyle.IOS27 -> listOf(Color(0x66172A4A), Color(0x380A1730))
    GlassStyle.MAC_TEMA -> listOf(Color(0xBF28283E), Color(0x8C1C1C2A), Color(0x8C12121D))
    GlassStyle.AMOLED -> listOf(Color(0xCC0A0A0C), Color(0x99050506))
    GlassStyle.SEMC -> listOf(Color(0x60202822), Color(0x42141816))
    GlassStyle.FLY_UI, GlassStyle.GLASSMORPHISM ->
        listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.12f))
    GlassStyle.GLASS_GRI -> listOf(Color(0x47F1F5F9), Color(0x1ACBD5E1))
    GlassStyle.DARK_FLAT, GlassStyle.FLAT_BLACK, GlassStyle.TV_LITE, GlassStyle.MACOS_TV ->
        listOf(Color(0xFF26262E), Color(0xFF1C1C22))
    GlassStyle.DARK -> listOf(Color(0x551E1E28), Color(0x38141820))
    GlassStyle.LIGHT -> listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.38f))
    else -> listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.08f))
}

fun RayGlass.capsuleStroke(): Color = when (kind) {
    GlassStyle.MACOS_GLASS -> Color.White.copy(alpha = 0.25f)
    GlassStyle.IOS27 -> Color.White.copy(alpha = 0.30f)
    GlassStyle.AMOLED -> Color.White.copy(alpha = 0.14f)
    GlassStyle.SEMC -> Color(0x4D00C989)
    GlassStyle.FLY_UI, GlassStyle.GLASSMORPHISM -> Color.White.copy(alpha = 0.52f)
    GlassStyle.GLASS_GRI -> Color.White.copy(alpha = 0.50f)
    GlassStyle.DARK_FLAT, GlassStyle.FLAT_BLACK, GlassStyle.TV_LITE, GlassStyle.MACOS_TV -> Color(0xFF3D3D4A)
    GlassStyle.DARK -> Color.White.copy(alpha = 0.24f)
    else -> Color.White.copy(alpha = 0.30f)
}

fun RayGlass.capsuleForeground(): Color = when {
    kind == GlassStyle.MACOS_GLASS -> Color.White
    usesLightChrome -> Color(0xFF0F172A)
    else -> Color.White
}

fun RayGlass.sectionGradient(): List<Color> = when (kind) {
    GlassStyle.MACOS_GLASS -> listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
    GlassStyle.IOS27 -> listOf(Color(0x9E0E1B33), Color(0x750A1428))
    GlassStyle.MAC_TEMA -> listOf(Color(0xB81C1C2A), Color(0x8A12121E))
    GlassStyle.SEMC -> listOf(Color(0xA0222E26), Color(0x88141816))
    GlassStyle.AMOLED -> listOf(Color(0xCC050507), Color(0xA6020203))
    GlassStyle.FLY_UI, GlassStyle.GLASSMORPHISM ->
        listOf(Color(0x940F172A), Color(0x700B1220))
    GlassStyle.GLASS_GRI -> listOf(Color(0xA61E293B), Color(0x800F172A))
    GlassStyle.DARK_FLAT, GlassStyle.FLAT_BLACK, GlassStyle.TV_LITE, GlassStyle.MACOS_TV ->
        listOf(Color(0xFF1C1C22), Color(0xFF121218))
    GlassStyle.DARK -> listOf(Color(0x90181820), Color(0x720E1016))
    else -> listOf(Color.Black.copy(alpha = 0.42f), Color.Black.copy(alpha = 0.28f))
}

fun RayGlass.dockFill(): Color = when {
    usesLightChrome -> Color(0xD10C0C0C)
    kind == GlassStyle.AMOLED -> Color(0xE6050507)
    darkFlatStyle -> Color(0xE61E1E24)
    kind == GlassStyle.SEMC -> Color(0xD1161C20)
    else -> Color(0xD10C0C0C)
}

fun RayGlass.wallpaperScrim(): Pair<Float, Float> = when {
    wallpaperRes == 0 -> 0.22f to 0.48f
    usesLightChrome -> 0.04f to 0.12f
    kind == GlassStyle.AMOLED || kind == GlassStyle.FLAT_BLACK -> 0.08f to 0.16f
    else -> 0.06f to 0.14f
}

@Composable
fun RayTheme(
    style: GlassStyle = GlassStyle.DARK,
    reduceEffects: Boolean = false,
    fontKey: String = "sony",
    content: @Composable () -> Unit
) {
    val glass = style.toPalette().copy(reduceEffects = reduceEffects)
    val family = remember(fontKey) { appFontFamily(fontKey) }
    CompositionLocalProvider(LocalGlass provides glass) {
        MaterialTheme(
            colorScheme = scheme(glass),
            typography = rayTypography(family),
            content = content
        )
    }
}

