package com.reproductor.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.reproductor.app.data.GifItem
import com.reproductor.app.data.VideoItem
import com.reproductor.app.ui.components.VolumeSlider
import com.reproductor.app.ui.components.blockTouches

/** Reproductor de video integrado: reproducir/pausar, avanzar/retroceder, barra de progreso y volumen. */
@Composable
fun VideoScreen(video: VideoItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val exo = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }

    // El video no debe seguir sonando cuando la app pasa a segundo plano (eso es cosa de la música).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, exo) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) exo.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize().background(Color.Black).blockTouches().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White) }
            Text(
                video.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium, color = Color.White,
                modifier = Modifier.weight(1f),
            )
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = true
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        VolumeSlider(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), tint = Color.White)
    }
}

/** Visor de GIF animado. */
@Composable
fun GifScreen(gif: GifItem, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Color.Black).blockTouches().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White) }
            Text(
                gif.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium, color = Color.White,
                modifier = Modifier.weight(1f),
            )
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = gif.uri, contentDescription = gif.title,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
