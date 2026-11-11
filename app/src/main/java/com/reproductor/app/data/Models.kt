package com.reproductor.app.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import java.text.Normalizer

private val diacritics = Regex("\\p{Mn}+")
private val waFileName = Regex("^(AUD|PTT|VN)-\\d{8}-WA\\d+", RegexOption.IGNORE_CASE)

/** Minúsculas y sin acentos, para búsquedas tolerantes. */
fun normalizeText(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(diacritics, "")

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String?,
    val durationMs: Long,
    val dateAdded: Long,
    val path: String,
    val trackNumber: Int,
    val mime: String = "",
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val albumArtUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    val folder: String get() = path.substringBeforeLast('/', "")
    val fileName: String get() = path.substringAfterLast('/')

    /** Ruta sin extensión, en minúsculas: sirve para emparejar con videos/GIF del mismo nombre. */
    val basePath: String get() = path.substringBeforeLast('.', path).lowercase()
    /** Audio de WhatsApp: por carpeta (WhatsApp Audio, WhatsApp Voice Notes, com.whatsapp...) o por nombre (AUD-20240101-WA0001). */
    val isWhatsApp: Boolean =
        path.contains("whatsapp", ignoreCase = true) || waFileName.containsMatchIn(path.substringAfterLast('/'))
    val searchKey: String =
        normalizeText("$title $artist $album ${genre.orEmpty()} ${path.substringAfterLast('/')} ${path.substringBeforeLast('/', "").substringAfterLast('/')}")
}

data class VideoItem(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val dateAdded: Long,
    val path: String,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
    val basePath: String get() = path.substringBeforeLast('.', path).lowercase()
}

data class GifItem(
    val id: Long,
    val title: String,
    val dateAdded: Long,
    val path: String,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
    val basePath: String get() = path.substringBeforeLast('.', path).lowercase()
}

data class ArtistGroup(val name: String, val tracks: List<Track>)
data class AlbumGroup(val id: Long, val name: String, val artist: String, val tracks: List<Track>)
data class FolderGroup(val path: String, val tracks: List<Track>) {
    val name: String get() = path.substringAfterLast('/').ifBlank { path }
}
data class GenreGroup(val name: String, val tracks: List<Track>)

/** Lista de pistas que se abre en una pantalla de detalle (artista, álbum, carpeta...). */
data class TrackCollection(val title: String, val subtitle: String, val tracks: List<Track>)

data class PlaylistUi(val id: Long, val name: String, val tracks: List<Track>)
