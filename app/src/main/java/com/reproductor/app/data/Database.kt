package com.reproductor.app.data

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
}

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, HiddenTrackEntity::class, RecentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
