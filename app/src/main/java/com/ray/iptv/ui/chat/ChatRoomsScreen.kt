package com.ray.iptv.ui.chat

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import com.ray.iptv.data.chat.CommunityChatMessage
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.admin.AdminBackdrop
import com.ray.iptv.ui.admin.AdminGradientButton
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.DarkGlassPopupTheme
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatRoomsScreen(
    vm: RayViewModel,
    tr: Boolean,
    onExit: () -> Unit
) {
    val account by vm.account.collectAsState()
    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val isGoogleSignedIn = account.signedIn || (authUser != null && !authUser.isAnonymous && !authUser.email.isNullOrBlank())
    val currentUid = account.uid.ifBlank { authUser?.uid.orEmpty() }
    val isAdmin = account.isAdmin

    val messages by vm.communityMessages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current

    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var deleteCandidateId by remember { mutableStateOf<String?>(null) }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler(onBack = onExit)

    AdminBackdrop {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .rayClickable(onClick = onExit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF00F0FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Forum,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (tr) "Topluluk Sohbeti" else "Community Chat",
                            color = Color.White,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80))
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = if (tr) "Tüm Ray TV kullanıcılarına açık genel sohbet" else "Public chat open to all Ray TV users",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
            }

            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Message List
            if (messages.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Forum,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (tr) "Henüz mesaj yok. İlk mesajı siz yazın!" else "No messages yet. Be the first to say hi!",
                            color = Color.White.copy(alpha = 0.50f),
                            fontSize = 13.5.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isSelf = currentUid.isNotBlank() && msg.senderUid == currentUid
                        val canDelete = isSelf || isAdmin

                        ChatMessageItem(
                            message = msg,
                            isSelf = isSelf,
                            canDelete = canDelete,
                            onDeleteClick = { deleteCandidateId = msg.id }
                        )
                    }
                }
            }

            // Bottom Input Bar or Sign-in Prompt
            if (isGoogleSignedIn) {
                ChatInputBar(
                    text = messageText,
                    onTextChanged = { if (it.length <= 500) messageText = it },
                    isSending = isSending,
                    tr = tr,
                    onSend = {
                        val trimmed = messageText.trim()
                        if (trimmed.isNotBlank() && !isSending) {
                            isSending = true
                            vm.sendCommunityMessage(trimmed) { success, errorMsg ->
                                isSending = false
                                if (success) {
                                    messageText = ""
                                    focusManager.clearFocus()
                                } else {
                                    android.widget.Toast.makeText(
                                        ctx,
                                        errorMsg ?: (if (tr) "Mesaj gönderilemedi" else "Failed to send message"),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                )
            } else {
                ChatSignInBanner(vm = vm, tr = tr)
            }
        }
    }

    // Delete Confirmation Dialog
    if (deleteCandidateId != null) {
        val targetId = deleteCandidateId!!
        DarkGlassPopupTheme {
            Dialog(onDismissRequest = { deleteCandidateId = null }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF10131B).copy(alpha = 0.98f))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (tr) "Mesajı Sil" else "Delete Message",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (tr) "Bu mesajı sohbetten kalıcı olarak silmek istiyor musunuz?" else "Are you sure you want to permanently delete this message?",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                label = if (tr) "Vazgeç" else "Cancel",
                                modifier = Modifier.weight(1f)
                            ) {
                                deleteCandidateId = null
                            }
                            GlassButton(
                                label = if (tr) "Sil" else "Delete",
                                primary = true,
                                modifier = Modifier.weight(1f)
                            ) {
                                deleteCandidateId = null
                                vm.deleteCommunityMessage(targetId) { ok ->
                                    if (!ok) {
                                        android.widget.Toast.makeText(
                                            ctx,
                                            if (tr) "Mesaj silinemedi" else "Could not delete message",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
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

@Composable
private fun ChatMessageItem(
    message: CommunityChatMessage,
    isSelf: Boolean,
    canDelete: Boolean,
    onDeleteClick: () -> Unit
) {
    val timeFormatted = remember(message.createdAt) {
        if (message.createdAt > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt))
        } else ""
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Other User Avatar
        if (!isSelf) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        if (message.isAdmin) Color(0xFFFF5252)
                        else if (message.isPremium) Color(0xFFF59E0B)
                        else Color.White.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (message.senderPhotoUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(message.senderPhotoUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = (message.senderName.firstOrNull() ?: '?').uppercaseChar().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        // Message Bubble
        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            // Sender Name and Badges (if not self)
            if (!isSelf) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = message.senderName,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (message.isAdmin) {
                        Spacer(Modifier.width(4.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "Admin",
                                color = Color(0xFFFF5252),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (message.isPremium) {
                        Spacer(Modifier.width(4.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "VIP",
                                color = Color(0xFFF59E0B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bubble Box
            Box(
                Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSelf) 16.dp else 4.dp,
                            bottomEnd = if (isSelf) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isSelf) {
                            Brush.linearGradient(
                                listOf(Color(0xFF0F766E), Color(0xFF0284C7))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B).copy(alpha = 0.90f), Color(0xFF0F172A).copy(alpha = 0.95f))
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelf) Color(0xFF38BDF8).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSelf) 16.dp else 4.dp,
                            bottomEnd = if (isSelf) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (timeFormatted.isNotBlank()) {
                            Text(
                                text = timeFormatted,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.5.sp
                            )
                        }
                        if (canDelete) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .rayClickable(onClick = onDeleteClick)
                            )
                        }
                    }
                }
            }
        }

        // Self Avatar
        if (isSelf) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        if (message.isAdmin) Color(0xFFFF5252)
                        else if (message.isPremium) Color(0xFFF59E0B)
                        else Color(0xFF38BDF8).copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (message.senderPhotoUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(message.senderPhotoUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = (message.senderName.firstOrNull() ?: '?').uppercaseChar().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    isSending: Boolean,
    tr: Boolean,
    onSend: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E131F).copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = if (tr) "Bir mesaj yazın…" else "Type a message…",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(10.dp))

            val canSend = text.trim().isNotEmpty() && !isSending
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) {
                            Brush.linearGradient(
                                listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                            )
                        } else {
                            SolidColor(Color.White.copy(alpha = 0.10f))
                        }
                    )
                    .border(
                        1.dp,
                        if (canSend) Color(0xFF67E8F9) else Color.White.copy(alpha = 0.10f),
                        CircleShape
                    )
                    .then(if (canSend) Modifier.rayClickable(onClick = onSend) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatSignInBanner(
    vm: RayViewModel,
    tr: Boolean
) {
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            isSigningIn = true
            vm.handleGoogleSignInIntent(result.data)
            isSigningIn = false
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E131F).copy(alpha = 0.98f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF6366F1).copy(alpha = 0.35f), Color.Transparent)
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (tr) "Sohbete katılmak için oturum açın" else "Sign in to join community chat",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (tr) "Mesaj yazmak ve toplulukla sohbet etmek için Google hesabınızı bağlayın."
                else "Connect your Google account to send messages and chat with the community.",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            AdminGradientButton(
                label = if (isSigningIn) (if (tr) "Giriş Yapılıyor…" else "Signing In…") else (if (tr) "Google ile Giriş Yap" else "Sign In with Google"),
                icon = Icons.Filled.Login,
                loading = isSigningIn,
                colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)),
                onClick = {
                    googleSignInLauncher.launch(vm.getGoogleSignInIntent())
                }
            )
        }
    }
}
