package com.reproductor.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.reproductor.app.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QueueEntry(val trackId: Long, val title: String, val artist: String)

data class PlayerState(
    val connected: Boolean = false,
    val currentId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = 0,
)

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build(),
    )
    .build()

/** Puente entre la interfaz y [PlaybackService]: expone el estado y las acciones de reproducción. */
class PlayerController(private val context: Context) {
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = ArrayList<(MediaController) -> Unit>()
    private var queueCache: List<QueueEntry> = emptyList()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Se invoca cuando empieza una pista (para el historial de reproducidas recientemente). */
    var onTrackStarted: ((Long) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) rebuildQueue()
            refresh()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.toLongOrNull()?.let { onTrackStarted?.invoke(it) }
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                connecting = false
                val c = runCatching { future.get() }.getOrNull()
                if (c != null) {
                    controller = c
                    c.addListener(listener)
                    rebuildQueue()
                    refresh()
                    pending.toList().forEach { it(c) }
                    pending.clear()
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else { pending += block; connect() }
    }

    private fun rebuildQueue() {
        val c = controller ?: return
        queueCache = List(c.mediaItemCount) { i ->
            val m = c.getMediaItemAt(i)
            QueueEntry(
                m.mediaId.toLongOrNull() ?: -1L,
                m.mediaMetadata.title?.toString().orEmpty(),
                m.mediaMetadata.artist?.toString().orEmpty(),
            )
        }
    }

    private fun refresh() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val duration = c.duration
        _state.value = PlayerState(
            connected = true,
            currentId = item?.mediaId?.toLongOrNull(),
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            album = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
            isPlaying = c.isPlaying,
            durationMs = if (duration == C.TIME_UNSET || duration < 0) 0L else duration,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            queue = queueCache,
            currentIndex = c.currentMediaItemIndex,
        )
    }

    fun positionMs(): Long = controller?.currentPosition ?: 0L

    // ---------- Acciones ----------

    /** Reemplaza la cola por [tracks] y empieza en [startIndex]. */
    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean? = null) = withController { c ->
        if (tracks.isEmpty()) return@withController
        if (shuffle != null) c.shuffleModeEnabled = shuffle
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(0, tracks.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun pause() = withController { it.pause() }
    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    /** Ninguna -> Playlist (toda la cola) -> Canción actual -> Ninguna */
    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Devuelve false si la pista ya estaba en la cola. */
    fun enqueue(track: Track): Boolean {
        if (queueCache.any { it.trackId == track.id }) return false
        withController { c ->
            if (c.mediaItemCount == 0) {
                c.setMediaItems(listOf(track.toMediaItem()))
                c.prepare()
            } else {
                c.addMediaItem(track.toMediaItem())
            }
        }
        return true
    }

    fun playNext(track: Track) = withController { c ->
        if (c.mediaItemCount == 0) {
            c.setMediaItems(listOf(track.toMediaItem()))
            c.prepare()
            c.play()
            return@withController
        }
        val cur = c.currentMediaItemIndex
        val existing = queueCache.indexOfFirst { it.trackId == track.id }
        when {
            existing == cur -> Unit
            existing >= 0 -> c.moveMediaItem(existing, if (existing < cur) cur else cur + 1)
            else -> c.addMediaItem(cur + 1, track.toMediaItem())
        }
    }

    fun removeFromQueue(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun moveQueueItem(from: Int, to: Int) = withController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    fun playQueueIndex(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    fun clearQueue() = withController { c ->
        c.stop()
        c.clearMediaItems()
    }
}
