package com.reproductor.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga portadas de audio y miniaturas de video con una caché pequeña en memoria. */
@Suppress("DEPRECATION")
object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val missing = HashSet<String>()

    suspend fun trackArt(context: Context, track: Track, px: Int): Bitmap? {
        val key = if (track.albumId > 0) "a${track.albumId}_$px" else "t${track.id}_$px"
        cache.get(key)?.let { return it }
        if (synchronized(missing) { key in missing }) return null
        val bmp = withContext(Dispatchers.IO) { runCatching { loadAudio(context, track, px) }.getOrNull() }
        if (bmp != null) cache.put(key, bmp) else synchronized(missing) { missing += key }
        return bmp
    }

    suspend fun videoThumb(context: Context, video: VideoItem, px: Int): Bitmap? {
        val key = "v${video.id}_$px"
        cache.get(key)?.let { return it }
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(video.uri, Size(px, px), null)
                } else {
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver, video.id, MediaStore.Video.Thumbnails.MINI_KIND, null,
                    )
                }
            }.getOrNull()
        }
        if (bmp != null) cache.put(key, bmp)
        return bmp
    }

    private fun loadAudio(context: Context, track: Track, px: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.contentResolver.loadThumbnail(track.uri, Size(px, px), null)
        }
        // Android 8 a 9: portada incrustada y, si no hay, la de MediaStore.
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, track.uri)
            val bytes = mmr.embeddedPicture
            if (bytes != null) decode(bytes, px)?.let { return it }
        } finally {
            runCatching { mmr.release() }
        }
        return context.contentResolver.openInputStream(track.albumArtUri)?.use { BitmapFactory.decodeStream(it) }
    }

    private fun decode(bytes: ByteArray, px: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= px && bounds.outHeight / (sample * 2) >= px) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
