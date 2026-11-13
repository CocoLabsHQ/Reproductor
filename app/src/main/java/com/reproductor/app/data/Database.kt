package com.reproductor.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class, parentColumns = ["id"],
        childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("playlistId")],
)
data class PlaylistTrackEntity(val playlistId: Long, val trackId: Long, val position: Int)

/** Canciones que el usuario quitó de la biblioteca. El archivo original NUNCA se toca. */
@Entity(tableName = "hidden_tracks")
data class HiddenTrackEntity(@PrimaryKey val trackId: Long)

@Entity(tableName = "recents")
data class RecentEntity(@PrimaryKey val trackId: Long, val playedAt: Long)

/** Cada fila es un tramo de escucha de una canción. [isNewPlay] marca la primera vez que cuenta como reproducción. */
@Entity(tableName = "play_log", indices = [Index("playedAt")])
data class PlayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val playedAt: Long,
    val listenedMs: Long,
    val isNewPlay: Boolean,
    /** Borrado recuperable: true = oculto en los resúmenes pero aún se puede restaurar. */
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
)

/** Caché de videoclips ya encontrados (evita gastar cuota de la API de YouTube). */
@Entity(tableName = "clip_cache")
data class ClipCacheEntity(@PrimaryKey val searchKey: String, val videoIds: String, val savedAt: Long)

data class SongStat(val trackId: Long, val title: String, val artist: String, val album: String, val plays: Int, val ms: Long)
data class ArtistStat(val name: String, val plays: Int, val ms: Long)
data class Totals(val ms: Long, val plays: Int, val songs: Int)

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `play_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`trackId` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, " +
                "`playedAt` INTEGER NOT NULL, `listenedMs` INTEGER NOT NULL, `isNewPlay` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_log_playedAt` ON `play_log` (`playedAt`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `play_log` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `clip_cache` (`searchKey` TEXT NOT NULL, `videoIds` TEXT NOT NULL, " +
                "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`searchKey`))",
        )
    }
}

@Dao
abstract class AppDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    abstract fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, position")
    abstract fun playlistTracks(): Flow<List<PlaylistTrackEntity>>

    @Insert
    abstract suspend fun insertPlaylist(p: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertPlaylistTracks(items: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :pid AND trackId = :tid")
    abstract suspend fun removePlaylistTrack(pid: Long, tid: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :pid")
    abstract suspend fun maxPosition(pid: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :pid")
    abstract suspend fun clearPlaylistTracks(pid: Long)

    @Transaction
    open suspend fun addTracksToPlaylist(pid: Long, ids: List<Long>) {
        val start = maxPosition(pid) + 1
        insertPlaylistTracks(ids.mapIndexed { i, id -> PlaylistTrackEntity(pid, id, start + i) })
    }

    @Transaction
    open suspend fun setPlaylistOrder(pid: Long, ids: List<Long>) {
        clearPlaylistTracks(pid)
        insertPlaylistTracks(ids.mapIndexed { i, id -> PlaylistTrackEntity(pid, id, i) })
    }

    @Query("SELECT trackId FROM hidden_tracks")
    abstract fun hiddenIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun hide(item: HiddenTrackEntity)

    @Query("DELETE FROM hidden_tracks")
    abstract suspend fun unhideAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addRecent(item: RecentEntity)

    @Query("SELECT trackId FROM recents ORDER BY playedAt DESC LIMIT 40")
    abstract fun recentIds(): Flow<List<Long>>

    @Query("DELETE FROM recents WHERE trackId NOT IN (SELECT trackId FROM recents ORDER BY playedAt DESC LIMIT 100)")
    abstract suspend fun trimRecents()

    // ---------- Registro de escucha (resúmenes) ----------
    @Insert
    abstract suspend fun insertPlay(p: PlayLogEntity)

    @Query("SELECT COUNT(*) FROM play_log WHERE deleted = 0")
    abstract fun logCount(): Flow<Int>

    @Query("SELECT MIN(playedAt) FROM play_log WHERE deleted = 0")
    abstract suspend fun firstPlay(): Long?

    @Query("SELECT trackId, title, artist, album, SUM(isNewPlay) AS plays, SUM(listenedMs) AS ms FROM play_log WHERE deleted = 0 AND playedAt >= :from AND playedAt < :to GROUP BY trackId ORDER BY plays DESC, ms DESC LIMIT :limit")
    abstract suspend fun topSongs(from: Long, to: Long, limit: Int): List<SongStat>

    @Query("SELECT artist AS name, SUM(isNewPlay) AS plays, SUM(listenedMs) AS ms FROM play_log WHERE deleted = 0 AND playedAt >= :from AND playedAt < :to GROUP BY artist ORDER BY ms DESC LIMIT :limit")
    abstract suspend fun topArtists(from: Long, to: Long, limit: Int): List<ArtistStat>

    @Query("SELECT COALESCE(SUM(listenedMs), 0) AS ms, COALESCE(SUM(isNewPlay), 0) AS plays, COUNT(DISTINCT trackId) AS songs FROM play_log WHERE deleted = 0 AND playedAt >= :from AND playedAt < :to")
    abstract suspend fun totals(from: Long, to: Long): Totals

    @Query("SELECT CAST(strftime('%H', playedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS h FROM play_log WHERE deleted = 0 AND playedAt >= :from AND playedAt < :to GROUP BY h ORDER BY SUM(listenedMs) DESC LIMIT 1")
    abstract suspend fun peakHour(from: Long, to: Long): Int?

    @Query("SELECT DISTINCT strftime('%Y-%m', playedAt / 1000, 'unixepoch', 'localtime') FROM play_log WHERE deleted = 0 ORDER BY 1 DESC")
    abstract suspend fun months(): List<String>

    // ---------- Borrar / recuperar datos del resumen ----------
    @Query("UPDATE play_log SET deleted = 1 WHERE deleted = 0")
    abstract suspend fun softDeleteAll(): Int

    @Query("UPDATE play_log SET deleted = 0 WHERE deleted = 1")
    abstract suspend fun restoreAll(): Int

    @Query("DELETE FROM play_log WHERE deleted = 1")
    abstract suspend fun purgeDeleted(): Int

    @Query("SELECT COUNT(*) FROM play_log WHERE deleted = 1")
    abstract fun deletedCount(): Flow<Int>

    // ---------- Caché de videoclips ----------
    @Query("SELECT videoIds FROM clip_cache WHERE searchKey = :key")
    abstract suspend fun cachedClip(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveClip(e: ClipCacheEntity)

    @Query("DELETE FROM clip_cache WHERE searchKey = :key")
    abstract suspend fun forgetClip(key: String)
}

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, HiddenTrackEntity::class, RecentEntity::class, PlayLogEntity::class, ClipCacheEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
