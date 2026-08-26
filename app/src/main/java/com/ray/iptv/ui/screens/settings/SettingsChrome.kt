package com.ray.iptv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.ray.iptv.ui.input.rayFocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.repo.GlassStyle
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.components.GlassToggle
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.capsuleForeground
import com.ray.iptv.ui.theme.capsuleGradient
import com.ray.iptv.ui.theme.capsuleStroke
import com.ray.iptv.ui.theme.subtitle
import com.ray.iptv.ui.theme.title
import com.ray.iptv.ui.theme.toPalette
import com.ray.iptv.ui.theme.usesLightChrome

val LocalMobileSettingsChrome = compositionLocalOf { false }

@Composable
fun SettingsSectionLabel(text: String) {
    val g = LocalGlass.current
    val mobile = LocalMobileSettingsChrome.current
    Text(
        text.uppercase(),
        color = if (mobile) Color(0xFF22D3EE) else g.muted,
        fontSize = if (mobile) 12.5.sp else MaterialTheme.typography.labelMedium.fontSize,
        fontWeight = if (mobile) FontWeight.Bold else FontWeight.Medium,
        letterSpacing = if (mobile) 0.5.sp else 0.sp,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = if (mobile) 4.dp else 6.dp)
    )
}

@Composable
fun MobileSettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val g = LocalGlass.current
    val accent = if (danger) g.danger else Color(0xFF22D3EE)
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF16382B).copy(alpha = 0.38f), shape)
            .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.18f), shape)
            .rayClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f))
                .border(0.8.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MobileSettingsFrame(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF16382B).copy(alpha = 0.38f), shape)
            .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.18f), shape)
            .then(if (onClick != null) Modifier.rayClickable(onClick) else Modifier)
    ) {
        content()
    }
}

@Composable
fun MobileSwitch(checked: Boolean) {
    Box(
        Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.18f))
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .size(20.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(if (checked) Color(0xFF0C1914) else Color.White)
        )
    }
}

@Composable
fun MobileOptionTile(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showSwitch: Boolean = false,
    checked: Boolean = false,
    valueTrailing: String? = null,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    accent: Color? = null
) {
    val resolvedAccent = accent ?: Color(0xFF22D3EE)
    MobileSettingsFrame(onClick = if (enabled && actionButtonText == null) onClick else null) {
        Row(
            Modifier
                .alpha(if (enabled) 1f else 0.42f)
                .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(resolvedAccent.copy(alpha = 0.14f))
                        .border(0.8.dp, resolvedAccent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = resolvedAccent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            when {
                showSwitch -> {
                    MobileSwitch(checked)
                }
                actionButtonText != null -> {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF133E37).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .rayClickable(onClick = { onActionClick?.invoke() ?: onClick() })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            actionButtonText,
                            color = Color(0xFF22D3EE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
                valueTrailing != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.rayClickable(onClick)
                    ) {
                        Text(
                            valueTrailing,
                            color = Color(0xFF22D3EE),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                else -> {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HubGlassTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    onLeft: (() -> Unit)? = null,
    isTopRow: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        strong = true,
        radius = 16.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .rayFocusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> {
                        if (onLeft != null) {
                            onLeft()
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        if (isTopRow) true else false
                    }
                    else -> false
                }
            }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background((if (danger) g.danger else g.accent).copy(alpha = if (g.frostDark) 0.34f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (danger) g.danger else g.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (danger) g.danger else g.text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    subtitle,
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SettingsSubpage(title: String, hint: String? = null, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!LocalMobileSettingsChrome.current) {
                GlassButton("←") { onBack() }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = g.text, style = MaterialTheme.typography.headlineMedium)
                if (!hint.isNullOrBlank()) Text(hint, color = g.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    }
}

@Composable
fun SettingsNavRow(title: String, subtitle: String, icon: ImageVector? = null, onClick: () -> Unit) {
    if (LocalMobileSettingsChrome.current) {
        MobileOptionTile(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
        return
    }
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        radius = 14.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = g.text, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = g.muted)
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector? = null,
    onToggle: () -> Unit
) {
    if (LocalMobileSettingsChrome.current) {
        MobileOptionTile(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onToggle,
            showSwitch = true,
            checked = checked
        )
        return
    }
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        radius = 14.dp,
        scaleOnFocus = false,
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = g.text, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Spacer(Modifier.width(12.dp))
            GlassToggle(checked)
        }
    }
}

@Composable
fun <T> SettingsPickerRow(
    title: String,
    valueLabel: String,
    options: List<Pair<T, String>>,
    selected: T,
    body: String? = null,
    extraAction: Pair<String, () -> Unit>? = null,
    icon: ImageVector? = null,
    onPick: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    if (LocalMobileSettingsChrome.current) {
        MobileOptionTile(
            icon = icon,
            title = title,
            subtitle = body ?: "",
            valueTrailing = valueLabel,
            onClick = { open = true }
        )
    } else {
        SettingsNavRow(title, valueLabel, icon) { open = true }
    }
    if (open) {
        GlassChoiceDialog(
            title = title,
            options = options,
            selected = selected,
            body = body,
            extraAction = extraAction,
            onDismiss = { open = false },
            onPick = {
                onPick(it)
                open = false
            }
        )
    }
}

@Composable
fun <T> GlassChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    body: String? = null,
    extraAction: Pair<String, () -> Unit>? = null,
    preview: ((T) -> Unit)? = null,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit
) {
    val g = LocalGlass.current
    var pending by remember { mutableStateOf(selected) }
    val dialogMaxH = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        GlassPanel(
            strong = true,
            radius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = dialogMaxH)
                .navigationBarsPadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogMaxH)
                    .padding(18.dp)
            ) {
                Text(title, color = g.text, style = MaterialTheme.typography.headlineSmall)
                if (!body.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(body, color = g.muted, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                GlassPanel(
                    radius = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(Modifier.padding(6.dp).verticalScroll(rememberScrollState())) {
                        val mobile = LocalMobileSettingsChrome.current
                        options.forEach { (value, label) ->
                            val on = value == pending
                            var focused by remember { mutableStateOf(false) }
                            val pick: () -> Unit = {
                                pending = value
                                preview?.invoke(value)
                            }
                            if (mobile) {
                                MobileSettingsFrame(onClick = pick) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            label,
                                            color = LocalGlass.current.text,
                                            fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (on) Icon(Icons.Filled.Check, contentDescription = null, tint = LocalGlass.current.accent)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            } else {
                            GlassPanel(
                                focused = focused || on,
                                accentFill = on,
                                radius = 12.dp,
                                onClick = pick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .onFocusChanged { focused = it.isFocused }
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        label,
                                        color = g.text,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (on) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val mobile = LocalMobileSettingsChrome.current
                if (mobile) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassButton(
                            "Kaydet",
                            primary = true,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) { onPick(pending) }
                        extraAction?.let { (label, action) ->
                            GlassButton(
                                label,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                action()
                                onDismiss()
                            }
                        }
                        GlassButton(
                            "Vazgeç",
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { onDismiss() }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassButton("Vazgeç") { onDismiss() }
                        extraAction?.let { (label, action) ->
                            GlassButton(label) {
                                action()
                                onDismiss()
                            }
                        }
                        GlassButton("Kaydet", primary = true) { onPick(pending) }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeChoiceTile(
    style: GlassStyle,
    selected: Boolean,
    tr: Boolean,
    onClick: () -> Unit
) {
    val g = LocalGlass.current
    val pal = style.toPalette()
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused || selected,
        strong = true,
        radius = 18.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.wallpaperDark)
            ) {
                if (pal.wallpaperRes != 0) {
                    AsyncImage(
                        model = pal.wallpaperRes,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(pal.accent)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(style.title(tr), color = g.text, style = MaterialTheme.typography.titleMedium)
                Text(style.subtitle(tr), color = g.muted, style = MaterialTheme.typography.bodyMedium)
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = g.accent, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun MobileThemePickerDialog(
    tr: Boolean,
    styles: List<GlassStyle>,
    selected: GlassStyle,
    preview: (GlassStyle) -> Unit,
    onDismiss: () -> Unit,
    onPick: (GlassStyle) -> Unit
) {
    val g = LocalGlass.current
    var pending by remember { mutableStateOf(selected) }
    val dialogMaxH = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        GlassPanel(
            strong = true,
            radius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = dialogMaxH)
                .navigationBarsPadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogMaxH)
                    .padding(18.dp)
            ) {
                Text(if (tr) "Tema" else "Theme", color = g.text, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (tr) "Duvar kâğıdı ve vurgu rengi seçince hemen uygulanır."
                    else "Wallpaper and accent apply instantly.",
                    color = g.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    styles.forEach { style ->
                        ThemeChoiceTile(
                            style = style,
                            selected = pending == style,
                            tr = tr,
                            onClick = {
                                pending = style
                                preview(style)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassButton(
                        "Kaydet",
                        primary = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) { onPick(pending) }
                    GlassButton(
                        "Vazgeç",
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { onDismiss() }
                }
            }
        }
    }
}

@Composable
fun GlassConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    cancel: String = "Vazgeç",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF111714))
                .border(1.2.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                .padding(22.dp)
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    body,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                val destructive = confirm == "Sil" || confirm.equals("Delete", true)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                            .rayClickable(onClick = onDismiss)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cancel, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (destructive) Color(0xFF8B1D1D) else Color(0xFF133630))
                            .border(
                                1.dp,
                                if (destructive) Color(0xFFEF5350) else Color(0xFF22D3EE).copy(alpha = 0.65f),
                                RoundedCornerShape(12.dp)
                            )
                            .rayClickable(onClick = {
                                onConfirm()
                                onDismiss()
                            })
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(confirm, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}
