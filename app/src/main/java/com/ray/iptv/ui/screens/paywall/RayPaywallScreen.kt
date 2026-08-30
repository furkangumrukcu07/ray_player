package com.ray.iptv.ui.screens.paywall

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.repo.LicensingState
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.launch

private val GoldAccent = Color(0xFFFFD700)
private val CyanAccent = Color(0xFF18FFFF)
private val NeonEmerald = Color(0xFF10B981)

@Composable
fun RayPaywallScreen(
    licensingState: LicensingState,
    copy: Copy,
    onRedeemCode: suspend (String) -> Result<String>,
    onBuyPlayStore: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    var showRedeemDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617), Color.Black),
                    radius = 1600f
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header: Premium Crown & Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.15f))
                        .border(1.5.dp, GoldAccent.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Ray TV Premium",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                    Text(
                        "Ömür Boyu Kesintisiz Yayın Deneyimi",
                        color = g.muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Trial Status Pill / Banner
            GlassPanel(
                strong = true,
                radius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        tint = if (licensingState.isTrialActive) CyanAccent else Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (licensingState.isPremium) "Ömür Boyu Lisans Aktif"
                            else if (licensingState.isTrialActive) "4 Günlük Ücretsiz Deneme Sürümü Aktif"
                            else "Deneme Süreniz Sona Erdi",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                        if (!licensingState.isPremium && licensingState.isTrialActive) {
                            Text(
                                "Kalan Süre: ${licensingState.trialRemainingFormatted}",
                                color = CyanAccent,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (licensingState.isPremium || licensingState.isTrialActive) NeonEmerald.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (licensingState.isPremium) "PREMIUM"
                            else if (licensingState.isTrialActive) "TRIAL"
                            else "EXPIRED",
                            color = if (licensingState.isPremium || licensingState.isTrialActive) NeonEmerald else Color(0xFFEF4444),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 4 Key Value Highlights Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Filled.LiveTv,
                        title = "Kesintisiz 4K & Canlı TV",
                        desc = "Media3 & C++ libmpv hibrit oynatıcı ile anlık zap",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Filled.Movie,
                        title = "TMDB Sinema & Dizi Arşivi",
                        desc = "Oyuncu kadrosu, Türkçe özetler ve IMDb puanları",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Filled.CloudDone,
                        title = "Bulut Yedekleme",
                        desc = "Favorilerinizi ve ayarlarınızı bulutta güvenle saklayın",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Filled.Devices,
                        title = "3 Cihazda Eşzamanlı Kullanım",
                        desc = "TV Box, Android Telefon ve Tablet desteği",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Buy button
                GlassPanel(
                    focused = false,
                    strong = true,
                    radius = 14.dp,
                    onClick = onBuyPlayStore,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Google Play ile Satın Al",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Redeem Code button
                GlassPanel(
                    focused = false,
                    strong = false,
                    radius = 14.dp,
                    onClick = { showRedeemDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Lisans Kodu Etkinleştir",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // If not strictly enforced or still in trial, show close / continue
            if (!licensingState.isEnforced || licensingState.isTrialActive || licensingState.isPremium) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Şimdilik Kapat ve Devam Et",
                    color = g.muted,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (showRedeemDialog) {
        RedeemLicenseDialog(
            onDismiss = { showRedeemDialog = false },
            onRedeem = { code ->
                onRedeemCode(code)
            }
        )
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    GlassPanel(
        radius = 12.dp,
        modifier = modifier.height(68.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                )
                Text(
                    desc,
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RedeemLicenseDialog(
    onDismiss: () -> Unit,
    onRedeem: suspend (String) -> Result<String>
) {
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val g = LocalGlass.current

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            strong = true,
            radius = 20.dp,
            modifier = Modifier
                .width(420.dp)
                .padding(16.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Key,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Lisans Kodu Girişi",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Satın aldığınız lisans anahtarını girin",
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (code.isNotBlank()) CyanAccent else Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (code.isEmpty()) {
                        Text("Örn: RAY-XXXX-XXXX-XXXX", color = g.muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    BasicTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        message.orEmpty(),
                        color = if (isSuccess) NeonEmerald else Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassPanel(
                        radius = 10.dp,
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (isSuccess) "Tamam" else "İptal", color = g.muted)
                        }
                    }

                    if (!isSuccess) {
                        GlassPanel(
                            strong = true,
                            radius = 10.dp,
                            onClick = {
                                if (code.isNotBlank() && !loading) {
                                    loading = true
                                    scope.launch {
                                        val res = onRedeem(code)
                                        loading = false
                                        if (res.isSuccess) {
                                            isSuccess = true
                                            message = res.getOrNull()
                                        } else {
                                            isSuccess = false
                                            message = res.exceptionOrNull()?.message ?: "Hata oluştu"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (loading) {
                                    PaywallSpinner(CyanAccent)
                                } else {
                                    Text("Etkinleştir", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallSpinner(color: Color, modifier: Modifier = Modifier) {
    val spin = rememberInfiniteTransition(label = "paywall-spin")
    val deg by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "paywall-deg"
    )
    Canvas(
        modifier
            .size(20.dp)
            .rotate(deg)
    ) {
        drawArc(
            color = color,
            startAngle = 16f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
