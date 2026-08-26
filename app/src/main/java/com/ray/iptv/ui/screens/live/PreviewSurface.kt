package com.ray.iptv.ui.screens.live

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.ray.iptv.player.AndroidPlaybackSocHints
import com.ray.iptv.ui.theme.LocalGlass

@OptIn(UnstableApi::class)
@Composable
fun LivePreviewSurface(url: String, logo: String, title: String, enabled: Boolean) {
    val g = LocalGlass.current
    val usePlayer = enabled && url.isNotBlank()
    Box(Modifier.fillMaxSize().background(g.wallpaperDark)) {
        if (usePlayer) {
            LivePreviewPlayer(url)
        } else if (logo.isNotBlank()) {
            AsyncImage(logo, title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LivePreviewPlayer(url: String) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            volume = 1f
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
        }
    }
    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.release()
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    player.playWhenReady = false
                    player.pause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (url.isNotBlank()) {
                        player.playWhenReady = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    DisposableEffect(url) {
        if (url.isNotBlank()) {
            val playing = player.currentMediaItem?.localConfiguration?.uri?.toString()
            if (playing != url) {
                player.volume = 1f
                player.setMediaItem(MediaItem.fromUri(url), true)
                player.prepare()
            }
            player.playWhenReady = true
        } else {
            player.playWhenReady = false
            player.stop()
            player.clearMediaItems()
        }
        onDispose { }
    }
    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
        modifier = Modifier.fillMaxSize()
    )
}
