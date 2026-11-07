package com.reproductor.app.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reproductor.app.ReproductorApp
import com.reproductor.app.data.AlbumGroup
import com.reproductor.app.data.ArtistGroup
import com.reproductor.app.data.FolderGroup
import com.reproductor.app.data.GenreGroup
import com.reproductor.app.data.GifItem
import com.reproductor.app.data.HiddenTrackEntity
import com.reproductor.app.data.PlaylistEntity
import com.reproductor.app.data.PlaylistUi
import com.reproductor.app.data.Track
import com.reproductor.app.data.TrackCollection
import com.reproductor.app.data.VideoItem
import com.reproductor.app.data.normalizeText
import com.reproductor.app.domain.LyricsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as ReproductorApp).container
    private val eager = SharingStarted.Eagerly

    val player = c.player
    val playerState = c.player.state
    val scanning = c.library.scanning

    // ---------- Biblioteca ----------
    val allTracks: StateFlow<List<Track>> =
        combine(c.library.tracks, c.dao.hiddenIds()) { tracks, hidden ->
            if (hidden.isEmpty()) tracks else {
                val h = hidden.toHashSet()
                tracks.filter { it.id !in h }
            }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    val trackMap: StateFlow<Map<Long, Track>> =
        allTracks.map { l -> l.associateBy { it.id } }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, eager, emptyMap())

    val videos: StateFlow<List<VideoItem>> = c.library.videos
    val gifs: StateFlow<List<GifItem>> = c.library.gifs
    val hiddenCount: StateFlow<Int> =
        c.dao.hiddenIds().map { it.size }.stateIn(viewModelScope, eager, 0)

    val recents: StateFlow<List<Track>> =
        combine(allTracks, c.dao.recentIds()) { tracks, ids ->
            val m = tracks.associateBy { it.id }
            ids.mapNotNull { m[it] }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    val recentlyAdded: StateFlow<List<Track>> =
        allTracks.map { l -> l.sortedByDescending { it.dateAdded }.take(100) }
            .flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    val artists: StateFlow<List<ArtistGroup>> =
        allTracks.map { l ->
            l.groupBy { it.artist }.map { (k, v) -> ArtistGroup(k, v) }.sortedBy { it.name.lowercase() }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    val albums: StateFlow<List<AlbumGroup>> =
        allTracks.map { l ->
            l.groupBy { it.albumId }.map { (id, v) ->
                AlbumGroup(id, v.first().album, v.first().artist, v.sortedBy { it.trackNumber })
            }.sortedBy { it.name.lowercase() }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    val folders: StateFlow<List<FolderGroup>> =
        allTracks.map { l ->
            l.groupBy { it.folder }.map { (k, v) -> FolderGroup(k, v) }.sortedBy { it.name.lowercase() }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    val genres: StateFlow<List<GenreGroup>> =
        allTracks.map { l ->
            l.filter { it.genre != null }.groupBy { it.genre!! }
                .map { (k, v) -> GenreGroup(k, v) }.sortedBy { it.name.lowercase() }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    /** Videos y GIF con el mismo nombre que una canción: se muestran junto a ella al reproducirla. */
    val gifByBase: StateFlow<Map<String, GifItem>> =
        c.library.gifs.map { l -> l.associateBy { it.basePath } }.stateIn(viewModelScope, eager, emptyMap())
    val videoByBase: StateFlow<Map<String, VideoItem>> =
        c.library.videos.map { l -> l.associateBy { it.basePath } }.stateIn(viewModelScope, eager, emptyMap())

    // ---------- Playlists ----------
    val playlists: StateFlow<List<PlaylistUi>> =
        combine(c.dao.playlists(), c.dao.playlistTracks(), trackMap) { pls, pts, map ->
            val byPlaylist = pts.groupBy { it.playlistId }
            pls.map { p ->
                PlaylistUi(
                    p.id, p.name,
                    byPlaylist[p.id].orEmpty().sortedBy { it.position }.mapNotNull { map[it.trackId] },
                )
            }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    // ---------- Búsqueda ----------
    val searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<Track>> =
        combine(searchQuery.debounce(150), allTracks) { q, tracks ->
            val terms = normalizeText(q).split(' ').filter { it.isNotBlank() }
            if (terms.isEmpty()) emptyList() else tracks.filter { t -> terms.all { it in t.searchKey } }.take(300)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, eager, emptyList())

    // ---------- Letras ----------
    val lyricsTree: StateFlow<String?> = c.settings.lyricsTree.stateIn(viewModelScope, eager, null)
    val lyrics: StateFlow<LyricsState> =
        combine(playerState.map { it.currentId }.distinctUntilChanged(), c.settings.lyricsTree) { id, tree -> id to tree }
            .mapLatest { (id, tree) ->
                val t = id?.let { trackMap.value[it] }
                if (t == null) LyricsState.None else c.lyrics.load(t, tree)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsState.None)

    // ---------- Ajustes ----------
    val themeMode: StateFlow<Int> = c.settings.themeMode.stateIn(viewModelScope, eager, 0)

    // ---------- Estado de navegación ----------
    var playerOpen by mutableStateOf(false)
    var openVideo by mutableStateOf<VideoItem?>(null)
    var openGif by mutableStateOf<GifItem?>(null)
    var libraryTab by mutableIntStateOf(0)
    var collection by mutableStateOf<TrackCollection?>(null)
    var openPlaylistId by mutableStateOf<Long?>(null)
    var trackForPlaylistDialog by mutableStateOf<Track?>(null)
    var trackToHide by mutableStateOf<Track?>(null)

    // ---------- Ciclo de vida ----------
    private var observing = false

    fun onAppStart(permitted: Boolean) {
        player.connect()
        if (permitted) startLibrary()
    }

    /** Se llama cuando el usuario concede el permiso con la app abierta. */
    fun onPermissionGranted() = startLibrary()

    private fun startLibrary() {
        if (!observing) {
            c.library.startObserving()
            observing = true
        }
        c.library.refresh()
    }

    fun onAppStop() {
        if (observing) {
            c.library.stopObserving()
            observing = false
        }
    }

    fun refreshLibrary() = c.library.refresh()

    fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    // ---------- Reproducción ----------
    fun play(tracks: List<Track>, index: Int) = player.playTracks(tracks, index)

    fun shufflePlay(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        player.playTracks(tracks, Random.nextInt(tracks.size), shuffle = true)
        playerOpen = true
    }

    fun enqueue(track: Track) {
        toast(if (player.enqueue(track)) "Añadida a la cola" else "Ya está en la cola")
    }

    fun playNext(track: Track) {
        player.playNext(track)
        toast("Sonará a continuación")
    }

    fun playVideo(video: VideoItem) {
        player.pause()
        openVideo = video
    }

    // ---------- Playlists ----------
    fun createPlaylist(name: String, firstTrack: Track? = null) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = c.dao.insertPlaylist(PlaylistEntity(name = clean, createdAt = System.currentTimeMillis()))
            if (firstTrack != null) c.dao.addTracksToPlaylist(id, listOf(firstTrack.id))
            toast(if (firstTrack != null) "Añadida a «$clean»" else "Playlist creada")
        }
    }

    fun addToPlaylist(playlistId: Long, playlistName: String, track: Track) {
        viewModelScope.launch {
            c.dao.addTracksToPlaylist(playlistId, listOf(track.id))
            toast("Añadida a «$playlistName»")
        }
    }

    fun addAllToPlaylist(playlistId: Long, tracks: List<Track>) {
        viewModelScope.launch { c.dao.addTracksToPlaylist(playlistId, tracks.map { it.id }) }
    }

    fun removeFromPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch { c.dao.removePlaylistTrack(playlistId, track.id) }
    }

    fun renamePlaylist(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) viewModelScope.launch { c.dao.renamePlaylist(id, clean) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { c.dao.deletePlaylist(id) }
    }

    fun savePlaylistOrder(id: Long, trackIds: List<Long>) {
        viewModelScope.launch { c.dao.setPlaylistOrder(id, trackIds) }
    }

    // ---------- Biblioteca: ocultar (nunca borra el archivo) ----------
    fun hide(track: Track) {
        viewModelScope.launch { c.dao.hide(HiddenTrackEntity(track.id)) }
        toast("Quitada de la biblioteca (el archivo no se borra)")
    }

    fun restoreHidden() {
        viewModelScope.launch { c.dao.unhideAll() }
    }

    // ---------- Ajustes ----------
    fun setTheme(mode: Int) {
        viewModelScope.launch { c.settings.setThemeMode(mode) }
    }

    fun setLyricsTree(uri: String?) {
        c.lyrics.invalidate()
        viewModelScope.launch { c.settings.setLyricsTree(uri) }
    }

    fun reindexLyrics() {
        c.lyrics.invalidate()
        viewModelScope.launch {
            // Re-emite el mismo valor para forzar una nueva búsqueda de letras.
            val current = lyricsTree.value
            c.settings.setLyricsTree(null)
            c.settings.setLyricsTree(current)
        }
    }
}
