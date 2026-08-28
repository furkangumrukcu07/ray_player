package com.ray.iptv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.ray.iptv.net.DataUsageState
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.theme.LocalGlass

@Composable
fun DataUsageScreen(
    tr: Boolean,
    state: DataUsageState,
    onToggleDataSaver: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onBack: () -> Unit
) {
    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            "%.2f GB".format(mb / 1024.0)
        } else {
            "%.1f MB".format(mb)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .rayClickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = if (tr) "Veri Kullanım Takibi" else "Data Usage Tracking",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (tr) "Wi-Fi & Mobil veri tüketimi ve tasarruf modu" else "Wi-Fi & Cellular data metrics and data saver",
                    color = Color(0xFF22D3EE),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Total Consumed Hero Card
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF16382B).copy(alpha = 0.38f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (tr) "Toplam Tüketilen Veri" else "Total Data Consumed",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatBytes(state.totalAllBytes),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (tr) "Bu oturumda: ${formatBytes(state.sessionBytes)}" else "This session: ${formatBytes(state.sessionBytes)}",
                        color = Color(0xFF22D3EE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Wi-Fi vs Mobile Grid
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wi-Fi Box
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16382B).copy(alpha = 0.30f))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wifi, null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Wi-Fi",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = formatBytes(state.totalWifiBytes),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "İndirme: ${formatBytes(state.wifiRxBytes)}",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
            }

            // Mobile / Cellular Box
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16382B).copy(alpha = 0.30f))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SignalCellularAlt, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (tr) "Mobil Veri" else "Cellular",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = formatBytes(state.totalMobileBytes),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "İndirme: ${formatBytes(state.mobileRxBytes)}",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Data Saver Card
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF16382B).copy(alpha = 0.38f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.DataSaverOn,
                        null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (tr) "Mobil Veri Tasarruf Modu" else "Mobile Data Saver",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (tr) "Hücresel bağlantıda otomatik düşük bitrate seçimi" else "Automatically select lower bitrates on mobile data",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 11.5.sp
                        )
                    }
                }
                Switch(
                    checked = state.isDataSaverEnabled,
                    onCheckedChange = onToggleDataSaver,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF22D3EE),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Reset Stats Button
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .rayClickable(onClick = onResetStats)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DeleteSweep,
                    null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (tr) "Veri Sayaçlarını Sıfırla" else "Reset Data Usage Stats",
                    color = Color(0xFFEF4444),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
