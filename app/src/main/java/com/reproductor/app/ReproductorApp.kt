package com.reproductor.app

import android.app.Application
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import androidx.room.Room
import com.reproductor.app.data.AppDatabase
import com.reproductor.app.data.MIGRATION_1_2
import com.reproductor.app.data.PlayLogEntity
import com.reproductor.app.data.UpdateManager
import com.reproductor.app.data.LibraryRepository
import com.reproductor.app.data.LyricsRepository
import com.reproductor.app.data.RecentEntity
import com.reproductor.app.data.SettingsStore
import com.reproductor.app.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Contenedor de dependencias manual (sin Hilt, para mantener la app ligera). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, "reproductor.db")
        .addMigrations(MIGRATION_1_2)
        .build()
    val dao = db.dao()
    val settings = SettingsStore(appContext)
    val library = LibraryRepository(appContext)
    val lyrics = LyricsRepository(appContext)
    val player = PlayerController(appContext)
    val updates = UpdateManager(appContext)

    init {
        player.onTrackStarted = { id ->
            scope.launch {
                dao.addRecent(RecentEntity(id, System.currentTimeMillis()))
                dao.trimRecents()
            }
        }
        player.onListened = { e ->
            scope.launch {
                dao.insertPlay(
                    PlayLogEntity(
                        trackId = e.trackId, title = e.title, artist = e.artist, album = e.album,
                        playedAt = System.currentTimeMillis(), listenedMs = e.listenedMs, isNewPlay = e.isNewPlay,
                    ),
                )
            }
        }
    }
}

class ReproductorApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Coil con soporte de GIF animados. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }
        .build()
}
