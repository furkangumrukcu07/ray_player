package com.ray.iptv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.repo.DockbarStyle
import com.ray.iptv.data.repo.PageTransitionEffect
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.mobile.MobileCyan
import com.ray.iptv.ui.theme.LocalGlass

@Composable
fun ShowcaseHomeSettings(vm: RayViewModel, settings: RaySettings, tr: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                if (tr) "Aşağıdaki anahtarlar ana ekranda hangi şeritlerin görüneceğini belirler. Her satırın altındaki önizleme, açıldığında nelerin yer alacağını gösterir."
                else "These switches control which strips appear on the home screen. The preview under each row shows what will be added.",
                color = LocalGlass.current.muted,
                fontSize = 12.sp
            )
        }
        item {
            TransitionSection(
                tr = tr,
                current = settings.pageTransitionEffect,
                onPick = vm::setPageTransitionEffect
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.Schedule,
                title = if (tr) "Sıradaki Yayınlar (EPG)" else "Upcoming broadcasts (EPG)",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: popüler kanalların sıradaki yayın saatlerini ve geri sayımlarını gösterir"
                else "Showcase only: next air times and countdowns for popular channels",
                checked = settings.homeUpcomingEpg,
                onToggle = { vm.setHomeUpcomingEpg(!settings.homeUpcomingEpg) },
                preview = { UpcomingEpgPreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.LocalFireDepartment,
                title = if (tr) "Trend Filmler" else "Trending movies",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: IMDB 7 puan ve üzeri en iyi 50 filmi şerit olarak gösterir"
                else "Showcase only: top 50 movies rated 7+ as a home strip",
                checked = settings.homeTrendFilms,
                onToggle = { vm.setHomeTrendFilms(!settings.homeTrendFilms) },
                preview = { TrendPreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.TrendingUp,
                title = if (tr) "Trend Diziler" else "Trending series",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: IMDB 7 puan ve üzeri en iyi 50 diziyi şerit olarak gösterir"
                else "Showcase only: top 50 series rated 7+ as a home strip",
                checked = settings.homeTrendSeries,
                onToggle = { vm.setHomeTrendSeries(!settings.homeTrendSeries) },
                preview = { TrendPreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.Favorite,
                title = if (tr) "Favori Filmler" else "Favorite movies",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: favorilere eklediğin filmleri ana ekranda şerit olarak gösterir"
                else "Showcase only: movies you favourited as a home strip",
                checked = settings.homeFavoriteFilms,
                onToggle = { vm.setHomeFavoriteFilms(!settings.homeFavoriteFilms) },
                preview = { FavoritePreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.FavoriteBorder,
                title = if (tr) "Favori Diziler" else "Favorite series",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: favorilere eklediğin dizileri ana ekranda şerit olarak gösterir"
                else "Showcase only: series you favourited as a home strip",
                checked = settings.homeFavoriteSeries,
                onToggle = { vm.setHomeFavoriteSeries(!settings.homeFavoriteSeries) },
                preview = { FavoritePreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.Shuffle,
                title = if (tr) "Karışık Filmler" else "Mixed movies",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: tüm kategorilerden rastgele karışık filmleri ana ekranda şerit olarak gösterir"
                else "Showcase only: a random mix of movies from all categories",
                checked = settings.homeMixedFilms,
                onToggle = { vm.setHomeMixedFilms(!settings.homeMixedFilms) },
                preview = { MixedPreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.Shuffle,
                title = if (tr) "Karışık Diziler" else "Mixed series",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: tüm kategorilerden rastgele karışık dizileri ana ekranda şerit olarak gösterir"
                else "Showcase only: a random mix of series from all categories",
                checked = settings.homeMixedSeries,
                onToggle = { vm.setHomeMixedSeries(!settings.homeMixedSeries) },
                preview = { MixedPreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.LiveTv,
                title = if (tr) "Karışık Canlı TV" else "Mixed Live TV",
                subtitle = if (tr) "Yalnızca vitrin düzeninde: rastgele canlı kanalları ana ekranda şerit olarak gösterir"
                else "Showcase only: a random live channel strip on home",
                checked = settings.homeMixedLive,
                onToggle = { vm.setHomeMixedLive(!settings.homeMixedLive) },
                preview = { UpcomingEpgPreview() }
            )
        }
        item {
            DockbarSection(tr, settings.dockbarStyle, vm::setDockbarStyle)
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.PlayCircle,
                title = if (tr) "Son İzlenen Butonu" else "Last watched button",
                subtitle = if (tr) "Vitrin düzeninde arama butonunun üzerindeki son izlenen dairesel butonu gösterir/gizler"
                else "Show or hide the last-watched circle above the search button",
                checked = settings.homeLastWatchedButton,
                onToggle = { vm.setHomeLastWatchedButton(!settings.homeLastWatchedButton) }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.SportsSoccer,
                title = if (tr) "Sıradaki Maçlar" else "Upcoming matches",
                subtitle = if (tr) "Yaklaşan futbol/spor karşılaşmalarını ana ekranda kart şeridi olarak göster"
                else "Show upcoming football/sports fixtures as a home strip",
                checked = settings.homeUpcomingMatches,
                onToggle = { vm.setHomeUpcomingMatches(!settings.homeUpcomingMatches) },
                preview = { MatchesPreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.PlayCircleOutline,
                title = if (tr) "İzlemeye Devam Et" else "Continue watching",
                subtitle = if (tr) "Yarıda bıraktığın film ve dizileri, en son izlediğinden başlayarak ana ekranda göster. Kapatıldığında şerit kaldırılır."
                else "Show movies and series you left unfinished, most recent first. Turning this off hides the strip.",
                checked = settings.homeContinue,
                onToggle = { vm.setHomeContinue(!settings.homeContinue) },
                preview = { ContinuePreview() }
            )
        }
        item {
            HomeToggleCard(
                icon = Icons.Filled.AutoAwesome,
                title = if (tr) "Yapay Zekâ Destekli Ana Ekran Önerileri" else "AI home recommendations",
                subtitle = if (tr) "Ray AI izleme alışkanlığını analiz eder; kategori ve saat dilimine göre karma canlı/film/dizi önerir"
                else "Ray AI mixes live, movie and series suggestions from your viewing habits",
                checked = settings.homeAiRecommendations,
                onToggle = { vm.setHomeAiRecommendations(!settings.homeAiRecommendations) },
                preview = { AiPreview() }
            )
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun HomeToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    preview: (@Composable () -> Unit)? = null
) {
    HomeCardFrame {
        Row(
            Modifier
                .fillMaxWidth()
                .rayClickable(onToggle)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeIconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            MobileSwitch(checked)
        }
        if (preview != null) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B100D).copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Box(Modifier.fillMaxWidth(), content = { preview() })
            }
        }
    }
}

@Composable
private fun HomeCardFrame(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF101713).copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) { content() }
}

@Composable
private fun HomeIconBadge(icon: ImageVector, accent: Color = Color.White) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TransitionSection(tr: Boolean, current: PageTransitionEffect, onPick: (PageTransitionEffect) -> Unit) {
    HomeCardFrame {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
            HomeIconBadge(Icons.Filled.Animation)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (tr) "Geçiş Efekti" else "Transition effect", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (tr) "Sayfalar arası geçiş animasyonunu seç." else "Choose the animation between pages.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PageTransitionEffect.entries.forEach { fx ->
            val on = current == fx
            val title = when (fx) {
                PageTransitionEffect.IOS -> "iOS"
                PageTransitionEffect.FADE_SCALE -> if (tr) "Yumuşak" else "Soft"
                PageTransitionEffect.JELLY -> if (tr) "Sallanan Pencereler" else "Jelly windows"
            }
            val sub = when (fx) {
                PageTransitionEffect.IOS -> if (tr) "iOS tarzı sağdan sola kaydırma geçişi." else "iOS-style slide from the right."
                PageTransitionEffect.FADE_SCALE -> if (tr) "Yumuşak fade + scale geçişi." else "Soft fade and scale."
                PageTransitionEffect.JELLY -> if (tr) "Linux Compiz tarzı sallanan, elastik pencere geçişi." else "Compiz-style elastic window transition."
            }
            ChoiceTile(title, sub, on) { onPick(fx) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DockbarSection(tr: Boolean, current: DockbarStyle, onPick: (DockbarStyle) -> Unit) {
    HomeCardFrame {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
            HomeIconBadge(Icons.Filled.Dock)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (tr) "Dockbar Seçimi" else "Dock bar style", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (tr) "Vitrin alt sekme menüsünün görünüm stilini seçin." else "Choose the look of the showcase bottom bar.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        DockbarStyle.entries.forEach { style ->
            val on = current == style
            val title = when (style) {
                DockbarStyle.ORIGINAL -> if (tr) "Orijinal Pill" else "Original pill"
                DockbarStyle.CAPSULE -> if (tr) "Bitişik Kapsüller" else "Attached capsules"
                DockbarStyle.MODERN_GLASS -> if (tr) "Modern Neon Kapsüller" else "Modern neon capsules"
            }
            val sub = when (style) {
                DockbarStyle.ORIGINAL -> if (tr) "Yüzen ana cam çubuk ve sağ dairesel butonlar." else "Floating glass bar with circular buttons on the right."
                DockbarStyle.CAPSULE -> if (tr) "Ana menü ve arama butonunun yan yana kapsül tasarımı." else "Menu and search as side-by-side capsules."
                DockbarStyle.MODERN_GLASS -> if (tr) "Her sekmenin özel neon çerçeveli ve şeffaf cam kapsül tasarımı." else "Each tab in its own neon-framed glass capsule."
            }
            ChoiceTile(title, sub, on, cyan = true) { onPick(style) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChoiceTile(title: String, subtitle: String, selected: Boolean, cyan: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.035f))
            .border(
                if (selected) 1.2.dp else 1.dp,
                if (selected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.09f),
                RoundedCornerShape(14.dp)
            )
            .rayClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) Color.White else Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            Spacer(Modifier.height(1.dp))
            Text(subtitle, color = Color.White.copy(alpha = if (selected) 0.75f else 0.55f), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun MiniPoster(color: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .width(38.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.55f))))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun TrendPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("9.2", "8.7", "8.4", "8.1", "7.6", "7.2").forEach { r ->
            MiniPoster(Color(0xFF7E57C2)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(11.dp))
                    Text(r, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun FavoritePreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(6) {
            MiniPoster(Color(0xFFE91E63)) {
                Icon(Icons.Filled.Favorite, null, tint = Color(0xFFFF80AB), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MixedPreview() {
    val colors = listOf(Color(0xFF26A69A), Color(0xFF5C6BC0), Color(0xFFEF5350), Color(0xFFFFA726), Color(0xFF8D6E63), Color(0xFF42A5F5))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { c -> MiniPoster(c) { } }
    }
}

@Composable
private fun UpcomingEpgPreview() {
    val items = listOf("TRT 1" to "21:00", "Star" to "22:30", "Show" to "20:00", "atv" to "23:00")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (name, time) ->
            Column(
                Modifier
                    .width(64.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0x594482FF), Color(0x339C27B0))))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.LiveTv, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                Text(time, color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun MatchesPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("GS–FB", "BJK–TS", "UCL").forEach { n ->
            MiniPoster(Color(0xFF2E7D32)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SportsSoccer, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(n, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ContinuePreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(5) {
            MiniPoster(Color(0xFF455A64)) {
                Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AiPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MiniPoster(Color(0xFFE53935)) { Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
        MiniPoster(Color(0xFF7C4DFF)) { Text("DİZİ", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
        MiniPoster(Color(0xFFFFC107)) { Text("FİLM", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
    }
}
