package com.reproductor.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.reproductor.app.data.ArtworkLoader
import com.reproductor.app.data.Track
import com.reproductor.app.data.VideoItem
import com.reproductor.app.data.formatDuration
import com.reproductor.app.playback.PlayerController
import com.reproductor.app.playback.PlayerState
import com.reproductor.app.ui.theme.LocalResolvedTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Acciones comunes del menú de una canción; se proveen una sola vez en la raíz de la app. */
class TrackActions(
    val playNext: (Track) -> Unit,
    val enqueue: (Track) -> Unit,
    val addToPlaylist: (Track) -> Unit,
    val hide: (Track) -> Unit,
)

val LocalTrackActions = staticCompositionLocalOf<TrackActions> { error("TrackActions no proporcionado") }

@Composable
fun TrackArt(
    track: Track?,
    modifier: Modifier = Modifier,
    px: Int = 256,
    shape: Shape = RoundedCornerShape(14.dp),
    iconSize: Dp = 24.dp,
) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = null, track?.id, px) {
        value = if (track != null) ArtworkLoader.trackArt(context, track, px) else null
    }
    ArtBox(bmp, modifier, shape, iconSize)
}

@Composable
fun VideoThumb(video: VideoItem, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(14.dp)) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = null, video.id) {
        value = ArtworkLoader.videoThumb(context, video, 256)
    }
    ArtBox(bmp, modifier, shape, 24.dp, Icons.Rounded.Videocam)
}

@Composable
private fun ArtBox(
    bmp: Bitmap?,
    modifier: Modifier,
    shape: Shape,
    iconSize: Dp,
    placeholder: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.MusicNote,
) {
    val theme = LocalResolvedTheme.current
    val brush = remember(theme) { theme.placeholderBrush() }
    Box(modifier.clip(shape).background(brush), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(placeholder, contentDescription = null, tint = theme.onAccent.copy(alpha = 0.9f), modifier = Modifier.size(iconSize))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList(),
) {
    val actions = LocalTrackActions.current
    val sel = LocalSelection.current
    val haptic = LocalHapticFeedback.current
    val selected = sel.isSelected(track.id)
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (sel.active) sel.toggle(track) else onClick() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sel.toggle(track)
                },
            )
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sel.active) {
            Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
        } else {
            leading?.invoke()
        }
        TrackArt(track, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${track.artist} · ${track.album}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatDuration(track.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (sel.active) Spacer(Modifier.width(12.dp)) else Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Reproducir a continuación") }, onClick = { menu = false; actions.playNext(track) })
                DropdownMenuItem(text = { Text("Añadir a la cola") }, onClick = { menu = false; actions.enqueue(track) })
                DropdownMenuItem(text = { Text("Añadir a una playlist") }, onClick = { menu = false; actions.addToPlaylist(track) })
                extraMenu.forEach { (label, action) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                }
                DropdownMenuItem(text = { Text("Quitar de la biblioteca") }, onClick = { menu = false; actions.hide(track) })
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun PlayShuffleButtons(onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onPlay, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.PlayArrow, null)
            Spacer(Modifier.width(6.dp))
            Text("Reproducir")
        }
        FilledTonalButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Shuffle, null)
            Spacer(Modifier.width(6.dp))
            Text("Aleatorio")
        }
    }
}

@Composable
fun PlayPauseButton(isPlaying: Boolean, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = isPlaying, label = "playpause") { playing ->
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Pausar" else "Reproducir",
                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

/** Posición actual de reproducción. Solo consulta mientras la pantalla que la usa está visible. */
@Composable
fun rememberPositionMs(player: PlayerController, playing: Boolean, key: Any?): State<Long> {
    val pos = remember { mutableStateOf(0L) }
    LaunchedEffect(playing, key) {
        do {
            pos.value = player.positionMs()
            delay(400)
        } while (playing)
    }
    return pos
}

@Composable
fun MiniPlayer(state: PlayerState, track: Track?, player: PlayerController, onOpen: () -> Unit) {
    val pos by rememberPositionMs(player, state.isPlaying, state.currentId)
    val dur = if (state.durationMs > 0) state.durationMs else track?.durationMs ?: 0L
    val progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth(),
    ) {
        Column {
            Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TrackArt(track, Modifier.size(44.dp), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.title.ifBlank { "Music Box" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
                PlayPauseButton(state.isPlaying, 42.dp, onClick = { player.togglePlayPause() })
                IconButton(onClick = { player.next() }) {
                    Icon(Icons.Rounded.SkipNext, "Siguiente", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
    }
}

/** Control del volumen del sistema (flujo multimedia). */
@Composable
fun VolumeSlider(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    val context = LocalContext.current
    val am = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var touching by remember { mutableStateOf(false) }
    // Sincroniza con los botones físicos de volumen mientras el control está visible.
    LaunchedEffect(Unit) {
        while (true) {
            if (!touching) volume = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            delay(700)
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.VolumeDown, null, tint = tint)
        Slider(
            value = volume,
            onValueChange = {
                touching = true
                volume = it
                runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0) }
            },
            onValueChangeFinished = { touching = false },
            valueRange = 0f..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = tint, activeTrackColor = tint, inactiveTrackColor = tint.copy(alpha = 0.3f),
            ),
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Rounded.VolumeUp, null, tint = tint)
    }
}

/**
 * Lista con reordenación manual: arrastra desde el asa (el modificador `handle`).
 * Trabaja sobre una copia local [items]; al soltar llama a [onCommit] con el origen y destino finales.
 */
@Composable
fun <T> ReorderableList(
    items: SnapshotStateList<T>,
    key: (T) -> Any,
    onCommit: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (item: T, handle: Modifier, dragging: Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    var dragging by remember { mutableStateOf<Int?>(null) }
    var startIndex by remember { mutableIntStateOf(-1) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val commit by rememberUpdatedState(onCommit)

    val finish = {
        val from = startIndex
        val to = dragging
        dragging = null
        offsetY = 0f
        startIndex = -1
        if (from >= 0 && to != null && from != to) commit(from, to)
    }

    LazyColumn(state = listState, modifier = modifier, contentPadding = contentPadding) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            val isDragging = dragging == index
            val currentIndex by rememberUpdatedState(index)
            val handle = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        startIndex = currentIndex
                        dragging = currentIndex
                        offsetY = 0f
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val cur = dragging
                        if (cur != null) {
                            offsetY += amount.y
                            val visible = listState.layoutInfo.visibleItemsInfo
                            val me = visible.firstOrNull { it.index == cur }
                            if (me != null) {
                                val center = me.offset + me.size / 2f + offsetY
                                val target = visible.firstOrNull {
                                    it.index != cur && center >= it.offset && center < it.offset + it.size
                                }
                                if (target != null) {
                                    items.add(target.index, items.removeAt(cur))
                                    offsetY += (me.offset - target.offset).toFloat()
                                    dragging = target.index
                                }
                            }
                        }
                    },
                    onDragEnd = { finish() },
                    onDragCancel = { finish() },
                )
            }
            Box(
                Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .then(
                        if (isDragging) Modifier.graphicsLayer { translationY = offsetY }
                        else Modifier.animateItem()
                    ),
            ) {
                itemContent(item, handle, isDragging)
            }
        }
    }
}

/** Evita que los toques atraviesen una pantalla superpuesta (reproductor, video, GIF). */
fun Modifier.blockTouches(): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
}
