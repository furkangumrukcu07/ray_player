package com.ray.iptv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.ui.theme.LocalGlass

private val ToastBg = Color(0xF0141821)
private val ToastErrorBg = Color(0xF02A0E13)
private val ToastError = Color(0xFFFF5252)

@Composable
fun RayToastHost(
    message: String,
    modifier: Modifier = Modifier
) {
    var last by remember { mutableStateOf("") }
    if (message.isNotBlank()) last = message
    val reduce = LocalGlass.current.reduceEffects
    val ms = if (reduce) 0 else 250
    AnimatedVisibility(
        visible = message.isNotBlank(),
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(ms)),
        exit = fadeOut(animationSpec = tween(ms))
    ) {
        RayGlassToast(text = last)
    }
}

@Composable
fun RayGlassToast(text: String) {
    val g = LocalGlass.current
    val error = toastLooksError(text)
    val accent = if (error) ToastError else g.accent
    val shape = RoundedCornerShape(18.dp)
    val card = Modifier
        .padding(horizontal = 24.dp)
        .widthIn(min = 160.dp, max = 520.dp)
        .then(
            if (g.reduceEffects) Modifier
            else Modifier.shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
        )
        .clip(shape)
        .background(if (error) ToastErrorBg else ToastBg)
        .border(
            1.2.dp,
            if (error) ToastError.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.24f),
            shape
        )
        .padding(horizontal = 20.dp, vertical = 14.dp)
    Row(card, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.20f))
                .border(1.2.dp, accent.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (error) Icons.Filled.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp
        )
    }
}

private fun toastLooksError(msg: String): Boolean {
    val t = msg.lowercase()
    return listOf(
        "hata", "error", "fail", "failed", "başarısız", "basarisiz",
        "could not", "açılamadı", "acilamadi", "geçersiz", "gecersiz", "invalid"
    ).any { it in t }
}
