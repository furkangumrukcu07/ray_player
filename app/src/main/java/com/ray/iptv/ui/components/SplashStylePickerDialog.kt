package com.ray.iptv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.tv.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.ray.iptv.R
import com.ray.iptv.data.repo.SplashStyle
import com.ray.iptv.ui.glass.DarkGlassPopupTheme

@Composable
fun SplashStylePickerDialog(
    tr: Boolean,
    currentStyle: SplashStyle,
    onDismiss: () -> Unit,
    onSelect: (SplashStyle) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            val isLandscape = maxWidth > maxHeight
            val isCompactHeight = maxHeight < 520.dp

            DarkGlassPopupTheme {
                Box(
                    modifier = Modifier
                        .padding(horizontal = if (isCompactHeight) 14.dp else 20.dp, vertical = if (isCompactHeight) 10.dp else 20.dp)
                        .widthIn(max = if (isLandscape) 560.dp else 480.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF10131B).copy(alpha = 0.95f))
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF64D2FF).copy(alpha = 0.50f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color(0xFF64D2FF).copy(alpha = 0.30f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // consume click to prevent dismissing
                        )
                        .padding(if (isCompactHeight) 16.dp else 22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (tr) "Açılış Ekranı (Splash)" else "Splash Screen Theme",
                                    color = Color.White,
                                    fontSize = if (isCompactHeight) 16.sp else 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (tr) {
                                        "Uygulama açılırken gösterilecek açılış temasını seçin."
                                    } else {
                                        "Select the splash screen theme shown on app startup."
                                    },
                                    color = Color.White.copy(alpha = 0.60f),
                                    fontSize = if (isCompactHeight) 11.sp else 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(if (isCompactHeight) 12.dp else 16.dp))

                        // List of 3 styles
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 11.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(SplashStyle.entries) { style ->
                                val selected = style == currentStyle

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selected) {
                                                Color(0xFF64D2FF).copy(alpha = 0.12f)
                                            } else {
                                                Color.White.copy(alpha = 0.05f)
                                            }
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) Color(0xFF64D2FF).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.10f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            onSelect(style)
                                            onDismiss()
                                        }
                                        .padding(if (isCompactHeight) 9.dp else 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Miniature Preview Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(
                                                    width = if (isCompactHeight) 56.dp else 68.dp,
                                                    height = if (isCompactHeight) 62.dp else 76.dp
                                                )
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Natural blurred leaf background for all
                                            Image(
                                                painter = painterResource(id = R.drawable.splash_nature_bg),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            // Mini Scrim
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.25f))
                                            )

                                            // Concept-specific micro preview
                                            when (style) {
                                                SplashStyle.DARK_GLASS_CARD -> {
                                                    // Mini Center Card
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (isCompactHeight) 42.dp else 50.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xD910131B))
                                                            .border(1.dp, Color(0xFF64D2FF).copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_ray_splash_logo),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(if (isCompactHeight) 22.dp else 26.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                        )
                                                    }
                                                }
                                                SplashStyle.MINIMAL_NEON_PULSE -> {
                                                    // Mini Free Floating Logo with Neon Glow Ring
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (isCompactHeight) 44.dp else 52.dp)
                                                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.7f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_ray_splash_logo),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(if (isCompactHeight) 24.dp else 28.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                        )
                                                    }
                                                }
                                                SplashStyle.GLASS_CAPSULE_PROGRESS -> {
                                                    // Mini Logo + Bottom Progress Line
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center,
                                                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_ray_splash_logo),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(if (isCompactHeight) 20.dp else 24.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                        )
                                                        Spacer(Modifier.height(4.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(5.dp)
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(Color(0xD910131B))
                                                                .border(0.5.dp, Color(0xFF4EECD2).copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.65f)
                                                                    .fillMaxHeight()
                                                                    .background(Color(0xFF4EECD2))
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        // Text Details
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (tr) style.titleTr else style.titleEn,
                                                color = if (selected) Color(0xFF64D2FF) else Color.White,
                                                fontSize = if (isCompactHeight) 13.5.sp else 14.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = if (tr) style.subtitleTr else style.subtitleEn,
                                                color = Color.White.copy(alpha = 0.60f),
                                                fontSize = if (isCompactHeight) 10.5.sp else 11.5.sp,
                                                lineHeight = if (isCompactHeight) 13.sp else 14.sp
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        // Selection Radio Icon
                                        Icon(
                                            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (selected) Color(0xFF64D2FF) else Color.White.copy(alpha = 0.30f),
                                            modifier = Modifier.size(if (isCompactHeight) 20.dp else 24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
