package com.ray.iptv.ui.components

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.ui.i18n.Copy
import kotlinx.coroutines.delay

@Composable
fun ResumePlaybackDialog(
    item: VodEntity,
    prog: ProgressEntity,
    strings: Copy,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit
) {
    val resumeFocusRequester = remember { FocusRequester() }
    val startOverFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(item.id) {
        delay(120L)
        try {
            resumeFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(onBack = onDismiss)

    val posMs = prog.positionMs.coerceAtLeast(0L)
    val durMs = prog.durationMs.coerceAtLeast(0L)
    val percent = if (durMs > 0L) {
        ((posMs.toFloat() / durMs.toFloat()) * 100f).toInt().coerceIn(1, 99)
    } else 0

    val timeFormatted = formatDuration(posMs)
    val totalFormatted = if (durMs > 0L) formatDuration(durMs) else ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF141A29).copy(alpha = 0.98f),
                            Color(0xFF0C101A).copy(alpha = 0.99f)
                        )
                    )
                )
                .border(
                    1.2.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF00F0FF).copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF00F0FF))
                            )
                        )
                        .border(1.5.dp, Color(0xFF67E8F9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle & Position Info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = strings.resumePrompt,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    if (timeFormatted.isNotBlank()) {
                        val posText = if (totalFormatted.isNotBlank()) {
                            "$timeFormatted / $totalFormatted (%$percent)"
                        } else {
                            "$timeFormatted (%$percent)"
                        }
                        Text(
                            text = posText,
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Progress Bar
                if (percent > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percent / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF06B6D4), Color(0xFF00F0FF))
                                    )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Action Buttons with TV remote D-Pad Focus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Resume Button (Primary, Auto-focused)
                    TvDialogButton(
                        label = strings.resumeButton,
                        icon = Icons.Filled.PlayArrow,
                        primary = true,
                        focusRequester = resumeFocusRequester,
                        modifier = Modifier.weight(1f),
                        onClick = onResume
                    )

                    // Start Over Button
                    TvDialogButton(
                        label = strings.startOver,
                        icon = Icons.Filled.Replay,
                        primary = false,
                        focusRequester = startOverFocusRequester,
                        modifier = Modifier.weight(1f),
                        onClick = onStartOver
                    )
                }

                // Cancel Button
                TvDialogButton(
                    label = strings.cancel,
                    icon = Icons.Filled.Close,
                    primary = false,
                    isGhost = true,
                    focusRequester = cancelFocusRequester,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvDialogButton(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    isGhost: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(150),
        label = "button-scale"
    )

    val backgroundBrush = when {
        isFocused && primary -> Brush.linearGradient(
            listOf(Color(0xFF0284C7), Color(0xFF00F0FF))
        )
        isFocused -> Brush.linearGradient(
            listOf(Color(0xFF334155), Color(0xFF1E293B))
        )
        primary -> Brush.linearGradient(
            listOf(Color(0xFF0369A1).copy(alpha = 0.90f), Color(0xFF0284C7).copy(alpha = 0.90f))
        )
        isGhost -> Brush.linearGradient(
            listOf(Color.Transparent, Color.Transparent)
        )
        else -> Brush.linearGradient(
            listOf(Color(0xFF1E293B).copy(alpha = 0.70f), Color(0xFF0F172A).copy(alpha = 0.80f))
        )
    }

    val borderModifier = when {
        isFocused -> Modifier.border(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF00F0FF), Color(0xFF67E8F9))
            ),
            shape = RoundedCornerShape(14.dp)
        )
        primary -> Modifier.border(
            width = 1.dp,
            color = Color(0xFF38BDF8).copy(alpha = 0.50f),
            shape = RoundedCornerShape(14.dp)
        )
        isGhost -> Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp)
        )
        else -> Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(14.dp)
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .then(borderModifier)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isFocused || primary) Color.White else Color.White.copy(alpha = 0.80f),
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isFocused || primary) Color.White else Color.White.copy(alpha = 0.85f),
                fontSize = 13.5.sp,
                fontWeight = if (isFocused || primary) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
