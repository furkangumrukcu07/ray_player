package com.ray.iptv.player

import android.content.Context
import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.ray.iptv.data.repo.AspectMode

/** libmpv yüzey — [RayPlayer] MediaKit oturumuna bağlanır. */
class MediaKitSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    var player: RayPlayer? = null
        set(value) {
            field = value
            if (holder.surface.isValid) value?.attachMpvSurface(holder)
        }

    init {
        holder.setFormat(PixelFormat.RGBA_8888)
        holder.addCallback(this)
        keepScreenOn = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        player?.attachMpvSurface(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        player?.resizeMpvSurface(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        player?.detachMpvSurface()
    }
}

/** SurfaceView'ı FrameLayout içine alır — Compose AndroidView kökünde ham SurfaceView siyah kalır. */
class MediaKitSurfaceHost(context: Context) : FrameLayout(context) {
    private val surfaceView = MediaKitSurfaceView(context)

    var player: RayPlayer?
        get() = surfaceView.player
        set(value) { surfaceView.player = value }

    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        addView(
            surfaceView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }
}

/**
 * Telefon dikey oynatıcı: TextureView tabanlı yüzey.
 * SurfaceView, Compose animasyonu / siyah panel arkasında ses açıp görüntüyü yutar.
 */
@Composable
fun MediaKitComposeSurface(
    player: RayPlayer,
    aspect: AspectMode,
    modifier: Modifier = Modifier
) {
    val fill = aspect == AspectMode.FILL || aspect == AspectMode.STRETCH
    val zoom = aspect == AspectMode.ZOOM
    LaunchedEffect(fill, zoom) {
        player.applyVideoLayout(fill = fill, zoom = zoom)
    }
    AndroidEmbeddedExternalSurface(modifier = modifier, isOpaque = true) {
        onSurface { surface, width, height ->
            player.attachMpvSurface(surface, width, height, preferCopyHwdec = true)
            player.applyVideoLayout(fill = fill, zoom = zoom)
            surface.onChanged { w, h -> player.resizeMpvSurface(w, h) }
            surface.onDestroyed { player.detachMpvSurface() }
        }
    }
}
