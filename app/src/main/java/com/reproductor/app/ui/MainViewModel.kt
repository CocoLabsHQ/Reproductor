package com.reproductor.app.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
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
import com.reproductor.app.data.AppearancePrefs
import com.reproductor.app.data.GifItem
import com.reproductor.app.data.UpdateInfo
import com.reproductor.app.data.UpdateUi
import com.reproductor.app.domain.MonthRow
import com.reproductor.app.domain.Recap
import com.reproductor.app.domain.RecapRepository
import com.reproductor.app.domain.YearStatus
import com.reproductor.app.data.HiddenTrackEntity
import com.reproductor.app.data.PlaylistEntity
import com.reproductor.app.data.PlaylistUi
import com.reproductor.app.data.Track
import com.reproductor.app.data.TrackCollection
import com.reproductor.app.data.VideoItem
import com.reproductor.app.data.formatDuration
import com.reproductor.app.data.normalizeText
import com.reproductor.app.ui.components.SelectionState
import com.reproductor.app.domain.LyricsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import kotlin.math.max
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
    val hideWhatsApp: StateFlow<Boolean> = c.settings.hideWhatsApp.stateIn(viewModelScope, eager, false)
    val whatsAppCount: StateFlow<Int> =
        c.library.tracks.map { l -> l.count { it.isWhatsApp } }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, eager, 0)

    val allTracks: StateFlow<List<Track>> =
        combine(c.library.tracks, c.dao.hiddenIds(), hideWhatsApp) { tracks, hidden, hideWa ->
            val h = hidden.toHashSet()
            if (h.isEmpty() && !hideWa) tracks
            else tracks.filter { it.id !in h && !(hideWa && it.isWhatsApp) }
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

    // ---------- Apariencia ----------
    val appearance: StateFlow<AppearancePrefs> =
        c.settings.appearance.stateIn(viewModelScope, eager, AppearancePrefs())

    fun setThemeId(id: String) { viewModelScope.launch { c.settings.setThemeId(id) } }
    fun setAccent(argb: Int) { viewModelScope.launch { c.settings.setAccent(argb) } }
    fun setTransparency(v: Float) { viewModelScope.launch { c.settings.setTransparency(v) } }
    fun setBlur(v: Float) { viewModelScope.launch { c.settings.setBlur(v) } }
    fun setHideWhatsApp(v: Boolean) { viewModelScope.launch { c.settings.setHideWhatsApp(v) } }
    val autoUpdate: StateFlow<Boolean> = c.settings.autoUpdate.stateIn(viewModelScope, eager, true)
    fun setAutoUpdate(v: Boolean) { viewModelScope.launch { c.settings.setAutoUpdate(v) } }

    /** Copia la imagen elegida de la galería a la carpeta privada de la app (reducida) y activa el tema "Mi imagen". */
    fun setCustomImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val ctx = getApplication<Application>()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
                var bmp = ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: error("No se pudo leer la imagen")
                val orientation = ctx.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                val degrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (degrees != 0f) {
                    val m = Matrix().apply { postRotate(degrees) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                }
                val file = File(ctx.filesDir, "custom_bg.jpg")
                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                c.settings.setCustomImage(file.absolutePath, appearance.value.imageVersion + 1)
                c.settings.setThemeId("custom")
            }
            if (result.isFailure) withContext(Dispatchers.Main) { toast("No se pudo usar esa imagen") }
        }
    }

    // ---------- Resúmenes (mensual y anual) ----------
    private val recapRepo = RecapRepository(c.dao)
    val currentRecap: StateFlow<Recap?> =
        c.dao.logCount().mapLatest { recapRepo.month(YearMonth.now()) }.stateIn(viewModelScope, eager, null)
    val yearStatus: StateFlow<YearStatus> =
        c.dao.logCount().mapLatest { recapRepo.yearStatus() }.stateIn(viewModelScope, eager, YearStatus())
    val monthRows: StateFlow<List<MonthRow>> =
        c.dao.logCount().mapLatest { recapRepo.monthRows() }.stateIn(viewModelScope, eager, emptyList())

    var wrapped by mutableStateOf<Recap?>(null)
    var clipQuery by mutableStateOf<String?>(null)

    fun openMonthRecap(ym: YearMonth) { viewModelScope.launch { wrapped = recapRepo.month(ym) } }
    fun openCurrentRecap() = openMonthRecap(YearMonth.now())
    fun openYearRecap() { viewModelScope.launch { wrapped = recapRepo.year() } }

    /** Busca el videoclip dentro de la app. Usa Internet solo en este momento. */
    fun openClip(artist: String, title: String) {
        player.pause()
        clipQuery = "$artist $title official video"
    }

    // ---------- Actualizaciones ----------
    var updateUi by mutableStateOf<UpdateUi>(UpdateUi.Hidden)
    var whatsNew by mutableStateOf<String?>(null)
    val currentVersion: String get() = c.updates.currentVersion()

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            val info = c.updates.check()
            if (info != null) updateUi = UpdateUi.Available(info)
            else if (manual) toast("Ya tienes la última versión (o no hay conexión)")
        }
    }

    fun dismissUpdate() { updateUi = UpdateUi.Hidden }

    fun startUpdate(info: UpdateInfo) {
        updateUi = UpdateUi.Downloading(info, 0)
        viewModelScope.launch {
            val id = runCatching { c.updates.enqueue(info) }.getOrNull()
            if (id == null) { updateUi = UpdateUi.Error("No se pudo iniciar la descarga"); return@launch }
            while (true) {
                val p = c.updates.progress(id)
                if (p.status == android.app.DownloadManager.STATUS_SUCCESSFUL) { installUpdate(info); return@launch }
                if (p.status == android.app.DownloadManager.STATUS_FAILED) {
                    updateUi = UpdateUi.Error("La descarga falló. Revisa tu conexión e inténtalo de nuevo.")
                    return@launch
                }
                updateUi = UpdateUi.Downloading(info, p.percent)
                delay(500)
            }
        }
    }

    fun installUpdate(info: UpdateInfo) {
        if (!c.updates.canInstall()) {
            updateUi = UpdateUi.NeedsPermission(info)
        } else {
            runCatching { getApplication<Application>().startActivity(c.updates.installIntent(info)) }
            updateUi = UpdateUi.Hidden
        }
    }

    fun openInstallPermission() {
        runCatching { getApplication<Application>().startActivity(c.updates.permissionIntent()) }
    }

    fun dismissWhatsNew() {
        whatsNew = null
        viewModelScope.launch { c.settings.setLastSeenVersion(currentVersion) }
    }

    init {
        viewModelScope.launch {
            // "Novedades" tras actualizar: se muestran si la versión cambió desde la última vez.
            val last = c.settings.lastSeenVersion.first()
            val cur = currentVersion
            if (last == null) c.settings.setLastSeenVersion(cur)
            else if (last != cur) whatsNew = c.updates.bundledNotes().ifBlank { null }
            if (c.settings.autoUpdate.first()) checkForUpdates()
        }
    }

    // ---------- Diagnóstico de reproducción ----------
    val lastError = MutableStateFlow<String?>(null)

    /** Cierre inesperado de la última vez (si lo hubo): se ofrece enviarlo, nunca se envía solo. */
    var pendingCrash by mutableStateOf<String?>(c.reporter.pendingCrash())

    /** Aviso en pantalla de un error de reproducción (con opción de reportarlo). */
    var errorPrompt by mutableStateOf<String?>(null)

    fun reportIssue(title: String, details: String) {
        runCatching { getApplication<Application>().startActivity(c.reporter.issueIntent(title, details)) }
            .onFailure { toast("No se pudo abrir el navegador") }
    }

    fun dismissCrash(report: Boolean) {
        val text = pendingCrash
        if (report && text != null) reportIssue("Cierre inesperado", text)
        c.reporter.clearCrash()
        pendingCrash = null
    }

    init {
        player.onError = { msg ->
            val t = playerState.value.currentId?.let { trackMap.value[it] }
            lastError.value = msg + (t?.let {
                "\nArchivo: ${it.path}\nTipo: ${it.mime.ifBlank { "?" }} · ${formatDuration(it.durationMs)}"
            }.orEmpty())
            toast(msg, long = true)
            if (errorPrompt == null) errorPrompt = lastError.value
        }
    }

    // ---------- Estado de navegación ----------
    var playerOpen by mutableStateOf(false)
    var openVideo by mutableStateOf<VideoItem?>(null)
    var openGif by mutableStateOf<GifItem?>(null)
    var libraryTab by mutableIntStateOf(0)
    var collection by mutableStateOf<TrackCollection?>(null)
    var openPlaylistId by mutableStateOf<Long?>(null)
    val selection = SelectionState()
    var tracksForPlaylistDialog by mutableStateOf<List<Track>?>(null)
    var tracksToHide by mutableStateOf<List<Track>?>(null)

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
        errorPrompt = null
        if (observing) {
            c.library.stopObserving()
            observing = false
        }
    }

    fun refreshLibrary() = c.library.refresh()

    fun toast(message: String, long: Boolean = false) {
        Toast.makeText(getApplication(), message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
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
    fun createPlaylist(name: String, tracks: List<Track> = emptyList()) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = c.dao.insertPlaylist(PlaylistEntity(name = clean, createdAt = System.currentTimeMillis()))
            if (tracks.isNotEmpty()) c.dao.addTracksToPlaylist(id, tracks.map { it.id })
            toast(
                when (tracks.size) {
                    0 -> "Playlist creada"
                    1 -> "Añadida a «$clean»"
                    else -> "${tracks.size} canciones añadidas a «$clean»"
                },
            )
            selection.clear()
        }
    }

    /** Añade una o varias canciones (selección múltiple) a una playlist, ignorando las que ya estaban. */
    fun addToPlaylist(playlistId: Long, playlistName: String, tracks: List<Track>) {
        val existing = playlists.value.firstOrNull { it.id == playlistId }?.tracks?.map { it.id }?.toHashSet().orEmpty()
        val fresh = tracks.filter { it.id !in existing }
        viewModelScope.launch {
            if (fresh.isNotEmpty()) c.dao.addTracksToPlaylist(playlistId, fresh.map { it.id })
            toast(
                when (fresh.size) {
                    0 -> "Ya estaban en «$playlistName»"
                    1 -> "Añadida a «$playlistName»"
                    else -> "${fresh.size} canciones añadidas a «$playlistName»"
                },
            )
            selection.clear()
        }
    }

    fun enqueueAll(tracks: List<Track>) {
        val added = player.enqueueAll(tracks)
        toast(if (added == 0) "Ya estaban en la cola" else "$added canciones añadidas a la cola")
        selection.clear()
    }

    fun removeFromPlaylist(playlistId: Long, tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { c.dao.removePlaylistTrack(playlistId, it.id) } }
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
    fun hide(tracks: List<Track>) {
        viewModelScope.launch { tracks.forEach { c.dao.hide(HiddenTrackEntity(it.id)) } }
        toast(
            if (tracks.size == 1) "Quitada de la biblioteca (el archivo no se borra)"
            else "${tracks.size} canciones quitadas (los archivos no se borran)",
        )
        selection.clear()
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
