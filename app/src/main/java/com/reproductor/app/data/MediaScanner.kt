package com.reproductor.app.data

import android.content.Context
import android.os.Build
import android.provider.MediaStore

/** Lee la biblioteca multimedia del dispositivo mediante MediaStore (sin acceso a "todos los archivos"). */
@Suppress("DEPRECATION")
class MediaScanner(private val context: Context) {

    fun scanAudio(): List<Track> {
        val a = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = mutableListOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TRACK,
        )
        val hasGenre = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        if (hasGenre) cols += MediaStore.Audio.Media.GENRE
        val out = ArrayList<Track>()
        context.contentResolver.query(
            a, cols.toTypedArray(),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0",
            null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iDate = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iGenre = if (hasGenre) c.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
            while (c.moveToNext()) {
                val path = c.getString(iData).orEmpty()
                val title = c.getString(iTitle).orEmpty().ifBlank {
                    path.substringAfterLast('/').substringBeforeLast('.')
                }
                out += Track(
                    id = c.getLong(iId),
                    title = title,
                    artist = clean(c.getString(iArtist), "Artista desconocido"),
                    album = clean(c.getString(iAlbum), "Álbum desconocido"),
                    albumId = c.getLong(iAlbumId),
                    genre = if (iGenre >= 0) c.getString(iGenre)?.trim()?.takeIf { it.isNotEmpty() } else null,
                    durationMs = c.getLong(iDur),
                    dateAdded = c.getLong(iDate),
                    path = path,
                    trackNumber = c.getInt(iTrack) % 1000,
                )
            }
        }
        return out
    }

    fun scanVideos(): List<VideoItem> {
        val out = ArrayList<VideoItem>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media.DATA,
            ),
            null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                out += VideoItem(
                    id = c.getLong(0),
                    title = c.getString(1).orEmpty(),
                    durationMs = c.getLong(2),
                    dateAdded = c.getLong(3),
                    path = c.getString(4).orEmpty(),
                )
            }
        }
        return out
    }

    fun scanGifs(): List<GifItem> {
        val out = ArrayList<GifItem>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.DATA,
            ),
            "${MediaStore.Images.Media.MIME_TYPE} = ?", arrayOf("image/gif"),
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                out += GifItem(
                    id = c.getLong(0),
                    title = c.getString(1).orEmpty().substringBeforeLast('.'),
                    dateAdded = c.getLong(2),
                    path = c.getString(3).orEmpty(),
                )
            }
        }
        return out
    }

    private fun clean(value: String?, fallback: String): String =
        if (value.isNullOrBlank() || value == "<unknown>") fallback else value
}
