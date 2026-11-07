package com.reproductor.app.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Mantiene en memoria el contenido de MediaStore y lo refresca cuando cambia (solo con la app abierta). */
class LibraryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scanner = MediaScanner(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _gifs = MutableStateFlow<List<GifItem>>(emptyList())
    private val _scanning = MutableStateFlow(false)

    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    val gifs: StateFlow<List<GifItem>> = _gifs.asStateFlow()
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    fun refresh() {
        job?.cancel()
        job = scope.launch {
            _scanning.value = true
            try {
                // Cada consulta falla por separado si falta el permiso correspondiente.
                _tracks.value = runCatching { scanner.scanAudio() }.getOrDefault(emptyList())
                _videos.value = runCatching { scanner.scanVideos() }.getOrDefault(emptyList())
                _gifs.value = runCatching { scanner.scanGifs() }.getOrDefault(emptyList())
            } finally {
                _scanning.value = false
            }
        }
    }

    private val debounced = Runnable { refresh() }
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            handler.removeCallbacks(debounced)
            handler.postDelayed(debounced, 1500)
        }
    }

    fun startObserving() {
        val r = appContext.contentResolver
        r.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        r.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        r.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
    }

    fun stopObserving() {
        handler.removeCallbacks(debounced)
        appContext.contentResolver.unregisterContentObserver(observer)
    }
}
