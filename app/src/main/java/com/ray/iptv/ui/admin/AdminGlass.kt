package com.ray.iptv.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.compose.foundation.layout.offset
import com.ray.iptv.ui.input.rayClickable

internal val AdminViolet = Color(0xFF2E0942)
internal val AdminBlue = Color(0xFF0F1E4C)
internal val AdminMagenta = Color(0xFF5A1A45)
internal val AdminDeep = Color(0xFF0F172A)
internal val AdminCard = Color(0x14FFFFFF)
internal val AdminStroke = Color(0x26FFFFFF)

@Composable
fun AdminBackdrop(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-40).dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(Color(0xFF00F0FF).copy(alpha = 0.08f))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 60.dp)
                .size(360.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF0055).copy(alpha = 0.08f))
        )
        Box(Modifier.fillMaxSize(), content = { content() })
    }
}

@Composable
fun AdminTopBar(title: String, subtitle: String = "", onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            null,
            tint = Color.White,
            modifier = Modifier.size(42.dp).padding(8.dp).rayClickable(onBack)
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
            }
        }
        trailing()
    }
}

@Composable
fun AdminGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.rayClickable(onClick) else Modifier)
            .background(AdminCard, shape)
            .border(1.dp, AdminStroke, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun AdminDarkField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    hint: String,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0xFF1E293B), shape)
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) {
                Text(hint, color = Color.White.copy(alpha = 0.32f), fontSize = 14.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color(0xFF6366F1)),
                singleLine = singleLine,
                minLines = minLines,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AdminGradientButton(
    label: String,
    icon: ImageVector,
    loading: Boolean,
    colors: List<Color>,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(colors), shape)
            .rayClickable(onClick = { if (!loading) onClick() })
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (loading) "…" else label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun AdminChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
    Text(
        label,
        color = if (selected) color else Color.White.copy(alpha = 0.54f),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (selected) color else Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .rayClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

@Composable
fun AdminInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = Color.White.copy(alpha = 0.54f), fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(value.ifBlank { "—" }, color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun AdminEmpty(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White.copy(alpha = 0.45f), fontSize = 16.sp)
    }
}
