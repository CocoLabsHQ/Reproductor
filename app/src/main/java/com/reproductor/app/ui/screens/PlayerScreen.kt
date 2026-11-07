package com.reproductor.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.reproductor.app.data.GifItem
import com.reproductor.app.data.Track
import com.reproductor.app.data.formatDuration
import com.reproductor.app.domain.LrcLine
import com.reproductor.app.domain.LrcParser
import com.reproductor.app.domain.LyricsState
import com.reproductor.app.playback.PlayerState
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.PlayPauseButton
import com.reproductor.app.ui.components.ReorderableList
import com.reproductor.app.ui.components.TrackArt
import com.reproductor.app.ui.components.VolumeSlider
import com.reproductor.app.ui.components.blockTouches
import com.reproductor.app.ui.components.rememberPositionMs
import com.reproductor.app.ui.theme.Beak
import com.reproductor.app.ui.theme.Mint
import com.reproductor.app.ui.theme.brandBrush

@Composable
fun PlayerScreen(vm: MainViewModel, isDark: Boolean, onClose: () -> Unit) {
    val state by vm.playerState.collectAsStateWithLifecycle()
    val trackMap by vm.trackMap.collectAsStateWithLifecycle()
    val gifMap by vm.gifByBase.collectAsStateWithLifecycle()
    val videoMap by vm.videoByBase.collectAsStateWithLifecycle()
    val track = state.currentId?.let { trackMap[it] }
    val gif = track?.let { gifMap[it.basePath] }
    val video = track?.let { videoMap[it.basePath] }

    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    val position = rememberPositionMs(vm.player, state.isPlaying, state.currentId)
    var dragPos by remember { mutableStateOf<Float?>(null) }
    val duration = if (state.durationMs > 0) state.durationMs else track?.durationMs ?: 0L
    val shownPos = (dragPos ?: position.value.toFloat()).coerceIn(0f, duration.coerceAtLeast(1L).toFloat())

    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize().background(brandBrush(isDark)).blockTouches()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 22.dp)) {
            // Barra superior
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Cerrar", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    "Reproduciendo", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium, color = Color.White,
                )
                Spacer(Modifier.width(48.dp))
            }

            // Portada / letras
            Crossfade(targetState = showLyrics, label = "cover-lyrics", modifier = Modifier.weight(1f).fillMaxWidth()) { lyrics ->
                if (lyrics) LyricsPanel(vm, position) else Cover(track, gif, state.isPlaying)
            }

            // Información de la pista
            Text(
                track?.title ?: state.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium, color = Color.White,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                listOf(track?.artist ?: state.artist, track?.album ?: state.album).filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge, color = Mint,
            )

            // Barra de progreso
            Slider(
                value = shownPos,
                onValueChange = { dragPos = it },
                onValueChangeFinished = {
                    dragPos?.let { vm.player.seekTo(it.toLong()) }
                    dragPos = null
                },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Mint, inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(shownPos.toLong()), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(formatDuration(duration), style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }

            // Controles
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.player.toggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, "Aleatorio", tint = if (state.shuffle) Beak else Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = { vm.player.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(38.dp))
                }
                PlayPauseButton(state.isPlaying, 72.dp, onClick = { vm.player.togglePlayPause() })
                IconButton(onClick = { vm.player.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Siguiente", tint = Color.White, modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { vm.player.cycleRepeat() }) {
                    val one = state.repeatMode == Player.REPEAT_MODE_ONE
                    Icon(
                        if (one) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        when (state.repeatMode) {
                            Player.REPEAT_MODE_ONE -> "Repetir canción"
                            Player.REPEAT_MODE_ALL -> "Repetir playlist"
                            else -> "Sin repetición"
                        },
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) Beak else Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            VolumeSlider(Modifier.fillMaxWidth(), tint = Color.White)

            // Acciones inferiores
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                Pill(if (showLyrics) "Portada" else "Letras", Icons.Rounded.MusicNote, showLyrics) { showLyrics = !showLyrics }
                Pill("Cola (${state.queue.size})", Icons.AutoMirrored.Rounded.QueueMusic, false) { showQueue = true }
                if (video != null) Pill("Video", Icons.Rounded.Videocam, false) { vm.playVideo(video) }
            }
        }
    }

    if (showQueue) QueueSheet(vm, state) { showQueue = false }
}

@Composable
private fun Pill(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape)
            .background(Color.White.copy(alpha = if (selected) 0.38f else 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun Cover(track: Track?, gif: GifItem?, playing: Boolean) {
    val scale by animateFloatAsState(if (playing) 1f else 0.9f, label = "cover-scale")
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val shape = RoundedCornerShape(32.dp)
        val m = Modifier.size(side).graphicsLayer { scaleX = scale; scaleY = scale }
        if (gif != null) {
            // GIF con el mismo nombre que la canción: se muestra animado en lugar de la portada.
            AsyncImage(model = gif.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = m.clip(shape))
        } else {
            TrackArt(track, m, px = 800, shape = shape, iconSize = 96.dp)
        }
    }
}

@Composable
private fun LyricsPanel(vm: MainViewModel, position: State<Long>) {
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    when (val l = lyrics) {
        is LyricsState.None -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "No se encontraron letras para esta canción.\nColoca un archivo .lrc o .txt con el mismo nombre, o elige tu carpeta de letras en Ajustes.",
                color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
            )
        }
        is LyricsState.Plain -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            Text(l.text, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        is LyricsState.Synced -> SyncedLyrics(l.lines, position) { vm.player.seekTo(it) }
    }
}

@Composable
private fun SyncedLyrics(lines: List<LrcLine>, position: State<Long>, onSeek: (Long) -> Unit) {
    val active = LrcParser.activeIndex(lines, position.value)
    val listState = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0))
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 24.dp)) {
        itemsIndexed(lines) { i, line ->
            val alpha by animateFloatAsState(if (i == active) 1f else 0.5f, label = "lrc-alpha")
            Text(
                line.text.ifBlank { "♪" },
                color = Color.White.copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onSeek(line.timeMs) }.padding(vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(vm: MainViewModel, state: PlayerState, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trackMap by vm.trackMap.collectAsStateWithLifecycle()
    val items = remember(state.queue) { state.queue.toMutableStateList() }
    val currentId = state.queue.getOrNull(state.currentIndex)?.trackId

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Cola de reproducción (${items.size})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) {
                TextButton(onClick = { vm.player.clearQueue(); onDismiss() }) { Text("Vaciar") }
            }
        }
        if (items.isEmpty()) {
            Text("La cola está vacía.", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ReorderableList(
                items = items,
                key = { it.trackId },
                onCommit = { from, to -> vm.player.moveQueueItem(from, to) },
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) { q, handle, dragging ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (dragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { vm.player.playQueueIndex(items.indexOf(q)) }
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.DragHandle, "Reordenar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = handle.size(36.dp).padding(6.dp),
                    )
                    TrackArt(trackMap[q.trackId], Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            q.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (q.trackId == currentId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            q.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.player.removeFromQueue(items.indexOf(q)) }) {
                        Icon(Icons.Rounded.Close, "Quitar de la cola")
                    }
                }
            }
        }
    }
}
