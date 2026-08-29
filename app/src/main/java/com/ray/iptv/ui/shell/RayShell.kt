package com.ray.iptv.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.R
import com.ray.iptv.data.account.AccountSession
import com.ray.iptv.ui.Dest
import com.ray.iptv.ui.components.tickingClock
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.glass.RayWallpaper
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.LocalTouchUi
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.motion.rayRailEnter
import com.ray.iptv.ui.motion.rayRailExit
import com.ray.iptv.ui.theme.LocalGlass
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.ray.iptv.ui.input.tryFocus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage

private data class NavItem(
    val dest: Dest?,
    val icon: ImageVector,
    val label: (Copy) -> String,
    val search: Boolean = false,
    val repeat: Boolean = false
)

private val items = listOf(
    NavItem(null, Icons.Filled.Search, { it.search }, search = true),
    NavItem(Dest.LIVE, Icons.Filled.LiveTv, { it.live }),
    NavItem(Dest.MOVIES, Icons.Filled.Movie, { it.movies }),
    NavItem(Dest.SERIES, Icons.Filled.VideoLibrary, { it.series }),
    NavItem(Dest.CONTINUE, Icons.Filled.History, { it.cont }),
    NavItem(Dest.PLAYLISTS, Icons.Filled.PlaylistPlay, { it.playlists }),
    NavItem(Dest.CATCHUP, Icons.Filled.Replay, { it.repeat }),
    NavItem(Dest.SETTINGS, Icons.Filled.Settings, { it.settings })
)

@Composable
fun RayShell(
    current: Dest,
    copy: Copy,
    syncMessage: String,
    account: AccountSession? = null,
    showLive: Boolean = true,
    showMovies: Boolean = true,
    showSeries: Boolean = true,
    showContinue: Boolean = true,
    showPlaylists: Boolean = true,
    showRepeat: Boolean = true,
    railExpanded: Boolean = true,
    railHidden: Boolean = false,
    searchSelected: Boolean = false,
    repeatSelected: Boolean = false,
    onGo: (Dest) -> Unit,
    onSearch: () -> Unit = {},
    onRepeat: () -> Unit = {},
    onRailFocused: () -> Unit = {},
    onToggleRail: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val g = LocalGlass.current
    val touch = LocalTouchUi.current
    val searchFocusRequester = remember { FocusRequester() }
    val repeatFocusRequester = remember { FocusRequester() }
    val liveFocusRequester = remember { FocusRequester() }
    val moviesFocusRequester = remember { FocusRequester() }
    val seriesFocusRequester = remember { FocusRequester() }
    val continueFocusRequester = remember { FocusRequester() }
    val playlistsFocusRequester = remember { FocusRequester() }
    val catchupFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    var initialFocusDone by remember { mutableStateOf(false) }

    LaunchedEffect(railExpanded) {
        if (railExpanded && !touch) {
            val target = when {
                searchSelected -> searchFocusRequester
                repeatSelected -> catchupFocusRequester
                current == Dest.LIVE -> liveFocusRequester
                current == Dest.MOVIES -> moviesFocusRequester
                current == Dest.SERIES -> seriesFocusRequester
                current == Dest.CONTINUE -> continueFocusRequester
                current == Dest.PLAYLISTS -> playlistsFocusRequester
                current == Dest.CATCHUP -> catchupFocusRequester
                current == Dest.SETTINGS -> settingsFocusRequester
                else -> liveFocusRequester
            }
            repeat(8) {
                delay(25)
                if (target.tryFocus()) return@LaunchedEffect
            }
        }
    }


    LaunchedEffect(Unit) {
        if (!touch && !initialFocusDone) {
            initialFocusDone = true
            repeat(12) {
                delay(60)
                if (liveFocusRequester.tryFocus()) return@LaunchedEffect
            }
        }
    }

    val railW by animateDpAsState(
        if (railExpanded) 176.dp else 64.dp,
        animationSpec = tween(if (g.reduceEffects) 0 else 240, easing = FastOutSlowInEasing),
        label = "rail-w"
    )
    val rootMod = if (touch) Modifier.fillMaxSize().systemBarsPadding() else Modifier.fillMaxSize()
    val hideChrome = current == Dest.LIVE || current == Dest.PLAYLISTS ||
        current == Dest.MOVIES || current == Dest.SERIES
    Box(rootMod) {
        RayWallpaper(
            overlayTop = if (current == Dest.SETTINGS) 0.42f else 0.32f,
            overlayBottom = if (current == Dest.SETTINGS) 0.72f else 0.55f
        )
        Row(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (railHidden) 0.dp else 8.dp)
        ) {
            AnimatedVisibility(
                visible = !railHidden,
                enter = rayRailEnter(),
                exit = rayRailExit(),
                modifier = Modifier.fillMaxHeight()
            ) {
                val railAlpha = if (current == Dest.SETTINGS && !railExpanded) 0.55f else 1f
                GlassPanel(
                    strong = true,
                    radius = 12.dp,
                    fillAlpha = if (current == Dest.SETTINGS && !railExpanded) 0.45f else 1f,
                    modifier = Modifier
                        .width(railW)
                        .fillMaxHeight()
                        .alpha(railAlpha)
                ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = if (railExpanded) Alignment.Start else Alignment.CenterHorizontally
                ) {
                    BrandHeader(
                        expanded = railExpanded,
                        onTap = if (touch) onToggleRail else null
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = if (railExpanded) Alignment.Start else Alignment.CenterHorizontally
                    ) {
                        items.filter {
                            when {
                                it.search -> true
                                it.repeat -> showRepeat
                                it.dest == Dest.LIVE -> showLive
                                it.dest == Dest.MOVIES -> showMovies
                                it.dest == Dest.SERIES -> showSeries
                                it.dest == Dest.CONTINUE -> showContinue
                                it.dest == Dest.PLAYLISTS -> showPlaylists
                                else -> true
                            }
                        }.forEach { item ->
                            val req = when {
                                item.search -> searchFocusRequester
                                item.repeat -> repeatFocusRequester
                                item.dest == Dest.LIVE -> liveFocusRequester
                                item.dest == Dest.MOVIES -> moviesFocusRequester
                                item.dest == Dest.SERIES -> seriesFocusRequester
                                item.dest == Dest.CONTINUE -> continueFocusRequester
                                item.dest == Dest.PLAYLISTS -> playlistsFocusRequester
                                item.dest == Dest.CATCHUP -> catchupFocusRequester
                                item.dest == Dest.SETTINGS -> settingsFocusRequester
                                else -> null
                            }
                            RailIcon(
                                icon = item.icon,
                                label = item.label(copy),
                                selected = when {
                                    item.search -> searchSelected
                                    item.repeat -> repeatSelected
                                    else -> current == item.dest
                                },
                                expanded = railExpanded,
                                focusRequester = req,
                                onClick = {
                                    when {
                                        item.search -> onSearch()
                                        item.repeat -> onRepeat()
                                        else -> item.dest?.let(onGo)
                                    }
                                },
                                onFocused = {
                                    if (railExpanded) onRailFocused()
                                }
                            )
                        }
                        if (account?.signedIn == true) {
                            Spacer(Modifier.height(4.dp))
                            RailUserProfileBadge(account = account, expanded = railExpanded)
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (!hideChrome) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        if (syncMessage.isNotBlank()) {
                            Text(
                                syncMessage,
                                color = g.accent,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 14.dp)
                            )
                        }
                        ClockLabel()
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ClockLabel() {
    val g = LocalGlass.current
    Text(tickingClock(), color = g.muted, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun BrandHeader(
    expanded: Boolean,
    onTap: (() -> Unit)?
) {
    val g = LocalGlass.current
    val brand = stringResource(R.string.brand_name)
    val logoSize = if (expanded) 30.dp else 26.dp
    val click = if (onTap != null) Modifier.rayClickable(onTap) else Modifier
    if (expanded) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(click)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.ray.iptv.ui.mobile.RayAnimatedUmbrella(
                modifier = Modifier.size(logoSize)
            )
            Text(
                brand,
                color = g.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .then(click)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            com.ray.iptv.ui.mobile.RayAnimatedUmbrella(
                modifier = Modifier.size(logoSize)
            )
        }
    }
}

@Composable
private fun RailIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    val touch = LocalTouchUi.current
    val active = focused || selected
    val tile = if (expanded) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.5.dp)
            .height(42.dp)
    } else {
        Modifier
            .padding(vertical = 3.5.dp)
            .size(if (touch) 48.dp else 44.dp)
    }
    val iconScale by animateFloatAsState(
        targetValue = if (focused) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rail-icon-scale"
    )

    GlassPanel(
        focused = active,
        strong = selected,
        accentFill = false,
        radius = 8.dp,
        scaleOnFocus = false,
        onClick = onClick,
        modifier = tile
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused && expanded) onFocused()
            }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight, Key.Enter, Key.DirectionCenter -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
    ) {
        if (expanded) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(g.accent)
                    )
                }
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) g.accent else if (focused) Color.White else if (g.frostDark) g.text.copy(alpha = 0.88f) else g.muted,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(iconScale)
                )
                Text(
                    label,
                    color = if (selected) g.accent else if (focused) Color.White else g.text.copy(alpha = if (g.frostDark) 0.92f else 0.82f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (selected) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(3.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(g.accent)
                    )
                }
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) g.accent else if (focused) Color.White else if (g.frostDark) g.text.copy(alpha = 0.88f) else g.muted,
                    modifier = Modifier
                        .size(if (touch) 24.dp else 22.dp)
                        .scale(iconScale)
                )
            }
        }
    }
}

@Composable
private fun RailUserProfileBadge(
    account: AccountSession,
    expanded: Boolean
) {
    val g = LocalGlass.current
    val touch = LocalTouchUi.current
    val tile = if (expanded) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.5.dp)
            .height(42.dp)
    } else {
        Modifier
            .padding(vertical = 3.5.dp)
            .size(if (touch) 48.dp else 44.dp)
    }
    val name = account.displayName.ifBlank { account.email.substringBefore('@') }
    val avatarSize = if (expanded) 24.dp else 26.dp

    Box(
        modifier = tile
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (expanded) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (account.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = account.photoUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.6f), CircleShape)
                    )
                } else {
                    Box(
                        Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF22D3EE), Color(0xFF3B82F6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.letter,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Text(
                    text = name,
                    color = g.text.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            if (account.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = account.photoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.6f), CircleShape)
                )
            } else {
                Box(
                    Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF22D3EE), Color(0xFF3B82F6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.letter,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
