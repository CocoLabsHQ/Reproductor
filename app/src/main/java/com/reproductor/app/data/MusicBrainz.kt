package com.reproductor.app.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Identified(val artist: String, val title: String, val genre: String?, val verified: Boolean)

sealed interface EnrichUi {
    data object Idle : EnrichUi
    data class Running(val done: Int, val total: Int, val found: Int) : EnrichUi
    data class Done(val total: Int, val found: Int) : EnrichUi
    data class Error(val message: String) : EnrichUi
}

/**
 * Cliente de MusicBrainz (base de datos musical abierta, sin clave). Respeta su límite de 1 consulta
 * por segundo e identifica a la app con un User-Agent. Solo se envían los textos de autor y título.
 */
class MusicBrainzClient {
    private var lastCall = 0L

    private suspend fun get(path: String): JSONObject? = withContext(Dispatchers.IO) {
        repeat(2) {
            val wait = 1100L - (SystemClock.elapsedRealtime() - lastCall)
            if (wait > 0) delay(wait)
            lastCall = SystemClock.elapsedRealtime()
            val conn = URL("https://musicbrainz.org/ws/2/$path").openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/json")
                val code = conn.responseCode
                if (code == 200) return@withContext JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                if (code == 503 || code == 429) {
                    delay(3000) // demasiadas consultas: esperar y reintentar una vez
                } else {
                    return@withContext null
                }
            } finally {
                conn.disconnect()
            }
        }
        null
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private class Match(val artist: String, val title: String, val artistId: String)

    /** Busca una grabación con ese título y ese autor (coincidencia exacta, sin acentos). */
    private suspend fun matchRecording(artist: String, title: String): Match? {
        val q = "recording:\"${esc(title)}\" AND artist:\"${esc(artist)}\""
        val json = get("recording?query=${enc(q)}&fmt=json&limit=5") ?: return null
        val recs = json.optJSONArray("recordings") ?: json.optJSONArray("recording") ?: return null
        val na = normalizeText(artist)
        val nt = normalizeText(title)
        for (i in 0 until recs.length()) {
            val r = recs.getJSONObject(i)
            if (r.optInt("score") < 85) continue
            if (normalizeText(r.optString("title")) != nt) continue
            val credits = r.optJSONArray("artist-credit") ?: continue
            for (j in 0 until credits.length()) {
                val a = credits.getJSONObject(j).optJSONObject("artist") ?: continue
                if (normalizeText(a.optString("name")) == na) {
                    return Match(a.optString("name"), r.optString("title"), a.optString("id"))
                }
            }
        }
        return null
    }

    private suspend fun findArtistId(name: String): String? {
        val q = "artist:\"${esc(name)}\""
        val json = get("artist?query=${enc(q)}&fmt=json&limit=5") ?: return null
        val arr = json.optJSONArray("artists") ?: return null
        val nn = normalizeText(name)
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            if (a.optInt("score") >= 95 && normalizeText(a.optString("name")) == nn) return a.optString("id")
        }
        return null
    }

    private suspend fun artistGenre(id: String): String? {
        val json = get("artist/$id?inc=genres&fmt=json") ?: return null
        val arr = json.optJSONArray("genres") ?: return null
        var best: String? = null
        var bestCount = -1
        for (i in 0 until arr.length()) {
            val g = arr.getJSONObject(i)
            if (g.optInt("count") > bestCount) {
                bestCount = g.optInt("count")
                best = g.optString("name")
            }
        }
        return best?.replaceFirstChar { it.uppercase() }
    }

    /**
     * Intenta confirmar cuál de los dos textos es el autor. Si encuentra la grabación en un orden, queda verificado.
     * Si no, usa el orden local ([firstIsArtistByDefault]) y solo lo guarda si el autor existe y tiene género.
     */
    suspend fun identify(
        first: String,
        second: String,
        firstIsArtistByDefault: Boolean,
        genreCache: MutableMap<String, String?>,
    ): Identified? {
        val m1 = matchRecording(first, second)
        val m2 = if (m1 == null) matchRecording(second, first) else null
        val artist: String
        val title: String
        val id: String?
        val verified: Boolean
        when {
            m1 != null -> { artist = m1.artist; title = m1.title; id = m1.artistId; verified = true }
            m2 != null -> { artist = m2.artist; title = m2.title; id = m2.artistId; verified = true }
            else -> {
                artist = if (firstIsArtistByDefault) first else second
                title = if (firstIsArtistByDefault) second else first
                id = findArtistId(artist)
                verified = false
            }
        }
        val genre = if (id == null) null else {
            if (genreCache.containsKey(id)) genreCache[id] else artistGenre(id).also { genreCache[id] = it }
        }
        if (!verified && genre == null) return null // sin evidencia: se queda la detección local
        return Identified(artist, title, genre, verified)
    }

    private companion object {
        const val USER_AGENT = "MusicBox/1.5 ( https://github.com/CocoLabsHQ/Reproductor )"
    }
}
